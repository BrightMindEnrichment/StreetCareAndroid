package org.brightmindenrichment.street_care.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.brightmindenrichment.street_care.R

class StepProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val totalSteps = 7
    private var currentStep = 1
    private var stepStates: Map<Int, StepState> = emptyMap()
    var onDotClicked: ((step: Int) -> Unit)? = null

    private val dotW = dpToPx(40)
    private val dotH = dpToPx(15)
    private val dotM = dpToPx(3)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setCurrentStep(step: Int) {
        currentStep = step
        buildDots()
    }

    fun setStepStates(states: Map<Int, StepState>) {
        stepStates = states
        buildDots()
    }

    private fun buildDots() {
        removeAllViews()
        repeat(totalSteps) { index ->
            val step = index + 1
            val state = stepStates[step] ?: StepState.EMPTY
            val dot = View(context).apply {
                val lp = LayoutParams(dotW, dotH).apply { setMargins(dotM, 0, dotM, 0) }
                layoutParams = lp
                background = ContextCompat.getDrawable(context, when {
                    step < currentStep  -> R.drawable.progress_dot_yellow
                    step == currentStep -> R.drawable.progress_dot_yellow_with_border
                    else                -> R.drawable.progress_dot_green_dark
                    // Future: map StepState to colors when drawables are ready
                })
                if (step < currentStep) {
                    isClickable = true
                    isFocusable = true
                    // Apply ripple effect for completed steps
                    val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                    val ta = context.obtainStyledAttributes(attrs)
                    foreground = ta.getDrawable(0)
                    ta.recycle()
                    setOnClickListener { onDotClicked?.invoke(step) }
                }
            }
            addView(dot)
        }
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()
}
