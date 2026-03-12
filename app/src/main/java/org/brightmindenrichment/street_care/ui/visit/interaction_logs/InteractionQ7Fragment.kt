package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ7Binding
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.Constants

class InteractionQ7Fragment : Fragment(), StepValidator {

    private var _binding: FragmentLogInteractionQ7Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()
    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    private var selectedAnswer: Boolean? = null
    private var wasSkipped = false
    private var isTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ7Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----------------------
        // 1. Show Bottom Navigation
        // -----------------------
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // -----------------------
        // 4. Restore Previous Value
        // -----------------------
        val current = viewModel.interactionLog.value ?: InteractionLog()
        selectedAnswer = current.wantsToProvideDetails

        fun updateUI() {
            if (selectedAnswer == true) {
                binding.txtYes.alpha = 1f
                binding.txtNo.alpha = 0.5f
            } else if (selectedAnswer == false) {
                binding.txtYes.alpha = 0.5f
                binding.txtNo.alpha = 1f
            } else {
                binding.txtYes.alpha = 1f
                binding.txtNo.alpha = 1f
            }
        }

        // -----------------------
        // 5. Button Logic
        // -----------------------
        binding.txtYes.setOnClickListener {
            selectedAnswer = true
            updateUI()
            goToNext()
        }

        binding.txtNo.setOnClickListener {
            selectedAnswer = false
            updateUI()
            goToNext()
        }

        binding.txtSkip.setOnClickListener {
            wasSkipped = true
            goToNext()
        }

        binding.txtPrevious5.setOnClickListener {
            viewModel.updateQ7Answer(selectedAnswer)
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
        }

        updateUI()

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(7)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(Constants.INTERACTION_LOG_DEST_IDS[step - 1], false)
            }
        }
    }

    // -----------------------
    // Save + Navigate
    // -----------------------
    private fun goToNext() {

        viewModel.updateQ7Answer(selectedAnswer)
        viewModel.saveDraft {
            when (selectedAnswer) {
                true -> {
                    val hasExisting = !iiViewModel.committedInteractions.value.isNullOrEmpty()
                    if (hasExisting) {
                        findNavController().navigate(R.id.action_q7_yes_to_individualInteractionList)
                    } else {
                        findNavController().navigate(R.id.action_q7_yes_to_individualInteraction1)
                    }
                }
                false -> {
                    findNavController().navigate(
                        R.id.action_q7_no_to_consentPage
                    )
                }
                null -> {
                    // If skipped, decide default behavior
                    findNavController().navigate(
                        R.id.action_q7_no_to_consentPage
                    )
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}