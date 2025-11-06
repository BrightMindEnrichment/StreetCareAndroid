package org.brightmindenrichment.street_care.ui.visit


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.visit_forms.ThanksForHelpingDialog

class ConsentFragment : Fragment(R.layout.fragment_consent) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // frontend: show the popup when this screen opens
        ThanksForHelpingDialog().show(childFragmentManager, "thanks")

        val cb = view.findViewById<CheckBox>(R.id.cbConsent)
        view.findViewById<Button>(R.id.btnSubmitConsent).setOnClickListener {
            if (cb.isChecked) {
                findNavController().navigate(R.id.action_consentFragment_to_successFragment)
            }
            // else do nothing (frontend only)
        }
    }
}

