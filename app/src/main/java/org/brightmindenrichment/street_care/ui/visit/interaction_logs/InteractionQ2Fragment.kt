package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ2Binding
import org.brightmindenrichment.street_care.ui.user.UserSingleton
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator

class InteractionQ2Fragment : Fragment(), StepValidator {

    private var _binding: FragmentLogInteractionQ2Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()
    private var wasSkipped = false
    private var isTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val log = viewModel.interactionLog.value

        // Prefill from Firebase Auth if fields haven't been set yet
        if (log?.firstName.isNullOrEmpty() && log?.lastName.isNullOrEmpty() && log?.email.isNullOrEmpty()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val displayName = currentUser?.displayName?.trim()
            val email = currentUser?.email.orEmpty()

            val (firstName, lastName) = if (!displayName.isNullOrEmpty()) {
                val spaceIndex = displayName.indexOf(' ')
                if (spaceIndex >= 0) {
                    displayName.substring(0, spaceIndex) to displayName.substring(spaceIndex + 1)
                } else {
                    displayName to ""
                }
            } else {
                // Fall back to Firestore username in first name field
                val username = UserSingleton.userModel.userName.orEmpty()
                username to ""
            }

            binding.inputFirstName.setText(firstName)
            binding.inputLastName.setText(lastName)
            binding.inputEmail.setText(email)
        } else {
            binding.inputFirstName.setText(log?.firstName.orEmpty())
            binding.inputLastName.setText(log?.lastName.orEmpty())
            binding.inputEmail.setText(log?.email.orEmpty())
            binding.inputPhoneNumber.setText(log?.phoneNumber.orEmpty())
        }

        setPreviousButton()
        setNextButton()
        setSkipButton()

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(2)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(DOT_DEST_IDS[step - 1], false)
            }
        }
    }

    private fun setPreviousButton() {
        binding.txtPrevious2.setOnClickListener {
            val firstName = binding.inputFirstName.text.toString().trim()
            val lastName = binding.inputLastName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val phone = binding.inputPhoneNumber.text.toString().trim()
            viewModel.updateFirstName(firstName)
            viewModel.updateLastName(lastName)
            viewModel.updateEmail(email)
            viewModel.updatePhone(phone)
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
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

            // ---- Save to ViewModel ----
            viewModel.updateFirstName(firstName)
            viewModel.updateLastName(lastName)
            viewModel.updateEmail(email)
            viewModel.updatePhone(phone)

            // ---- DEBUG PRINT ----
            android.util.Log.d(
                "Q2_DEBUG",
                "InteractionLog after Q2 save: ${viewModel.interactionLog.value}"
            )

            viewModel.saveDraft {
                // ---- Navigate to Q3 ----
                findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
            }
        }
    }

    private fun setSkipButton() {
        binding.txtSkip3.setOnClickListener {
            wasSkipped = true
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
            }
        }
    }

    override fun saveCurrentState() {
        isTouched = true
        val firstName = binding.inputFirstName.text.toString().trim()
        val lastName = binding.inputLastName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val phone = binding.inputPhoneNumber.text.toString().trim()
        viewModel.updateFirstName(firstName)
        viewModel.updateLastName(lastName)
        viewModel.updateEmail(email)
        viewModel.updatePhone(phone)
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
        val firstName = binding.inputFirstName.text.toString().trim()
        val lastName = binding.inputLastName.text.toString().trim()
        return firstName.isNotEmpty() && lastName.isNotEmpty()
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
