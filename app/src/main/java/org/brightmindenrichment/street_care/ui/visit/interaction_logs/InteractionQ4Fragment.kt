package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ4Binding

class InteractionQ4Fragment : Fragment(R.layout.fragment_log_interaction_q4) {

    private var _binding: FragmentLogInteractionQ4Binding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLogInteractionQ4Binding.bind(view)

        initializeViews()
        setupClickListeners()
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
            binding.otherInput.visibility =
                if (isChecked) View.VISIBLE else View.GONE
        }

        // Next button → navigate forward
        binding.btnNext.setOnClickListener {

            val selectedOptions = getSelectedOptions()

            if (selectedOptions.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please select at least one option",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                findNavController().popBackStack(R.id.nav_home, false)
                // Or navigate somewhere specific if needed
            }
        }

        // Previous → go back in stack
        binding.btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.skipBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCloseContainer.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
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
                        if (otherText.isNotBlank()) otherText else "Other"
                    )

                } else {
                    selected.add(child.text.toString())
                }
            }
        }

        return selected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
