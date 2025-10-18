package org.brightmindenrichment.street_care.ui.visit

import android.graphics.Color
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class BlankInteractionLogFragment : Fragment() {

    private var shouldShowDialogAgain = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Show dialog after view is created
        Handler(Looper.getMainLooper()).post {
            showDialog()
        }

        // Return a blank view (e.g., transparent or white)
        return View(requireContext()).apply {
            setBackgroundColor(Color.TRANSPARENT) // or use white
        }
    }

    private fun showDialog() {
        val dialog = InteractionLogDialog()
        dialog.setOnContinueListener {
            shouldShowDialogAgain = true // Mark that user clicked continue
        }
        dialog.show(parentFragmentManager, "InteractionDialog")
    }

    override fun onResume() {  // Called when returning from browser
        super.onResume()
        if (shouldShowDialogAgain) {
            shouldShowDialogAgain = false // Reset flag to avoid loop
            showDialog()
        }
    }
}