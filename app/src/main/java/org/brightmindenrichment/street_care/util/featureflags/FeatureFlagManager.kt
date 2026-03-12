package org.brightmindenrichment.street_care.util.featureflags

/**
 * Singleton entry-point for checking feature flags throughout the app.
 *
 * Defaults to [FirebaseFeatureFlagProvider]. To switch providers:
 *   FeatureFlagManager.setProvider(MyOtherProvider())
 *   FeatureFlagManager.fetch()
 *
 * Usage:
 *   if (FeatureFlagManager.isEnabled(FeatureFlag.CLEAR_FORM_ON_WORKFLOW_EXIT)) { ... }
 */
object FeatureFlagManager {

    private var provider: FeatureFlagProvider = FirebaseFeatureFlagProvider()

    /** Replace the backing provider. Must be called before [fetch] to take effect. */
    fun setProvider(newProvider: FeatureFlagProvider) {
        provider = newProvider
    }

    /** Returns true if [flag] is enabled. Falls back to false if not yet fetched or unknown. */
    fun isEnabled(flag: FeatureFlag): Boolean = provider.isEnabled(flag)

    /**
     * Fetches / refreshes all flags from the remote source.
     * Called once at app startup (see [MyApplication]). Safe to call again to force a refresh.
     */
    fun fetch(onComplete: () -> Unit = {}) = provider.fetch(onComplete)
}
