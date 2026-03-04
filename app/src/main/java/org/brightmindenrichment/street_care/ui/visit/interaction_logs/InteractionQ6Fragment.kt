package org.brightmindenrichment.street_care.ui.visit.interaction_logs


import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog

class InteractionQ6Fragment : Fragment() {

    private val viewModel: InteractionLogViewModel by activityViewModels()
    private var carePackageCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_log_interaction_q6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----------------------
        // 1. Show Bottom Navigation
        // -----------------------
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // -----------------------
        // 3. Initialize Views
        // -----------------------
        val btnSkip = view.findViewById<TextView>(R.id.btn_skip)
        val btnPrevious = view.findViewById<TextView>(R.id.btn_previous)
        val btnNext = view.findViewById<TextView>(R.id.btn_next)

        val btnIncrease = view.findViewById<FrameLayout>(R.id.btn_increase)
        val btnDecrease = view.findViewById<FrameLayout>(R.id.btn_decrease)

        val tvCount = view.findViewById<TextView>(R.id.tv_count)
        val etNotes = view.findViewById<EditText>(R.id.et_notes)

        // -----------------------
        // 4. Restore Previous Values
        // -----------------------
        val current = viewModel.interactionLog.value ?: InteractionLog()

        carePackageCount = current.carePackagesDistributed
        tvCount.text = carePackageCount.toString()
        etNotes.setText(current.carePackageContents)

        fun updateUI() {
            tvCount.text = carePackageCount.toString()
        }

        // -----------------------
        // 5. Counter Logic
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
        // 6. Previous Button
        // -----------------------
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        // -----------------------
        // 7. Skip Button
        // -----------------------
        btnSkip.setOnClickListener {
            goToNext(etNotes.text.toString())
        }

        // -----------------------
        // 8. Next Button
        // -----------------------
        btnNext.setOnClickListener {
            goToNext(etNotes.text.toString())
        }

        updateUI()
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

    // -----------------------
    // Save Data + Navigate
    // -----------------------
    private fun goToNext(notes: String) {

        viewModel.updateCarePackage(carePackageCount, notes)

        findNavController().navigate(
            R.id.action_q6_to_q7
        )
    }
}
