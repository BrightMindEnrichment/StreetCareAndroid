package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel
import org.brightmindenrichment.street_care.ui.widget.StepState
import androidx.fragment.app.activityViewModels

class InteractionQ7Fragment : BaseILQuestionFragment() {

    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    private var selectedAnswer: Boolean? = null
    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var txtYes: TextView
    private lateinit var txtNo: TextView

    override val stepNumber = 7

    override fun showSkipButton() = false
    override fun showNextButton() = false
    override fun showYesNoButtons() = true
    override fun showPreviousButton() = true

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_interaction_log_q7, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Show Bottom Navigation
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // Restore previous value
        val current = viewModel.interactionLog.value ?: InteractionLog()
        selectedAnswer = current.wantsToProvideDetails

        fun updateUI() {
            if (selectedAnswer == true) {
                binding.btnYes.alpha = 1f
                binding.btnNo.alpha = 0.5f
            } else if (selectedAnswer == false) {
                binding.btnYes.alpha = 0.5f
                binding.btnNo.alpha = 1f
            } else {
                binding.btnYes.alpha = 1f
                binding.btnNo.alpha = 1f
            }
        }

        // Wire yes/no buttons
        binding.btnYes.setOnClickListener {
            selectedAnswer = true
            updateUI()
            onYesSelected()
        }

        binding.btnNo.setOnClickListener {
            selectedAnswer = false
            updateUI()
            onNoSelected()
        }

        updateUI()
    }

    private fun onYesSelected() {
        viewModel.updateQ7Answer(selectedAnswer)
        viewModel.saveDraft {
            val hasExisting = !iiViewModel.committedInteractions.value.isNullOrEmpty()
            Log.d("Q7Navigation", "onYesSelected: hasExisting=$hasExisting, backStackId=${findNavController().currentDestination?.id}")
            if (hasExisting) {
                Log.d("Q7Navigation", "Navigating to IndividualInteractionList")
                findNavController().navigate(R.id.action_q7_yes_to_individualInteractionList)
            } else {
                Log.d("Q7Navigation", "Navigating to IndividualInteractionQ1 (new)")
                findNavController().navigate(R.id.action_q7_yes_to_individualInteraction1)
            }
        }
    }

    private fun onNoSelected() {
        viewModel.updateQ7Answer(selectedAnswer)
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_q7_no_to_consentPage)
        }
    }

    override fun saveCurrentState() {
        isTouched = true
        viewModel.updateQ7Answer(selectedAnswer)
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
        return selectedAnswer != null
    }

}