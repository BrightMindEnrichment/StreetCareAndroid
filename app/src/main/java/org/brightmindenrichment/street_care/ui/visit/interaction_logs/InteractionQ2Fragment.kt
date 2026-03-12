package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ2Binding
import org.brightmindenrichment.street_care.ui.user.UserSingleton
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.Constants
import org.brightmindenrichment.street_care.util.isInvalidEmail
import org.brightmindenrichment.street_care.util.isInvalidPhone
import org.brightmindenrichment.street_care.util.isValidEmail
import org.brightmindenrichment.street_care.util.isValidPhone

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

        // Phone InputFilter: prevent non-digit and non-+ characters
        // Length is enforced via android:maxLength="17" in XML (best practice for UI constraints)
        binding.inputPhoneNumber.filters = arrayOf(InputFilter { src, start, end, dest, dstart, _ ->
            for (i in start until end) {
                val c = src[i]
                if (!c.isDigit() && c != '+') return@InputFilter ""
                if (c == '+' && dstart > 0) return@InputFilter ""
            }
            null
        })

        // Email and phone focus-loss validation
        binding.inputEmail.setOnFocusChangeListener { _, hasFocus ->
            val text = binding.inputEmail.text.toString().trim()
            if (!hasFocus && text.isInvalidEmail())
                binding.inputEmail.showFormatError("Enter a valid email (e.g. name@example.com)")
            else if (hasFocus)
                binding.inputEmail.clearFormatError()
        }

        binding.inputPhoneNumber.setOnFocusChangeListener { _, hasFocus ->
            val text = binding.inputPhoneNumber.text.toString().trim()
            if (!hasFocus && text.isInvalidPhone())
                binding.inputPhoneNumber.showFormatError("Format: +12025550123 (7–15 digits after +)")
            else if (hasFocus)
                binding.inputPhoneNumber.clearFormatError()
        }

        // Dynamic error clearing as user types
        binding.inputEmail.doAfterTextChanged { s ->
            if (s.toString().trim().isValidEmail()) binding.inputEmail.clearFormatError()
        }

        binding.inputPhoneNumber.doAfterTextChanged { s ->
            if (s.toString().trim().isValidPhone()) binding.inputPhoneNumber.clearFormatError()
        }

        setPreviousButton()
        setNextButton()
        setSkipButton()

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(2)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(Constants.INTERACTION_LOG_DEST_IDS[step - 1], false)
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

            // Next-as-Skip: if both names are empty, delegate to skip logic
            if (firstName.isEmpty() && lastName.isEmpty()) {
                performSkip()
                return@setOnClickListener
            }

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

            // Format validation (only if non-empty)
            if (email.isInvalidEmail()) {
                binding.inputEmail.showFormatError("Enter a valid email (e.g. name@example.com)")
                binding.inputEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isInvalidPhone()) {
                binding.inputPhoneNumber.showFormatError("Format: +12025550123 (7–15 digits after +)")
                binding.inputPhoneNumber.requestFocus()
                return@setOnClickListener
            }

            // ---- Save to ViewModel ----
            viewModel.updateFirstName(firstName)
            viewModel.updateLastName(lastName)
            viewModel.updateEmail(email)
            viewModel.updatePhone(phone)

            // Mark Q2 as user-edited (they entered data beyond autofill)
            viewModel.updateQ2WasUserEdited(true)

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
            performSkip()
        }
    }

    private fun performSkip() {
        wasSkipped = true
        saveCurrentState()
        viewModel.saveDraft {
            findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
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

    private fun EditText.showFormatError(msg: String) {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_rounded_error)
        error = msg
    }

    private fun EditText.clearFormatError() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_rounded)
        error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
