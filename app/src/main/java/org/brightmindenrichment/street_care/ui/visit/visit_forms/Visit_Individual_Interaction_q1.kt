package org.brightmindenrichment.street_care.ui.visit.visit_forms

import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import org.brightmindenrichment.street_care.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Visit_Individual_Interaction_q1 : Fragment() {

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    private val sharedVisitViewModel: VisitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_individual_interaction_q1, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //(activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //(activity as? AppCompatActivity)?.supportActionBar
        //    ?.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)

        sharedVisitViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
            tvHeader.text = if (idx <= 1) {
                getString(R.string.individual_interaction_title_base)  // e.g. "Individual Interaction"
            } else {
                getString(R.string.individual_interaction_title_numbered, idx) // e.g. "Individual Interaction 2"
            }
        }

        // Inputs
        val etFirstName = view.findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName  = view.findViewById<TextInputEditText>(R.id.etLastName)
        val etLocation  = view.findViewById<TextInputEditText>(R.id.etLocation)
        val actState    = view.findViewById<MaterialAutoCompleteTextView>(R.id.actState)
        val etZip       = view.findViewById<TextInputEditText>(R.id.etZip)

        // Date/Time
        val dateCard = view.findViewById<MaterialCardView>(R.id.datePickerCard)
        val timeCard = view.findViewById<MaterialCardView>(R.id.timePickerCard)
        val tvDate   = view.findViewById<TextView>(R.id.tvDate)
        val tvTime   = view.findViewById<TextView>(R.id.tvTime)

        // Buttons
        val btnPrevious = view.findViewById<TextView>(R.id.txt_previous2)
        val btnNext     = view.findViewById<TextView>(R.id.txt_next2)
        val btnSkip     = view.findViewById<TextView>(R.id.txt_skip)
        val btnClose    = view.findViewById<ImageButton>(R.id.btnClose)

        // Optional: state dropdown items (if you have an array)
        // If you already set adapter elsewhere, remove this.
        try {
            val states = resources.getStringArray(R.array.us_states)
            actState.setSimpleItems(states)
        } catch (_: Exception) {
            // ignore if array not present
        }

        // Date picker (same approach as Q4)
        dateCard.setOnClickListener {
            val mountainZone = ZoneId.of("America/Denver")
            val baseDate = selectedDate ?: LocalDate.now(mountainZone)

            val initialMillis = baseDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

            val picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_StreetCare_DatePicker)
                .setTitleText(getString(R.string.select_interaction_date))
                .setSelection(initialMillis)
                .build()

            picker.addOnPositiveButtonClickListener { millis ->
                val pickedDate = Instant.ofEpochMilli(millis)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate()

                selectedDate = pickedDate
                tvDate.error = null
                tvDate.text = dateFormatter.format(pickedDate)
            }

            picker.show(parentFragmentManager, "date_picker_q1")
        }

        // Time picker (same approach as Q4)
        timeCard.setOnClickListener {
            val mountainZone = ZoneId.of("America/Denver")
            val baseTime = selectedTime ?: LocalTime.now(mountainZone)

            val picker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_StreetCare_TimePicker)
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                .setHour(baseTime.hour)
                .setMinute(baseTime.minute)
                .setTitleText(getString(R.string.select_interaction_time))
                .build()

            picker.addOnPositiveButtonClickListener {
                val pickedTime = LocalTime.of(picker.hour, picker.minute)
                selectedTime = pickedTime
                tvTime.error = null
                tvTime.text = timeFormatter.format(pickedTime)
            }

            picker.show(parentFragmentManager, "time_picker_q1")
        }

        // Close: exit entire flow
        btnClose.setOnClickListener {
            findNavController().popBackStack(R.id.nav_visit, false)
        }

        // Previous -> back stack
        btnPrevious.setOnClickListener {
            findNavController().navigateUp()
        }

        // Skip -> go to Q2 (no validation)
        btnSkip.setOnClickListener {
            findNavController().navigate(
                R.id.action_visitIndividualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }

        // Next -> validate -> go to Q2
        btnNext.setOnClickListener {
            // clear old errors (we only have EditTexts, so just validate with messages)
            tvDate.error = null
            tvTime.error = null

            val first = etFirstName.text?.toString()?.trim().orEmpty()
            val last  = etLastName.text?.toString()?.trim().orEmpty()
            val loc   = etLocation.text?.toString()?.trim().orEmpty()
            val state = actState.text?.toString()?.trim().orEmpty()
            val zip   = etZip.text?.toString()?.trim().orEmpty()

            // You can switch these to TextInputLayout errors if you want (better UX),
            // but I’m staying consistent with your Q4 style.
            if (first.isEmpty()) {
                etFirstName.error = "Required"
                return@setOnClickListener
            }
            if (last.isEmpty()) {
                etLastName.error = "Required"
                return@setOnClickListener
            }
            if (loc.isEmpty()) {
                etLocation.error = "Required"
                return@setOnClickListener
            }
            if (state.isEmpty()) {
                actState.error = "Required"
                return@setOnClickListener
            }

            // ZIP rule: adjust if not strictly US
            if (zip.length < 5) {
                etZip.error = "Invalid"
                return@setOnClickListener
            }

            if (selectedDate == null) {
                tvDate.error = "Required"
                return@setOnClickListener
            }
            if (selectedTime == null) {
                tvTime.error = "Required"
                return@setOnClickListener
            }

            // TODO: persist Q1 data somewhere (NavGraph ViewModel recommended)

            findNavController().navigate(
                R.id.action_visitIndividualInteractionQ1_to_visitIndividualInteractionQ2
            )
        }
    }
}
