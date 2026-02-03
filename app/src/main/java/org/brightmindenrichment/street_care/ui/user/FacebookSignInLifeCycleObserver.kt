package org.brightmindenrichment.street_care.ui.user

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.util.Log
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.ktx.Firebase


class FacebookSignInLifeCycleObserver(private val registryOwner: ActivityResultRegistryOwner, private val signInListener:SignInListener, private val lifecycle: Lifecycle) : DefaultLifecycleObserver {
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        callbackManager = CallbackManager.Factory.create()


        auth = FirebaseAuth.getInstance()
    }
    fun requestFacebookSignin() {

        LoginManager.getInstance().logInWithReadPermissions(registryOwner,callbackManager,mutableListOf("public_profile","email"))
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(ContentValues.TAG, "facebook:onSuccess:$loginResult")
                    handleFacebookAccessToken(loginResult.accessToken)
                }

                override fun onCancel() {
                    Log.d(ContentValues.TAG, "facebook:onCancel")
                }

                override fun onError(error: FacebookException) {

                    Log.d(ContentValues.TAG, "facebook:onError", error)
                }
            },
        )

    }

    private fun handleFacebookAccessToken(accessToken: AccessToken) {

            Log.d(TAG, "Firebase signInWithCredential:started")
            val credential = FacebookAuthProvider.getCredential(accessToken.token)
            auth.signInWithCredential(credential)
                .addOnCompleteListener() { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            // connect if not connected
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val isNew = task.result.additionalUserInfo!!.isNewUser
                            if(isNew){
                                val userData = Users(currentUser?.displayName.toString(),currentUser?.uid ?: "??",currentUser?.email.toString())
                                val db = FirebaseFirestore.getInstance()

                                db.collection("users").document(currentUser?.uid ?: "??").set(userData).addOnCompleteListener { task ->
                                    if (task.isSuccessful){
                                        Log.d(TAG, "uploading user data to firebase:success")
                                        signInListener.onSignInSuccess()
                                    }
                                    else{
                                        Log.w(TAG, "Error uploading user data to firebase", task.exception)
                                        signInListener.onSignInError()
                                    }
                                }
                            }
                            else{
                                signInListener.onSignInSuccess()
                            }
                        }

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            // connect if not connected
                            signInListener.onSignInError()
                        }

                    }
                }

    }
}