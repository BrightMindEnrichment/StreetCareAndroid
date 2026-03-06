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
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import org.brightmindenrichment.street_care.R

class InteractionLogFormConsentFragment : Fragment(R.layout.fragment_log_interaction_consent) {

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
        val submit = view.findViewById<Button>(R.id.btnSubmitConsent)

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

            submit.isEnabled = false

            val db = Firebase.firestore
            val now = FieldValue.serverTimestamp()

            if (mode == "publish" && interactionId.isNotBlank()) {
                findNavController().navigate(
                    R.id.action_consentFragment_to_surveySubmittedFragment,
                    bundleOf("interactionId" to interactionId)
                )

                db.collection("interactions")
                    .document(interactionId)
                    .update(
                        mapOf(
                            "isPublic" to true,
                            "status" to "Pending",
                            "lastModifiedTimestamp" to now
                        )
                    )
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        Toast.makeText(
                            requireContext(),
                            "Couldn’t sync right now. Your submission will retry when online.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {
                val docRef = db.collection("interactions").document()
                val newId = docRef.id

                findNavController().navigate(
                    R.id.action_consentFragment_to_surveySubmittedFragment,
                    bundleOf("interactionId" to newId)
                )

                val interaction = hashMapOf(
                    "isPublic" to true,
                    "status" to "Pending",
                    "createdTimestamp" to now,
                    "lastModifiedTimestamp" to now,
                    "helpRequestCount" to 0,
                    "helpRequestDocIds" to emptyList<String>(),
                    "userId" to (Firebase.auth.currentUser?.uid ?: "")
                )

                docRef.set(interaction)
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        val ctx = activity ?: return@addOnFailureListener
                        Toast.makeText(
                            ctx,
                            "Couldn’t sync right now. Will retry when online.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

//        submit.setOnClickListener {
//            if (!cb.isChecked) return@setOnClickListener
//
//            val db = Firebase.firestore
//            val now = FieldValue.serverTimestamp()
//
//            submit.isEnabled = false
//
//            if (mode == "publish" && interactionId.isNotBlank()) {
//                // UPDATE existing interaction: make public (and bump timestamp)
//                db.collection("interactions")
//                    .document(interactionId)
//                    .update(
//                        mapOf(
//                            "isPublic" to true,
//                            "status" to "Pending",
//                            "lastModifiedTimestamp" to now
//                        )
//                    )
//                    .addOnSuccessListener {
//                        findNavController().navigate(
//                            R.id.action_consentFragment_to_surveySubmittedFragment,
//                            bundleOf("interactionId" to interactionId)
//                        )
//                    }
//                    .addOnFailureListener {
//                        submit.isEnabled = true
//                        Toast.makeText(
//                            requireContext(),
//                            "Failed to share. Please try again.",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//            } else {
//                val interaction = hashMapOf(
//                    "isPublic" to cb.isChecked,
//                    "status" to "Pending",
//                    "createdTimestamp" to now,
//                    "lastModifiedTimestamp" to now,
//                    "helpRequestCount" to 0,
//                    "helpRequestDocIds" to emptyList<String>(),
//                    "userId" to (Firebase.auth.currentUser?.uid ?: "")
//                )
//
//                db.collection("interactions").add(interaction)
//                    .addOnSuccessListener { doc ->
//                        findNavController().navigate(
//                            R.id.action_consentFragment_to_surveySubmittedFragment,
//                            bundleOf("interactionId" to doc.id)
//                        )
//                    }
//                    .addOnFailureListener {
//                        submit.isEnabled = true
//                        Toast.makeText(
//                            requireContext(),
//                            "Failed to save. Please try again.",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//            }
//        }
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