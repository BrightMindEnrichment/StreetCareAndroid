package org.brightmindenrichment.street_care.util

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.brightmindenrichment.street_care.R

fun showLoginDialog(context: Context) {
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.please_login_title))
        .setMessage(context.getString(R.string.please_login_message))
        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        .setPositiveButton(context.getString(R.string.login)) { dialog, _ ->
            val activity = context as AppCompatActivity
            val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.profile)
            dialog.dismiss()
        }
        .show()
}