package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import org.brightmindenrichment.street_care.R

class InteractionLogActivityq4 : AppCompatActivity() {

    private lateinit var otherCheckBox: CheckBox
    private lateinit var otherInput: EditText
    private lateinit var tilOther: TextInputLayout
    private lateinit var btnNext: TextView
    private lateinit var btnPrevious: TextView
    private lateinit var skipBtn: TextView
    private lateinit var checkboxContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_log_interaction_q4)

        // 1. Initialize UI Views
        initializeViews()

        // 2. Set up Button Listeners (Next, Previous, Skip, Close)
        setupClickListeners()
    }

    private fun initializeViews() {
        otherCheckBox = findViewById(R.id.other_checkbox)
        otherInput = findViewById(R.id.other_input)
        tilOther = findViewById(R.id.tilOther)
        btnNext = findViewById(R.id.btn_next)
        btnPrevious = findViewById(R.id.btn_previous)
        skipBtn = findViewById(R.id.skip_btn)
        checkboxContainer = findViewById(R.id.checkbox_list)
    }


    private fun setupClickListeners() {
        // Toggle visibility for "Other" input field
        otherCheckBox.setOnCheckedChangeListener { _, isChecked ->
            tilOther.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Navigation: Next Question
        btnNext.setOnClickListener {
            val selectedOptions = getSelectedOptions()
            if (selectedOptions.isEmpty()) {
                Toast.makeText(this, "Please select at least one option", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Moving to Question 4", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigation: Back to previous activity
        btnPrevious.setOnClickListener {
            val intent = Intent(this, InteractionLogActivityq3::class.java)
            startActivity(intent)
            finish()
        }

        // Skip logic
        skipBtn.setOnClickListener {
            Toast.makeText(this, "Question skipped", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Logic to extract selected answers from the checkbox container
     */
    private fun getSelectedOptions(): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until checkboxContainer.childCount) {
            val child = checkboxContainer.getChildAt(i)
            if (child is CheckBox && child.isChecked) {
                if (child.id == R.id.other_checkbox) {
                    val otherText = otherInput.text.toString()
                    selected.add(if (otherText.isNotBlank()) otherText else "Other")
                } else {
                    selected.add(child.text.toString())
                }
            }
        }
        return selected
    }
}
