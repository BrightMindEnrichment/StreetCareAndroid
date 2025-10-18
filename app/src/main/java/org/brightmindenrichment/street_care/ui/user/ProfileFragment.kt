package org.brightmindenrichment.street_care.ui.user

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentProfileBinding
import org.brightmindenrichment.street_care.ui.visit.VisitDataAdapter


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val RC_SIGN_IN = 9001

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val currentUser get() = UserSingleton.userModel.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        getUserData()
        binding.txteditprofile.setOnClickListener{
            findNavController().navigate(R.id.action_nav_profile_to_nav_editprofile)
        }
        binding.textbadges.setOnClickListener{
            findNavController().navigate(R.id.action_nav_profile_to_nav_profileBadges)
        }/*
        binding.
        textMyEvents.setOnClickListener{
            findNavController().navigate(R.id.action_nav_profile_to_profileMyEvents)
        }*/
        binding.btnsignout.setOnClickListener{
            buttonSignOutOnClick()
        }
        binding.textDeleteAccount.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.delete_acc_title))
            builder.setMessage(getString(R.string.delete_acc_msg))
                .setCancelable(false)
                .setPositiveButton(getString(
                    R.string.confirm), DialogInterface.OnClickListener { dialog, _ ->
                    deleteAccount()
                }
                )
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
            val alert = builder.create()
            alert.show()
        }

    }

    private fun buttonSignOutOnClick() {
        if (currentUser != null) {
            googleSignOut()
            Firebase.auth.signOut()
            UserSingleton.userModel = UserModel()
            Log.d(TAG, "Firebase user sign out")
            findNavController().popBackStack()
        }
    }

    private fun deleteAccount(){

        currentUser?.let { user ->
            val providerData = currentUser!!.providerData
            var isGoogle = false;
            var isFacebook = false;
            for (userInfo in providerData) {
                val providerId = userInfo.providerId
                Log.d("userInfo.providerId", "userInfo.providerId."+userInfo.providerId)
                if(providerId=="google.com"){
                    isGoogle=true
                    reAuthGoogleAccount()
                }
                if(providerId=="facebook.com"){
                    isFacebook = true
                    val accessToken = AccessToken.getCurrentAccessToken()
                    val credential = FacebookAuthProvider.getCredential(accessToken.toString())
                    // Reauthenticate the user
                    currentUser!!.reauthenticate(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Reauthentication", "User reauthenticated successfully.")
                                deleteFirebaseUserAccount()
                            } else {
                                // Reauthentication failed
                                buttonSignOutOnClick()
                                Toast.makeText(context,
                                    getString(R.string.please_login_again_inorder_to_delete_your_account),Toast.LENGTH_LONG).show()
                                Log.e("Reauthentication", "Failed to reauthenticate user.", task.exception)
                            }
                        }
                }


            }
            if(!isGoogle && !isFacebook){
                deleteFirebaseUserAccount()
            }


        }


    }

    private fun reAuthGoogleAccount() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(requireActivity().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!) // Proceed to Firebase reauth
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    deleteFirebaseUserAccount()
                } else {
                    Log.e(TAG, "Reauthentication failed. ${reauthTask.exception}")
                }
            }
    }

    private fun googleSignOut(){
        val providerData = currentUser!!.providerData
        for (userInfo in providerData) {
            val providerId = userInfo.providerId
            if(providerId=="google.com"){
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(requireActivity().getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)


                googleSignInClient.signOut().addOnCompleteListener(requireActivity(), OnCompleteListener<Void?> {
                    // ...
                    Log.d(TAG, "Google user sign out")

                })
            }
        }
    }

    private fun deleteFirebaseUserAccount(){
        currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "User account deleted.")
                UserSingleton.userModel = UserModel()
                findNavController().popBackStack()
            } else {
                Log.w(TAG, "Problem deleting User account", task.exception)
                if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                    buttonSignOutOnClick()
                    Toast.makeText(
                        context,
                        context?.getString(R.string.please_login_again_inorder_to_delete_your_account),
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Reauthentication", "Failed to reauthenticate user.", task.exception)
                }

            }
        }
    }
    private fun getUserData() {
        val userModel = UserSingleton.userModel
        Log.d(TAG, "getUserData :: userName: ${userModel.userName}, imageUri: ${userModel.imageUri}")
        binding.txtprofileusername.text = userModel.userName ?: userModel.currentUser?.displayName.toString()
        Picasso.get().load(userModel.imageUri).into(binding.profileimageview)
        val visitDataAdapter = VisitDataAdapter()
        visitDataAdapter.refreshAll {
            var totalItemsDonated = visitDataAdapter.getTotalItemsDonated
            var totalOutreaches = visitDataAdapter.size
            var totalPeopleHelped = visitDataAdapter.getTotalPeopleCount
            binding.cardImpactLayout.itemsDonated.text = totalItemsDonated.toString()
            binding.cardImpactLayout.totalOutreaches.text = totalOutreaches.toString()
            binding.cardImpactLayout.textHelped.text = totalPeopleHelped.toString()
        }
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}