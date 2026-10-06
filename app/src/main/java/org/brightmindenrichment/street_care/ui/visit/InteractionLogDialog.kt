package org.brightmindenrichment.street_care.ui.visit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.brightmindenrichment.street_care.R

class InteractionLogDialog : BottomSheetDialogFragment() {

    private var onContinueListener: (() -> Unit)? = null

    fun setOnContinueListener(listener: () -> Unit) {
        onContinueListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_interaction_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            onContinueListener?.invoke()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://streetcarenow.org/profile/interactionLogForm"))
            startActivity(intent)
            dismiss()
        }
    }
}
