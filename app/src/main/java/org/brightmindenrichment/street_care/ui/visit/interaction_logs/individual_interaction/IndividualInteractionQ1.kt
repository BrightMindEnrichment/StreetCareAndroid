package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import org.brightmindenrichment.street_care.util.launchPlacesAutocomplete
import org.brightmindenrichment.street_care.util.reverseGeocodeAndFill
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.brightmindenrichment.street_care.util.toLocalDateFromPicker
import org.brightmindenrichment.street_care.util.toPickerMillis
import org.brightmindenrichment.street_care.util.toZonedString
import org.brightmindenrichment.street_care.util.formatTimeWithTz
import org.brightmindenrichment.street_care.util.isInvalidZip
import org.brightmindenrichment.street_care.util.isValidZip

class IndividualInteractionQ1 : BaseIIQuestionFragment() {

    companion object {
        private const val TAG = "IIQ1Nav"
    }

    override val questionNumber = 1

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    // Content view references
    private lateinit var tilFirstName: com.google.android.material.textfield.TextInputLayout
    private lateinit var etFirstName: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilLastName: com.google.android.material.textfield.TextInputLayout
    private lateinit var etLastName: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilLocation: com.google.android.material.textfield.TextInputLayout
    private lateinit var etLocation: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilState: com.google.android.material.textfield.TextInputLayout
    private lateinit var actState: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var tilZip: com.google.android.material.textfield.TextInputLayout
    private lateinit var etZip: com.google.android.material.textfield.TextInputEditText
    private lateinit var datePickerCard: com.google.android.material.card.MaterialCardView
    private lateinit var timePickerCard: com.google.android.material.card.MaterialCardView
    private lateinit var tvDate: android.widget.TextView
    private lateinit var tvTime: android.widget.TextView

    /** Get the timezone from the InteractionLogViewModel (set in ILq1). */
    private fun getInteractionTimezone(): ZoneId {
        val tzString = interactionLogViewModel.interactionLog.value?.timezone
        return if (tzString.isNullOrBlank()) {
            ZoneId.systemDefault()
        } else {
            try {
                ZoneId.of(tzString)
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }
        }
    }


