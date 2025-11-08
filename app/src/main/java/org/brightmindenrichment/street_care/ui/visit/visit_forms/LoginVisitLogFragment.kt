package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
// import com.google.firebase.auth.auth MOD
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLoginVisitLogBinding


class LoginVisitLogFragment : Fragment() {
    private var _binding: FragmentLoginVisitLogBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginVisitLogBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        binding.guestBtn.setOnClickListener {
                 showCustomDialog()
        }
        binding.userButtonLogin.setOnClickListener {
            val bundle = Bundle().apply {
                putString("from", "nav_visit")
            }
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.profile
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginRedirectFragment, false)  // This will clear everything up to the Profile fragment in the back stack
                .build()


            findNavController().navigate(R.id.action_nav_user_to_nav_login,bundle,navOptions)

        }
    }

    fun showCustomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_login, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // This removes the black border and makes corners visible
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        val btnOK = dialogView.findViewById<TextView>(R.id.ok_btn)
        val btnCancel = dialogView.findViewById<TextView>(R.id.cancel_btn)

        btnOK.setOnClickListener {
            findNavController().navigate(R.id.action_loginVisitLogFragment_to_nav_visit)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            // Perform your action

            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.75).toInt(), // 85% of screen width
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }


}