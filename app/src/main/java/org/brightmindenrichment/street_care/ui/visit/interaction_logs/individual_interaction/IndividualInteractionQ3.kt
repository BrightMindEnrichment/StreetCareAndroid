package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import androidx.fragment.app.activityViewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel

class IndividualInteractionQ3 : Fragment() {

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_individual_interaction_q3, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
            ab.title = "Individual Interaction"
        }

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)

        interactionLogViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
            tvHeader.text = if (idx <= 1) {
                getString(R.string.individual_interaction_title_base)
            } else {
                getString(R.string.individual_interaction_title_numbered, idx)
            }
        }

        // Top bar close
        //val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        // Buttons
        val btnPrevious = view.findViewById<TextView>(R.id.txt_previous2)
        val btnNext = view.findViewById<TextView>(R.id.txt_next2)
        val btnSkip = view.findViewById<TextView>(R.id.txt_skip)

        // Inputs
        val cbFood = view.findViewById<AppCompatCheckBox>(R.id.cb_food)
        val cbClothes = view.findViewById<AppCompatCheckBox>(R.id.cb_clothes)
        val cbHygiene = view.findViewById<AppCompatCheckBox>(R.id.cb_hygiene)
        val cbWellness = view.findViewById<AppCompatCheckBox>(R.id.cb_wellness)
        val cbMedical = view.findViewById<AppCompatCheckBox>(R.id.cb_medical)
        val cbSocialWorker = view.findViewById<AppCompatCheckBox>(R.id.cb_social_worker)
        val cbLegal = view.findViewById<AppCompatCheckBox>(R.id.cb_legal)
        val cbOther = view.findViewById<AppCompatCheckBox>(R.id.cb_other)
        val etOther = view.findViewById<TextInputEditText>(R.id.etOther)

        // Enable/disable Other text
        fun refreshOtherEnabled() {
            val enabled = cbOther.isChecked
            etOther.isEnabled = enabled
            if (!enabled) etOther.setText("")
        }
        cbOther.setOnCheckedChangeListener { _, _ -> refreshOtherEnabled() }
        refreshOtherEnabled()

        // Restore previously selected further-help items when navigating back
        viewModel.currentInteraction.value?.furtherHelpNeeded?.forEach { item ->
            when (item) {
                "Food" -> cbFood.isChecked = true
                "Clothes" -> cbClothes.isChecked = true
                "Hygiene" -> cbHygiene.isChecked = true
                "Wellness" -> cbWellness.isChecked = true
                "Medical" -> cbMedical.isChecked = true
                "Social Worker" -> cbSocialWorker.isChecked = true
                "Legal" -> cbLegal.isChecked = true
                else -> { cbOther.isChecked = true; etOther.setText(item) }
            }
        }
        refreshOtherEnabled()

        // Helper to build the selected list
        fun collectFurtherHelp(): List<String> {
            val list = mutableListOf<String>()
            if (cbFood.isChecked) list.add("Food")
            if (cbClothes.isChecked) list.add("Clothes")
            if (cbHygiene.isChecked) list.add("Hygiene")
            if (cbWellness.isChecked) list.add("Wellness")
            if (cbMedical.isChecked) list.add("Medical")
            if (cbSocialWorker.isChecked) list.add("Social Worker")
            if (cbLegal.isChecked) list.add("Legal")
            if (cbOther.isChecked) etOther.text?.toString()?.trim()
                ?.takeUnless { it.isEmpty() }?.let { list.add(it) }
            return list
        }

        // Previous: back to q2
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        // Skip: save empty and proceed
        btnSkip.setOnClickListener {
            viewModel.saveQ3(emptyList())
            findNavController().navigate(
                R.id.action_individualInteractionQ3_to_individualInteractionQ4
            )
        }

        // Next: must select at least one
        btnNext.setOnClickListener {
            val anyChecked = cbFood.isChecked || cbClothes.isChecked || cbHygiene.isChecked ||
                    cbWellness.isChecked || cbMedical.isChecked || cbSocialWorker.isChecked ||
                    cbLegal.isChecked || cbOther.isChecked

            if (!anyChecked) {
                Toast.makeText(requireContext(), "Select at least one option.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cbOther.isChecked && etOther.text?.toString()?.trim().isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please specify Other.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveQ3(collectFurtherHelp())
            findNavController().navigate(
                R.id.action_individualInteractionQ3_to_individualInteractionQ4
            )
        }
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onPause() {
        super.onPause()
        @Suppress("DEPRECATION")
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)
    }
}
