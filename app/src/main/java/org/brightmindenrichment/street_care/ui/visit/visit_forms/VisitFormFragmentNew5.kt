package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentVisitForm5NewBinding

class VisitFormFragmentNew5 : Fragment() {

    private var _binding: FragmentVisitForm5NewBinding? = null
    private val binding get() = _binding!!

    private val sharedVisitViewModel: VisitViewModel by activityViewModels()

    private var helpedCount = 1
    private var joinedCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisitForm5NewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----------------------
        // 1. Show Bottom Navigation
        // -----------------------
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        // -----------------------
        // 2. Configure ActionBar
        // -----------------------
        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
            ab.title = "Interaction Log"
        }

        setHasOptionsMenu(true)

        fun updateUI() {
            binding.tvCountHelped.text = helpedCount.toString()
            binding.tvCountJoined.text = joinedCount.toString()
        }

        // -----------------------
        // Counter Logic
        // -----------------------
        binding.btnIncreaseHelped.setOnClickListener {
            helpedCount++
            updateUI()
        }

        binding.btnDecreaseHelped.setOnClickListener {
            if (helpedCount > 0) {
                helpedCount--
                updateUI()
            }
        }

        binding.btnIncreaseJoined.setOnClickListener {
            joinedCount++
            updateUI()
        }

        binding.btnDecreaseJoined.setOnClickListener {
            if (joinedCount > 0) {
                joinedCount--
                updateUI()
            }
        }

        // -----------------------
        // Previous Button
        // -----------------------
        binding.btnPrevious.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitFormFragmentNew5_to_visitFormFragment4
            )
        }

        // -----------------------
        // Skip Button
        // -----------------------
        binding.btnSkip.setOnClickListener {
            navigateNext()
        }

        // -----------------------
        // Next Button
        // -----------------------
        binding.btnNext.setOnClickListener {
            navigateNext()
        }

        updateUI()
    }

    // -----------------------
    // Handle ActionBar Close Click
    // -----------------------
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateNext() {

        // Save data into ViewModel
        sharedVisitViewModel.visitLog.peopleHelped = helpedCount
        sharedVisitViewModel.visitLog.peopleJoined = joinedCount

        // Navigate to Fragment 6
        findNavController().navigate(
            R.id.action_visitFormFragmentNew5_to_visitFormFragmentNew6
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
