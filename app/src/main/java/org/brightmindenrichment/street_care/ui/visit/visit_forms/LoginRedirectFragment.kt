package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLoginRedirectBinding
import org.brightmindenrichment.street_care.databinding.FragmentLoginVisitLogBinding


class LoginRedirectFragment : Fragment() {
    private var _binding: FragmentLoginRedirectBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController()

        if (FirebaseAuth.getInstance().currentUser != null) {
           navController.navigate( R.id.nav_visit)
        } else {
           navController.navigate( R.id.loginVisitLogFragment)
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginRedirectBinding.inflate(inflater, container, false)
        return _binding!!.root
    }


}