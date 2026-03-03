package org.brightmindenrichment.street_care.ui.visit

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import org.brightmindenrichment.street_care.R

class InteractionLogDialog: DialogFragment() {

    private var onContinueListener: (() -> Unit)? = null

    fun setOnContinueListener(listener: () -> Unit) {
        onContinueListener = listener
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(),R.style.InteractionDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_interaction_log, null)
        builder.setView(view)

        val btnContinue = view.findViewById<Button>(R.id.btnContinue)
        btnContinue.setOnClickListener {
            onContinueListener?.invoke()
            val url = "https://streetcarenow.org/profile/interactionLogForm"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            dismiss()
        }

        return builder.create()
    }
}