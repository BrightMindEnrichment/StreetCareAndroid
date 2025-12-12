package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.brightmindenrichment.street_care.R

class ThanksForHelpingDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = requireActivity().layoutInflater.inflate(R.layout.dialog_thanks, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .setPositiveButton(R.string.ok, null)
            .create()
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

}
