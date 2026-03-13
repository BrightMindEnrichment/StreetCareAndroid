package org.brightmindenrichment.street_care.util.featureflags

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Fetches feature flags from Firebase Remote Config.
 *
 * Console setup:
 *   Firebase Console → Remote Config → Add parameter
 *   Parameter key   = FeatureFlag.key  (e.g. "clearFormOnWorkflowExit")
 *   Value type      = Boolean
 *   Default value   = false
 *
 * The in-app default for every flag is false, matching the Remote Config console default.
 * On first launch (or if the network call fails), the in-app defaults are used.
 *
 * Fetch interval: 1 hour in production (Firebase-recommended minimum).
 * During development, set a shorter interval by calling
 *   FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(
 *     FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
 *   )
 * before [fetch] — or simply reduce FETCH_INTERVAL_SECONDS here.
 */
class FirebaseFeatureFlagProvider : FeatureFlagProvider {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        // Set in-app defaults so isEnabled() returns a sensible value before the first fetch.
        val defaults = FeatureFlag.entries.associate { it.key to false }
        remoteConfig.setDefaultsAsync(defaults)

        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
    }

    override fun isEnabled(flag: FeatureFlag): Boolean =
        remoteConfig.getBoolean(flag.key)

    /**
     * Fetches and activates remote config values in one call.
     * [onComplete] is invoked on both success and failure.
     */
    override fun fetch(onComplete: () -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                Log.d(TAG, "Remote Config fetched and activated")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Remote Config fetch failed — in-app defaults will be used", e)
                onComplete()
            }
    }

    companion object {
        private const val TAG = "FeatureFlags"
        private const val FETCH_INTERVAL_SECONDS = 1L // 1 hour
    }
}
