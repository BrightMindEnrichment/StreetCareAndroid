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
import org.brightmindenrichment.street_care.databinding.FragmentInterLogQuestion7Binding

class VisitFormFragment5 : Fragment() {

    private var _binding: FragmentInterLogQuestion7Binding? = null
    private val binding get() = _binding!!

    private val visitId: String by lazy {
        arguments?.getString("visitId").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterLogQuestion7Binding.inflate(inflater, container, false)
        return binding.root
    }

    private fun navWithVisitId(actionId: Int) {
        findNavController().navigate(
            actionId,
            Bundle().apply { putString("visitId", visitId) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar
            ?.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        binding.txtNo.setOnClickListener {
            navWithVisitId(R.id.action_visitFormFragment5_to_action_visitFormFragment6)
        }

        binding.txtYes.setOnClickListener {
            navWithVisitId(R.id.action_visitFormFragment5_to_action_visitFormFragment4)
        }

        binding.txtSkip.setOnClickListener {
            navWithVisitId(R.id.action_visitFormFragment5_to_action_visitFormFragment6)
        }

        binding.txtPrevious5.setOnClickListener {
            navWithVisitId(R.id.action_visitFormFragment5_to_action_visitFormFragment4)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
