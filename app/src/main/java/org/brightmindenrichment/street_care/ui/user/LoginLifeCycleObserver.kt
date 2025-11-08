package org.brightmindenrichment.street_care.ui.user

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.R


class LoginLifeCycleObserver(
    private val context: Context,
    private val signInListener: SignInListener
) : DefaultLifecycleObserver {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(TAG, "GoogleSignInLifeCycleObserver created")
        auth = FirebaseAuth.getInstance() // MOD
    }

    suspend fun fetchGoogleSignInCredentials() {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
//            .setNonce()
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()


        coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context,
                    )

                    // If credential is found, proceed with sign-in
                    initGoogleSignIn(result)

                } catch (e: NoCredentialException) {
                    // No Google account found on the device
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "No Google account found on this device",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting credential Google", e)
                }
            }
        }

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
                        launchFirebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
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
                    signInListener.onSignInError()

                }
            }
    }

    private fun handleFirebaseLogin(authResult: AuthResult) {
        UserSingleton.userModel = UserModel(currentUser = FirebaseAuth.getInstance().currentUser) // MOD
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

        auth.startActivityForSignInWithProvider(context as Activity, provider.build())
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
