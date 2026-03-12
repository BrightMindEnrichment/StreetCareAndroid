package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ5Binding
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.Constants

class InteractionQ5Fragment : Fragment(), StepValidator {

    private var _binding: FragmentLogInteractionQ5Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()

    private var helpedCount = 1
    private var joinedCount = 0
    private var wasSkipped = false
    private var isTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore saved counts if returning from a later screen
        val saved = viewModel.interactionLog.value
        if (saved != null) {
            helpedCount = saved.numPeopleHelped.takeIf { it > 0 } ?: 1
            joinedCount = saved.numPeopleJoined
        }

        updateUI()

        binding.btnIncreaseHelped.setOnClickListener { syncFromInput(); helpedCount++; updateUI() }
        binding.btnDecreaseHelped.setOnClickListener { syncFromInput(); if (helpedCount > 1) { helpedCount--; updateUI() } }

        binding.btnIncreaseJoined.setOnClickListener { syncFromInput(); joinedCount++; updateUI() }
        binding.btnDecreaseJoined.setOnClickListener { syncFromInput(); if (joinedCount > 0) { joinedCount--; updateUI() } else binding.btnDecreaseJoined.isEnabled = false }

        binding.btnPrevious.setOnClickListener {
            syncFromInput()
            viewModel.updateCounts(helpedCount, joinedCount)
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
        }

        binding.btnSkip.setOnClickListener {
            wasSkipped = true
            navigateNext()
        }
        binding.btnNext.setOnClickListener { navigateNext() }

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(5)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(Constants.INTERACTION_LOG_DEST_IDS[step - 1], false)
            }
        }
    }

    private fun syncFromInput() {
        helpedCount = binding.etCountHelped.text.toString().toIntOrNull() ?: helpedCount
        joinedCount = binding.etCountJoined.text.toString().toIntOrNull() ?: joinedCount
    }

    private fun updateUI() {
        binding.etCountHelped.setText(helpedCount.toString())
        binding.etCountJoined.setText(joinedCount.toString())
        // Disable minus button when helpedCount is 1 (minimum)
        binding.btnDecreaseHelped.isEnabled = helpedCount > 1
        binding.btnDecreaseHelped.alpha = if (helpedCount > 1) 1f else 0.5f
        // Disable minus button when joinedCount is 0 (minimum)
        binding.btnDecreaseJoined.isEnabled = joinedCount > 0
        binding.btnDecreaseJoined.alpha = if (joinedCount > 0) 1f else 0.5f
    }

    private fun navigateNext() {
        syncFromInput()
        viewModel.updateCounts(helpedCount, joinedCount)
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_q5_to_q6)
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
