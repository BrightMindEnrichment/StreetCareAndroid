package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            if (!isChecked) binding.etOther.setText("")
        }
        binding.cbOther.setOnCheckedChangeListener { _, _ -> refreshOtherVisibility() }
        refreshOtherVisibility()

        // Restore previously selected supports when navigating back
        viewModel.currentInteraction.value?.supportsProvided?.forEach { support ->
            when (support) {
                "Food"          -> binding.cbFood.isChecked = true
                "Clothes"       -> binding.cbClothes.isChecked = true
                "Hygiene"       -> binding.cbHygiene.isChecked = true
                "Wellness"      -> binding.cbWellness.isChecked = true
                "Medical"       -> binding.cbMedical.isChecked = true
                "Social Worker" -> binding.cbSocialWorker.isChecked = true
                "Legal"         -> binding.cbLegal.isChecked = true
                else -> { binding.cbOther.isChecked = true; binding.etOther.setText(support) }
            }
        }
        refreshOtherVisibility()

        // Helper to build the selected list
        fun collectSupports(): List<String> {
            val list = mutableListOf<String>()
            if (binding.cbFood.isChecked)        list.add("Food")
            if (binding.cbClothes.isChecked)     list.add("Clothes")
            if (binding.cbHygiene.isChecked)     list.add("Hygiene")
            if (binding.cbWellness.isChecked)    list.add("Wellness")
            if (binding.cbMedical.isChecked)     list.add("Medical")
            if (binding.cbSocialWorker.isChecked) list.add("Social Worker")
            if (binding.cbLegal.isChecked)       list.add("Legal")
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
            viewModel.saveQ2(emptyList())
            mergeIntoILAndSave(viewModel.editingIndex) {
                findNavController().navigate(
                    R.id.action_individualInteractionQ2_to_individualInteractionQ3
                )
            }
        }

        // Next: must select at least one
        binding.txtNext2.setOnClickListener {
            val anyChecked = binding.cbFood.isChecked || binding.cbClothes.isChecked ||
                    binding.cbHygiene.isChecked || binding.cbWellness.isChecked ||
                    binding.cbMedical.isChecked || binding.cbSocialWorker.isChecked ||
                    binding.cbLegal.isChecked || binding.cbOther.isChecked

            if (!anyChecked) {
                Toast.makeText(requireContext(), "Select at least one option.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.cbOther.isChecked && binding.etOther.text?.toString()?.trim().isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please specify Other.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
