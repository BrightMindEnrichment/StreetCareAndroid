package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel

class InteractionQ7Fragment : Fragment() {

    private val viewModel: InteractionLogViewModel by activityViewModels()
    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    private var selectedAnswer: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_log_interaction_q7, container, false)
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
        val btnYes = view.findViewById<TextView>(R.id.txt_yes)
        val btnNo = view.findViewById<TextView>(R.id.txt_no)
        val btnSkip = view.findViewById<TextView>(R.id.txt_skip)
        val btnPrevious = view.findViewById<TextView>(R.id.txt_previous5)

        // -----------------------
        // 4. Restore Previous Value
        // -----------------------
        val current = viewModel.interactionLog.value ?: InteractionLog()
        selectedAnswer = current.wantsToProvideDetails

        fun updateUI() {
            if (selectedAnswer == true) {
                btnYes.alpha = 1f
                btnNo.alpha = 0.5f
            } else if (selectedAnswer == false) {
                btnYes.alpha = 0.5f
                btnNo.alpha = 1f
            } else {
                btnYes.alpha = 1f
                btnNo.alpha = 1f
            }
        }

        // -----------------------
        // 5. Button Logic
        // -----------------------
        btnYes.setOnClickListener {
            selectedAnswer = true
            updateUI()
            goToNext()
        }

        btnNo.setOnClickListener {
            selectedAnswer = false
            updateUI()
            goToNext()
        }

        btnSkip.setOnClickListener {
            goToNext()
        }

        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        updateUI()
    }

    // -----------------------
    // Save + Navigate
    // -----------------------
    private fun goToNext() {

        viewModel.updateQ7Answer(selectedAnswer)

        when (selectedAnswer) {
            true -> {
                val hasExisting = !iiViewModel.committedInteractions.value.isNullOrEmpty()
                if (hasExisting) {
                    findNavController().navigate(R.id.action_q7_yes_to_individualInteractionList)
                } else {
                    findNavController().navigate(R.id.action_q7_yes_to_individualInteraction1)
                }
            }
            false -> {
                findNavController().navigate(
                    R.id.action_q7_no_to_consentPage
                )
            }
            null -> {
                // If skipped, decide default behavior
                findNavController().navigate(
                    R.id.action_q7_no_to_consentPage
                )
            }
        }
    }
}