package org.brightmindenrichment.street_care.ui.interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.EditIndividualInteractionBinding
import org.brightmindenrichment.street_care.ui.visit.IndividualInteractionAdapter
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import org.brightmindenrichment.street_care.ui.visit.details.IndividualInteractionViewModel


class IndividualInteractionFragment : Fragment() {
    private var _binding: EditIndividualInteractionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IndividualInteractionViewModel by navGraphViewModels(R.id.individual_interaction)

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
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        arguments?.let {
            val id = it.getString(ARGUMENT_INTERACTION_LOG_ID)
            id?.let {
                viewModel.fetchInteractions(id)
            }
        }

        adapter = IndividualInteractionAdapter(requireContext(), emptyList(), listener)
        binding.listViewInteractions.adapter = adapter

        viewModel.interactions.observe(viewLifecycleOwner) { list ->
            if (list.size > 1) {
                binding.sectionHeading.text = "Individual Interactions"
            } else {
                binding.sectionHeading.text = "Individual Interaction"
            }
            adapter.updateList(list)
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddMore.setOnClickListener {
//            findNavController().navigate(R.id.individualInteractionFragment)
        }

        // Next button
        binding.btnNext.setOnClickListener {
            // Navigate to next screen
            findNavController().navigate(R.id.action_nav_visit_to_visitFormFragment2)
        }

        binding.listViewInteractions.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }

    private val listener = object : IndividualInteractionAdapter.InteractionListener {
        override fun onEditClicked(item: IndividualInteraction) {

        }

        override fun onDeleteClicked(item: IndividualInteraction) {
            viewModel.deleteInteraction(item)
        }

    }

    companion object {
        private const val ARGUMENT_INTERACTION_LOG_ID = "InteractionLogId"    }
}