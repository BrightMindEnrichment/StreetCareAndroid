package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ5Binding

class InteractionQ5Fragment : Fragment() {

    private var _binding: FragmentLogInteractionQ5Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()

    private var helpedCount = 1
    private var joinedCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore saved counts if returning from a later screen
        val saved = viewModel.interactionLog.value
        if (saved != null) {
            helpedCount = saved.numPeopleHelped.takeIf { it > 0 } ?: 1
            joinedCount = saved.numPeopleJoined
        }

        updateUI()

        binding.btnIncreaseHelped.setOnClickListener { syncFromInput(); helpedCount++; updateUI() }
        binding.btnDecreaseHelped.setOnClickListener { syncFromInput(); if (helpedCount > 0) { helpedCount--; updateUI() } }

        binding.btnIncreaseJoined.setOnClickListener { syncFromInput(); joinedCount++; updateUI() }
        binding.btnDecreaseJoined.setOnClickListener { syncFromInput(); if (joinedCount > 0) { joinedCount--; updateUI() } }

        binding.btnPrevious.setOnClickListener {
            syncFromInput()
            viewModel.updateCounts(helpedCount, joinedCount)
            viewModel.saveDraft()
            findNavController().popBackStack()
        }

        binding.btnSkip.setOnClickListener { navigateNext() }
        binding.btnNext.setOnClickListener { navigateNext() }
    }

    private fun syncFromInput() {
        helpedCount = binding.tvCountHelped.text.toString().toIntOrNull() ?: helpedCount
        joinedCount = binding.tvCountJoined.text.toString().toIntOrNull() ?: joinedCount
    }

    private fun updateUI() {
        binding.tvCountHelped.setText(helpedCount.toString())
        binding.tvCountJoined.setText(joinedCount.toString())
    }

    private fun navigateNext() {
        syncFromInput()
        viewModel.updateCounts(helpedCount, joinedCount)
        viewModel.saveDraft()
        findNavController().navigate(R.id.action_q5_to_q6)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
