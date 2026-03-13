package org.brightmindenrichment.street_care.util.featureflags

/**
 * Contract for feature flag providers.
 *
 * To swap to a different provider (LaunchDarkly, Firebase Remote Config, etc.):
 *   1. Implement this interface.
 *   2. Call [FeatureFlagManager.setProvider] before [FeatureFlagManager.fetch].
 */
interface FeatureFlagProvider {
    /** Returns whether [flag] is enabled. Returns false if the flag is unknown or not yet fetched. */
    fun isEnabled(flag: FeatureFlag): Boolean

    /** Fetches / refreshes flag values from the remote source. Calls [onComplete] when done (success or failure). */
    fun fetch(onComplete: () -> Unit)
}
