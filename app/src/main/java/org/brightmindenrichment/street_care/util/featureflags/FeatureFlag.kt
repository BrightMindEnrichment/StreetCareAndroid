package org.brightmindenrichment.street_care.util.featureflags

enum class FeatureFlag(val key: String) {
    /**
     * Controls the discard dialog behaviour when the user navigates away via the bottom nav
     * while filling an Interaction Log.
     *
     * OFF (Case 1 — cross-referencing): Shows a 3-button dialog:
     *   "Keep editing" — stays on form
     *   "Continue later" — navigates away, state preserved
     *   "Discard" — navigates away, clears both IL and II state
     *
     * ON (Case 2 — simple): Shows the 2-button dialog:
     *   "Keep editing" — stays on form
     *   "Discard" — navigates away, clears both IL and II state
     */
    CLEAR_FORM_ON_WORKFLOW_EXIT("clearFormOnWorkflowExit")
}
