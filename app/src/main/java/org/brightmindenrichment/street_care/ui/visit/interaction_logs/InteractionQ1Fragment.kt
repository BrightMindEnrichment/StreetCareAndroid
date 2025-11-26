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
import com.google.firebase.firestore.FirebaseFirestore
import org.brightmindenrichment.street_care.ui.visit.InteractionLogDataAdapter


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

//        val db = FirebaseFirestore.getInstance()
//        db.collection("InteractionLog").whereEqualTo("email", "monicasri@brightmindenrichment.org")
//            //.document("monicasri@brightmindenrichment.org")
//
//
//            .get()
//            .addOnSuccessListener { result ->
//                if (result.isEmpty) {
//                    Log.d("Test", "No documents found")
//                } else {
//                    for (doc in result) {
//                        Log.d("Test", "Found doc: ${doc.id} -> ${doc.data}")
//                    }
//                }
//            }
        //******************
//            .addOnSuccessListener { doc ->
//                if (doc.exists()) {
//                    Log.d("Test", "Document data: ${doc.data}")
//                } else {
//                    Log.d("Test", "Document does not exist")
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("Test", "Error fetching document", e)
//            }
//**********************
//        val adapter = InteractionLogDataAdapter()
//        adapter.refreshAll {
//            Log.d("Test", "Fetched ${adapter.size} interactions")
//            adapter.interactions.forEach { log ->
//                Log.d("Test", log.toString())
//            }
//        }

        val adapter = InteractionLogDataAdapter()

        adapter.refreshAll {
            Log.d("FetchTest", "🔥 Fetch complete. Count = ${adapter.size}")

            adapter.interactions.forEachIndexed { index, log ->
                Log.d("FetchTest", "[$index] ID=${log.id}, Email=${log.email}, UserId=${log.userId}")
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
//
//    private var _binding: FragmentQuestion1Binding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: InteractionLogViewModel by activityViewModels()
//
//    private val calendarStart = Calendar.getInstance()
//    private val calendarEnd = Calendar.getInstance()
//
//    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
//    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentQuestion1Binding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        setupDatePickerStart()
//        setupTimePickerStart()
//
//        setupDatePickerEnd()
//        setupTimePickerEnd()
//
//        setupTimezonePicker()
//
//        setupNextButton()
//        setupCloseButton()
//    }
//
//    private fun setupDatePickerStart() {
//        binding.datePickerCard.setOnClickListener {
//            val c = calendarStart
//            DatePickerDialog(
//                requireContext(),
//                { _, y, m, d ->
//                    c.set(y, m, d)
//                    binding.datePickerAction.text = dateFormat.format(c.time)
//                    viewModel.updateInteractionDateStart(c.time)
//                },
//                c.get(Calendar.YEAR),
//                c.get(Calendar.MONTH),
//                c.get(Calendar.DAY_OF_MONTH)
//            ).show()
//        }
//    }
//
//    private fun setupTimePickerStart() {
//        binding.timePickerCard.setOnClickListener {
//            val c = calendarStart
//            TimePickerDialog(
//                requireContext(),
//                { _, hour, minute ->
//                    c.set(Calendar.HOUR_OF_DAY, hour)
//                    c.set(Calendar.MINUTE, minute)
//                    binding.startTime.text = timeFormat.format(c.time)
//                    viewModel.updateInteractionTimeStart(c.time)
//                },
//                c.get(Calendar.HOUR_OF_DAY),
//                c.get(Calendar.MINUTE),
//                false
//            ).show()
//        }
//    }
//
//    private fun setupDatePickerEnd() {
//        binding.datePickerCard1.setOnClickListener {
//            val c = calendarEnd
//            DatePickerDialog(
//                requireContext(),
//                { _, y, m, d ->
//                    c.set(y, m, d)
//                    binding.datePickerActions.text = dateFormat.format(c.time)
//                    viewModel.updateInteractionDateEnd(c.time)
//                },
//                c.get(Calendar.YEAR),
//                c.get(Calendar.MONTH),
//                c.get(Calendar.DAY_OF_MONTH)
//            ).show()
//        }
//    }
//
//    private fun setupTimePickerEnd() {
//        binding.timePickerCard1.setOnClickListener {
//            val c = calendarEnd
//            TimePickerDialog(
//                requireContext(),
//                { _, hour, minute ->
//                    c.set(Calendar.HOUR_OF_DAY, hour)
//                    c.set(Calendar.MINUTE, minute)
//                    binding.timePicker.text = timeFormat.format(c.time)
//                    viewModel.updateInteractionTimeEnd(c.time)
//                },
//                c.get(Calendar.HOUR_OF_DAY),
//                c.get(Calendar.MINUTE),
//                false
//            ).show()
//        }
//    }
//
//    private fun setupTimezonePicker() {
//        binding.timezonePickerCard.setOnClickListener {
//            val dialog = android.app.AlertDialog.Builder(requireContext())
//            val zones = arrayOf("UTC", "EST", "CST", "MST", "PST")
//            dialog.setItems(zones) { _, which ->
//                binding.timezoneText.text = zones[which]
//                viewModel.updateTimezone(zones[which])
//            }
//            dialog.show()
//        }
//    }
//
//    private fun setupNextButton() {
//        binding.txtNext2.setOnClickListener {
//
//            if (!validateInputs()) return@setOnClickListener
//
//            // Navigate to next screen
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, InteractionQ2Fragment())
//                .addToBackStack(null)
//                .commit()
//        }
//    }
//
//    private fun validateInputs(): Boolean {
//        var isValid = true
//
//        if (binding.datePickerAction.text.toString().contains("Start Date", ignoreCase = true)) {
//            binding.dateErrorText.text = "Please select a start date"
//            binding.dateErrorText.visibility = View.VISIBLE
//            isValid = false
//        } else {
//            binding.dateErrorText.visibility = View.GONE
//        }
//
//        if (binding.startTime.text.toString().contains("Enter", ignoreCase = true)) {
//            binding.timeErrorText.text = "Please select a start time"
//            binding.timeErrorText.visibility = View.VISIBLE
//            isValid = false
//        } else {
//            binding.timeErrorText.visibility = View.GONE
//        }
//
//        return isValid
//    }
//
//    private fun setupCloseButton() {
//        binding.btnClose.setOnClickListener {
//            requireActivity().onBackPressedDispatcher.onBackPressed()
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
}