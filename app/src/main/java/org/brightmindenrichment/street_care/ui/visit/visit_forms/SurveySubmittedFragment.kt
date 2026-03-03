package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentSurvaySubmittedBinding

class SurveySubmittedFragment : Fragment() {
    private var prevTitle: CharSequence? = null
    private var prevHomeAsUpEnabled: Boolean? = null
    private var prevHomeIndicator: android.graphics.drawable.Drawable? = null
    private var _binding: FragmentSurvaySubmittedBinding? = null
    private val binding get() = _binding!!

    private val sharedVisitViewModel: VisitViewModel by activityViewModels()

    private var clicked = false
    private var sharedCommunity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // needed for the ActionBar close click
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSurvaySubmittedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            // store previous state
            prevTitle = ab.title
            prevHomeAsUpEnabled = ab.isShowing
            prevHomeIndicator = ab.themedContext.let { null }

            ab.setDisplayHomeAsUpEnabled(true)
            ab.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
            ab.title = "Interaction Log"
        }

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

//        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
//            ab.setDisplayHomeAsUpEnabled(true)
//            ab.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
//
//            val title = SpannableString("Interaction Log").apply {
//                setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
//            }
//            ab.title = title
//        }

        binding.btnAnotherVisit.setOnClickListener {
            sharedCommunity = false
            clicked = false
            sharedVisitViewModel.resetVisitLogPage() // Add this to clear old visit data
            findNavController().navigate(R.id.action_surveySubmittedFragment_to_interactionQ1Fragment)
        }

        binding.btnReturnHome.setOnClickListener {
            clicked = true
            sharedCommunity = false
            findNavController().navigate(R.id.action_surveySubmittedFragment_to_nav_visit)
        }

        // Handle back button press → go to Interaction Log home
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                clicked = true
                sharedCommunity = false
                findNavController().navigate(R.id.action_surveySubmittedFragment_to_nav_visit)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                clicked = true
                sharedCommunity = false
                findNavController().navigate(R.id.action_surveySubmittedFragment_to_nav_visit)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSharePopup() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.share_popup_title))
            .setMessage(getString(R.string.share_popup_message))
            .setPositiveButton(getString(R.string.share_popup_confirm)) { dialog, _ ->
                sharedVisitViewModel.visitLog.share = true
                val docId = sharedVisitViewModel.visitLog.documentId
                if (docId != null) updateVisitLogField(docId)
                sharedVisitViewModel.resetVisitLogPage()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.share_popup_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun updateVisitLogField(documentId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val userType = document.getString("Type") ?: ""
                val status =
                    if (userType == "Chapter Leader" || userType == "Street Care Hub Leader") "approved" else "pending"

                val updateMap = mapOf(
                    "isPublic" to true,
                    "status" to status
                )

                db.collection("VisitLogBook_New").document(documentId).update(updateMap)
                    .addOnSuccessListener { Log.d("Firestore", "Interaction Log published.") }
                    .addOnFailureListener { e -> Log.w("Firestore", "Failed to publish.", e) }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Failed to retrieve user type", e)
            }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
            title = "Interaction Log"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (clicked && !sharedCommunity) {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.loginRedirectFragment
        }

        _binding = null
    }

}