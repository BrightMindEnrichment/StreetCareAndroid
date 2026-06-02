package org.brightmindenrichment.street_care.ui.user

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {
    companion object {
        private const val TAG = "LoginFragment"
        private const val ROUTE_DIAG_REV = "login-route-2026-05-27-1729"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var loginObserver: LoginLifeCycleObserver
    private lateinit var bottomNavigationView: BottomNavigationView

    private val legacyGoogleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loginObserver.handleLegacyGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LoginFragment diagnostic revision=$ROUTE_DIAG_REV")

        val signInListener = object : SignInListener {
            override fun onSignInSuccess() {
                Log.d(TAG, "Firebase user signIn success")
                routeAfterSuccessfulSignIn()
            }

            override fun onSignInError() {
                Log.e(TAG, "Firebase user signIn fail")
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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav)

        binding.editTextTextEmailAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { 
                val email = binding.editTextTextEmailAddress.text.toString().trim()
                when {
                    email.isBlank() -> binding.txtemail.error = "Email is mandatory"
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.txtemail.error = "Enter a valid email address"
                }
            }
        }

        binding.editTextTextEmailAddress.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.txtemail.isErrorEnabled = false
            }
        }

        binding.editTextTextPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { 
                val password = binding.editTextTextPassword.text.toString()
                if (password.isBlank()) {
                    binding.txtpassword.error = "Password is mandatory"
                }
            }
        }

        binding.editTextTextPassword.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.txtpassword.isErrorEnabled = false
            }
        }

        val buttonLogin = view.findViewById<Button>(R.id.loginButton)
        buttonLogin.setOnClickListener {
            var email = binding.editTextTextEmailAddress.text.toString()
            var password = binding.editTextTextPassword.text.toString()
            if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password))
            {
                binding.editTextTextEmailAddress.setError(getString(R.string.mandatory))
                binding.editTextTextPassword.setError(getString(R.string.mandatory))
            }   else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.editTextTextEmailAddress.setError(getString(R.string.enter_valid_email_address))
            }else {
                disableUI(true)
                auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            UserSingleton.userModel = UserRepository().fetchUserData()
                            Log.d(TAG, "getUserData :: userName: ${UserSingleton.userModel}, imageUri: ${UserSingleton.userModel}")
                            withContext(Dispatchers.Main) {
                                disableUI(false)
                                routeAfterSuccessfulSignIn()
                            }
                        }
                    } else {
                        disableUI(false)
                        Toast.makeText(
                            activity,
                            getString(R.string.error_login_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.txtforget.setOnClickListener {
            findNavController().navigate(R.id.action_nav_login_to_nav_forgetPass)
        }

        /*
        *Commented out for final bug fixed version 2. Will be uncommented in version 3  when 3rd party authentication is enabled.
        *
            binding.layoutsiginmethod.cardFacebook.setOnClickListener {
                fbObserver.requestFacebookSignin()
            }
            binding.layoutsiginmethod.cardTwitter.setOnClickListener {
                twitterObserver.requestTwitterSignIn()
            }
        *
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
            lifecycleScope.launch(Dispatchers.IO) {
                loginObserver.launchTwitterXSignIn()
            }
        }
        */
    }

    private fun disableUI(disableUI: Boolean) {
        binding.loadingOverlay.visibility = if (disableUI) View.VISIBLE else View.GONE
        bottomNavigationView.isEnabled = !disableUI
    }

    private fun updateUI() {
        Toast.makeText(
            requireContext(),
            getString(R.string.successfully_login),
            Toast.LENGTH_SHORT
        ).show()
        _binding?.editTextTextEmailAddress?.text?.clear()
        _binding?.editTextTextPassword?.text?.clear()
    }

    private fun routeAfterSuccessfulSignIn() {
        try {
            val currentDestinationId = findNavController().currentDestination?.id
            Log.d(
                TAG,
                "routeAfterSuccessfulSignIn start: rev=$ROUTE_DIAG_REV, " +
                    "from=${arguments?.getString("from")}, currentDestinationId=$currentDestinationId"
            )
            updateUI()
            Log.d(TAG, "routeAfterSuccessfulSignIn after updateUI")
            val fromVisit = arguments?.getString("from") == "nav_visit"
            val actionId = if (fromVisit) {
                R.id.action_global_login_success_to_nav_visit
            } else {
                R.id.action_global_login_success_to_nav_user
            }
            Log.d(
                TAG,
                "Scheduling post-login navigation: fromVisit=$fromVisit, " +
                    "actionId=$actionId"
            )
            _binding?.root?.post {
                try {
                    if (!isAdded) {
                        Log.w(TAG, "Skipping post-login navigation because fragment is detached")
                        return@post
                    }
                    val navOptions = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.nav_login, true)
                        .build()
                    Log.d(TAG, "Issuing delayed navigate() using actionId=$actionId")
                    findNavController().navigate(actionId, null, navOptions)
                    Log.d(TAG, "Navigation requested using actionId=$actionId")
                } catch (e: Exception) {
                    Log.e(TAG, "Delayed post-login navigation failed", e)
                    signInFallbackToast()
                }
            } ?: run {
                Log.e(TAG, "Cannot navigate after sign-in because binding is null")
                signInFallbackToast()
            }
        } catch (e: Exception) {
            Log.e(TAG, "routeAfterSuccessfulSignIn failed", e)
            signInFallbackToast()
        }
    }

    private fun resourceName(id: Int?): String {
        if (id == null) return "null"
        return runCatching { resources.getResourceEntryName(id) }
            .getOrElse { id.toString() }
    }

    private fun signInFallbackToast() {
        activity?.let {
            Toast.makeText(
                it,
                getString(R.string.error_login_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
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
