package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import androidx.fragment.app.activityViewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.textfield.TextInputEditText
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Visit_Individual_Interaction_q4 : Fragment() {

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    private val viewModel: InteractionLogViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_individual_interaction_q4, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)

        viewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
            tvHeader.text = if (idx <= 1) {
                getString(R.string.individual_interaction_title_base)  // e.g. "Individual Interaction"
            } else {
                getString(R.string.individual_interaction_title_numbered, idx) // e.g. "Individual Interaction 2"
            }
        }

        val dateCard = view.findViewById<MaterialCardView>(R.id.datePickerCard)
        val timeCard = view.findViewById<MaterialCardView>(R.id.timePickerCard)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val etNotes = view.findViewById<TextInputEditText>(R.id.etNotes)

        val btnPrevious = view.findViewById<TextView>(R.id.txt_previous2)
        val btnSave = view.findViewById<TextView>(R.id.txt_next2)
        val btnSkip = view.findViewById<TextView>(R.id.txt_skip)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

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

            picker.show(parentFragmentManager, "date_picker_q4")
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
            }

            picker.show(parentFragmentManager, "time_picker_q4")
        }

        // Close - Exit Screen
        btnClose.setOnClickListener {
            findNavController().popBackStack(R.id.nav_visit, false)
        }

        // Previous -> back to Q3 (back stack)
        btnPrevious.setOnClickListener {
            findNavController().navigateUp()
        }

        // Skip -> go to saveInteraction (no validation)
        btnSkip.setOnClickListener {
            viewModel.nextInteraction()
            findNavController().navigate(R.id.individualInteractionFragment)
        }

        // Save -> go to saveInteraction
        btnSave.setOnClickListener {
            val notes = etNotes.text?.toString()?.trim().orEmpty()
            tvDate.error = null
            tvTime.error = null

            if (selectedDate == null) {
                tvDate.error = "Required"
                return@setOnClickListener
            }
            if (selectedTime == null) {
                tvTime.error = "Required"
                return@setOnClickListener
            }

            // TODO: persist selectedDate/selectedTime/notes somewhere (ViewModel, shared repo, arguments, etc.)

            viewModel.nextInteraction()
            findNavController().navigate(R.id.individualInteractionFragment)
        }
    }
}
