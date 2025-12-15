package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentVisitForm5Binding

class VisitFormFragment5 : Fragment() {

    private var _binding: FragmentVisitForm5Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisitForm5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the default action bar (we use our own toolbar in the layout)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)

        // Make sure bottom nav is visible
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // YES → go to Individual Interaction 1 (visitForm7a)
        binding.txtYes.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragment5_to_action_visitFormFragment4
            )
        }

        // NO → go straight to "Thank you / Interaction logged"
        binding.txtNo.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragment5_to_action_visitFormFragment6
            )
        }

        // Skip → same as NO
        binding.txtSkip.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragment5_to_action_visitFormFragment6
            )
        }

        // Previous → back to Question 6
        binding.txtPrevious5.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragment5_to_visitFormFragment4
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
