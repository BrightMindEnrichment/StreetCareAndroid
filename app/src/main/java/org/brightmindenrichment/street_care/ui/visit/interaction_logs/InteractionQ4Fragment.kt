package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.widget.StepState

class InteractionQ4Fragment : BaseILQuestionFragment() {

    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var checkboxList: ViewGroup
    private lateinit var otherCheckbox: CheckBox
    private lateinit var tilOther: TextInputLayout
    private lateinit var otherInput: android.widget.EditText

    override val stepNumber = 4

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_interaction_log_q4, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        checkboxList = contentView.findViewById(R.id.checkbox_list)
        otherCheckbox = contentView.findViewById(R.id.other_checkbox)
        tilOther = contentView.findViewById(R.id.tilOther)
        otherInput = contentView.findViewById(R.id.other_input)

        initializeViews()
        setupClickListeners()
        restoreSelections()
    }

    private fun initializeViews() {
        // Remove default tint from checkboxes
        for (i in 0 until checkboxList.childCount) {
            val child = checkboxList.getChildAt(i)
            if (child is CheckBox) {
                child.buttonTintList = null
            }
        }
    }

    private fun setupClickListeners() {
        // Show/hide Other input
        otherCheckbox.setOnCheckedChangeListener { _, isChecked ->
            tilOther.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) tilOther.error = null
        }

        // Clear error when Other input is focused or text changes
        otherInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilOther.error = null
        }

        otherInput.doAfterTextChanged {
            if (it.toString().isNotEmpty()) tilOther.error = null
        }
    }

    override fun onNextNavigate() {
        val selectedOptions = getSelectedOptions()

        if (selectedOptions.isEmpty()) {
            onSkipNavigate()
            return
        }

        // Other field validation if checkbox is checked
        if (otherCheckbox.isChecked && otherInput.text.toString().trim().isEmpty()) {
            tilOther.error = "Please describe what you provided"
            otherInput.requestFocus()
            return
        }

        // Save into ViewModel
        viewModel.setSupportsProvided(selectedOptions)
        android.util.Log.d("FORM_DEBUG", "After Q4 Save: ${viewModel.interactionLog.value}")
        findNavController().navigate(R.id.action_q4_to_q5)
    }

    override fun onSkipNavigate() {
        wasSkipped = true
        saveCurrentState()
        findNavController().navigate(R.id.action_q4_to_q5)
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

        for (i in 0 until checkboxList.childCount) {
            val child = checkboxList.getChildAt(i)
            if (child !is CheckBox) continue

            if (child.id == R.id.other_checkbox) {
                val customOther = saved.firstOrNull { it !in standardTexts }
                when {
                    customOther != null -> {
                        child.isChecked = true
                        tilOther.visibility = View.VISIBLE
                        otherInput.setText(customOther)
                    }
                    getString(R.string.other) in saved -> {
                        child.isChecked = true
                        tilOther.visibility = View.VISIBLE
                    }
                }
            } else {
                child.isChecked = child.text.toString() in saved
            }
        }
    }

    private fun getSelectedOptions(): List<String> {
        val selected = mutableListOf<String>()

        for (i in 0 until checkboxList.childCount) {
            val child = checkboxList.getChildAt(i)

            if (child is CheckBox && child.isChecked) {
                if (child.id == R.id.other_checkbox) {
                    val otherText = otherInput.text.toString()
                    selected.add(if (otherText.isNotBlank()) otherText else getString(R.string.other))
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

}
