package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.brightmindenrichment.street_care.R
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton

class SuccessFragment : Fragment(R.layout.fragment_success) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialButton>(R.id.btnAddAnother).setOnClickListener {
            findNavController().navigate(R.id.action_successFragment_to_visitFormFragment1)
        }
        view.findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.action_successFragment_to_visitLogFragment)
        }
        view.findViewById<ImageView>(R.id.btn_close).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
