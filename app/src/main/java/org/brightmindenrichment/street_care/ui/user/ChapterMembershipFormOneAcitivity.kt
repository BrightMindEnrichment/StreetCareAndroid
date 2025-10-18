package org.brightmindenrichment.street_care.ui.user

import org.brightmindenrichment.street_care.R
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.textfield.TextInputEditText
import org.brightmindenrichment.street_care.MainActivity


class ChapterMembershipFormOneAcitivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chapter_membership_form)

        val introductionTextView: TextView = findViewById(R.id.introduction)
        val introductionText = getString(R.string.chaptermembership_introduction)

// Parse HTML content
        introductionTextView.text = Html.fromHtml(introductionText, Html.FROM_HTML_MODE_LEGACY)

// Enable link clicking
        introductionTextView.movementMethod = LinkMovementMethod.getInstance()

        // Initialize the form fields
        val editFirstName = findViewById<TextInputEditText>(R.id.editFirstName)
        val editLastName = findViewById<TextInputEditText>(R.id.editLastName)
        val editEmail = findViewById<TextInputEditText>(R.id.editEmail)
        val editPhone = findViewById<TextInputEditText>(R.id.editPhone)
        val editAddress = findViewById<TextInputEditText>(R.id.editAddress)
        val editAddress2 = findViewById<TextInputEditText>(R.id.editAddress2)
        val editCity = findViewById<TextInputEditText>(R.id.editCity)
        val editState = findViewById<TextInputEditText>(R.id.editState)
        val editZipcode = findViewById<TextInputEditText>(R.id.editZipcode)
        val editCountry = findViewById<Spinner>(R.id.spinnerCountry)

        val countryOptions = arrayOf("", "United States", "Afghanistan", "Aland Islands", "Albania", "Algeria", "American Samoa",
            "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua and Barbuda",
            "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan",
            "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belau",
            "Belgium", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia",
            "Bonaire, Saint Eustatius and Saba", "Bosnia and Herzegovina",
            "Botswana", "Bouvet Island", "Brazil", "British Indian Ocean Territory",
            "British Virgin Islands", "Brunei", "Bulgaria", "Burkina Faso",
            "Burundi", "Cambodia", "Cameroon", "Canada", "Cape Verde",
            "Cayman Islands", "Central African Republic", "Chad", "Chile",
            "China", "Christmas Island", "Cocos (Keeling) Islands", "Colombia",
            "Comoros", "Cook Islands", "Costa Rica", "Croatia", "Cuba",
            "Curaçao", "Cyprus", "Czech Republic", "Democratic Republic of the Congo (Kinshasa)",
            "Denmark", "Djibouti", "Dominica", "Dominican Republic", "Ecuador",
            "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia",
            "Ethiopia", "Falkland Islands", "Faroe Islands", "Fiji", "Finland",
            "France", "French Guiana", "French Polynesia", "French Southern Territories",
            "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Gibraltar",
            "Greece", "Greenland", "Grenada", "Guadeloupe", "Guam", "Guatemala",
            "Guernsey", "Guinea", "Guinea-Bissau", "Guyana", "Haiti",
            "Heard Island and McDonald Islands", "Honduras", "Hong Kong", "Hungary",
            "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland",
            "Isle of Man", "Israel", "Italy", "Ivory Coast", "Jamaica",
            "Japan", "Jersey", "Jordan", "Kazakhstan", "Kenya", "Kiribati",
            "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon",
            "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania",
            "Luxembourg", "Macao S.A.R., China", "Macedonia", "Madagascar",
            "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands",
            "Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico",
            "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro",
            "Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia",
            "Nauru", "Nepal", "Netherlands", "New Caledonia", "New Zealand",
            "Nicaragua", "Niger", "Nigeria", "Niue", "Norfolk Island",
            "North Korea", "Northern Mariana Islands", "Norway", "Oman",
            "Pakistan", "Palestinian Territory", "Panama", "Papua New Guinea",
            "Paraguay", "Peru", "Philippines", "Pitcairn", "Poland", "Portugal",
            "Puerto Rico", "Qatar", "Republic of the Congo (Brazzaville)",
            "Reunion", "Romania", "Russia", "Rwanda", "Saint Barthélemy",
            "Saint Helena", "Saint Kitts and Nevis", "Saint Lucia",
            "Saint Martin (Dutch part)", "Saint Martin (French part)",
            "Saint Pierre and Miquelon", "Saint Vincent and the Grenadines",
            "Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia",
            "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore",
            "Sint Maarten", "Slovakia", "Slovenia", "Solomon Islands",
            "Somalia", "South Africa", "South Georgia and the South Sandwich Islands",
            "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan",
            "Suriname", "Svalbard and Jan Mayen", "Swaziland", "Sweden",
            "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania",
            "Thailand", "Timor-Leste", "Togo", "Tokelau", "Tonga",
            "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan",
            "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates",
            "United Kingdom", "Uruguay", "Uzbekistan",
            "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Western Sahara",
            "Yemen", "Zambia", "Zimbabwe")

        setUpSpinner(editCountry, countryOptions)

        val backButton: Button? = findViewById(R.id.btn_back)
        backButton?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                startActivity(Intent(this@ChapterMembershipFormOneAcitivity, MainActivity::class.java))
            }
        })

        // Initialize the next button
        val nextButton: Button? = findViewById(R.id.btn_next)

        // Set up button click listener
        nextButton?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                // Collect form data
                val firstName = editFirstName.getText().toString()
                val lastName: String = editLastName.getText().toString()
                val email: String = editEmail.getText().toString()
                val phone: String = editPhone.getText().toString()
                val address: String = editAddress.getText().toString()
                val address2: String = editAddress2.getText().toString()
                val city: String = editCity.getText().toString()
                val state: String = editState.getText().toString()
                val zipcode: String = editZipcode.getText().toString()
                val country = editCountry.selectedItem?.toString()

                if (country != null) {
                    if (firstName.isEmpty() || lastName.isEmpty() || city.isEmpty() || state.isEmpty() ||
                        address.isEmpty() || zipcode.isEmpty() || country.isEmpty() ||email.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this@ChapterMembershipFormOneAcitivity, "Please fill out all required fields", Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                // Pass data to the next activity
                val intent: Intent = Intent(this@ChapterMembershipFormOneAcitivity, ChapterMembershipFormTwoAcitivity::class.java)
                intent.putExtra("firstName", firstName)
                intent.putExtra("lastName", lastName)
                intent.putExtra("email", email)
                intent.putExtra("phone", phone)
                intent.putExtra("addressLine1", address)
                intent.putExtra("addressLine2", address2)
                intent.putExtra("city", city)
                intent.putExtra("state", state)
                intent.putExtra("zipcode", zipcode)
                intent.putExtra("country", country)

                startActivity(intent)
            }
        })
    }

    private fun setUpSpinner(spinner: Spinner, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}