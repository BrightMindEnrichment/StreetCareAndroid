package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentQuestion1Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.InteractionLogViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.brightmindenrichment.street_care.ui.visit.InteractionLogDataAdapter
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.repository.InteractionLogRepositoryImpl


class InteractionQ1Fragment: Fragment() {


    private var _binding: FragmentQuestion1Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestion1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val adapter = InteractionLogDataAdapter()

        /* Test fetching by Document ID */
        adapter.fetchByDocumentId(
            documentId = "AB342F2E-28E5-4D19-B8B5-E68DDAD8D032"
        ) { success ->

            Log.d("FetchTest", "Fetch complete. Success = $success, Count = ${adapter.size}")

            adapter.interactions.forEachIndexed { index, log ->
                Log.d("FetchTest", "[$index] $log")
            }
        }


        /* Test saving a new InteractionLog */
        val repository = InteractionLogRepositoryImpl()

        val testLog = InteractionLog(
            firstName = "Test",
            lastName = "User",
            city = "San Jose",
            state = "CA",
            isPublic = true,
            numPeopleHelped = 5,
            interactionDate = Timestamp.now()
        )

        repository.saveInteractionLog(testLog) { success, documentId ->
            if (success) {
                Log.d("FirestoreTest", "Saved! Document ID = $documentId")
            } else {
                Log.e("FirestoreTest", "Save failed")
            }
        }


        setStartDatePicker()
        setStartTimePicker()
        setEndDatePicker()
        setEndTimePicker()
        setTimezonePicker()
        setCloseButton()
        setNextButton()
    }

    // --------------------------------------------------------
    // Date Picker (Start Date)
    // --------------------------------------------------------
    private fun setStartDatePicker() {
        binding.datePickerCard.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Start Date")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                startCalendar.timeInMillis = selection
                val date = startCalendar.time

                binding.startDate.text = dateFormatter.format(date)

                // Update ViewModel
                mergeDateTimeIntoViewModel(start = true)
            }

            picker.show(parentFragmentManager, "DATE_PICK")
        }
    }

    // --------------------------------------------------------
    // Time Picker (Start Time)
    // --------------------------------------------------------
    private fun setStartTimePicker() {
        binding.timePickerCard.setOnClickListener {
            val hour = startCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = startCalendar.get(Calendar.MINUTE)

            val dialog = TimePickerDialog(requireContext(), { _, h, m ->
                startCalendar.set(Calendar.HOUR_OF_DAY, h)
                startCalendar.set(Calendar.MINUTE, m)

                binding.startTime.text = timeFormatter.format(startCalendar.time)

                // Update ViewModel
                mergeDateTimeIntoViewModel(start = true)

            }, hour, minute, false)

            dialog.show()
        }
    }


    // --------------------------------------------------------
    // Merge selected date + time into ViewModel
    // --------------------------------------------------------
    private fun mergeDateTimeIntoViewModel(start: Boolean) {
        val finalDate: Date = startCalendar.time
        viewModel.interactionDate.value = finalDate
    }

    // --------------------------------------------------------
    // END DATE
    // --------------------------------------------------------
    private fun setEndDatePicker() {
        binding.datePickerCard1.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select End Date")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                endCalendar.timeInMillis = selection
                val date = endCalendar.time

                binding.endDate.text = dateFormatter.format(date)
            }

            picker.show(parentFragmentManager, "END_DATE_PICK")
        }
    }

    // --------------------------------------------------------
    // END TIME
    // --------------------------------------------------------
    private fun setEndTimePicker() {
        binding.timePickerCard1.setOnClickListener {
            val hour = endCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = endCalendar.get(Calendar.MINUTE)

            val dialog = TimePickerDialog(requireContext(), { _, h, m ->
                endCalendar.set(Calendar.HOUR_OF_DAY, h)
                endCalendar.set(Calendar.MINUTE, m)

                binding.endTime.text = timeFormatter.format(endCalendar.time)
            }, hour, minute, false)

            dialog.show()
        }
    }

    // --------------------------------------------------------
    // TIMEZONE PICKER (placeholder — fill with your list)
    // --------------------------------------------------------
    private fun setTimezonePicker() {
        binding.timezonePickerCard.setOnClickListener {

            // TODO: Replace this with your dialog/list
            binding.timezoneText.text = "UTC-06:00"
        }
    }


    // --------------------------------------------------------
    // Close Button
    // --------------------------------------------------------
    private fun setCloseButton() {
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // --------------------------------------------------------
    // Next Button
    // --------------------------------------------------------
    private fun setNextButton() {
        binding.txtNext2.setOnClickListener {

            // Simple validation
            if (binding.startDate.text == getString(R.string.start_date)) {
                binding.dateErrorText.text = "Please select a date"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                binding.dateErrorText.visibility = View.GONE
            }

            if (binding.startTime.text == getString(R.string.enter_time_without_star)) {
                binding.timeErrorText.text = "Please select a time"
                binding.timeErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                binding.timeErrorText.visibility = View.GONE
            }

            if (binding.endDate.text == getString(R.string.end_date)) {
                binding.dateErrorText.text = "Please select a date"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                binding.dateErrorText.visibility = View.GONE
            }

            // --- Validate End Time ---
            if (binding.endTime.text == getString(R.string.enter_time_without_star)) {
                binding.timeErrorText.visibility = View.VISIBLE
                binding.timeErrorText.text = "Please select an end time"
                return@setOnClickListener
            }

            binding.dateErrorText.visibility = View.GONE
            binding.timeErrorText.visibility = View.GONE

            // Optionally: check start < end
            if (startCalendar.time.after(endCalendar.time)) {
                binding.dateErrorText.visibility = View.VISIBLE
                binding.dateErrorText.text = "End time must be after start time"
                return@setOnClickListener
            }

            // Navigate to Q2
            //findNavController().navigate(R.id.action_interactionQ1_to_interactionQ2)
            try {
                findNavController().navigate(R.id.action_interactionQ1_to_interactionQ2)
            } catch (e: IllegalArgumentException) {
                Log.e("NavDebug", "Navigation failed: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}