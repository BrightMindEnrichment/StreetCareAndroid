package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ3Binding
import java.util.Locale

class InteractionQ3Fragment : Fragment() {

    private var _binding: FragmentLogInteractionQ3Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()

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

                    binding.inputAddress.setText(street)
                    binding.inputCity.setText(city.orEmpty())
                    binding.inputState.setText(state.orEmpty())
                    binding.inputZip.setText(zipCode.orEmpty())
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
                    binding.inputAddress.setText(spokenText)
                    launchPlacesAutocomplete(spokenText)
                }
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
        _binding = FragmentLogInteractionQ3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val log = viewModel.interactionLog.value
        binding.inputAddress.setText(log?.addr1.orEmpty())
        binding.inputCity.setText(log?.city.orEmpty())
        binding.inputState.setText(log?.state.orEmpty())
        binding.inputZip.setText(log?.zipcode.orEmpty())

        if (log?.addr1.isNullOrEmpty()) {
            tryPrefillFromLocation()
        }

        binding.inputAddress.setOnClickListener {
            if (binding.inputAddress.text.isNullOrBlank()) launchPlacesAutocomplete()
        }

        binding.inputAddress.setOnEditorActionListener { _, _, _ ->
            val query = binding.inputAddress.text.toString().trim()
            launchPlacesAutocomplete(query)
            true
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.iconMic.setOnClickListener { startVoiceInput() }

        binding.btnNext.setOnClickListener {
            val address = binding.inputAddress.text.toString().trim()
            val city = binding.inputCity.text.toString().trim()
            val state = binding.inputState.text.toString().trim()
            val zip = binding.inputZip.text.toString().trim()

            viewModel.updateAddress(address)
            viewModel.updateCity(city)
            viewModel.updateState(state)
            viewModel.updateZipcode(zip)

            Log.d("Q3_DEBUG", "After Q3 Save: ${viewModel.interactionLog.value}")
            findNavController().navigate(R.id.action_q3_to_q4)
        }

        binding.btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.skipBtn.setOnClickListener {
            findNavController().navigate(R.id.action_q3_to_q4)
        }
    }

    private fun launchPlacesAutocomplete(initialQuery: String = "") {
        try {
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
            val builder = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            if (initialQuery.isNotBlank()) builder.setInitialQuery(initialQuery)
            placesLauncher.launch(builder.build(requireContext()))
        } catch (e: Exception) {
            Log.e("Q3_Places", "Failed to launch autocomplete: ${e.message}")
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        voiceLauncher.launch(intent)
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
                reverseGeocodeAndFill(location.latitude, location.longitude)
            }
    }

    private fun reverseGeocodeAndFill(lat: Double, lon: Double) {
        if (!Geocoder.isPresent()) return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val results = Geocoder(requireContext(), Locale.getDefault())
                    .getFromLocation(lat, lon, 1)
                val address = results?.firstOrNull() ?: return@launch

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    val street = listOfNotNull(
                        address.subThoroughfare,
                        address.thoroughfare
                    ).joinToString(" ")
                    binding.inputAddress.setText(street)
                    fillFields(address)
                }
            } catch (e: Exception) {
                Log.e("Q3_Location", "Reverse geocode failed: ${e.message}")
            }
        }
    }

    private fun fillFields(address: Address) {
        binding.inputCity.setText(address.locality ?: address.subLocality ?: "")
        binding.inputState.setText(address.adminArea ?: "")
        binding.inputZip.setText(address.postalCode ?: "")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
