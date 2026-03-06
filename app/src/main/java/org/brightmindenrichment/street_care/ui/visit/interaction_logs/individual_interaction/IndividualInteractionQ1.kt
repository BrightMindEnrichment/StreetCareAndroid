package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ1Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import org.brightmindenrichment.street_care.util.isInvalidZip
import org.brightmindenrichment.street_care.util.launchPlacesAutocomplete
import org.brightmindenrichment.street_care.util.reverseGeocodeAndFill
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.brightmindenrichment.street_care.util.localDateNow
import org.brightmindenrichment.street_care.util.localTimeNow
import org.brightmindenrichment.street_care.util.toLocalDateFromPicker
import org.brightmindenrichment.street_care.util.toPickerMillis

class IndividualInteractionQ1 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ1Binding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

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
                selectedTime = LocalTime.parse(it)
                binding.tvTime.text = timeFormatter.format(selectedTime)
            }
        }

        try {
            val states = resources.getStringArray(R.array.us_states)
            binding.actState.setSimpleItems(states)
        } catch (_: Exception) {
            // ignore if array not present
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
            val baseDate = selectedDate ?: localDateNow()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_StreetCare_DatePicker)
                .setTitleText(getString(R.string.select_interaction_date))
                .setSelection(baseDate.toPickerMillis())
                .build()

            picker.addOnPositiveButtonClickListener { millis ->
                val pickedDate = millis.toLocalDateFromPicker()
                selectedDate = pickedDate
                binding.tvDate.error = null
                binding.tvDate.text = dateFormatter.format(pickedDate)
            }

            picker.show(parentFragmentManager, "date_picker_q1")
        }

        // Time picker
        binding.timePickerCard.setOnClickListener {
            val baseTime = selectedTime ?: localTimeNow()

            val picker = MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_StreetCare_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(baseTime.hour)
                .setMinute(baseTime.minute)
                .setTitleText(getString(R.string.select_interaction_time))
                .build()

            picker.addOnPositiveButtonClickListener {
                val pickedTime = LocalTime.of(picker.hour, picker.minute)
                selectedTime = pickedTime
                binding.tvTime.error = null
                binding.tvTime.text = timeFormatter.format(pickedTime)
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
            viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime)
            findNavController().navigateUp()
        }

        // Skip -> go to Q2 (no validation)
        binding.txtSkip.setOnClickListener {
            findNavController().navigate(
                R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }

        // Next -> validate -> go to Q2
        binding.txtNext2.setOnClickListener {
            binding.tvDate.error = null
            binding.tvTime.error = null

            val first = binding.etFirstName.text?.toString()?.trim().orEmpty()
            val last  = binding.etLastName.text?.toString()?.trim().orEmpty()
            val loc   = binding.etLocation.text?.toString()?.trim().orEmpty()
            val state = binding.actState.text?.toString()?.trim().orEmpty()
            val zip   = binding.etZip.text?.toString()?.trim().orEmpty()

            if (first.isEmpty()) { binding.etFirstName.error = "Required"; return@setOnClickListener }
            if (last.isEmpty())  { binding.etLastName.error  = "Required"; return@setOnClickListener }
            if (loc.isEmpty())   { binding.etLocation.error  = "Required"; return@setOnClickListener }
            if (state.isEmpty()) { binding.actState.error    = "Required"; return@setOnClickListener }
            if (zip.isInvalidZip())  { binding.etZip.error = "Invalid";  return@setOnClickListener }

            viewModel.saveQ1(first, last, loc, state, zip, selectedDate, selectedTime)

            findNavController().navigate(
                R.id.action_individualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }
    }

    // ---- GPS prefill ----

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
