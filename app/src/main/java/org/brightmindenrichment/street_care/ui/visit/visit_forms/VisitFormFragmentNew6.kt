package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class VisitFormFragmentNew6 : Fragment() {

    private val sharedVisitViewModel: VisitViewModel by activityViewModels()

    private var carePackageCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_visit_form_new6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        val btnClose = view.findViewById<FrameLayout>(R.id.btn_close)
        val btnSkip = view.findViewById<TextView>(R.id.btn_skip)
        val btnPrevious = view.findViewById<TextView>(R.id.btn_previous)
        val btnNext = view.findViewById<TextView>(R.id.btn_next)

        val btnIncrease = view.findViewById<FrameLayout>(R.id.btn_increase)
        val btnDecrease = view.findViewById<FrameLayout>(R.id.btn_decrease)

        val tvCount = view.findViewById<TextView>(R.id.tv_count)
        val etNotes = view.findViewById<EditText>(R.id.et_notes)

        // Restore previous value if exists
        carePackageCount = sharedVisitViewModel.visitLog.carePackagesGiven ?: 0
        tvCount.text = carePackageCount.toString()
        etNotes.setText(sharedVisitViewModel.visitLog.carePackageNotes ?: "")

        fun updateUI() {
            tvCount.text = carePackageCount.toString()
        }

        // -----------------------
        // Counter Logic
        // -----------------------
        btnIncrease.setOnClickListener {
            carePackageCount++
            updateUI()
        }

        btnDecrease.setOnClickListener {
            if (carePackageCount > 0) {
                carePackageCount--
                updateUI()
            }
        }

        // -----------------------
        // Close Button
        // -----------------------
        btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // -----------------------
        // Previous Button
        // -----------------------
        btnPrevious.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragmentNew6_to_visitFormFragmentNew5
            )
        }

        // -----------------------
        // Skip Button
        // -----------------------
        btnSkip.setOnClickListener {
            goToNext(etNotes.text.toString())
        }

        // -----------------------
        // Next Button
        // -----------------------
        btnNext.setOnClickListener {
            goToNext(etNotes.text.toString())
        }

        updateUI()
    }

    private fun goToNext(notes: String) {

        // Save into ViewModel
        sharedVisitViewModel.visitLog.carePackagesGiven = carePackageCount
        sharedVisitViewModel.visitLog.carePackageNotes = notes

        // Navigate forward
        findNavController().navigate(
            R.id.action_visitFormFragmentNew6_to_visitForm7a
        )
    }
}
