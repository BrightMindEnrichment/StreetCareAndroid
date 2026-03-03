package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.EditIndividualInteractionBinding
import org.brightmindenrichment.street_care.ui.visit.IndividualInteractionAdapter
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel

class IndividualInteractionFragment : Fragment() {

    private var _binding: EditIndividualInteractionBinding? = null
    private val binding get() = _binding!!

    // For list + delete + later saving (Firebase)
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    private lateinit var adapter: IndividualInteractionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditIndividualInteractionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
        }

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        adapter = IndividualInteractionAdapter(requireContext(), emptyList(), listener)
        binding.listViewInteractions.adapter = adapter

        viewModel.interactions.observe(viewLifecycleOwner) { list ->
            binding.sectionHeading.text =
                if (list.size > 1) "Individual Interactions" else "Individual Interaction"
            adapter.updateList(list)
        }

        binding.btnAddMore.setOnClickListener {
            findNavController().navigate(
                R.id.action_individualInteractionFragment_to_individualInteractionQ1
            )
        }

        binding.btnNext.setOnClickListener {
            // TODO: next screen later
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val listener = object : IndividualInteractionAdapter.InteractionListener {
        override fun onEditClicked(item: IndividualInteraction) {
            // TODO later
        }

        override fun onDeleteClicked(item: IndividualInteraction) {
            viewModel.deleteInteraction(item)
        }
    }
}