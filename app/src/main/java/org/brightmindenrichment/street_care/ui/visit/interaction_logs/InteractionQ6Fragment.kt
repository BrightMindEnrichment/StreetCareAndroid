package org.brightmindenrichment.street_care.ui.visit.interaction_logs


import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ6Binding
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.Constants

class InteractionQ6Fragment : Fragment(), StepValidator {

    private var _binding: FragmentLogInteractionQ6Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()
    private var carePackageCount = 0
    private var wasSkipped = false
    private var isTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ6Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----------------------
        // 4. Restore Previous Values
        // -----------------------
        val current = viewModel.interactionLog.value ?: InteractionLog()

        carePackageCount = current.carePackagesDistributed
        android.util.Log.d("Q6_DEBUG", "Restored carePackageCount from ViewModel: $carePackageCount (full value: ${current.carePackagesDistributed})")
        binding.etCount.setText(carePackageCount.toString())
        val notes = current.carePackageContents.joinToString(", ")
        binding.etNotes.setText(notes)

        fun updateUI() {
            binding.etCount.setText(carePackageCount.toString())
            // Disable minus button when carePackageCount is 0 (minimum)
            binding.btnDecrease.isEnabled = carePackageCount > 0
            binding.btnDecrease.alpha = if (carePackageCount > 0) 1f else 0.5f
        }

        // -----------------------
        // 5. Counter Logic
        // -----------------------
        binding.btnIncrease.setOnClickListener {
            syncFromInput()
            carePackageCount++
            updateUI()
        }

        binding.btnDecrease.setOnClickListener {
            syncFromInput()
            if (carePackageCount > 0) {
                carePackageCount--
                updateUI()
            }
        }

        // -----------------------
        // 6. Previous Button
        // -----------------------
        binding.btnPrevious.setOnClickListener {
            syncFromInput()
            viewModel.updateCarePackage(carePackageCount, binding.etNotes.text.toString())
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
        }

        // -----------------------
        // 7. Skip Button
        // -----------------------
        binding.btnSkip.setOnClickListener {
            wasSkipped = true
            goToNext(binding.etNotes.text.toString())
        }

        // -----------------------
        // 8. Next Button
        // -----------------------
        binding.btnNext.setOnClickListener {
            goToNext(binding.etNotes.text.toString())
        }

        updateUI()

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(6)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(Constants.INTERACTION_LOG_DEST_IDS[step - 1], false)
            }
        }
    }

    // -----------------------
    // Save Data + Navigate
    // -----------------------
    private fun syncFromInput() {
        carePackageCount = binding.etCount.text.toString().toIntOrNull() ?: carePackageCount
    }

    private fun goToNext(notes: String) {
        syncFromInput()
        viewModel.updateCarePackage(carePackageCount, notes)

        viewModel.saveDraft {
            findNavController().navigate(
                R.id.action_q6_to_q7
            )
        }
    }

    override fun saveCurrentState() {
        isTouched = true
        viewModel.updateCarePackage(carePackageCount, binding.etNotes.text.toString())
    }

    override fun getStepState(): StepState {
        return when {
            wasSkipped -> StepState.SKIPPED
            isCurrentStepValid() -> StepState.VALID
            isTouched -> StepState.TOUCHED
            else -> StepState.EMPTY
        }
    }

    private fun isCurrentStepValid(): Boolean {
        return carePackageCount > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
