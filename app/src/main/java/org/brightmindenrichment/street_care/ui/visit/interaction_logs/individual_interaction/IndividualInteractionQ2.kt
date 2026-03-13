package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.brightmindenrichment.street_care.R

class IndividualInteractionQ2 : BaseIIQuestionFragment() {

    override val questionNumber = 2

    // Content view references
    private lateinit var cbFood: AppCompatCheckBox
    private lateinit var cbClothes: AppCompatCheckBox
    private lateinit var cbHygiene: AppCompatCheckBox
    private lateinit var cbWellness: AppCompatCheckBox
    private lateinit var cbMedical: AppCompatCheckBox
    private lateinit var cbSocialWork: AppCompatCheckBox
    private lateinit var cbLegal: AppCompatCheckBox
    private lateinit var cbOther: AppCompatCheckBox
    private lateinit var tilOther: TextInputLayout
    private lateinit var etOther: TextInputEditText

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_individual_interaction_q2, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Initialize view references
        cbFood = contentView.findViewById(R.id.cb_food)
        cbClothes = contentView.findViewById(R.id.cb_clothes)
        cbHygiene = contentView.findViewById(R.id.cb_hygiene)
        cbWellness = contentView.findViewById(R.id.cb_wellness)
        cbMedical = contentView.findViewById(R.id.cb_medical)
        cbSocialWork = contentView.findViewById(R.id.cb_social_work)
        cbLegal = contentView.findViewById(R.id.cb_legal)
        cbOther = contentView.findViewById(R.id.cb_other)
        tilOther = contentView.findViewById(R.id.tilOther)
        etOther = contentView.findViewById(R.id.etOther)

        // Show/hide Other text
        fun refreshOtherVisibility() {
            val isChecked = cbOther.isChecked
            etOther.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                etOther.setText("")
                tilOther.error = null
            }
        }
        cbOther.setOnCheckedChangeListener { _, _ -> refreshOtherVisibility() }
        refreshOtherVisibility()

        // Clear error when Other input is focused or text changes
        etOther.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilOther.error = null
        }

        etOther.doAfterTextChanged {
            if (it.toString().isNotEmpty()) tilOther.error = null
        }

        // Build map of checkbox ID to string resource ID (single source of truth)
        val checkboxStringMap = mapOf(
            R.id.cb_food to R.string.food_drinks,
            R.id.cb_clothes to R.string.clothes,
            R.id.cb_hygiene to R.string.hygiene,
            R.id.cb_wellness to R.string.wellness_emotional_support,
            R.id.cb_medical to R.string.medical_help,
            R.id.cb_social_work to R.string.social_work_psychiatrist,
            R.id.cb_legal to R.string.lawyer_legal
        )

        // Restore previously selected supports when navigating back
        viewModel.currentInteraction.value?.supportsProvided?.forEach { support ->
            val matchedCheckbox = checkboxStringMap.entries.firstOrNull { (_, stringResId) ->
                getString(stringResId) == support
            }
            if (matchedCheckbox != null) {
                when (matchedCheckbox.key) {
                    R.id.cb_food -> cbFood.isChecked = true
                    R.id.cb_clothes -> cbClothes.isChecked = true
                    R.id.cb_hygiene -> cbHygiene.isChecked = true
                    R.id.cb_wellness -> cbWellness.isChecked = true
                    R.id.cb_medical -> cbMedical.isChecked = true
                    R.id.cb_social_work -> cbSocialWork.isChecked = true
                    R.id.cb_legal -> cbLegal.isChecked = true
                }
            } else {
                // Unknown/custom support goes to Other
                cbOther.isChecked = true
                etOther.setText(support)
            }
        }
        refreshOtherVisibility()
    }

    override fun onPreviousClicked() {
        viewModel.saveQ2(collectSupports())
        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().popBackStack()
        }
    }

    override fun onNextClicked() {
        val anyChecked = cbFood.isChecked || cbClothes.isChecked ||
                cbHygiene.isChecked || cbWellness.isChecked ||
                cbMedical.isChecked || cbSocialWork.isChecked ||
                cbLegal.isChecked || cbOther.isChecked

        if (!anyChecked) {
            performSkip()
            return
        }

        if (cbOther.isChecked && etOther.text?.toString()?.trim().isNullOrEmpty()) {
            tilOther.error = "Please specify what you provided"
            etOther.requestFocus()
            return
        }

        tilOther.error = null
        viewModel.saveQ2(collectSupports())
        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().navigate(
                R.id.action_individualInteractionQ2_to_individualInteractionQ3
            )
        }
    }

    override fun onSkipClicked() {
        performSkip()
    }

    private fun collectSupports(): List<String> {
        val list = mutableListOf<String>()
        if (cbFood.isChecked)       list.add(getString(R.string.food_drinks))
        if (cbClothes.isChecked)    list.add(getString(R.string.clothes))
        if (cbHygiene.isChecked)    list.add(getString(R.string.hygiene))
        if (cbWellness.isChecked)   list.add(getString(R.string.wellness_emotional_support))
        if (cbMedical.isChecked)    list.add(getString(R.string.medical_help))
        if (cbSocialWork.isChecked) list.add(getString(R.string.social_work_psychiatrist))
        if (cbLegal.isChecked)      list.add(getString(R.string.lawyer_legal))
        if (cbOther.isChecked) etOther.text?.toString()?.trim()
            ?.takeUnless { it.isEmpty() }?.let { list.add(it) }
        return list
    }

    private fun mergeIntoILAndSave(editingIdx: Int?, onComplete: () -> Unit) {
        if (editingIdx != null) {
            val current = viewModel.currentInteraction.value ?: return onComplete()
            interactionLogViewModel.replaceIndividualInteraction(editingIdx, current)
        }
        interactionLogViewModel.saveDraft {
            onComplete()
        }
    }

    private fun performSkip() {
        viewModel.saveQ2(emptyList())
        mergeIntoILAndSave(viewModel.editingIndex) {
            findNavController().navigate(
                R.id.action_individualInteractionQ2_to_individualInteractionQ3
            )
        }
    }
}