    // ---- Places Autocomplete launcher ----
    private val placesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let {
                    val place = Autocomplete.getPlaceFromIntent(it)
                    val street = place.address?.split(',')?.firstOrNull()?.trim().orEmpty()

                    var city: String? = null
                    var state: String? = null
                    var zipCode: String? = null

                    place.addressComponents?.asList()?.forEach { comp ->
                        when {
                            comp.types.contains("locality") -> city = comp.name
                            comp.types.contains("sublocality") && city == null -> city = comp.name
                            comp.types.contains("postal_town") && city == null -> city = comp.name
                            comp.types.contains("administrative_area_level_2") && city == null -> city = comp.name
                            comp.types.contains("administrative_area_level_1") -> state = comp.name
                            comp.types.contains("postal_code") -> zipCode = comp.name
                        }
                    }

                    val location = listOfNotNull(
                        street.takeUnless { it.isEmpty() },
                        city
                    ).joinToString(", ")

                    etLocation.setText(location)
                    state?.let { s -> actState.setText(s, false) }
                    zipCode?.let { z -> etZip.setText(z) }
                    markFormDirty()
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                val status = Autocomplete.getStatusFromIntent(result.data!!)
                Log.e("IIQ1_Places", "Autocomplete error: ${status.statusMessage}")
            }
            Activity.RESULT_CANCELED -> Log.d("IIQ1_Places", "Autocomplete cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.API_KEY_PLACES)
        }
    }

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_individual_interaction_q1, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        val editingIdx = viewModel.editingIndex
        Log.d(TAG, "onContentViewCreated: editingIndex=$editingIdx, editingHeaderText=${viewModel.editingHeaderText()}, committedCount=${viewModel.committedInteractions.value?.size ?: 0}")

        // Initialize view references from content
        tilFirstName = contentView.findViewById(R.id.tilFirstName)
        etFirstName = contentView.findViewById(R.id.etFirstName)
        tilLastName = contentView.findViewById(R.id.tilLastName)
        etLastName = contentView.findViewById(R.id.etLastName)
        tilLocation = contentView.findViewById(R.id.tilLocation)
        etLocation = contentView.findViewById(R.id.etLocation)
        tilState = contentView.findViewById(R.id.tilState)
        actState = contentView.findViewById(R.id.actState)
        tilZip = contentView.findViewById(R.id.tilZip)
        etZip = contentView.findViewById(R.id.etZip)
        datePickerCard = contentView.findViewById(R.id.datePickerCard)
        timePickerCard = contentView.findViewById(R.id.timePickerCard)
        tvDate = contentView.findViewById(R.id.tvDate)
        tvTime = contentView.findViewById(R.id.tvTime)

        // Observe timezone changes and refresh time display
        interactionLogViewModel.interactionLog.observe(viewLifecycleOwner) { _ ->
            if (selectedTime != null && selectedDate != null) {
                tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
            }
        }

        // Restore previously entered values when navigating back
        viewModel.currentInteraction.value?.let { saved ->
            if (saved.firstName.isNotBlank()) etFirstName.setText(saved.firstName)
            saved.lastName?.let { etLastName.setText(it) }
            saved.locationLandmark?.let { etLocation.setText(it) }
            saved.state?.let { actState.setText(it, false) }
            saved.zip?.let { etZip.setText(it) }
            saved.date?.let {
                selectedDate = LocalDate.parse(it)
                tvDate.text = dateFormatter.format(selectedDate)
            }
            saved.time?.let {
                try {
                    // Try to parse as ZonedDateTime first (full timestamp with timezone)
                    val zdt = ZonedDateTime.parse(it)
                    selectedTime = zdt.toLocalTime()
                    if (selectedDate != null) {
                        tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                    }
                } catch (e: Exception) {
                    // Fall back to LocalTime parsing if it's just a time string
                    try {
                        selectedTime = LocalTime.parse(it)
                        if (selectedDate != null) {
                            tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                        }
                    } catch (e2: Exception) {
                        // Silently ignore if parsing fails
                    }
                }
            }
        }

        try {
            val states = resources.getStringArray(R.array.us_states)
            actState.setSimpleItems(states)
        } catch (_: Exception) {
            // ignore if array not present
        }

        // ZIP focus-loss and dynamic validation (TIL automatically shows outline red)
        etZip.setOnFocusChangeListener { _, hasFocus ->
            val text = etZip.text.toString()
            if (!hasFocus && text.isInvalidZip())
                tilZip.error = "Enter a valid 5-digit ZIP (e.g. 90210)"
            else if (hasFocus)
                tilZip.error = null
        }

        etZip.doAfterTextChanged { s ->
            if (s.toString().isValidZip()) tilZip.error = null
            markFormDirty()
        }

        // Mark form dirty on any text field change, and clear errors when user types
        etFirstName.doAfterTextChanged {
            if (it.toString().isNotBlank()) {
                tilFirstName.helperText = null
                tilFirstName.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.gray700))
            }
            markFormDirty()
        }
        etLastName.doAfterTextChanged { markFormDirty() }
        etLocation.doAfterTextChanged {
            if (it.toString().isNotBlank()) {
                tilLocation.helperText = null
                tilLocation.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.gray700))
            }
            markFormDirty()
        }
        actState.doAfterTextChanged { markFormDirty() }

        // GPS prefill if location is blank
        if (etLocation.text.isNullOrBlank()) {
            tryPrefillFromLocation()
        }

        // Launch Places autocomplete on tap when field is blank
        etLocation.setOnClickListener {
            if (etLocation.text.isNullOrBlank()) {
                launchPlacesAutocomplete(placesLauncher, requireContext())
            }
        }

        // Launch Places autocomplete on editor action (e.g. search key)
        etLocation.setOnEditorActionListener { _, _, _ ->
            val query = etLocation.text.toString().trim()
            launchPlacesAutocomplete(placesLauncher, requireContext(), query)
            true
        }

        // Date picker
        datePickerCard.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val keyboardWasVisible = imm.isActive

            val pickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_StreetCare_DatePicker)
                .setTitleText(getString(R.string.select_interaction_date))

            // Set date constraints: disable dates beyond 12 hours in the future
            val nowInTz = ZonedDateTime.now(getInteractionTimezone())
            val maxFutureMillis = nowInTz.plusHours(12).toInstant().toEpochMilli()
            val validator = com.google.android.material.datepicker.DateValidatorPointBackward.before(maxFutureMillis)
            val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(validator)
                .build()
            pickerBuilder.setCalendarConstraints(constraints)

            // Only set selection if a date was previously selected
            if (selectedDate != null) {
                pickerBuilder.setSelection(selectedDate!!.toPickerMillis())
            }

            val picker = pickerBuilder.build()

            picker.addOnPositiveButtonClickListener { millis ->
                val pickedDate = millis.toLocalDateFromPicker()

                selectedDate = pickedDate
                tvDate.error = null
                tvDate.text = dateFormatter.format(pickedDate)

                // If time is already selected, update its display with new timezone abbrev for this date
                if (selectedTime != null) {
                    tvTime.text = formatTimeWithTz(pickedDate, selectedTime!!, getInteractionTimezone())
                }

                markFormDirty()

                // Restore keyboard state
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.addOnDismissListener {
                // Restore keyboard state on dismiss (cancel or outside tap)
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.show(parentFragmentManager, "date_picker_q1")
        }

        // Time picker
        timePickerCard.setOnClickListener {

            // Check if date is selected first
            if (selectedDate == null) {
                Toast.makeText(
                    requireContext(),
                    "Please select a date first",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val keyboardWasVisible = imm.isActive

            val pickerBuilder = MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_StreetCare_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)

            // Default to selected timezone's current time if no time selected yet
            val defaultTime = selectedTime ?: LocalTime.now(getInteractionTimezone())
            pickerBuilder.setHour(defaultTime.hour)
            pickerBuilder.setMinute(defaultTime.minute)

            val picker = pickerBuilder
                .setTitleText(getString(R.string.select_interaction_time))
                .build()

            picker.addOnPositiveButtonClickListener {

                val pickedTime = LocalTime.of(picker.hour, picker.minute)

                // Validate: restrict to half day (12 hours) into the future
                if (selectedDate != null) {
                    val pickedDateTime = pickedTime.atDate(selectedDate!!).atZone(getInteractionTimezone())
                    val nowInTz = ZonedDateTime.now(getInteractionTimezone())
                    val maxFutureDateTime = nowInTz.plusHours(12)

                    if (pickedDateTime.isAfter(maxFutureDateTime)) {

                        val latestAllowedTime = maxFutureDateTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                        Toast.makeText(
                            requireContext(),
                            "Select a valid time before $latestAllowedTime",
                            Toast.LENGTH_LONG
                        ).show()
                        tvTime.error = "Cannot select more than 12 hours in the future"
                        return@addOnPositiveButtonClickListener
                    }
                }

                selectedTime = pickedTime
                tvTime.error = null
                if (selectedDate != null) {
                    tvTime.text = formatTimeWithTz(selectedDate!!, pickedTime, getInteractionTimezone())
                }
                markFormDirty()
                // Restore keyboard state
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.addOnDismissListener {
                // Restore keyboard state on dismiss (cancel or outside tap)
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.show(parentFragmentManager, "time_picker_q1")
        }
    }

    override fun onPreviousClicked() {
        val first = etFirstName.text?.toString()?.trim().orEmpty()
        val last  = etLastName.text?.toString()?.trim().orEmpty()
        val loc   = etLocation.text?.toString()?.trim().orEmpty()
        val state = actState.text?.toString()?.trim().orEmpty()
        val zip   = etZip.text?.toString()?.trim().orEmpty()
        val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
        viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)

        // Update header immediately if name is not empty
        if (first.isNotBlank()) {
            val lastInitial = last.takeIf { it.isNotBlank() }?.firstOrNull()?.let { " ${it}." }.orEmpty()
            binding.tvHeader.text = "Interaction with ${first}$lastInitial"
        }

        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().popBackStack()
        }
    }

    override fun onNextClicked() {
        tvDate.error = null
        tvTime.error = null
        tilFirstName.helperText = null
        tilLocation.helperText = null

        val first = etFirstName.text?.toString()?.trim().orEmpty()
        val last  = etLastName.text?.toString()?.trim().orEmpty()
        val loc   = etLocation.text?.toString()?.trim().orEmpty()
        val state = actState.text?.toString()?.trim().orEmpty()
        val zip   = etZip.text?.toString()?.trim().orEmpty()

        // If Q3 has selections, require firstName and location (applies to both new and editing)
        val hasQ3Selections = viewModel.currentInteraction.value?.furtherHelpNeeded?.isNotEmpty() == true

        if (hasQ3Selections) {
            var isValid = true
            val redColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

            if (first.isEmpty()) {
                tilFirstName.apply {
                    helperText = "Required - volunteers need to know who to reach out to"
                    setHelperTextColor(android.content.res.ColorStateList.valueOf(redColor))
                    setBoxStrokeColor(redColor)
                }
                isValid = false
            }
            if (loc.isEmpty()) {
                tilLocation.apply {
                    helperText = "Required - volunteers need to know where to find this person"
                    setHelperTextColor(android.content.res.ColorStateList.valueOf(redColor))
                    setBoxStrokeColor(redColor)
                }
                isValid = false
            }
            if (!isValid) return
        }

        val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
        viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)

        // Update header immediately if name is not empty
        if (first.isNotBlank()) {
            val lastInitial = last.takeIf { it.isNotBlank() }?.firstOrNull()?.let { " ${it}." }.orEmpty()
            binding.tvHeader.text = "Interaction with ${first}$lastInitial"
        }

        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().navigate(
                R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }
    }

    override fun onSkipClicked() {
        val first = etFirstName.text?.toString()?.trim().orEmpty()
        val last  = etLastName.text?.toString()?.trim().orEmpty()
        val loc   = etLocation.text?.toString()?.trim().orEmpty()
        val state = actState.text?.toString()?.trim().orEmpty()
        val zip   = etZip.text?.toString()?.trim().orEmpty()
        val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
        viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)

        // Update header immediately if name is not empty
        if (first.isNotBlank()) {
            val lastInitial = last.takeIf { it.isNotBlank() }?.firstOrNull()?.let { " ${it}." }.orEmpty()
            binding.tvHeader.text = "Interaction with ${first}$lastInitial"
        }

        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().navigate(
                R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }
    }

    // ---- GPS prefill ----

    private fun mergeIntoILAndSave(editingIdx: Int?, onComplete: () -> Unit) {
        if (editingIdx != null) {
            // Editing: use current interaction from ViewModel
            val current = viewModel.currentInteraction.value ?: return onComplete()
            interactionLogViewModel.replaceIndividualInteraction(editingIdx, current)
        } else {
            // New interaction: nothing to merge yet, just save the draft
            // The actual merge happens in Q4 when the II is completed
        }
        interactionLogViewModel.saveDraft {
            onComplete()
        }
    }

    private fun tryPrefillFromLocation() {
        val ctx = requireContext()
        val hasPermission = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        LocationServices.getFusedLocationProviderClient(ctx)
            .lastLocation
            .addOnSuccessListener { location ->
                location ?: return@addOnSuccessListener
                reverseGeocodeAndFill(
                    location.latitude, location.longitude,
                    requireContext(),
                    viewLifecycleOwner.lifecycleScope,
                    { this.isAdded }
                ) { street, city, state, zip ->
                    val location = listOfNotNull(
                        street.takeUnless { it.isEmpty() },
                        city.takeUnless { it.isEmpty() }
                    ).joinToString(", ")
                    etLocation.setText(location)
                    actState.setText(state, false)
                    etZip.setText(zip)
                }
            }
    }

}
