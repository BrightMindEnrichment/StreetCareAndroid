package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ2Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel

class IndividualInteractionQ2 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ2Binding? = null
    private val binding get() = _binding!!

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualInteractionQ2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editHeader = viewModel.editingHeaderText()
        if (editHeader != null) {
            binding.tvHeader.text = editHeader
        } else {
            interactionLogViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
                binding.tvHeader.text = if (idx <= 1) {
                    getString(R.string.individual_interaction_title_base)
                } else {
                    getString(R.string.individual_interaction_title_numbered, idx)
                }
            }
        }

        // Show/hide Other text
        fun refreshOtherVisibility() {
            val isChecked = binding.cbOther.isChecked
            binding.etOther.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.etOther.setText("")
                binding.tilOther.error = null
            }
        }
        binding.cbOther.setOnCheckedChangeListener { _, _ -> refreshOtherVisibility() }
        refreshOtherVisibility()

        // Clear error when Other input is focused or text changes
        binding.etOther.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilOther.error = null
        }

        binding.etOther.doAfterTextChanged {
            if (it.toString().isNotEmpty()) binding.tilOther.error = null
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
                binding.root.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(matchedCheckbox.key).isChecked = true
            } else {
                // Unknown/custom support goes to Other
                binding.cbOther.isChecked = true
                binding.etOther.setText(support)
            }
        }
        refreshOtherVisibility()

        // Helper to build the selected list using string resources
        fun collectSupports(): List<String> {
            val list = mutableListOf<String>()
            if (binding.cbFood.isChecked)       list.add(getString(R.string.food_drinks))
            if (binding.cbClothes.isChecked)    list.add(getString(R.string.clothes))
            if (binding.cbHygiene.isChecked)    list.add(getString(R.string.hygiene))
            if (binding.cbWellness.isChecked)   list.add(getString(R.string.wellness_emotional_support))
            if (binding.cbMedical.isChecked)    list.add(getString(R.string.medical_help))
            if (binding.cbSocialWork.isChecked) list.add(getString(R.string.social_work_psychiatrist))
            if (binding.cbLegal.isChecked)      list.add(getString(R.string.lawyer_legal))
            if (binding.cbOther.isChecked) binding.etOther.text?.toString()?.trim()
                ?.takeUnless { it.isEmpty() }?.let { list.add(it) }
            return list
        }

        // Previous: back to Q1
        binding.txtPrevious2.setOnClickListener {
            viewModel.saveQ2(collectSupports())
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().popBackStack()
            }
        }

        // Skip: save empty and proceed
        binding.txtSkip.setOnClickListener {
            performSkip()
        }

        // Next: must select at least one
        binding.txtNext2.setOnClickListener {
            val anyChecked = binding.cbFood.isChecked || binding.cbClothes.isChecked ||
                    binding.cbHygiene.isChecked || binding.cbWellness.isChecked ||
                    binding.cbMedical.isChecked || binding.cbSocialWork.isChecked ||
                    binding.cbLegal.isChecked || binding.cbOther.isChecked

            if (!anyChecked) {
                performSkip()
                return@setOnClickListener
            }

            if (binding.cbOther.isChecked && binding.etOther.text?.toString()?.trim().isNullOrEmpty()) {
                binding.tilOther.error = "Please specify what you provided"
                binding.etOther.requestFocus()
                return@setOnClickListener
            }

            binding.tilOther.error = null
            viewModel.saveQ2(collectSupports())
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().navigate(
                    R.id.action_individualInteractionQ2_to_individualInteractionQ3
                )
            }
        }
    }

    private fun mergeIntoILAndSave(editingIdx: Int?, onComplete: () -> Unit) {
        if (editingIdx != null) {
            // Editing: use current interaction from ViewModel
            val current = viewModel.currentInteraction.value ?: return onComplete()
            interactionLogViewModel.replaceIndividualInteraction(editingIdx, current)
        } else {
            // New interaction: nothing to merge yet, just save the draft
            // The actual merge happens in Q4 when the II is completed
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
