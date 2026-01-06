package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)


        viewModel.saveQ4(1 ,1, object : IndividualInteractionViewModel.SaveFormListener {
            override fun onSaveFormSuccess() {
                // call this fragment individualinteractionfragment
            }

            override fun onSaveFormFailure(message: String) {
                // show error toast
            }

        })

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

        binding.btnAddMore.setOnClickListener {
            findNavController().navigate(R.id.action_nav_visit_to_interaction_q1)
        }

        // Next button
        binding.btnNext.setOnClickListener {
            // Navigate to next screen
//            findNavController().navigate(R.id.action_nav_visit_to_visitFormFragment2)
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


}