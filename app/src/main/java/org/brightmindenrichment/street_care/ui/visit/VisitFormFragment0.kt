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
import org.brightmindenrichment.street_care.ui.visit.InteractionLogDataAdapter
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.visit_forms.InteractionLogRecyclerAdapter
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlagManager
import org.brightmindenrichment.street_care.ui.visit.visit_forms.InteractionDetailsButtonClickListener

import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale


class VisitFormFragment0 : Fragment() {
    private var _binding: FragmentVisitBinding? = null
    val binding get() = _binding!!

    private val interactionLogDataAdapter = InteractionLogDataAdapter()
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

        interactionLogDataAdapter.refreshAll {
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_visit)

            recyclerView?.layoutManager = LinearLayoutManager(view?.context)

            recyclerView?.adapter = InteractionLogRecyclerAdapter(
                requireContext(),
                interactionLogDataAdapter,
                object : InteractionDetailsButtonClickListener {
                    override fun onClick(interactionLog: InteractionLog) {
                        showInteractionLogDetailsSheet(interactionLog)
                    }
                }
            )

            val totalItemsDonated =
                interactionLogDataAdapter.interactions.sumOf { it.carePackagesDistributed }

            val totalOutreaches =
                interactionLogDataAdapter.size

            val totalPeopleHelped =
                interactionLogDataAdapter.interactions.sumOf { it.numPeopleHelped }

            binding.txtItemDonate.text = totalItemsDonated.toString()
            binding.txtOutreaches.text = totalOutreaches.toString()
            binding.txtPplHelped.text = totalPeopleHelped.toString()
        }

    }

    private fun showInteractionLogDetailsSheet(interactionLog: InteractionLog) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(
            R.layout.bottom_sheet_interaction_log_details,
            null
        )

        sheetView.findViewById<TextView>(R.id.tvInteractionName).text =
            interactionLog.displayName()

        sheetView.findViewById<TextView>(R.id.tvInteractionDate).text =
            interactionLog.formattedDate()

        sheetView.findViewById<TextView>(R.id.tvInteractionCityState).text =
            interactionLog.formattedCityState()

        sheetView.findViewById<TextView>(R.id.tvInteractionPhone).text =
            interactionLog.phoneNumber.ifBlank { "N/A" }

        sheetView.findViewById<TextView>(R.id.tvInteractionTime).text =
            interactionLog.formattedTimeRange()

        sheetView.findViewById<TextView>(R.id.tvInteractionAddress).text =
            interactionLog.formattedAddress()

        sheetView.findViewById<TextView>(R.id.tvInteractionEmail).text =
            interactionLog.email.ifBlank { "N/A" }

        sheetView.findViewById<TextView>(R.id.tvPeopleJoined).text =
            getString(R.string.people_joined_count, interactionLog.numPeopleJoined)

        sheetView.findViewById<TextView>(R.id.tvHelpRequestCount).text =
            getString(R.string.help_request_count_value, interactionLog.helpRequestCount)

        sheetView.findViewById<TextView>(R.id.tvPeopleHelped).text =
            getString(R.string.people_helped_count, interactionLog.numPeopleHelped)

        sheetView.findViewById<TextView>(R.id.tvCarePackagesDistributed).text =
            getString(
                R.string.care_packages_distributed_count,
                interactionLog.carePackagesDistributed
            )

        val chipContainer =
            sheetView.findViewById<FlexboxLayout>(R.id.supportChipContainer)

        chipContainer.removeAllViews()

        val supportItems = interactionLog.listOfSupportsProvided
            .ifEmpty { interactionLog.carePackageContents }

        supportItems.forEach { support ->
            chipContainer.addView(createSupportChip(support))
        }

        sheetView.findViewById<TextView>(R.id.btnCloseInteractionDetails)
            .setOnClickListener {
                bottomSheetDialog.dismiss()
            }

        bottomSheetDialog.setContentView(sheetView)

        bottomSheetDialog.setOnShowListener { dialog ->
            val bottomSheet = (dialog as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.background = null
        }

        bottomSheetDialog.show()
    }

    private fun InteractionLog.displayName(): String {
        return listOf(firstName, lastName)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "N/A" }
    }

    private fun InteractionLog.formattedDate(): String {
        val date = startTimestamp?.toDate() ?: return "N/A"
        return SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
    }

    private fun InteractionLog.formattedTimeRange(): String {
        val start = startTimestamp?.toDate()
        val end = endTimestamp?.toDate()

        if (start == null && end == null) return "N/A"

        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return when {
            start != null && end != null -> "${formatter.format(start)} - ${formatter.format(end)}"
            start != null -> formatter.format(start)
            else -> "N/A"
        }
    }

    private fun InteractionLog.formattedCityState(): String {
        return listOf(city, state)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "N/A" }
    }

    private fun InteractionLog.formattedAddress(): String {
        return listOf(addr1, addr2, city, state, zipcode, country)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "N/A" }
    }

    private fun createSupportChip(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray700))
            textSize = 13f
            setPadding(14.dp(), 6.dp(), 14.dp(), 6.dp())
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_interaction_chip)

            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 8.dp(), 8.dp())
            }
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
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
