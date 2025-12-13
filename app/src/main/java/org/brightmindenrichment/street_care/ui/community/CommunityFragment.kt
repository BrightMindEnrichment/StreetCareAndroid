package org.brightmindenrichment.street_care.ui.community

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentCommunityBinding
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.util.Queries.getHelpRequestDefaultQueryUpTo50
import org.brightmindenrichment.street_care.util.Queries.getUpcomingEventsQueryUpTo50
import org.brightmindenrichment.street_care.util.Queries.getLoadVisitLogBookNewQueryUpTo50
import org.brightmindenrichment.street_care.util.Queries.getPublicInteractionLogQueryUpTo50

class CommunityFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    // Marker data
    private data class MarkerData(
        val position: LatLng,
        val title: String,
        val description: String,
        val markerColor: Float
    )

    private var cachedEvents: List<MarkerData>? = null
    private var cachedHelpRequests: List<MarkerData>? = null
    private var cachedPublicInteractionLog: List<MarkerData>? = null
    private var cachedVisitLogBookNew: List<MarkerData>? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private var hasShownLocationServiceToast = false
        private var hasPromptedLocationSettings = false
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val granted = results.values.any { it == true }

            if (granted) {
                if (isLocationEnabled()) {
                    getLocation()
                } else {
                    showLocationServiceToast(R.string.turn_on_location)
                    promptLocationSettingsOnce()
                }
            } else {
                moveToDefaultLocation()
            }
        }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupClickListeners()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (hasLocationPermission()) {
            @SuppressLint("MissingPermission")
            map.isMyLocationEnabled = true
            getLocation()
        } else {
            requestLocationPermissions()
        }

        // Load all datasets
        loadEvents()
        loadHelpRequests()
        loadPublicInteractionLog()
        loadVisitLogBookNew()
    }

    override fun onResume() {
        super.onResume()

        if (::map.isInitialized) {
            if (hasLocationPermission() && isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    map.isMyLocationEnabled = true
                    binding.root.postDelayed({
                        if (isAdded) {
                            getLocation()
                        }
                    }, 500)
                }
            } else if (!hasLocationPermission()) {
                showLocationServiceToast(R.string.location_permission_denied)
                moveToDefaultLocation()
            } else if (!isLocationEnabled()) {
                showLocationServiceToast(R.string.turn_on_location)
                moveToDefaultLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }

    // ------------------------------------------------------------------------
    // UI navigation
    // ------------------------------------------------------------------------

    private fun setupClickListeners() {
        binding.pastEventComponent.setOnClickListener {
            findNavController().navigate(
                R.id.communityEventFragment,
                Bundle().apply {
                    putString("pageTitle", it.context.getString(R.string.past_events))
                    putSerializable("communityPageName", CommunityPageName.PAST_EVENTS)
                }
            )
        }

        binding.upcomingEventComponent.setOnClickListener {
            findNavController().navigate(
                R.id.communityEventFragment,
                Bundle().apply {
                    putString("pageTitle", it.context.getString(R.string.future_events))
                    putSerializable("communityPageName", CommunityPageName.UPCOMING_EVENTS)
                }
            )
        }

        // Direct navigation to publicEvent
        binding.helpRequestsComponent.setOnClickListener {
            findNavController().navigate(R.id.publicEvent)
        }

        binding.requestComponent.setOnClickListener {
            findNavController().navigate(
                R.id.communityHelpFragment,
                Bundle().apply {
                    putString(NavigationUtil.FRAGMENT_KEY, NavigationUtil.FRAGMENT_REQUEST)
                }
            )
        }

        binding.helpComponent.setOnClickListener {
            findNavController().navigate(
                R.id.communityHelpFragment,
                Bundle().apply {
                    putString(NavigationUtil.FRAGMENT_KEY, NavigationUtil.FRAGMENT_HELP)
                }
            )
        }
    }

    // ------------------------------------------------------------------------
    // Map data loading
    // ------------------------------------------------------------------------

    private fun loadEvents() = loadMapData(
        isEvent = true,
        cached = cachedEvents,
        updateCache = { cachedEvents = it },
        query = { getUpcomingEventsQueryUpTo50().get() },
        getMarkerColor = { BitmapDescriptorFactory.HUE_YELLOW }
    )

    private fun loadHelpRequests() = loadMapData(
        isEvent = false,
        cached = cachedHelpRequests,
        updateCache = { cachedHelpRequests = it },
        query = { getHelpRequestDefaultQueryUpTo50().get() },
        getMarkerColor = { document ->
            if (document.getBoolean("isHelpNeeded") == true)
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_RED
        }
    )

    private fun loadPublicInteractionLog() = loadMapData(
        isEvent = false,
        cached = cachedPublicInteractionLog,
        updateCache = { cachedPublicInteractionLog = it },
        query = {
            // TODO: confirm collection name and sort field
            getPublicInteractionLogQueryUpTo50(Query.Direction.DESCENDING).get()
        },
        getMarkerColor = { document ->
            val whatGiven = document.get("whatGiven") as? List<*> ?: emptyList<Any>()
            if ("Food and Drink" in whatGiven) BitmapDescriptorFactory.HUE_RED
            else BitmapDescriptorFactory.HUE_CYAN
        }
    )

    private fun loadVisitLogBookNew() = loadMapData(
        isEvent = false,
        cached = cachedVisitLogBookNew,
        updateCache = { cachedVisitLogBookNew = it },
        query = {
            // TODO: confirm collection name and sort field
            getLoadVisitLogBookNewQueryUpTo50(Query.Direction.DESCENDING).get()
        },
        getMarkerColor = { document ->
            val whatGiven = document.get("whatGiven") as? List<*> ?: emptyList<Any>()
            if ("Food and Drink" in whatGiven) BitmapDescriptorFactory.HUE_RED
            else BitmapDescriptorFactory.HUE_CYAN
        }
    )

    private fun loadMapData(
        isEvent: Boolean,
        cached: List<MarkerData>?,
        updateCache: (List<MarkerData>) -> Unit,
        query: () -> Task<QuerySnapshot>,
        getMarkerColor: (DocumentSnapshot) -> Float
    ) {
        if (!binding.mapLoadingContainer.isVisible) {
            binding.mapLoadingContainer.visibility = View.VISIBLE
        }

        if (cached != null) {
            cached.forEach(::addMarkerToMap)
            binding.mapLoadingContainer.visibility = View.GONE
            return
        }

        query().addOnSuccessListener { snapshot ->
            coroutineScope.launch(Dispatchers.IO) {
                val markerList = mutableListOf<MarkerData>()
                val geocoder = Geocoder(requireContext())

                val deferredResults = snapshot.documents.map { document ->
                    async {
                        val locationMap = document.get("location") as? Map<*, *>

                        val address = buildString {
                            append(locationMap?.get("street") ?: document.get("street") ?: "")
                            append(", ")
                            append(locationMap?.get("city") ?: document.get("city") ?: "")
                            append(", ")
                            append(locationMap?.get("state") ?: document.get("state") ?: "")
                            append(" ")
                            append(locationMap?.get("zipcode") ?: document.get("zipcode") ?: "")
                        }.trim()

                        if (address.isBlank() || address == ", ,  ") return@async null

                        val descriptionText =
                            document.getString("description")
                                ?: document.getString("peopleHelpedDescription")
                                ?: ""

                        val whatGiven = document.get("whatGiven") as? List<*>
                            ?: emptyList<Any>()

                        val fullDescription = buildString {
                            if (descriptionText.isNotBlank()) append(descriptionText)
                            if (whatGiven.isNotEmpty()) {
                                if (isNotEmpty()) append("\n")
                                append("Items Given: ${whatGiven.joinToString(", ")}")
                            }
                        }

                        val title = document.getString("title") ?: if (isEvent) {
                            "Event"
                        } else {
                            "Help Request"
                        }

                        getMarkerDataFromLocation(
                            geocoder,
                            address,
                            title,
                            fullDescription,
                            getMarkerColor(document)
                        )
                    }
                }

                processMarkerResults(deferredResults, markerList) {
                    updateCache(markerList)
                }
            }
        }.addOnFailureListener {
            binding.mapLoadingContainer.visibility = View.GONE
        }
    }

    private suspend fun processMarkerResults(
        deferredResults: List<Deferred<MarkerData?>>,
        markerList: MutableList<MarkerData>,
        onComplete: () -> Unit
    ) {
        deferredResults.awaitAll()
            .filterNotNull()
            .forEach { marker ->
                markerList.add(marker)
                withContext(Dispatchers.Main) {
                    if (isAdded) addMarkerToMap(marker)
                }
            }

        withContext(Dispatchers.Main) {
            onComplete()
            binding.mapLoadingContainer.visibility = View.GONE
        }
    }

    private fun getMarkerDataFromLocation(
        geocoder: Geocoder,
        location: String,
        title: String,
        description: String,
        markerColor: Float
    ): MarkerData? {
        return try {
            val addresses = geocoder.getFromLocationName(location, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                MarkerData(
                    position = LatLng(address.latitude, address.longitude),
                    title = title,
                    description = description,
                    markerColor = markerColor
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun addMarkerToMap(marker: MarkerData) {
        activity?.runOnUiThread {
            map.addMarker(
                MarkerOptions()
                    .position(marker.position)
                    .title(marker.title)
                    .snippet(marker.description)
                    .icon(BitmapDescriptorFactory.defaultMarker(marker.markerColor))
            )
        }
    }

    // ------------------------------------------------------------------------
    // Location / permissions
    // ------------------------------------------------------------------------

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        binding.mapLoadingContainer.visibility = View.VISIBLE

        if (!hasLocationPermission()) {
            binding.mapLoadingContainer.visibility = View.GONE
            showLocationServiceToast(R.string.location_permission_denied)
            requestLocationPermissions()
            return
        }

        if (!isLocationEnabled()) {
            moveToDefaultLocation()
            binding.mapLoadingContainer.visibility = View.GONE
            showLocationServiceToast(R.string.turn_on_location)
            promptLocationSettingsOnce()
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
            val location: Location? = task.result
            if (location != null) {
                moveCameraTo(location)
                binding.mapLoadingContainer.visibility = View.GONE
            } else {
                fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { newLocation ->
                    if (newLocation != null) {
                        moveCameraTo(newLocation)
                    } else {
                        moveToDefaultLocation()
                    }
                    binding.mapLoadingContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun moveCameraTo(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))
    }

    private fun moveToDefaultLocation() {
        if (::map.isInitialized) {
            val defaultLocation = LatLng(42.333774, -71.064937) // Boston
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 11f))
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationServiceToast(msgResId: Int) {
        if (!hasShownLocationServiceToast) {
            Toast.makeText(requireContext(), msgResId, Toast.LENGTH_LONG).show()
            hasShownLocationServiceToast = true
        }
    }

    private fun promptLocationSettingsOnce() {
        if (!hasPromptedLocationSettings) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            hasPromptedLocationSettings = true
        }
    }
}
