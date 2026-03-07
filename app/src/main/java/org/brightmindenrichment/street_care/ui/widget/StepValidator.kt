package org.brightmindenrichment.street_care.ui.widget

import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel

interface StepValidator {
    fun saveCurrentState()
    fun getStepState(): StepState
    val viewModel: InteractionLogViewModel
}
