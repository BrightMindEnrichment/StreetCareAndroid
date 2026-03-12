package org.brightmindenrichment.street_care.ui.widget

enum class StepState {
    EMPTY,      // Not visited
    TOUCHED,    // Visited previously, came back to it
    SKIPPED,    // Explicitly skipped
    VALID,      // Completed with valid data
    CURRENT     // Currently on this step
}
