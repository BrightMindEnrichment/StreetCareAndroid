package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.widget.StepState

class InteractionQ5Fragment : BaseILQuestionFragment() {

    private var helpedCount = 1
    private var joinedCount = 0
    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var etCountHelped: EditText
    private lateinit var etCountJoined: EditText
    private lateinit var btnDecreaseHelped: FrameLayout
    private lateinit var btnIncreaseHelped: FrameLayout
    private lateinit var btnDecreaseJoined: FrameLayout
    private lateinit var btnIncreaseJoined: FrameLayout

    override val stepNumber = 5

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_il_q5, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Get references to content views
        etCountHelped = contentView.findViewById(R.id.et_count_helped)
        etCountJoined = contentView.findViewById(R.id.et_count_joined)
        btnDecreaseHelped = contentView.findViewById(R.id.btn_decrease_helped)
        btnIncreaseHelped = contentView.findViewById(R.id.btn_increase_helped)
        btnDecreaseJoined = contentView.findViewById(R.id.btn_decrease_joined)
        btnIncreaseJoined = contentView.findViewById(R.id.btn_increase_joined)

        // Restore saved counts if returning from a later screen
        val saved = viewModel.interactionLog.value
        if (saved != null) {
            helpedCount = saved.numPeopleHelped.takeIf { it > 0 } ?: 1
            joinedCount = saved.numPeopleJoined
        }

        updateUI()

        btnIncreaseHelped.setOnClickListener { syncFromInput(); helpedCount++; updateUI() }
        btnDecreaseHelped.setOnClickListener { syncFromInput(); if (helpedCount > 1) { helpedCount--; updateUI() } }

        btnIncreaseJoined.setOnClickListener { syncFromInput(); joinedCount++; updateUI() }
        btnDecreaseJoined.setOnClickListener { syncFromInput(); if (joinedCount > 0) { joinedCount--; updateUI() } else btnDecreaseJoined.isEnabled = false }
    }

    private fun syncFromInput() {
        helpedCount = etCountHelped.text.toString().toIntOrNull() ?: helpedCount
        joinedCount = etCountJoined.text.toString().toIntOrNull() ?: joinedCount
    }

    private fun updateUI() {
        etCountHelped.setText(helpedCount.toString())
        etCountJoined.setText(joinedCount.toString())
        // Disable minus button when helpedCount is 1 (minimum)
        btnDecreaseHelped.isEnabled = helpedCount > 1
        btnDecreaseHelped.alpha = if (helpedCount > 1) 1f else 0.5f
        // Disable minus button when joinedCount is 0 (minimum)
        btnDecreaseJoined.isEnabled = joinedCount > 0
        btnDecreaseJoined.alpha = if (joinedCount > 0) 1f else 0.5f
    }

    override fun onNextNavigate() {
        syncFromInput()
        viewModel.updateCounts(helpedCount, joinedCount)
        findNavController().navigate(R.id.action_q5_to_q6)
    }

    override fun onSkipNavigate() {
        wasSkipped = true
        saveCurrentState()
        onNextNavigate()
    }

    override fun saveCurrentState() {
        isTouched = true
        syncFromInput()
        viewModel.updateCounts(helpedCount, joinedCount)
    }

    override fun getStepState(): StepState {
        return when {
            wasSkipped -> StepState.SKIPPED
            isCurrentStepValid() -> StepState.VALID
            isTouched -> StepState.TOUCHED
            else -> StepState.EMPTY
        }
    }

    private fun isCurrentStepValid(): Boolean {
        return helpedCount > 0 || joinedCount > 0
    }

}
