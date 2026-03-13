package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIiQuestionBinding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel

/**
 * Base fragment for all 4 II (Individual Interaction) question screens.
 * Provides shared container layout, button wiring, and header text management.
 *
 * Each subclass must:
 * 1. Implement [inflateContent] (return content-only view)
 * 2. Implement [onContentViewCreated] (wire content-specific logic)
 * 3. Implement [onPreviousClicked], [onNextClicked]
 *
 * Optional overrides:
 * - [onSkipClicked] (default calls [onNextClicked])
 * - [showSkipButton], [skipButtonVisible]
 * - [nextButtonText]
 */
abstract class BaseIIQuestionFragment : Fragment() {

    private var _binding: FragmentIiQuestionBinding? = null
    protected val binding get() = _binding!!

    protected val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    protected val viewModel: IndividualInteractionViewModel by activityViewModels()

    // --- Abstract contract ---

    /** 1-based question number (1–4 for II). Used to generate dynamic label. */
    abstract val questionNumber: Int

    /** Total number of questions in this sequence (default: 4 for II). Override if different. */
    open val totalQuestions: Int = 4

    /**
     * Inflate and return the content-only view for this question.
     * The returned view will be added to the contentContainer FrameLayout.
     */
    abstract fun inflateContent(inflater: LayoutInflater, container: ViewGroup): View

    /**
     * Called after content view is added to the container.
     * Subclass should wire all content-specific logic here.
     */
    abstract fun onContentViewCreated(contentView: View, savedInstanceState: Bundle?)

    abstract fun onPreviousClicked()
    abstract fun onNextClicked()

    // --- Open hooks ---

    open fun onSkipClicked() {
        onNextClicked()
    }

    open fun showSkipButton(): Boolean = true

    /**
     * If false, skip button visibility is set to INVISIBLE (preserves layout space for alignment).
     * If true, skip button is VISIBLE.
     */
    open fun skipButtonVisible(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIiQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- Header setup: "Individual Interaction #N" or edit label ----
        val editingHeaderText = viewModel.editingHeaderText()
        if (editingHeaderText != null) {
            binding.tvHeader.text = editingHeaderText
        } else {
            // For new interactions, use committed interactions size to get correct index
            val committedCount = viewModel.committedInteractions.value?.size ?: 0
            binding.tvHeader.text = "Individual Interaction ${committedCount + 1}"

            // Observe display name changes (updates after Q1 when user enters name)
            viewModel.currentDisplayName.observe(viewLifecycleOwner, Observer { displayName ->
                if (displayName != null) {
                    binding.tvHeader.text = displayName
                }
            })
        }

        // ---- Question index label ----
        binding.tvQuestionIndex.text = getQuestionIndexLabel()

        // ---- Skip button visibility ----
        if (!showSkipButton()) {
            binding.btnSkip.visibility = View.GONE
        } else if (!skipButtonVisible()) {
            binding.btnSkip.visibility = View.INVISIBLE
        }

        // ---- Inflate and add content view ----
        val contentView = inflateContent(layoutInflater, binding.contentContainer)
        binding.contentContainer.addView(contentView)

        // ---- Wire standard buttons ----
        binding.btnPrevious.setOnClickListener {
            onPreviousClicked()
        }

        binding.btnSkip.setOnClickListener {
            onSkipClicked()
        }

        binding.btnNext.setOnClickListener {
            onNextClicked()
        }

        // ---- Let subclass set up content-specific logic ----
        onContentViewCreated(contentView, savedInstanceState)
    }

    /**
     * Generate dynamic question index label (e.g. "Question 1 of 4").
     * Uses the abstract [questionNumber] and configurable [totalQuestions].
     */
    protected open fun getQuestionIndexLabel(): String {
        return getString(R.string.question_x_of_y, questionNumber, totalQuestions)
    }

    /**
     * Mark the form as modified (dirty).
     * Call this whenever user changes any input field.
     */
    protected fun markFormDirty() {
        interactionLogViewModel.markAsDirty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
