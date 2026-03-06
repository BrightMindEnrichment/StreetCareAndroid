package org.brightmindenrichment.street_care.ui.visit

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentVisitBinding
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import org.brightmindenrichment.street_care.ui.visit.visit_forms.DetailsButtonClickListener
import org.brightmindenrichment.street_care.ui.visit.visit_forms.VisitLogRecyclerAdapter
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlag
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlagManager

class VisitFormFragment0 : Fragment() {
    private var _binding: FragmentVisitBinding? = null
    val binding get() = _binding!!
    private val viewModel: InteractionLogViewModel by activityViewModels()
    private val visitDataAdapter = VisitDataAdapter()
    private var draftExists = false
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

        binding.btnAddNew.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                if (draftExists) {
                    if (FeatureFlagManager.isEnabled(FeatureFlag.SHOW_IL_DRAFT_RESUME_DIALOG)) {
                        showDraftResumeDialog()
                    } else {
                        viewModel.loadDraft { _ ->
                            findNavController().navigate(R.id.interactionQ1Fragment)
                        }
                    }
                } else {
                    val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val shouldShowDialog = prefs.getBoolean("dont_show_again", false)
                    if (shouldShowDialog) {
                        viewModel.resetInteractionLog {
                            if (isAdded) {
                                findNavController().navigate(R.id.interactionQ1Fragment)
                            }
                        }
                    } else {
                        showCustomDialogPH()
                    }
                }
            } else {
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
                viewModel.resetInteractionLog {
                    if (isAdded) {
                        findNavController().navigate(R.id.interactionQ1Fragment)
                    }
                }
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
                    override fun onClick(visitLog: VisitLog) {
                        val bundle = bundleOf("visitlogId" to visitLog)
                        findNavController().navigate(
                            R.id.action_nav_visit_to_visitLogDetailsFragment, bundle
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
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.profile
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_log_interaction_thanks, null)
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
            viewModel.resetInteractionLog {
                if (isAdded) {
                    findNavController().navigate(R.id.interactionQ1Fragment)
                }
            }
            dialog.dismiss()

        }



        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.80).toInt(), // 85% of screen width
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private fun showDraftResumeDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Resume previous session?")
            .setMessage("You have an unfinished Interaction Log. Would you like to continue where you left off?")
            .setPositiveButton("Continue Draft") { _, _ ->
                viewModel.loadDraft { restored ->
                    if (restored && isAdded) {
                        findNavController().navigate(R.id.interactionQ1Fragment)
                    }
                }
            }
            .setNegativeButton("Start Fresh") { _, _ ->
                viewModel.resetInteractionLog {
                    if (isAdded) {
                        findNavController().navigate(R.id.interactionQ1Fragment)
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val b = _binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            draftExists = viewModel.hasDraft()
            b.btnAddNew.text = if (draftExists) getString(R.string.continue_draft) else getString(R.string.add_new)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



}
