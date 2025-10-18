package org.brightmindenrichment.street_care.ui.user

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.MultiAutoCompleteTextView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import org.brightmindenrichment.street_care.R

class ChapterMembershipFormTwoAcitivity :AppCompatActivity (){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chapter_membership_form_2) // Update to match your actual layout file name


        // Initialize Views
        val multiSelectDays = findViewById<MultiAutoCompleteTextView>(R.id.multiSelectDays)
        val editTextHoursAvailable = findViewById<TextInputEditText>(R.id.editHours)
        val spinnerConsent = findViewById<Spinner>(R.id.spinnerConsent)
        val spinnerSource = findViewById<Spinner>(R.id.spinnerSource)
        val editTextWhyVolunteer = findViewById<TextInputEditText>(R.id.editWhyVolunteer)
        val btnNext = findViewById<Button>(R.id.btn_next)
        val btnBack = findViewById<Button>(R.id.btn_back)

        // Populate Days Available with dummy data
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days)
        multiSelectDays.setAdapter(adapter)
        multiSelectDays.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        multiSelectDays.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) multiSelectDays.showDropDown()
        }
        multiSelectDays.setOnClickListener { multiSelectDays.showDropDown() }

        // Populate Spinners with dummy data
        val consentOptions = arrayOf("", "Yes", "No")
        val sourceOptions = arrayOf("", "Social Media", "Friends", "Flyer", "Other")

        setUpSpinner(spinnerConsent, consentOptions)
        setUpSpinner(spinnerSource, sourceOptions)

        // Get data passed from StepOneActivity
        val personalData = intent.extras

        // Next button click listener
        btnNext.setOnClickListener {
            val firstName = personalData?.getString("firstName")
            val lastName = personalData?.getString("lastName")
            val email = personalData?.getString("email")
            val phone = personalData?.getString("phone")
            val addressLine1 = personalData?.getString("addressLine1")
            val addressLine2 = personalData?.getString("addressLine2")
            val city = personalData?.getString("city")
            val state = personalData?.getString("state")
            val zipcode = personalData?.getString("zipcode")
            val country = personalData?.getString("country")
            val daysAvailable = multiSelectDays.text?.toString()
            val hoursAvailable = editTextHoursAvailable.text?.toString()
            val consent = spinnerConsent.selectedItem?.toString()
            val source = spinnerSource.selectedItem?.toString()
            val whyVolunteer = editTextWhyVolunteer.text?.toString()

            // Validation
            if (hoursAvailable.isNullOrEmpty() || whyVolunteer.isNullOrEmpty() || consent.isNullOrEmpty() || consent.equals("No")) {
                Toast.makeText(this, "Please fill out all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass data to the next activity
            val intent = Intent(this, ChapterMembershipFormThreeAcitivity::class.java)
            intent.putExtra("firstName", firstName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("email", email)
            intent.putExtra("phone", phone)
            intent.putExtra("addressLine1", addressLine1)
            intent.putExtra("addressLine2", addressLine2)
            intent.putExtra("city", city)
            intent.putExtra("state", state)
            intent.putExtra("zipcode", zipcode)
            intent.putExtra("country", country)
            intent.putExtra("daysAvailable", daysAvailable)
            intent.putExtra("hoursAvailable", hoursAvailable)
            intent.putExtra("consent", consent)
            intent.putExtra("source", source)
            intent.putExtra("whyVolunteer", whyVolunteer)
            startActivity(intent)
        }

        // Back button click listener
        btnBack.setOnClickListener {
            finish() // Close the current activity and go back to the previous step
        }
    }

    // Helper function to set up spinners
    private fun setUpSpinner(spinner: Spinner, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

}