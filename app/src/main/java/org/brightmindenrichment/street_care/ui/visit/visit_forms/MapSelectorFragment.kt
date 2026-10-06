package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R
import java.io.IOException
import java.util.Locale

class MapSelectorFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var confirmLocationButton: Button
    private lateinit var geocoder: Geocoder
    private lateinit var centerMarker: ImageView
    private lateinit var searchTextView: TextView
    private var selectedLatLng: LatLng? = null

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.API_KEY_PLACES)
        }
        geocoder = Geocoder(requireContext(), Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        confirmLocationButton = view.findViewById(R.id.confirmLocationButton)
        centerMarker = view.findViewById(R.id.centerMarker)
        searchTextView = view.findViewById(R.id.searchTextView)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        confirmLocationButton.setOnClickListener {
            handleConfirmLocation()
        }

        searchTextView.setOnClickListener {
            launchAutocompleteActivity()
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Move camera to a default location
        val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Update selectedLatLng when camera stops moving
        googleMap.setOnCameraIdleListener {
            selectedLatLng = googleMap.cameraPosition.target
        }
    }

    private fun handleConfirmLocation() {
        selectedLatLng?.let { latLng ->
            val address = getAddressFromLatLng(latLng)
            if (address != null) {
                val resultBundle = Bundle().apply {
                    putString("street", address.thoroughfare ?: "")
                    putString("city", address.locality ?: "")
                    putString("state", address.adminArea ?: "")
                    putString("zipCode", address.postalCode ?: "")
                }
                setFragmentResult("addressRequestKey", resultBundle)
                findNavController().popBackStack()
            } else {
                Toast.makeText(
                    context,
                    "Unable to get address for this location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: Toast.makeText(context, "Please select a location.", Toast.LENGTH_SHORT).show()
    }

    private fun launchAutocompleteActivity() {
        val fields = listOf(
            Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        )
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountries(listOf("US"))
            .build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        requireActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.no_animation)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                    place?.let {
                        val latLng = it.latLng
                        if (latLng != null) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            selectedLatLng = latLng
                        }
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = data?.let { Autocomplete.getStatusFromIntent(it) }
                    Log.e("MapSelectorFragment", "Autocomplete error: ${status?.statusMessage}")
                }

                Activity.RESULT_CANCELED -> {
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getAddressFromLatLng(latLng: LatLng): Address? {
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0]
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }
}
