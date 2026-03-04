package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ4Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class IndividualInteractionQ4 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ4Binding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualInteractionQ4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE

        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setHomeAsUpIndicator(R.drawable.ic_close_red_circle)
            ab.title = "Individual Interaction"
        }

        interactionLogViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
            binding.tvHeader.text = if (idx <= 1) {
                getString(R.string.individual_interaction_title_base)
            } else {
                getString(R.string.individual_interaction_title_numbered, idx)
            }
        }

        // Restore previously entered follow-up data when navigating back
        viewModel.currentInteraction.value?.let { saved ->
            saved.followUpDate?.let {
                selectedDate = LocalDate.parse(it)
                binding.tvDate.text = dateFormatter.format(selectedDate)
            }
            saved.followUpTime?.let {
                selectedTime = LocalTime.parse(it)
                binding.tvTime.text = timeFormatter.format(selectedTime)
            }
            saved.additionalDetails?.let { binding.etNotes.setText(it) }
        }

        // Date picker
        binding.datePickerCard.setOnClickListener {
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
                binding.tvDate.text = dateFormatter.format(pickedDate)
            }

            picker.show(parentFragmentManager, "date_picker_q4")
        }

        // Time picker
        binding.timePickerCard.setOnClickListener {
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
                binding.tvTime.text = timeFormatter.format(pickedTime)
            }

            picker.show(parentFragmentManager, "time_picker_q4")
        }

        // Previous -> back to Q3
        binding.txtPrevious2.setOnClickListener {
            findNavController().navigateUp()
        }

        // Skip -> commit with no follow-up data and return
        binding.txtSkip.setOnClickListener {
            viewModel.saveQ4(null, null, null)
            interactionLogViewModel.nextInteraction()
            findNavController().navigate(R.id.individualInteractionFragment)
        }

        // Save -> validate, persist, then return to list
        binding.txtNext2.setOnClickListener {
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
            binding.tvDate.error = null
            binding.tvTime.error = null

            if (selectedDate == null) {
                binding.tvDate.error = "Required"
                return@setOnClickListener
            }
            if (selectedTime == null) {
                binding.tvTime.error = "Required"
                return@setOnClickListener
            }

            viewModel.saveQ4(
                selectedDate.toString(),
                selectedTime.toString(),
                notes.takeUnless { it.isEmpty() }
            )
            interactionLogViewModel.nextInteraction()
            findNavController().navigate(R.id.individualInteractionFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
