package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.util.launchPlacesAutocomplete
import org.brightmindenrichment.street_care.util.reverseGeocodeAndFill
import org.brightmindenrichment.street_care.util.isInvalidZip
import org.brightmindenrichment.street_care.util.isValidZip

class InteractionQ3Fragment : BaseILQuestionFragment() {

    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var inputAddress: EditText
    private lateinit var inputCity: EditText
    private lateinit var inputState: EditText
    private lateinit var inputZip: EditText
    private lateinit var inputDescription: EditText
    private lateinit var iconMic: ImageView

    override val stepNumber = 3

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_il_q3, container, false)
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

                    inputAddress.setText(street)
                    inputCity.setText(city.orEmpty())
                    inputState.setText(state.orEmpty())
                    inputZip.setText(zipCode.orEmpty())
                    markFormDirty()
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                val status = Autocomplete.getStatusFromIntent(result.data!!)
                Log.e("Q3_Places", "Autocomplete error: ${status.statusMessage}")
            }
            Activity.RESULT_CANCELED -> Log.d("Q3_Places", "Autocomplete cancelled")
        }
    }

    // ---- Voice input launcher ----
    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    inputAddress.setText(spokenText)
                    markFormDirty()
                    launchPlacesAutocomplete(placesLauncher, requireContext(), spokenText)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.API_KEY_PLACES)
        }
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Get view references
        inputAddress = contentView.findViewById(R.id.input_address)
        inputCity = contentView.findViewById(R.id.input_city)
        inputState = contentView.findViewById(R.id.input_state)
        inputZip = contentView.findViewById(R.id.input_zip)
        inputDescription = contentView.findViewById(R.id.input_description)
        iconMic = contentView.findViewById(R.id.icon_mic)

        val log = viewModel.interactionLog.value
        inputAddress.setText(log?.addr1.orEmpty())
        inputCity.setText(log?.city.orEmpty())
        inputState.setText(log?.state.orEmpty())
        inputZip.setText(log?.zipcode.orEmpty())

        if (log?.addr1.isNullOrEmpty()) {
            tryPrefillFromLocation()
        }

        inputAddress.setOnClickListener {
            if (inputAddress.text.isNullOrBlank()) launchPlacesAutocomplete(placesLauncher, requireContext())
        }

        inputAddress.setOnEditorActionListener { _, _, _ ->
            val query = inputAddress.text.toString().trim()
            launchPlacesAutocomplete(placesLauncher, requireContext(), query)
            true
        }

        // ZIP focus-loss and dynamic validation
        inputZip.setOnFocusChangeListener { _, hasFocus ->
            val text = inputZip.text.toString()
            if (!hasFocus && text.isInvalidZip())
                inputZip.showFormatError("Enter a valid 5-digit ZIP (e.g. 90210)")
            else if (hasFocus)
                inputZip.clearFormatError()
        }

        inputZip.doAfterTextChanged { s ->
            if (s.toString().isValidZip()) inputZip.clearFormatError()
            markFormDirty()
        }

        // Mark form dirty on any address field change
        inputAddress.doAfterTextChanged { markFormDirty() }
        inputCity.doAfterTextChanged { markFormDirty() }
        inputState.doAfterTextChanged { markFormDirty() }
        inputDescription.doAfterTextChanged { markFormDirty() }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        iconMic.setOnClickListener { startVoiceInput() }
    }

    override fun onNextNavigate() {
        val address = inputAddress.text.toString().trim()
        val city = inputCity.text.toString().trim()
        val state = inputState.text.toString().trim()
        val zip = inputZip.text.toString().trim()

        // Next-as-Skip: if all location fields are empty, delegate to skip logic
        if (address.isEmpty() && city.isEmpty() && state.isEmpty() && zip.isEmpty()) {
            onSkipNavigate()
            return
        }

        // ZIP format validation (only if non-empty)
        if (zip.isInvalidZip()) {
            inputZip.showFormatError("Enter a valid 5-digit ZIP (e.g. 90210)")
            inputZip.requestFocus()
            return
        }

        viewModel.updateAddress(address)
        viewModel.updateCity(city)
        viewModel.updateState(state)
        viewModel.updateZipcode(zip)

        Log.d("Q3_DEBUG", "After Q3 Save: ${viewModel.interactionLog.value}")
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_q3_to_q4)
        }
    }

    override fun onSkipNavigate() {
        wasSkipped = true
        saveCurrentState()
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_q3_to_q4)
        }
    }


    override fun saveCurrentState() {
        isTouched = true
        val address = inputAddress.text.toString().trim()
        val city = inputCity.text.toString().trim()
        val state = inputState.text.toString().trim()
        val zip = inputZip.text.toString().trim()
        viewModel.updateAddress(address)
        viewModel.updateCity(city)
        viewModel.updateState(state)
        viewModel.updateZipcode(zip)
    }

    override fun getStepState(): StepState {
        return when {
            wasSkipped -> StepState.SKIPPED
            isCurrentStepValid() -> StepState.VALID
            isTouched -> StepState.TOUCHED
            else -> StepState.EMPTY
        }
    }

    private fun isCurrentStepValid(): Boolean {
        return inputAddress.text.isNotEmpty()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        voiceLauncher.launch(intent)
    }

    private fun EditText.showFormatError(msg: String) {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_rounded_error)
        error = msg
    }

    private fun EditText.clearFormatError() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_rounded)
        error = null
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
                    { ::inputAddress.isInitialized }
                ) { street, city, state, zip ->
                    inputAddress.setText(street)
                    inputCity.setText(city)
                    inputState.setText(state)
                    inputZip.setText(zip)
                }
            }
    }

}
