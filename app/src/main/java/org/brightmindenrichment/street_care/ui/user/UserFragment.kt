package org.brightmindenrichment.street_care.ui.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import org.brightmindenrichment.street_care.R


class UserFragment : Fragment() {
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignUp: Button
    private lateinit var textViewWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser==null) {
            buttonLogin = view.findViewById<Button>(R.id.user_button_login)
            buttonSignUp = view.findViewById<Button>(R.id.user_button_sign_up)
            textViewWelcome = view.findViewById<TextView>(R.id.textViewWelcome)


            buttonLogin.setOnClickListener {
                buttonLoginOnClick()
            }
            buttonSignUp.setOnClickListener {
                buttonSignUpOnClick()
            }

        }
        else{
            if (UserSingleton.userModel.currentUser?.uid != currentUser.uid) {
                UserSingleton.userModel = UserModel(currentUser)
            }
            findNavController().navigate(R.id.action_nav_user_to_nav_profile)
        }


    }
    private fun buttonLoginOnClick() {
        findNavController().navigate(R.id.action_nav_user_to_nav_login)
    }
    private fun buttonSignUpOnClick() {
        findNavController().navigate(R.id.action_nav_user_to_nav_sign_up)
    }

}
