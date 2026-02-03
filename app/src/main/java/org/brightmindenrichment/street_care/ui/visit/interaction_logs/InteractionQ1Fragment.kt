package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentQuestion1Binding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class InteractionQ1Fragment : Fragment() {

    private var _binding: FragmentQuestion1Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestion1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("Q1_DEBUG", "NEW Q1 FRAGMENT LOADED")

        setStartDatePicker()
        setStartTimePicker()
        setEndDatePicker()
        setEndTimePicker()
        setTimezonePicker()
        setCloseButton()
        setNextButton()
    }

    // ---------------- Start Date ----------------
    private fun setStartDatePicker() {
        binding.datePickerCard.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Start Date")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                // Extract Y/M/D in UTC
                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCalendar.timeInMillis = selection

                // Rebuild date in LOCAL timezone (no time shift)
                startCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )

                binding.startDate.text = dateFormatter.format(startCalendar.time)

                mergeDateTimeIntoViewModel()
            }


            picker.show(parentFragmentManager, "START_DATE_PICK")
        }
    }

    // ---------------- Start Time ----------------
    private fun setStartTimePicker() {
        binding.timePickerCard.setOnClickListener {
            val dialog = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    startCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    startCalendar.set(Calendar.MINUTE, minute)
                    binding.startTime.text = timeFormatter.format(startCalendar.time)
                    mergeDateTimeIntoViewModel()
                },
                startCalendar.get(Calendar.HOUR_OF_DAY),
                startCalendar.get(Calendar.MINUTE),
                false
            )
            dialog.show()
        }
    }

    private fun mergeDateTimeIntoViewModel() {
        viewModel.interactionDate.value = startCalendar.time
    }

    // ---------------- End Date ----------------
    private fun setEndDatePicker() {
        binding.datePickerCard1.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.MyDatePickerDialogTheme)
                .setTitleText("Select End Date")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                endCalendar.timeInMillis = selection
                binding.endDate.text = dateFormatter.format(endCalendar.time)
            }

            picker.show(parentFragmentManager, "END_DATE_PICK")
        }
    }

    // ---------------- End Time ----------------
    private fun setEndTimePicker() {
        binding.timePickerCard1.setOnClickListener {
            val dialog = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    endCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    endCalendar.set(Calendar.MINUTE, minute)
                    binding.endTime.text = timeFormatter.format(endCalendar.time)
                },
                endCalendar.get(Calendar.HOUR_OF_DAY),
                endCalendar.get(Calendar.MINUTE),
                false
            )
            dialog.show()
        }
    }

    // ---------------- Timezone ----------------
    private fun setTimezonePicker() {
        binding.timezonePickerCard.setOnClickListener {
            binding.timezoneText.text = "UTC-06:00"
        }
    }

    // ---------------- Close ----------------
    private fun setCloseButton() {
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ---------------- Next ----------------
    private fun setNextButton() {
        binding.txtNext2.setOnClickListener {

            if (binding.startDate.text == getString(R.string.start_date)) {
                binding.dateErrorText.text = "Please select a date"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (binding.startTime.text == getString(R.string.enter_time_without_star)) {
                binding.timeErrorText.text = "Please select a time"
                binding.timeErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (binding.endDate.text == getString(R.string.end_date)) {
                binding.dateErrorText.text = "Please select an end date"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (binding.endTime.text == getString(R.string.enter_time_without_star)) {
                binding.timeErrorText.text = "Please select an end time"
                binding.timeErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (startCalendar.time.after(endCalendar.time)) {
                binding.dateErrorText.text = "End time must be after start time"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.dateErrorText.visibility = View.GONE
            binding.timeErrorText.visibility = View.GONE

            // NAVIGATION
            findNavController().navigate(
                R.id.action_interactionQ1_to_interactionQ2
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
