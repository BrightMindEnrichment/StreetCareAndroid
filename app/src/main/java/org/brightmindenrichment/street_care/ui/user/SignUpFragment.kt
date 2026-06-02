package org.brightmindenrichment.street_care.ui.user

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentSignUpBinding
import java.util.*


class SignUpFragment : Fragment() {
    companion object {
        private const val TAG = "SignUpFragment"
    }

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private var userName: String = ""
    private var email: String = ""
    private var password: String = ""
    private var company: String = ""
    lateinit var loginObserver: LoginLifeCycleObserver

    private val legacyGoogleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loginObserver.handleLegacyGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val signInListener = object : SignInListener {
            override fun onSignInSuccess() {
                Log.d(TAG, "Firebase user signin success")
                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                if (bottomNav.selectedItemId == R.id.profile) {
                    findNavController().navigate(R.id.nav_user)
                } else {
                    bottomNav.selectedItemId = R.id.profile
                }
            }

            override fun onSignInError() {
                Log.e(TAG, "Firebase user signin fail")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_login_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        loginObserver = LoginLifeCycleObserver(requireActivity(), signInListener) {
            legacyGoogleSignInLauncher.launch(loginObserver.getLegacyGoogleSignInIntent())
        }
        lifecycle.addObserver(loginObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextSignUpUserName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { 
                val name = binding.editTextSignUpUserName.text.toString().trim()
                if (name.isBlank()) {
                    binding.TextSignUpUserName.error = "Username is mandatory"
                }
            }
        }

        binding.editTextSignUpUserName.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.TextSignUpUserName.isErrorEnabled = false
            }
        }

        binding.editTextSignUpEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { 
                val email = binding.editTextSignUpEmail.text.toString().trim()
                when{
                    email.isBlank() -> {
                        binding.TextSignUpEmail.error = "Email is mandatory"
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        binding.TextSignUpEmail.error = "Enter a valid email address"
                    }
                }
            }
        }

        binding.editTextSignUpEmail.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.TextSignUpEmail.isErrorEnabled = false
            }
        }

        binding.editTextSignUpPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { 
                val passwd = binding.editTextSignUpPassword.text.toString().trim()
                if (passwd.isBlank()) {
                    binding.TextSignUpPassword.error = "Password is mandatory"
                }
            }
        }

        binding.editTextSignUpPassword.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.TextSignUpPassword.isErrorEnabled = false
            }
        }

        binding.buttonSignUpSignUp.setOnClickListener {
            userName = binding.editTextSignUpUserName.text.toString()
            email = binding.editTextSignUpEmail.text.toString()
            password = binding.editTextSignUpPassword.text.toString()
            company = binding.editTextSignUpCompany.text.toString()
            if (TextUtils.isEmpty(userName)) {
                binding.editTextSignUpUserName.setError(getString(R.string.mandatory))
            } else if (TextUtils.isEmpty(email)  ) {
                binding.editTextSignUpEmail.setError(getString(R.string.mandatory))
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.editTextSignUpEmail.setError(getString(R.string.enter_valid_email_address))
            } else if (TextUtils.isEmpty(password)) {
                binding.editTextSignUpPassword.setError(getString(R.string.mandatory))
            } else {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val userData = hashMapOf<String, Any>(
                                "dateCreated" to Date(),
                                "deviceType" to "Android",
                                "email" to email,
                                "isValid" to true,
                                "organization" to company,
                                "username" to userName,
                                "uid" to (currentUser?.uid ?: "??")
                            )
                            UserSingleton.userModel = UserModel(currentUser).apply {
                                userName = userData["username"].toString()
                                email = userData["email"].toString()
                            }
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(currentUser?.uid ?: "??").set(userData).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "uploading user data to firebase:success")
                                }
                                Toast.makeText(activity,
                                    getString(R.string.successfully_register), Toast.LENGTH_SHORT).show();
                                findNavController().navigateUp()
                                binding.editTextSignUpCompany.text?.clear()
                                binding.editTextSignUpEmail.text?.clear()
                                binding.editTextSignUpPassword.text?.clear()
                                binding.editTextSignUpUserName.text?.clear()
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                getString(R.string.error_failed_to_create_user),
                                Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            }
        }

        /*
        *Commented out for final bug fixed version 2. Will be uncommented in version 3  when 3rd party authentication is enabled.
        *
        *
        binding.layoutsiginmethod.cardFacebook.setOnClickListener {
            fbObserver.requestFacebookSignin()
        }

        */

        binding.layoutsiginmethod.cardGoogle.setOnClickListener {
            lifecycleScope.launch {
                loginObserver.fetchGoogleSignInCredentials()
            }
        }
        /*
        *Commenting out twitter button for new release. Will be uncommented once the token issue is fixed
        *
        binding.layoutsiginmethod.cardTwitter.setOnClickListener {
            loginObserver.launchTwitterXSignIn()
       }
        */

    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the observer when the Fragment is destroyed
        lifecycle.removeObserver(loginObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
