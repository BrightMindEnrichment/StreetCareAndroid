package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.widget.StepState

class InteractionQ6Fragment : BaseILQuestionFragment() {

    private var carePackageCount = 0
    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var etCount: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnDecrease: FrameLayout
    private lateinit var btnIncrease: FrameLayout

    override val stepNumber = 6

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_interaction_log_q6, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Get references to content views
        etCount = contentView.findViewById(R.id.et_count)
        etNotes = contentView.findViewById(R.id.et_notes)
        btnDecrease = contentView.findViewById(R.id.btn_decrease)
        btnIncrease = contentView.findViewById(R.id.btn_increase)

        // Restore previous values
        val current = viewModel.interactionLog.value ?: InteractionLog()
        carePackageCount = current.carePackagesDistributed
        android.util.Log.d("Q6_DEBUG", "Restored carePackageCount from ViewModel: $carePackageCount")
        etCount.setText(carePackageCount.toString())
        val notes = current.carePackageContents.joinToString(", ")
        etNotes.setText(notes)

        fun updateUI() {
            etCount.setText(carePackageCount.toString())
            // Disable minus button when carePackageCount is 0 (minimum)
            btnDecrease.isEnabled = carePackageCount > 0
            btnDecrease.alpha = if (carePackageCount > 0) 1f else 0.5f
        }

        // Counter logic
        btnIncrease.setOnClickListener {
            syncFromInput()
            carePackageCount++
            updateUI()
        }

        btnDecrease.setOnClickListener {
            syncFromInput()
            if (carePackageCount > 0) {
                carePackageCount--
                updateUI()
            }
        }

        updateUI()
    }

    private fun syncFromInput() {
        carePackageCount = etCount.text.toString().toIntOrNull() ?: carePackageCount
    }

    override fun onNextNavigate() {
        syncFromInput()
        viewModel.updateCarePackage(carePackageCount, etNotes.text.toString())
        findNavController().navigate(R.id.action_q6_to_q7)
    }

    override fun onSkipNavigate() {
        wasSkipped = true
        saveCurrentState()
        onNextNavigate()
    }

    override fun saveCurrentState() {
        isTouched = true
        viewModel.updateCarePackage(carePackageCount, etNotes.text.toString())
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
        return carePackageCount > 0
    }

}
