package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.user.UserSingleton
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.util.isInvalidEmail
import org.brightmindenrichment.street_care.util.isInvalidPhone
import org.brightmindenrichment.street_care.util.isValidEmail
import org.brightmindenrichment.street_care.util.isValidPhone

class InteractionQ2Fragment : BaseILQuestionFragment() {

    private var wasSkipped = false
    private var isTouched = false

    // Content view references
    private lateinit var inputFirstName: EditText
    private lateinit var inputLastName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPhoneNumber: EditText

    override val stepNumber = 2

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.content_il_q2, container, false)
    }

    override fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?) {
        // Get references to content views
        inputFirstName = contentView.findViewById(R.id.input_first_name)
        inputLastName = contentView.findViewById(R.id.input_last_name)
        inputEmail = contentView.findViewById(R.id.input_email)
        inputPhoneNumber = contentView.findViewById(R.id.input_phone_number)

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

            inputFirstName.setText(firstName)
            inputLastName.setText(lastName)
            inputEmail.setText(email)
        } else {
            inputFirstName.setText(log?.firstName.orEmpty())
            inputLastName.setText(log?.lastName.orEmpty())
            inputEmail.setText(log?.email.orEmpty())
            inputPhoneNumber.setText(log?.phoneNumber.orEmpty())
        }

        // Phone InputFilter: prevent non-digit and non-+ characters
        // Length is enforced via android:maxLength="17" in XML (best practice for UI constraints)
        inputPhoneNumber.filters = arrayOf(InputFilter { src, start, end, dest, dstart, _ ->
            for (i in start until end) {
                val c = src[i]
                if (!c.isDigit() && c != '+') return@InputFilter ""
                if (c == '+' && dstart > 0) return@InputFilter ""
            }
            null
        })

        // Email and phone focus-loss validation
        inputEmail.setOnFocusChangeListener { _, hasFocus ->
            val text = inputEmail.text.toString().trim()
            if (!hasFocus && text.isInvalidEmail())
                inputEmail.showFormatError("Enter a valid email (e.g. name@example.com)")
            else if (hasFocus)
                inputEmail.clearFormatError()
        }

        inputPhoneNumber.setOnFocusChangeListener { _, hasFocus ->
            val text = inputPhoneNumber.text.toString().trim()
            if (!hasFocus && text.isInvalidPhone())
                inputPhoneNumber.showFormatError("Format: +12025550123 (7–15 digits after +)")
            else if (hasFocus)
                inputPhoneNumber.clearFormatError()
        }

        // Dynamic error clearing as user types + mark form dirty
        inputFirstName.doAfterTextChanged { markFormDirty() }
        inputLastName.doAfterTextChanged { markFormDirty() }

        inputEmail.doAfterTextChanged { s ->
            if (s.toString().trim().isValidEmail()) inputEmail.clearFormatError()
            markFormDirty()
        }

        inputPhoneNumber.doAfterTextChanged { s ->
            if (s.toString().trim().isValidPhone()) inputPhoneNumber.clearFormatError()
            markFormDirty()
        }
    }

    override fun onNextNavigate() {
        val firstName = inputFirstName.text.toString().trim()
        val lastName = inputLastName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val phone = inputPhoneNumber.text.toString().trim()

        // Next-as-Skip: if both names are empty, delegate to skip logic
        if (firstName.isEmpty() && lastName.isEmpty()) {
            onSkipNavigate()
            return
        }

        // ---- Validation ----
        if (firstName.isEmpty()) {
            inputFirstName.error = "Please enter first name"
            inputFirstName.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            inputLastName.error = "Please enter last name"
            inputLastName.requestFocus()
            return
        }

        // Format validation (only if non-empty)
        if (email.isInvalidEmail()) {
            inputEmail.showFormatError("Enter a valid email (e.g. name@example.com)")
            inputEmail.requestFocus()
            return
        }

        if (phone.isInvalidPhone()) {
            inputPhoneNumber.showFormatError("Format: +12025550123 (7–15 digits after +)")
            inputPhoneNumber.requestFocus()
            return
        }

        // ---- Save to ViewModel ----
        viewModel.updateFirstName(firstName)
        viewModel.updateLastName(lastName)
        viewModel.updateEmail(email)
        viewModel.updatePhone(phone)

        // Mark Q2 as user-edited (they entered data beyond autofill)
        viewModel.updateQ2WasUserEdited(true)

        // ---- Navigate to Q3 ----
        findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
    }

    override fun onSkipNavigate() {
        wasSkipped = true
        saveCurrentState()
        findNavController().navigate(R.id.action_interactionQ2_to_visitForm3)
    }

    override fun saveCurrentState() {
        isTouched = true
        val firstName = inputFirstName.text.toString().trim()
        val lastName = inputLastName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val phone = inputPhoneNumber.text.toString().trim()
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
        val firstName = inputFirstName.text.toString().trim()
        val lastName = inputLastName.text.toString().trim()
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

}
