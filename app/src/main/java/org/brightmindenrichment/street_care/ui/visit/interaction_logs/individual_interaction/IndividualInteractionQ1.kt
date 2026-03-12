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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ1Binding
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

class IndividualInteractionQ1 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ1Binding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

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
                val b = _binding ?: return@registerForActivityResult
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

                    b.etLocation.setText(location)
                    state?.let { s -> b.actState.setText(s, false) }
                    zipCode?.let { z -> b.etZip.setText(z) }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualInteractionQ1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editHeader = viewModel.editingHeaderText()
        if (editHeader != null) {
            binding.tvHeader.text = editHeader
        } else {
            interactionLogViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
                binding.tvHeader.text = if (idx <= 1) {
                    getString(R.string.individual_interaction_title_base)
                } else {
                    getString(R.string.individual_interaction_title_numbered, idx)
                }
            }
        }

        // Observe timezone changes and refresh time display
        interactionLogViewModel.interactionLog.observe(viewLifecycleOwner) { _ ->
            if (selectedTime != null && selectedDate != null) {
                binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
            }
        }

        // Restore previously entered values when navigating back
        viewModel.currentInteraction.value?.let { saved ->
            if (saved.firstName.isNotBlank()) binding.etFirstName.setText(saved.firstName)
            saved.lastName?.let { binding.etLastName.setText(it) }
            saved.locationLandmark?.let { binding.etLocation.setText(it) }
            saved.state?.let { binding.actState.setText(it, false) }
            saved.zip?.let { binding.etZip.setText(it) }
            saved.date?.let {
                selectedDate = LocalDate.parse(it)
                binding.tvDate.text = dateFormatter.format(selectedDate)
            }
            saved.time?.let {
                try {
                    // Try to parse as ZonedDateTime first (full timestamp with timezone)
                    val zdt = ZonedDateTime.parse(it)
                    selectedTime = zdt.toLocalTime()
                    if (selectedDate != null) {
                        binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                    }
                } catch (e: Exception) {
                    // Fall back to LocalTime parsing if it's just a time string
                    try {
                        selectedTime = LocalTime.parse(it)
                        if (selectedDate != null) {
                            binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                        }
                    } catch (e2: Exception) {
                        // Silently ignore if parsing fails
                    }
                }
            }
        }

        try {
            val states = resources.getStringArray(R.array.us_states)
            binding.actState.setSimpleItems(states)
        } catch (_: Exception) {
            // ignore if array not present
        }

        // ZIP focus-loss and dynamic validation (TIL automatically shows outline red)
        binding.etZip.setOnFocusChangeListener { _, hasFocus ->
            val text = binding.etZip.text.toString()
            if (!hasFocus && text.isInvalidZip())
                binding.tilZip.error = "Enter a valid 5-digit ZIP (e.g. 90210)"
            else if (hasFocus)
                binding.tilZip.error = null
        }

        binding.etZip.doAfterTextChanged { s ->
            if (s.toString().isValidZip()) binding.tilZip.error = null
        }

        // GPS prefill if location is blank
        if (binding.etLocation.text.isNullOrBlank()) {
            tryPrefillFromLocation()
        }

        // Launch Places autocomplete on tap when field is blank
        binding.etLocation.setOnClickListener {
            if (binding.etLocation.text.isNullOrBlank()) {
                launchPlacesAutocomplete(placesLauncher, requireContext())
            }
        }

        // Launch Places autocomplete on editor action (e.g. search key)
        binding.etLocation.setOnEditorActionListener { _, _, _ ->
            val query = binding.etLocation.text.toString().trim()
            launchPlacesAutocomplete(placesLauncher, requireContext(), query)
            true
        }

        // Date picker
        binding.datePickerCard.setOnClickListener {
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
                binding.tvDate.error = null
                binding.tvDate.text = dateFormatter.format(pickedDate)

                // If time is already selected, update its display with new timezone abbrev for this date
                if (selectedTime != null) {
                    binding.tvTime.text = formatTimeWithTz(pickedDate, selectedTime!!, getInteractionTimezone())
                }

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
        binding.timePickerCard.setOnClickListener {
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
                        binding.tvTime.error = "Cannot select more than 12 hours in the future"
                        return@addOnPositiveButtonClickListener
                    }
                }

                selectedTime = pickedTime
                binding.tvTime.error = null
                if (selectedDate != null) {
                    binding.tvTime.text = formatTimeWithTz(selectedDate!!, pickedTime, getInteractionTimezone())
                }
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

        // Previous -> back stack
        binding.txtPrevious2.setOnClickListener {
            val first = binding.etFirstName.text?.toString()?.trim().orEmpty()
            val last  = binding.etLastName.text?.toString()?.trim().orEmpty()
            val loc   = binding.etLocation.text?.toString()?.trim().orEmpty()
            val state = binding.actState.text?.toString()?.trim().orEmpty()
            val zip   = binding.etZip.text?.toString()?.trim().orEmpty()
            val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
            viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().popBackStack()
            }
        }

        // Skip -> go to Q2 (no validation)
        binding.txtSkip.setOnClickListener {
            val first = binding.etFirstName.text?.toString()?.trim().orEmpty()
            val last  = binding.etLastName.text?.toString()?.trim().orEmpty()
            val loc   = binding.etLocation.text?.toString()?.trim().orEmpty()
            val state = binding.actState.text?.toString()?.trim().orEmpty()
            val zip   = binding.etZip.text?.toString()?.trim().orEmpty()
            val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
            viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().navigate(
                    R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
                )
            }
        }

        // Next -> go to Q2 (validate only if editing with Q3 selections)
        binding.txtNext2.setOnClickListener {
            binding.tvDate.error = null
            binding.tvTime.error = null
            binding.tilFirstName.error = null
            binding.tilLocation.error = null

            val first = binding.etFirstName.text?.toString()?.trim().orEmpty()
            val last  = binding.etLastName.text?.toString()?.trim().orEmpty()
            val loc   = binding.etLocation.text?.toString()?.trim().orEmpty()
            val state = binding.actState.text?.toString()?.trim().orEmpty()
            val zip   = binding.etZip.text?.toString()?.trim().orEmpty()

            // If editing and Q3 has selections, require firstName and location
            val isEditing = viewModel.editingIndex != null
            val hasQ3Selections = viewModel.currentInteraction.value?.furtherHelpNeeded?.isNotEmpty() == true

            if (isEditing && hasQ3Selections) {
                var isValid = true
                if (first.isEmpty()) {
                    binding.tilFirstName.error = "Required"
                    isValid = false
                }
                if (loc.isEmpty()) {
                    binding.tilLocation.error = "Required"
                    isValid = false
                }
                if (!isValid) return@setOnClickListener
            }

            val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
            viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime, timeWithTz)
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().navigate(
                    R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
                )
            }
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
                    { _binding != null }
                ) { street, city, state, zip ->
                    val location = listOfNotNull(
                        street.takeUnless { it.isEmpty() },
                        city.takeUnless { it.isEmpty() }
                    ).joinToString(", ")
                    binding.etLocation.setText(location)
                    binding.actState.setText(state, false)
                    binding.etZip.setText(zip)
                }
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
