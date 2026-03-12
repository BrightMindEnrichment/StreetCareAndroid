package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel

class InteractionLogFormConsentFragment : Fragment(R.layout.fragment_log_interaction_consent) {

    private val ilViewModel: InteractionLogViewModel by activityViewModels()
    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    // Save previous ActionBar state so we can restore it when leaving this fragment
    private var prevTitle: CharSequence? = null
    private var prevHomeAsUpEnabled: Boolean? = null
    private var prevHomeIndicator: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // needed so fragment can receive onOptionsItemSelected
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2) Configure ActionBar: red close icon + title
        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            // store previous state
            prevTitle = ab.title
            prevHomeAsUpEnabled = ab.isShowing
            prevHomeIndicator = ab.themedContext.let { null }

            ab.setDisplayHomeAsUpEnabled(true)
            ab.title = "Interaction Log"
        }

        val cb = view.findViewById<CheckBox>(R.id.cbConsent)
        val cbContainer = view.findViewById<android.widget.LinearLayout>(R.id.cbConsentContainer)
        val submit = view.findViewById<Button>(R.id.btnSubmitConsent)

        fun setEnabled(b: Boolean) {
            submit.isEnabled = b
            submit.alpha = if (b) 1f else 0.5f
        }

        setEnabled(cb.isChecked)
        cb.setOnCheckedChangeListener { _, checked -> setEnabled(checked) }

        // Make the container clickable to toggle the checkbox
        cbContainer.setOnClickListener {
            cb.isChecked = !cb.isChecked
        }

        submit.setOnClickListener {
            if (!cb.isChecked) return@setOnClickListener
            submit.isEnabled = false

            // Update isPublic based on consent checkbox state
            ilViewModel.updateIsPublic(cb.isChecked)

            ilViewModel.saveWithIIs { success ->
                if (!isAdded) return@saveWithIIs
                if (success) {
                    ilViewModel.resetInteractionLog {
                        if (isAdded) {
                            iiViewModel.reset()
                            findNavController().navigate(
                                R.id.action_consentFragment_to_surveySubmittedFragment
                            )
                        }
                    }
                } else {
                    submit.isEnabled = true
                    Toast.makeText(requireContext(),
                        "Couldn’t submit right now. Please try again.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setHomeAsUpIndicator(null)
            title = prevTitle
        }
    }
}