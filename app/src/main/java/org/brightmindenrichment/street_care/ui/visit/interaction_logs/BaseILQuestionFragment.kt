package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIlQuestionBinding
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.Constants

/**
 * Base fragment for all 7 IL (Interaction Log) question screens.
 * Provides shared container layout, progress bar, button wiring, and DOT_DEST_IDS.
 *
 * Each subclass must:
 * 1. Define [stepNumber] (1-7)
 * 2. Define [stepLabelRes] (string resource ID for step label)
 * 3. Implement [inflateContent] (return content-only view)
 * 4. Implement [onContentViewCreated] (wire content-specific logic)
 * 5. Implement [saveCurrentState], [getStepState] (from StepValidator)
 *
 * Optional overrides for special cases:
 * - [showPreviousButton], [showSkipButton], [showNextButton], [showYesNoButtons]
 */
abstract class BaseILQuestionFragment : Fragment(), StepValidator {

    private var _binding: FragmentIlQuestionBinding? = null
    protected val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()

    // --- Abstract contract ---

    /** 1-based step index (1–7). Used to set progress bar, resolve DOT_DEST_IDS, and generate dynamic label. */
    abstract val stepNumber: Int

    /** Total number of steps in this sequence (default: 7 for IL). Override if different. */
    open val totalSteps: Int = 7

    /**
     * Inflate and return the content-only view for this question.
     * The returned view will be added to the contentContainer FrameLayout.
     */
    abstract fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View

    /**
     * Called after content view is added to the container.
     * Subclass should wire all content-specific logic here (listeners, focus handlers, etc).
     */
    abstract fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?)

    override abstract fun saveCurrentState()
    override abstract fun getStepState(): StepState

    // --- Open configuration hooks ---

    open fun showPreviousButton(): Boolean = true
    open fun showSkipButton(): Boolean = true
    open fun showNextButton(): Boolean = true
    open fun showYesNoButtons(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIlQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set step label dynamically
        binding.tvStepLabel.text = getString(R.string.question_x_of_y, stepNumber, totalSteps)

        // ---- Progress bar setup (IL only) ----
        binding.progressBar.setCurrentStep(stepNumber)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(Constants.INTERACTION_LOG_DEST_IDS[step - 1], false)
            }
        }

        // ---- Button visibility setup ----
        if (!showPreviousButton()) {
            binding.btnPrevious.visibility = View.GONE
            // Keep buttonSpacer visible so Next sticks to the right
        }

        if (!showSkipButton()) {
            // Use INVISIBLE to preserve header row height consistency across all screens
            binding.btnSkip.visibility = View.INVISIBLE
        }

        if (!showNextButton()) {
            binding.btnNext.visibility = View.GONE
            // Keep buttonSpacer visible so Previous sticks to the left
        }

        if (showYesNoButtons()) {
            binding.yesNoRow.visibility = View.VISIBLE
        }

        // ---- Inflate and add content view ----
        val contentView = inflateContent(layoutInflater, binding.contentContainer)
        binding.contentContainer.addView(contentView)

        // ---- Wire standard buttons ----
        binding.btnPrevious.setOnClickListener {
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack()
            }
        }

        binding.btnSkip.setOnClickListener {
            saveCurrentState()
            viewModel.saveDraft {
                onSkipNavigate()
            }
        }

        binding.btnNext.setOnClickListener {
            saveCurrentState()
            viewModel.saveDraft {
                onNextNavigate()
            }
        }

        // ---- Let subclass set up content-specific logic ----
        onContentViewCreated(contentView, savedInstanceState)
    }

    /**
     * Called when Skip button is clicked. Default is to call [onNextNavigate].
     * Subclass can override to have different behavior.
     */
    protected open fun onSkipNavigate() {
        onNextNavigate()
    }

    /**
     * Called when Next button is clicked. Subclass must override to handle navigation.
     */
    protected open fun onNextNavigate() {
        // Default: no-op. Subclass should override.
    }

    /**
     * Mark the form as modified (dirty).
     * Call this whenever user changes any input field.
     */
    protected fun markFormDirty() {
        viewModel.markAsDirty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val DOT_DEST_IDS = listOf(
            R.id.interactionQ1Fragment,
            R.id.interactionQ2Fragment,
            R.id.interactionQ3Fragment,
            R.id.interactionQ4Fragment,
            R.id.interactionQ5Fragment,
            R.id.interactionQ6Fragment,
            R.id.interactionQ7Fragment
        )
    }
}
