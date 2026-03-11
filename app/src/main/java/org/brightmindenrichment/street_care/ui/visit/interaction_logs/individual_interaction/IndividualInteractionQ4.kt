package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionQ4Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.brightmindenrichment.street_care.util.toLocalDateFromPicker
import org.brightmindenrichment.street_care.util.toPickerMillis
import org.brightmindenrichment.street_care.util.toZonedString
import org.brightmindenrichment.street_care.util.formatTimeWithTz

class IndividualInteractionQ4 : Fragment() {

    private var _binding: FragmentIndividualInteractionQ4Binding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    private val interactionLogViewModel: InteractionLogViewModel by activityViewModels()
    private val viewModel: IndividualInteractionViewModel by activityViewModels()

    /** Get the timezone from the InteractionLogViewModel (set in ILq1). */
    private fun getInteractionTimezone(): ZoneId {
        val tzString = interactionLogViewModel.interactionLog.value?.timezone
        return if (tzString.isNullOrBlank()) {
            ZoneId.systemDefault()
        } else {
            try {
                ZoneId.of(tzString)
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }
        }
    }


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

        val editHeader = viewModel.editingHeaderText()
        if (editHeader != null) {
            binding.tvHeader.text = editHeader
        } else {
            interactionLogViewModel.interactionIndex.observe(viewLifecycleOwner) { idx ->
                binding.tvHeader.text = if (idx <= 1) {
                    getString(R.string.individual_interaction_title_base)
                } else {
                    getString(R.string.individual_interaction_title_numbered, idx)
                }
            }
        }

        // Observe timezone changes and refresh time display
        interactionLogViewModel.interactionLog.observe(viewLifecycleOwner) { _ ->
            if (selectedTime != null && selectedDate != null) {
                binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
            }
        }

        // Restore previously entered follow-up data when navigating back
        viewModel.currentInteraction.value?.let { saved ->
            saved.followUpDate?.let {
                selectedDate = LocalDate.parse(it)
                binding.tvDate.text = dateFormatter.format(selectedDate)
            }
            saved.followUpTime?.let {
                try {
                    // Try to parse as ZonedDateTime first (full timestamp with timezone)
                    val zdt = ZonedDateTime.parse(it)
                    selectedTime = zdt.toLocalTime()
                    if (selectedDate != null) {
                        binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                    }
                } catch (e: Exception) {
                    // Fall back to LocalTime parsing if it's just a time string
                    try {
                        selectedTime = LocalTime.parse(it)
                        if (selectedDate != null) {
                            binding.tvTime.text = formatTimeWithTz(selectedDate!!, selectedTime!!, getInteractionTimezone())
                        }
                    } catch (e2: Exception) {
                        // Silently ignore if parsing fails
                    }
                }
            }
            saved.additionalDetails?.let { binding.etNotes.setText(it) }
        }

        // Date picker
        binding.datePickerCard.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val keyboardWasVisible = imm.isActive

            val pickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_StreetCare_DatePicker)
                .setTitleText(getString(R.string.select_interaction_date))

            // Only set selection if a date was previously selected
            if (selectedDate != null) {
                pickerBuilder.setSelection(selectedDate!!.toPickerMillis())
            }

            val picker = pickerBuilder.build()

            picker.addOnPositiveButtonClickListener { millis ->
                val pickedDate = millis.toLocalDateFromPicker()
                selectedDate = pickedDate
                binding.tvDate.text = dateFormatter.format(pickedDate)

                // If time is already selected, update its display with new timezone abbrev for this date
                if (selectedTime != null) {
                    binding.tvTime.text = formatTimeWithTz(pickedDate, selectedTime!!, getInteractionTimezone())
                }

                // Restore keyboard state
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.addOnDismissListener {
                // Restore keyboard state on dismiss (cancel or outside tap)
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.show(parentFragmentManager, "date_picker_q4")
        }

        // Time picker
        binding.timePickerCard.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val keyboardWasVisible = imm.isActive

            val pickerBuilder = MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_StreetCare_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)

            // Default to selected timezone's current time if no time selected yet
            val defaultTime = selectedTime ?: LocalTime.now(getInteractionTimezone())
            pickerBuilder.setHour(defaultTime.hour)
            pickerBuilder.setMinute(defaultTime.minute)

            val picker = pickerBuilder
                .setTitleText(getString(R.string.select_interaction_time))
                .build()

            picker.addOnPositiveButtonClickListener {
                val pickedTime = LocalTime.of(picker.hour, picker.minute)
                selectedTime = pickedTime
                if (selectedDate != null) {
                    binding.tvTime.text = formatTimeWithTz(selectedDate!!, pickedTime, getInteractionTimezone())
                }
                // Restore keyboard state
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.addOnDismissListener {
                // Restore keyboard state on dismiss (cancel or outside tap)
                if (!keyboardWasVisible) {
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

            picker.show(parentFragmentManager, "time_picker_q4")
        }

        // Previous -> back to Q3
        binding.txtPrevious2.setOnClickListener {
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
            val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
            val editingIdx = viewModel.editingIndex          // capture BEFORE saveQ4 resets it
            viewModel.saveQ4(
                selectedDate?.toString(),
                selectedTime?.toString(),
                notes.takeUnless { it.isEmpty() },
                timeWithTz
            )
            mergeIntoILAndSave(editingIdx) {
                findNavController().popBackStack()
            }
        }

        // Skip -> commit with no follow-up data and return
        binding.txtSkip.setOnClickListener {
            val editingIdx = viewModel.editingIndex          // capture BEFORE saveQ4 resets it
            viewModel.saveQ4(null, null, null, null)
            mergeIntoILAndSave(editingIdx) {
                if (editingIdx == null) interactionLogViewModel.nextInteraction()
                findNavController().navigate(R.id.individualInteractionFragment)
            }
        }

        // Save -> validate, persist, then return to list
        binding.txtNext2.setOnClickListener {
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
            binding.tvDate.error = null
            binding.tvTime.error = null
            val editingIdx = viewModel.editingIndex          // capture BEFORE saveQ4 resets it
            val timeWithTz = selectedTime?.let { it.toZonedString(getInteractionTimezone()) }
            viewModel.saveQ4(
                selectedDate?.toString(),
                selectedTime?.toString(),
                notes.takeUnless { it.isEmpty() },
                timeWithTz
            )
            mergeIntoILAndSave(editingIdx) {
                if (editingIdx == null) interactionLogViewModel.nextInteraction()
                findNavController().navigate(R.id.individualInteractionFragment)
            }
        }
    }

    private fun mergeIntoILAndSave(editingIdx: Int?, onComplete: (() -> Unit)? = null) {
        val committed = viewModel.committedInteractions.value
        if (committed == null) {
            onComplete?.invoke()
            return
        }
        if (editingIdx != null) {
            val updated = committed.getOrNull(editingIdx)
            if (updated == null) {
                onComplete?.invoke()
                return
            }
            interactionLogViewModel.replaceIndividualInteraction(editingIdx, updated)
        } else {
            val newItem = committed.lastOrNull()
            if (newItem == null) {
                onComplete?.invoke()
                return
            }
            interactionLogViewModel.addIndividualInteraction(newItem)
        }
        interactionLogViewModel.saveDraft {
            onComplete?.invoke()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
