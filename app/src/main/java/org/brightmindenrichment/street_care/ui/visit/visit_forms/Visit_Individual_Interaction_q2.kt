package org.brightmindenrichment.street_care.ui.visit.visit_forms

import androidx.fragment.app.activityViewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class Visit_Individual_Interaction_q2 : Fragment() {

    private val sharedVisitViewModel: VisitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_individual_interaction_q2, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)

        sharedVisitViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
            tvHeader.text = if (idx <= 1) {
                getString(R.string.individual_interaction_title_base)  // e.g. "Individual Interaction"
            } else {
                getString(R.string.individual_interaction_title_numbered, idx) // e.g. "Individual Interaction 2"
            }
        }

        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // Top bar close
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

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

        // Close: exit entire flow
        btnClose.setOnClickListener {
            findNavController().popBackStack(R.id.nav_visit, false)
        }

        // Previous: back to q1
        btnPrevious.setOnClickListener {
            findNavController().navigateUp()
        }

        // Skip: no validation
        btnSkip.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitIndividualInteractionQ2_to_visitIndividualInteractionQ3
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

            findNavController().navigate(
                R.id.action_visitIndividualInteractionQ2_to_visitIndividualInteractionQ3
            )
        }
    }
}
