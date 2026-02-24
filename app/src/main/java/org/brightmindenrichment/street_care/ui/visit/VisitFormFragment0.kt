package org.brightmindenrichment.street_care.ui.visit

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentVisitBinding
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import org.brightmindenrichment.street_care.ui.visit.visit_forms.DetailsButtonClickListener
import org.brightmindenrichment.street_care.ui.visit.visit_forms.VisitLogRecyclerAdapter
import org.brightmindenrichment.street_care.ui.visit.visit_forms.VisitViewModel
import org.brightmindenrichment.street_care.util.Extensions

class VisitFormFragment0 : Fragment() {
    private var _binding: FragmentVisitBinding? = null
    val binding get() = _binding!!
    private val sharedVisitViewModel: VisitViewModel by activityViewModels()
    private val visitDataAdapter = VisitDataAdapter()
    companion object {
        fun newInstance() = VisitFormFragment0()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVisitBinding.inflate(inflater, container, false)
        return _binding!!.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.btnAddNew.setOnClickListener {
            // if user is submitting multiple visit log together, the view model field should reset

            if(FirebaseAuth.getInstance().currentUser != null) {
                // showImpactDialog(requireContext())
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val shouldShowDialog = prefs.getBoolean("dont_show_again", false)
                if(shouldShowDialog){
                    sharedVisitViewModel.resetVisitLogPage()
                    findNavController().navigate(R.id.action_nav_visit_to_visitFormFragment2)
                }else{
                    showCustomDialogPH()
                }


            } else{
                /*  Extensions.showDialog(
                      requireContext(), requireContext().getString(R.string.alert), requireContext().getString(R.string.visit_log_can_be_recorded_by_logged_in_users),
                      requireContext().getString(R.string.ok),
                      requireContext().getString(R.string.cancel))*/
                showCustomDialog()
            }

        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            binding.historyMsg.visibility = View.GONE
            updateUI()
        } else {
            Log.d("BME", "not logged in")
        }




    }


    fun showImpactDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("I provided help!")
            .setMessage("Please fill out this form each time you perform an outreach. This helps you track your contributions and allows StreetCare to bring more support and services to help the community!")
            .setPositiveButton("OK") { dialog, _ ->
                sharedVisitViewModel.resetVisitLogPage()
                findNavController().navigate(R.id.action_nav_visit_to_visitFormFragment1)
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun updateUI() {
        visitDataAdapter.refreshAll {
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_visit)
            recyclerView?.layoutManager = LinearLayoutManager(view?.context)
            recyclerView?.adapter = VisitLogRecyclerAdapter(
                requireContext(),
                visitDataAdapter,
                object : DetailsButtonClickListener {
                    override fun onClick(visitLog:VisitLog) {
                        val bundle = bundleOf("visitLog" to visitLog)
                        findNavController().navigate(
                            R.id.action_nav_visit_to_visitLogDetailsFragment,bundle
                        )
                    }
                })
            val totalItemsDonated = visitDataAdapter.getTotalItemsDonated
            val totalOutreaches = visitDataAdapter.size
            val totalPeopleHelped = visitDataAdapter.getTotalPeopleCount


            binding.txtItemDonate.text = totalItemsDonated.toString()
            binding.txtOutreaches.text = totalOutreaches.toString()
            binding.txtPplHelped.text = totalPeopleHelped.toString()
        }


    }
    fun showCustomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login_2, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // This removes the black border and makes corners visible
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        val btnOK = dialogView.findViewById<TextView>(R.id.ok_btn)
        val btnCancel = dialogView.findViewById<TextView>(R.id.cancel_btn)

        btnOK.setOnClickListener {
            val bundle = Bundle().apply {
                putString("from", "nav_visit")
            }
            // findNavController().navigate(R.id.action_nav_visit_to_profile)
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.profile

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.profile, true)
                .build()


            findNavController().navigate(R.id.action_nav_user_to_nav_login,bundle,navOptions)


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

    fun showCustomDialogPH() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_provided_help, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // This removes the black border and makes corners visible
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        val btnOK = dialogView.findViewById<TextView>(R.id.ok_btn)
//        val checkBox = dialogView.findViewById<CheckBox>(R.id.cbDontShowAgain)


        btnOK.setOnClickListener {
//            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//            if (checkBox.isChecked) {
//
//                prefs.edit().putBoolean("dont_show_again", true).apply()
//            }
            sharedVisitViewModel.resetVisitLogPage()

            findNavController().navigate(R.id.action_nav_visit_to_visitFormFragment2)
            dialog.dismiss()

        }



        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.80).toInt(), // 85% of screen width
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



}