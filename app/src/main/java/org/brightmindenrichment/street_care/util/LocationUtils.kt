package org.brightmindenrichment.street_care.util

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

fun reverseGeocodeAndFill(
    lat: Double,
    lon: Double,
    context: Context,
    scope: CoroutineScope,
    isViewValid: () -> Boolean,
    onResult: (street: String, city: String, state: String, zip: String) -> Unit
) {
    if (!Geocoder.isPresent()) return
    scope.launch(Dispatchers.IO) {
        try {
            val results = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
            val address = results?.firstOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                if (!isViewValid()) return@withContext
                val street = listOfNotNull(address.subThoroughfare, address.thoroughfare)
                    .joinToString(" ")
                val city = address.locality ?: address.subLocality ?: ""
                val state = address.adminArea ?: ""
                val zip = address.postalCode ?: ""
                onResult(street, city, state, zip)
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "Reverse geocode failed: ${e.message}")
        }
    }
}

fun launchPlacesAutocomplete(
    launcher: ActivityResultLauncher<Intent>,
    context: Context,
    initialQuery: String = ""
) {
    try {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.ADDRESS_COMPONENTS
        )
        val builder = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
        if (initialQuery.isNotBlank()) builder.setInitialQuery(initialQuery)
        launcher.launch(builder.build(context))
    } catch (e: Exception) {
        Log.e("LocationUtils", "Failed to launch autocomplete: ${e.message}")
    }
}
