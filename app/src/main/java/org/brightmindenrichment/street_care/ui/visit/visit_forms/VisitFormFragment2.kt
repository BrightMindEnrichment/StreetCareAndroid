package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.brightmindenrichment.street_care.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.ZoneOffset

class VisitFormFragment2 : Fragment() {

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_visit_form2, container, false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
        requireActivity() .findViewById<BottomNavigationView>(R.id.bottomNav) ?.visibility = View.VISIBLE
        val tilFirstName = view.findViewById<TextInputLayout>(R.id.tilFirstName)
        val etFirstName = view.findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName = view.findViewById<TextInputEditText>(R.id.etLastName)
        val etLocation = view.findViewById<TextInputEditText>(R.id.etLocation)
        val etZip = view.findViewById<TextInputEditText>(R.id.etZip)
        val actState = view.findViewById<MaterialAutoCompleteTextView>(R.id.actState)

        val dateCard = view.findViewById<MaterialCardView>(R.id.datePickerCard)
        val timeCard = view.findViewById<MaterialCardView>(R.id.timePickerCard)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvTz = view.findViewById<TextView>(R.id.tvTz)

        val btnPrevious = view.findViewById<TextView>(R.id.txt_previous2)
        val btnNext = view.findViewById<TextView>(R.id.txt_next2)
        val btnSkip = view.findViewById<TextView>(R.id.txt_skip)

        setupStateDropdown(actState)
        etFirstName.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) {
                tilFirstName.error = null
                tilFirstName.isErrorEnabled = false
                tilFirstName.isHelperTextEnabled = false
            }
        }

        // Date picker
        dateCard.setOnClickListener {
            val mountainZone = ZoneId.of("America/Denver")

            val baseDate = selectedDate ?: LocalDate.now(mountainZone)

            val initialMillis = baseDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_StreetCare_DatePicker)
                .setTitleText(getString(R.string.select_interaction_date))
                .setSelection(initialMillis)
                .build()

            picker.addOnPositiveButtonClickListener { millis ->
                val pickedDate = Instant.ofEpochMilli(millis)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate()

                selectedDate = pickedDate
                tvDate.text = dateFormatter.format(pickedDate)
            }

            picker.show(parentFragmentManager, "date_picker")
        }
        // Time picker
        timeCard.setOnClickListener {
            val mountainZone = ZoneId.of("America/Denver")
            val baseTime = selectedTime ?: LocalTime.now(mountainZone)

            val picker = MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_StreetCare_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(baseTime.hour)
                .setMinute(baseTime.minute)
                .setTitleText(getString(R.string.select_interaction_time))
                .build()

            picker.addOnPositiveButtonClickListener {
                val pickedTime = LocalTime.of(picker.hour, picker.minute)
                selectedTime = pickedTime

                tvTime.text = timeFormatter.format(pickedTime)
                tvTz.text = "MDT"
                tvTz.visibility = View.VISIBLE
            }

            picker.show(parentFragmentManager, "time_picker")
        }
        fun validateFirstName(): String? {
            tilFirstName.error = null
            tilFirstName.isErrorEnabled = false

            val firstName = etFirstName.text?.toString()?.trim().orEmpty()
            if (firstName.isBlank()) {
                tilFirstName.isErrorEnabled = true
                tilFirstName.error = getString(R.string.error_missing_required_data)
                return null
            }
            return firstName
        }


        // Next
        btnNext.setOnClickListener {
            val firstName = validateFirstName() ?: return@setOnClickListener
            val lastName = etLastName.text?.toString()?.trim().orEmpty()
            val location = etLocation.text?.toString()?.trim().orEmpty()
            val state = actState.text?.toString()?.trim().orEmpty()
            val zip = etZip.text?.toString()?.trim().orEmpty()
            val chosenDate = selectedDate
            val chosenTime = selectedTime
            Toast.makeText(
                requireContext(),
                "Next\n$firstName $lastName\n$location, $state $zip\nDate=$chosenDate Time=$chosenTime",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.action_visitFormFragment2_to_visitFormFragment3)
        }

        // Skip
        btnSkip.setOnClickListener {
            val firstName = validateFirstName() ?: return@setOnClickListener
            findNavController().navigate(R.id.action_visitFormFragment2_to_visitFormFragment3)
        }
        // Previous
        btnPrevious.setOnClickListener {
            findNavController().navigateUp()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupStateDropdown(actState: MaterialAutoCompleteTextView) {
        val states = listOf(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, states)
        actState.setAdapter(adapter)


        actState.inputType = InputType.TYPE_CLASS_TEXT
        actState.isCursorVisible = false
        actState.showSoftInputOnFocus = false

        fun toggleDropdown() {
            actState.requestFocus()
            if (actState.isPopupShowing) {
                actState.dismissDropDown()
            } else {
                actState.showDropDown()
            }
        }

        actState.setOnClickListener {
            if (actState.isPopupShowing) actState.dismissDropDown()
        }


    }
}
