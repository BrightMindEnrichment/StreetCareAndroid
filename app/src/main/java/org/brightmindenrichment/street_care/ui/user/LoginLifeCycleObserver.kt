package org.brightmindenrichment.street_care.ui.user

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.BuildConfig
import org.brightmindenrichment.street_care.R


class LoginLifeCycleObserver(
    private val activity: Activity,
    private val signInListener: SignInListener,
    private val launchLegacyGoogleSignIn: (() -> Unit)? = null
) : DefaultLifecycleObserver {
    companion object {
        private const val TAG = "LoginLifeCycleObserver"
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(TAG, "GoogleSignInLifeCycleObserver created")
        auth = FirebaseAuth.getInstance()
    }

    suspend fun fetchGoogleSignInCredentials() {
        val credentialManager = CredentialManager.create(activity)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        )
//            .setNonce()
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()


        try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            // If credential is found, proceed with sign-in
            Log.d(TAG, "Credential Manager returned credential type=${result.credential.type}")
            initGoogleSignIn(result)

        } catch (e: NoCredentialException) {
            Log.w(
                TAG,
                "No Google credential available for Sign in with Google. " +
                    "applicationId=${BuildConfig.APPLICATION_ID}, " +
                    "webClientId=${activity.getString(R.string.default_web_client_id)}",
                e
            )
            withContext(Dispatchers.Main) {
                if (launchLegacyGoogleSignIn == null) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.error_google_sign_in_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                }
                launchLegacyGoogleSignIn?.invoke()
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error getting credential Google. " +
                    "applicationId=${BuildConfig.APPLICATION_ID}, " +
                    "webClientId=${activity.getString(R.string.default_web_client_id)}, " +
                    "exception=${e::class.java.name}, message=${e.message}",
                e
            )
            if (shouldUseLegacyGoogleSignIn(e)) {
                withContext(Dispatchers.Main) {
                    launchLegacyGoogleSignIn?.invoke() ?: signInListener.onSignInError()
                }
            } else {
                signInListener.onSignInError()
            }
        }
    }

    fun getLegacyGoogleSignInIntent(): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, signInOptions).signInIntent
    }

    fun handleLegacyGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Log.e(TAG, "Legacy Google sign in returned a null ID token")
                signInListener.onSignInError()
                return
            }
            Log.d(TAG, "Legacy Google sign in returned ID token")
            launchFirebaseAuthWithGoogle(idToken)
        } catch (e: ApiException) {
            Log.e(TAG, "Legacy Google sign in failed with statusCode=${e.statusCode}", e)
            signInListener.onSignInError()
        }
    }

    private fun shouldUseLegacyGoogleSignIn(exception: Exception): Boolean {
        val message = exception.message.orEmpty()
        return launchLegacyGoogleSignIn != null &&
            (message.contains("Account reauth failed", ignoreCase = true) ||
                message.contains("[16]"))
    }

    private fun initGoogleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            // GoogleIdToken credential
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        Log.d(TAG, "Parsed Google ID token credential")
                        launchFirebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        signInListener.onSignInError()
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected custom credential type=${credential.type}")
                    signInListener.onSignInError()
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected credential class=${credential::class.java.name}")
                signInListener.onSignInError()
            }
        }
    }

    private fun launchFirebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    handleFirebaseLogin(task.result)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.e(TAG, "Google firebase login fail", task.exception)
                    Log.e(TAG, "Google firebase login fail message=${task.exception?.message}")
                    signInListener.onSignInError()

                }
            }
    }

    private fun handleFirebaseLogin(authResult: AuthResult) {
        UserSingleton.userModel = UserModel(currentUser = FirebaseAuth.getInstance().currentUser)
        val isNew = authResult.additionalUserInfo!!.isNewUser
        if (isNew) {
            setFirebaseNewUser(UserSingleton.userModel.currentUser)
        } else {
            signInListener.onSignInSuccess()
        }
    }

    private fun setFirebaseNewUser(currentUser: FirebaseUser?) {
        Log.d(TAG, "uploading user data to firebase:success " + currentUser?.email.toString())
        val userData = Users(
            currentUser?.displayName.toString(),
            currentUser?.uid ?: "??",
            currentUser?.email.toString()
        )
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser?.uid ?: "??").set(userData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "uploading user data to firebase:success")
                    signInListener.onSignInSuccess()
                } else {
                    Log.e(
                        TAG,
                        "Error uploading user data to firebase",
                        task.exception
                    )
                    signInListener.onSignInError()
                }
            }
    }

    fun launchTwitterXSignIn() {
        val provider = OAuthProvider.newBuilder("twitter.com")
//        TODO: spanish localization
//        provider.addCustomParameter("lang", "es")

        auth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "Twitter sign in success from start Activity")
                if (authResult.additionalUserInfo?.isNewUser == true) {
                    handleFirebaseLogin(authResult)
                } else {
                    signInListener.onSignInSuccess()
                }
            }
            .addOnFailureListener {
                signInListener.onSignInError()
                Log.e(TAG, "Twitter sign in fail from start Activity", it)
            }
    }
}
