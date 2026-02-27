package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ4Binding

class InteractionQ4Fragment : Fragment(R.layout.fragment_log_interaction_q4) {

    private var _binding: FragmentLogInteractionQ4Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()


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
                Toast.makeText(
                    requireContext(),
                    "Please select at least one option",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 🔥 SAVE INTO VIEWMODEL
            val current = viewModel.interactionLog.value!!
            viewModel.interactionLog.value = current.copy(
                listOfSupportsProvided = selectedOptions
            )

            // 🔥 DEBUG PRINT
            android.util.Log.d(
                "FORM_DEBUG",
                "After Q4 Save: ${viewModel.interactionLog.value}"
            )

            // Navigate
            findNavController().navigate(R.id.action_q4_to_q5)
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
