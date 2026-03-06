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
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ3Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel

class IndividualInteractionQ3 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ3Binding? = null
    private val binding get() = _binding!!

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualInteractionQ3Binding.inflate(inflater, container, false)
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

        // Enable/disable Other text
        fun refreshOtherEnabled() {
            val enabled = binding.cbOther.isChecked
            binding.etOther.isEnabled = enabled
            if (!enabled) binding.etOther.setText("")
        }
        binding.cbOther.setOnCheckedChangeListener { _, _ -> refreshOtherEnabled() }
        refreshOtherEnabled()

        // Restore previously selected further-help items when navigating back
        viewModel.currentInteraction.value?.furtherHelpNeeded?.forEach { item ->
            when (item) {
                "Food"          -> binding.cbFood.isChecked = true
                "Clothes"       -> binding.cbClothes.isChecked = true
                "Hygiene"       -> binding.cbHygiene.isChecked = true
                "Wellness"      -> binding.cbWellness.isChecked = true
                "Medical"       -> binding.cbMedical.isChecked = true
                "Social Worker" -> binding.cbSocialWorker.isChecked = true
                "Legal"         -> binding.cbLegal.isChecked = true
                else -> { binding.cbOther.isChecked = true; binding.etOther.setText(item) }
            }
        }
        refreshOtherEnabled()

        // Helper to build the selected list
        fun collectFurtherHelp(): List<String> {
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

        // Helper: Q1 basic details are filled
        fun isQ1Filled() = viewModel.currentInteraction.value?.firstName?.isNotBlank() == true &&
                viewModel.currentInteraction.value?.locationLandmark?.isNotBlank() == true

        fun requireQ1(): Boolean {
            if (!isQ1Filled()) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in the individual's details first.",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack(R.id.individualInteractionQ1, false)
                return false
            }
            return true
        }

        // Previous: back to Q2
        binding.txtPrevious2.setOnClickListener {
            findNavController().popBackStack()
        }

        // Skip: save empty and proceed
        binding.txtSkip.setOnClickListener {
            viewModel.saveQ3(emptyList())
            findNavController().navigate(
                R.id.action_individualInteractionQ3_to_individualInteractionQ4
            )
        }

        // Next: must select at least one, and Q1 must be filled
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

            viewModel.saveQ3(collectFurtherHelp())

            if (!requireQ1()) return@setOnClickListener

            findNavController().navigate(
                R.id.action_individualInteractionQ3_to_individualInteractionQ4
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
