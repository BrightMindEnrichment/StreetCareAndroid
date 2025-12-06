package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R

class SuccessShareFragment : Fragment(R.layout.fragment_success_share) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val addAnother = view.findViewById<MaterialButton>(R.id.btnAddAnother)
        val back = view.findViewById<MaterialButton>(R.id.btnBack)
        val share = view.findViewById<MaterialButton>(R.id.btnShareCommunity)
        val close = view.findViewById<ImageView>(R.id.btn_close)

        addAnother.setOnClickListener {
            findNavController().navigate(R.id.action_successShareFragment_to_visitFormFragment2)
        }

        back.setOnClickListener {
            findNavController().navigate(R.id.action_successShareFragment_to_visitLogFragment)
        }

        close.setOnClickListener { findNavController().popBackStack() }

        // get the freshly-created doc id
//        val interactionId = arguments?.getString("interactionId").orEmpty()
//        share.isEnabled = interactionId.isNotBlank()
//
//        share.setOnClickListener {
//            if (interactionId.isBlank()) {
//                Toast.makeText(requireContext(), "Missing interaction id.", Toast.LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//            share.isEnabled = false
//
//            Firebase.firestore.collection("interactions")
//                .document(interactionId)
//                .update(
//                    mapOf(
//                        "isPublic" to true,
//                        "lastModifiedTimestamp" to FieldValue.serverTimestamp()
//                        // "status" to "Published"
//                    )
//                )
//                .addOnSuccessListener {
//                    Toast.makeText(requireContext(), "Shared with community!", Toast.LENGTH_SHORT).show()
//                    findNavController().navigate(R.id.action_successShareFragment_to_visitLogFragment)
//                }
//                .addOnFailureListener {
//                    share.isEnabled = true
//                    Toast.makeText(requireContext(), "Failed to share. Please try again.", Toast.LENGTH_LONG).show()
//                }
//        }
        view.findViewById<MaterialButton>(R.id.btnShareCommunity)?.setOnClickListener {
            val interactionId = arguments?.getString("interactionId").orEmpty()
            val args = Bundle().apply {
                putString("interactionId", interactionId)
                putString("mode", "publish")   // tell Consent we're publishing, not creating
            }
            findNavController().navigate(R.id.action_successShareFragment_to_consentFragment, args)
        }
    }
}
