package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ4Binding
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator

class InteractionQ4Fragment : Fragment(R.layout.fragment_log_interaction_q4), StepValidator {

    private var _binding: FragmentLogInteractionQ4Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()
    private var wasSkipped = false
    private var isTouched = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLogInteractionQ4Binding.bind(view)

        initializeViews()
        setupClickListeners()
        restoreSelections()

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(4)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(DOT_DEST_IDS[step - 1], false)
            }
        }
    }

    private fun initializeViews() {

        // Remove default tint from checkboxes
        for (i in 0 until binding.checkboxList.childCount) {
            val child = binding.checkboxList.getChildAt(i)
            if (child is CheckBox) {
                child.buttonTintList = null
            }
        }
    }

    private fun setupClickListeners() {

        // Show/hide Other input
        binding.otherCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.tilOther.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) binding.tilOther.error = null
        }

        // Clear error when Other input is focused or text changes
        binding.otherInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilOther.error = null
        }

        binding.otherInput.doAfterTextChanged {
            if (it.toString().isNotEmpty()) binding.tilOther.error = null
        }

        // Next button → navigate forward
        binding.btnNext.setOnClickListener {

            val selectedOptions = getSelectedOptions()

            if (selectedOptions.isEmpty()) {
                performSkip()
                return@setOnClickListener
            }

            // Other field validation if checkbox is checked
            if (binding.otherCheckbox.isChecked && binding.otherInput.text.toString().trim().isEmpty()) {
                binding.tilOther.error = "Please describe what you provided"
                binding.otherInput.requestFocus()
                return@setOnClickListener
            }

            // Save into ViewModel
            viewModel.setSupportsProvided(selectedOptions)

            // 🔥 DEBUG PRINT
            android.util.Log.d(
                "FORM_DEBUG",
                "After Q4 Save: ${viewModel.interactionLog.value}"
            )

            viewModel.saveDraft {
                // Navigate
                findNavController().navigate(R.id.action_q4_to_q5)
            }
        }


        // Previous → go back in stack
        binding.btnPrevious.setOnClickListener {
            val selectedOptions = getSelectedOptions()
            if (selectedOptions.isNotEmpty()) {
                viewModel.setSupportsProvided(selectedOptions)
            }
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
        }

        binding.skipBtn.setOnClickListener {
            performSkip()
        }
    }

    private fun performSkip() {
        wasSkipped = true
        saveCurrentState()
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_q4_to_q5)
        }
    }

    private fun restoreSelections() {
        val saved = viewModel.interactionLog.value?.listOfSupportsProvided ?: return
        if (saved.isEmpty()) return

        // Build standard text set from string resources (single source of truth)
        val standardTexts = setOf(
            getString(R.string.food_drinks),
            getString(R.string.clothes),
            getString(R.string.hygiene),
            getString(R.string.wellness_emotional_support),
            getString(R.string.medical_help),
            getString(R.string.social_work_psychiatrist),
            getString(R.string.lawyer_legal),
            getString(R.string.other)
        )

        for (i in 0 until binding.checkboxList.childCount) {
            val child = binding.checkboxList.getChildAt(i)
            if (child !is CheckBox) continue

            if (child.id == R.id.other_checkbox) {
                val customOther = saved.firstOrNull { it !in standardTexts }
                when {
                    customOther != null -> {
                        child.isChecked = true
                        binding.tilOther.visibility = View.VISIBLE
                        binding.otherInput.setText(customOther)
                    }
                    getString(R.string.other) in saved -> {
                        child.isChecked = true
                        binding.tilOther.visibility = View.VISIBLE
                    }
                }
            } else {
                child.isChecked = child.text.toString() in saved
            }
        }
    }

    private fun getSelectedOptions(): List<String> {

        val selected = mutableListOf<String>()

        for (i in 0 until binding.checkboxList.childCount) {

            val child = binding.checkboxList.getChildAt(i)

            if (child is CheckBox && child.isChecked) {

                if (child.id == R.id.other_checkbox) {

                    val otherText = binding.otherInput.text.toString()

                    selected.add(
                        if (otherText.isNotBlank()) otherText else getString(R.string.other)
                    )

                } else {
                    selected.add(child.text.toString())
                }
            }
        }

        return selected
    }

    override fun saveCurrentState() {
        isTouched = true
        val selectedOptions = getSelectedOptions()
        if (selectedOptions.isNotEmpty()) {
            viewModel.setSupportsProvided(selectedOptions)
        }
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
        return getSelectedOptions().isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DOT_DEST_IDS = listOf(
            R.id.interactionQ1Fragment,
            R.id.interactionQ2Fragment,
            R.id.interactionQ3Fragment,
            R.id.interactionQ4Fragment,
            R.id.interactionQ5Fragment,
            R.id.interactionQ6Fragment,
            R.id.interactionQ7Fragment
        )
    }
}
