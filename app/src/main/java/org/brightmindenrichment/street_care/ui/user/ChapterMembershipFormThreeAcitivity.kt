package org.brightmindenrichment.street_care.ui.user


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.MainActivity
import org.brightmindenrichment.street_care.R
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ChapterMembershipFormThreeAcitivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chapter_membership_form_3)

        val guardianSignatureTextView: TextView = findViewById(R.id.guardianSignatureTextView)

        guardianSignatureTextView.text = HtmlCompat.fromHtml(
            getString(R.string.guardian_signature),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        guardianSignatureTextView.movementMethod = LinkMovementMethod.getInstance()

        val editTextSignatureDate: TextInputEditText = findViewById(R.id.editTextSignatureDate)

// Initialize a calendar instance
        val calendar = Calendar.getInstance()

// Set up a DatePickerDialog to show when the TextInputEditText is clicked
        editTextSignatureDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Update the calendar with the selected date
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Format the date and set it to the TextInputEditText
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    editTextSignatureDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Show the DatePickerDialog
            datePickerDialog.show()
        }

        // Initialize views
        val editTextSignatureName = findViewById<TextInputEditText>(R.id.edit_text_signature_name)
        val editTextFullName = findViewById<TextInputEditText>(R.id.editTextFullName)
        val editTextComments = findViewById<TextInputEditText>(R.id.editTextComments)
        val btnSaveChanges = findViewById<Button>(R.id.btn_save_changes)
        val btnNext = findViewById<Button>(R.id.btn_cancel)

        val db = Firebase.firestore

        val previousFormData = intent.extras

        btnNext.setOnClickListener {
            finish() // Close the current activity and go back to the previous step
        }

        // Handle Save button click
        btnSaveChanges.setOnClickListener {

            val user = Firebase.auth.currentUser
            if (user == null) {
                Toast.makeText(this, getString(R.string.please_login_before_event), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            } else {

                val signatureName = editTextSignatureName.text.toString().trim()
                val fullName = editTextFullName.text.toString().trim()
                val signatureDate = editTextSignatureDate.text.toString().trim()
                val comments = editTextComments.text.toString().trim()

                val firstName     = previousFormData?.getString("firstName").orEmpty()
                val lastName      = previousFormData?.getString("lastName").orEmpty()
                val email         = previousFormData?.getString("email").orEmpty()
                val phone         = previousFormData?.getString("phone").orEmpty()
                val addressLine1  = previousFormData?.getString("addressLine1").orEmpty()
                val addressLine2  = previousFormData?.getString("addressLine2").orEmpty()
                val city          = previousFormData?.getString("city").orEmpty()
                val state         = previousFormData?.getString("state").orEmpty()
                val zipcode       = previousFormData?.getString("zipcode").orEmpty()
                val country       = previousFormData?.getString("country").orEmpty()

                val daysAvailable = previousFormData?.getString("daysAvailable").orEmpty()
                val hoursAvailable= previousFormData?.getString("hoursAvailable").orEmpty()
                val consent       = previousFormData?.getString("consent").orEmpty()     // “Yes/No”
                val source        = previousFormData?.getString("source").orEmpty()
                val whyVolunteer  = previousFormData?.getString("whyVolunteer").orEmpty()

                // Validate input
                if (signatureName.isEmpty() || fullName.isEmpty() || signatureDate.isEmpty()) {
                    Toast.makeText(this, "Please fill in all mandatory fields.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                addEvent(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    addressLine1 = addressLine1,
                    addressLine2 = addressLine2,
                    city = city,
                    state = state,
                    zipcode = zipcode,
                    country = country,
                    daysAvailable = daysAvailable,
                    hoursAvailable = hoursAvailable,
                    consent = consent,
                    source = source,
                    whyVolunteer = whyVolunteer,
                    signatureName = signatureName,
                    nameForSignature = fullName,
                    signatureDate = signatureDate,
                    comments = comments
                )
            }


        }

    }

    private fun onCancel() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun addEvent(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        addressLine1: String,
        addressLine2: String,
        city: String,
        state: String,
        zipcode: String,
        country: String,
        daysAvailable: String,
        hoursAvailable: String,
        consent: String,
        source: String,
        whyVolunteer: String,
        signatureName: String,
        nameForSignature: String,
        signatureDate: String,
        comments: String
    ) {

        val db = Firebase.firestore

        val randomId = java.util.UUID.randomUUID().toString()

        // Create a map to store data
        // Changed field names to match BME_Form_data tracker
        val data = hashMapOf(
            "FirstName" to firstName,
            "LastName" to lastName,
            "EmailAddress" to email,
            "PhoneNumber" to phone,
            "AddressLine1" to addressLine1,
            "AddressLine2" to addressLine2,
            "ZipCode" to zipcode,
            "Country" to country,
            "City" to city,
            "State" to state,
            "Weekdays" to daysAvailable,
            "VolunteerHours" to hoursAvailable,
            "Under18" to consent,
            "HearAboutUs" to source,
            "VolunteerText" to whyVolunteer,
            "Signature" to signatureName,
            "NameForSignature" to nameForSignature,
            "DateOfSignature" to signatureDate,
            "Comments" to comments
        )

        // Save to Firebase
        db.collection("BMEMembershipForm")
            .add(data)
            .addOnSuccessListener {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    val usersDocRef = db.collection("users").document(user.uid)

                    usersDocRef.update("Type", UserType.CHAPTER_MEMBER.name)
                        .addOnSuccessListener {
                            showSuccessDialog()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChapterForm", "Error updating user type", e)
                            Toast.makeText(
                                this,
                                "Failed to update user type: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChapterForm", "Error saving data", e)
                Toast.makeText(this, "Failed to save data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.chapter_membership_registered, null)
        dialogView.findViewById<TextView>(R.id.textViewMessage).text =
            getString(R.string.thank_you_registered)
        dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView).text =
            getString(R.string.chapter_membership_back_home)
        dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .show()

//        Toast.makeText(this, "You are a Chapter member!", Toast.LENGTH_SHORT).show()
    }
}
