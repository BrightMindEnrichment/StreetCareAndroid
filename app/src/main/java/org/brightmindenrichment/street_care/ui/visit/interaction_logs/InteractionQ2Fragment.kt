package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentQuestion2Binding

class InteractionQ2Fragment : Fragment() {

    private var _binding: FragmentQuestion2Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestion2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill fields from ViewModel (important for back navigation)
        binding.inputFirstName.setText(viewModel.firstName.value.orEmpty())
        binding.inputLastName.setText(viewModel.lastName.value.orEmpty())
        binding.inputEmail.setText(viewModel.email.value.orEmpty())
        binding.inputPhoneNumber.setText(viewModel.phoneNumber.value.orEmpty())

        setCloseButton()
        setPreviousButton()
        setNextButton()
        setSkipButton()
    }

    private fun setCloseButton() {
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setPreviousButton() {
        binding.txtPrevious2.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setNextButton() {
        binding.txtNext2.setOnClickListener {

            val firstName = binding.inputFirstName.text.toString().trim()
            val lastName = binding.inputLastName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val phone = binding.inputPhoneNumber.text.toString().trim()

            // ---- Validation ----
            if (firstName.isEmpty()) {
                binding.inputFirstName.error = "Please enter first name"
                binding.inputFirstName.requestFocus()
                return@setOnClickListener
            }

            if (lastName.isEmpty()) {
                binding.inputLastName.error = "Please enter last name"
                binding.inputLastName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                binding.inputEmail.error = "Please enter email"
                binding.inputEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                binding.inputPhoneNumber.error = "Please enter phone number"
                binding.inputPhoneNumber.requestFocus()
                return@setOnClickListener
            }

            // ---- Save to ViewModel ----
            viewModel.firstName.value = firstName
            viewModel.lastName.value = lastName
            viewModel.email.value = email
            viewModel.phoneNumber.value = phone

            // ---- Navigate to Q3 ----
            findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
        }
    }

    private fun setSkipButton() {
        binding.txtSkip3.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Skipped personal info",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
