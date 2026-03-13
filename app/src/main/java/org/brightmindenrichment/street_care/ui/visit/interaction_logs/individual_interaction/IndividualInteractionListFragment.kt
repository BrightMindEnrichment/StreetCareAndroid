package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionListBinding
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel

class IndividualInteractionListFragment : Fragment() {

    private var _binding: FragmentIndividualInteractionListBinding? = null
    private val binding get() = _binding!!

    // For list + delete + later saving (Firebase)
    private val viewModel: IndividualInteractionViewModel by activityViewModels()
    private val ilViewModel: InteractionLogViewModel by activityViewModels()

    private lateinit var adapter: IndividualInteractionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualInteractionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        setHasOptionsMenu(true)

        adapter = IndividualInteractionAdapter(requireContext(), emptyList(), listener)
        binding.listViewInteractions.adapter = adapter

        viewModel.committedInteractions.observe(viewLifecycleOwner) { list ->
            binding.sectionHeading.text = getString(R.string.your_interactions)
            adapter.updateList(list)
        }

        binding.btnAddMore.setOnClickListener {
            findNavController().navigate(
                R.id.action_individualInteractionFragment_to_individualInteractionQ1
            )
        }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(
                R.id.action_individualInteractionFragment_to_consentPage
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Back button pops back to Q7
                Log.d("IIListNav", "Back pressed from list, current dest=${findNavController().currentDestination?.id}, backStackSize=${findNavController().backQueue.size}")
                findNavController().backQueue.forEach {
                    Log.d("IIListNav", "  BackStack entry: ${it.destination.id} - ${it.destination.label}")
                }
                val result = findNavController().popBackStack()
                Log.d("IIListNav", "popBackStack result: $result, new dest=${findNavController().currentDestination?.id}")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val listener = object : IndividualInteractionAdapter.InteractionListener {
        override fun onEditClicked(item: IndividualInteraction, position: Int) {
            viewModel.startEditing(position)
            findNavController().navigate(
                R.id.action_individualInteractionFragment_to_individualInteractionQ1
            )
        }

        override fun onDeleteClicked(item: IndividualInteraction) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_interaction_title))
                .setMessage(getString(R.string.delete_interaction_message))
                .setPositiveButton(getString(R.string.delete_interaction_confirm)) { _, _ ->
                    val idx = viewModel.committedInteractions.value?.indexOf(item) ?: -1
                    viewModel.deleteCommittedInteraction(item)
                    if (idx >= 0) {
                        ilViewModel.removeIndividualInteraction(idx)
                        ilViewModel.saveDraft()
                    }
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
    }
}