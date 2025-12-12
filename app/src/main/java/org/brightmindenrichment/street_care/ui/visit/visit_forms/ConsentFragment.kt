package org.brightmindenrichment.street_care.ui.visit.visit_forms


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.visit_forms.ThanksForHelpingDialog
import android.widget.ImageView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue
import android.widget.Toast
import com.google.firebase.auth.auth
import androidx.core.os.bundleOf

class ConsentFragment : Fragment(R.layout.fragment_consent) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cb = view.findViewById<CheckBox>(R.id.cbConsent)
        val submit = view.findViewById<Button>(R.id.btnSubmitConsent)
        val close = view.findViewById<ImageView>(R.id.btn_close)
        // args from nav graph (defaults: interactionId="", mode="create")
        val interactionId = arguments?.getString("interactionId").orEmpty()
        val mode = arguments?.getString("mode") ?: "create"

        fun setEnabled(b: Boolean) {
            submit.isEnabled = b
            submit.alpha = if (b) 1f else 0.5f
        }
        setEnabled(cb.isChecked)
        cb.setOnCheckedChangeListener { _, checked -> setEnabled(checked) }

        submit.setOnClickListener {
            if (!cb.isChecked) return@setOnClickListener

            val db = Firebase.firestore
            val now = FieldValue.serverTimestamp()

            submit.isEnabled = false

            if (mode == "publish" && interactionId.isNotBlank()) {
                // UPDATE existing interaction: make public (and bump timestamp)
                db.collection("interactions")
                    .document(interactionId)
                    .update(
                        mapOf(
                            "isPublic" to true,
                            // keep status Pending if moderation exists; otherwise use Published
                            "status" to "Pending",
                            "lastModifiedTimestamp" to now
                        )
                    )
                    .addOnSuccessListener {
                        // back to Interaction Log home
                        findNavController().navigate(
                            R.id.action_consentFragment_to_successFragment,
                            bundleOf("interactionId" to interactionId)
                        )                    }
                    .addOnFailureListener {
                        submit.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to share. Please try again.", Toast.LENGTH_LONG).show()
                    }

            } else {
                // CREATE a new interaction (original flow)
                val interaction = hashMapOf(
                    "isPublic" to cb.isChecked,
                    "status" to "Pending",
                    "createdTimestamp" to now,
                    "lastModifiedTimestamp" to now,
                    "helpRequestCount" to 0,
                    "helpRequestDocIds" to emptyList<String>(),
                    "userId" to (Firebase.auth.currentUser?.uid ?: "")
                )

                db.collection("interactions").add(interaction)
                    .addOnSuccessListener { doc ->
                        // pass id forward so Success/Share screens can use it
//                        val args = Bundle().apply { putString("interactionId", doc.id) }
//                        findNavController().navigate(R.id.action_consentFragment_to_successFragment, args)
                        findNavController().navigate(
                            R.id.action_consentFragment_to_successFragment,
                            bundleOf("interactionId" to doc.id)
                        )
                    }
                    .addOnFailureListener {
                        submit.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to save. Please try again.", Toast.LENGTH_LONG).show()
                    }
            }
        }

        close.setOnClickListener { findNavController().popBackStack() }

        // Optional: only show the "Thanks" dialog in create mode
        if (mode != "publish") {
            ThanksForHelpingDialog().show(childFragmentManager, "thanks")
        }
    }
}

