//package org.brightmindenrichment.street_care.ui.visit.visit_forms
//
//
//import android.app.DatePickerDialog
//import android.app.TimePickerDialog
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.activityViewModels
//import androidx.navigation.fragment.findNavController
//import org.brightmindenrichment.street_care.R
//import org.brightmindenrichment.street_care.databinding.FragmentVisitForm2Binding
//import org.brightmindenrichment.street_care.util.Extensions
//import java.text.SimpleDateFormat
//import android.widget.ListView
//import android.widget.ArrayAdapter
//import androidx.appcompat.app.AlertDialog
//import java.util.*
//
//class VisitFormFragment2 : Fragment() {
//    private var _binding: FragmentVisitForm2Binding? = null
//    val binding get() = _binding!!
//
//    private val sharedVisitViewModel: VisitViewModel by activityViewModels()
//    private val myCalendar: Calendar = Calendar.getInstance()
//    private var displayDateFormat: String = "MM/dd/yyyy"
//    private var selectedTimezone = TimeZone.getDefault().id
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentVisitForm2Binding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.datePickerActions.text = getString(R.string.enter_date)
//        binding.timePicker.text = getString(R.string.enter_time)
//
//        // Always keep error icons gone since we only want built-in errors
//        //binding.dateErrorIcon.visibility = View.GONE
//        //binding.timeErrorIcon.visibility = View.GONE
//
//        // Set default timezone
//        val defaultTz = TimeZone.getDefault()
//        val abbreviation = defaultTz.getDisplayName(defaultTz.inDaylightTime(Date()), TimeZone.SHORT)
//        val defaultTzDisplay = "${getRegionName(defaultTz.id)} ($abbreviation)"
//        binding.timezoneText.text = defaultTzDisplay
//        selectedTimezone = defaultTz.id
//
//        val currentDate = Extensions.dateToString(myCalendar.time, displayDateFormat)
//        displayDate(currentDate)
//        updateTimeDisplay()
//
//        binding.datePickerActions.setOnClickListener {
//            binding.datePickerActions.error = null
//            // No need to change icon visibility
//
//            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
//                myCalendar.set(Calendar.YEAR, year)
//                myCalendar.set(Calendar.MONTH, month)
//                myCalendar.set(Calendar.DAY_OF_MONTH, day)
//                displayDate(Extensions.dateToString(myCalendar.time, displayDateFormat))
//                sharedVisitViewModel.visitLog.date = myCalendar.time
//            }
//
//            DatePickerDialog(
//                requireContext(),
//                dateSetListener,
//                myCalendar.get(Calendar.YEAR),
//                myCalendar.get(Calendar.MONTH),
//                myCalendar.get(Calendar.DAY_OF_MONTH)
//            ).show()
//        }
//
//        binding.timePicker.setOnClickListener {
//            binding.timePicker.error = null
//            // No need to change icon visibility
//
//            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
//                myCalendar.set(Calendar.HOUR_OF_DAY, hour)
//                myCalendar.set(Calendar.MINUTE, minute)
//                updateTimeDisplay()
//            }
//
//            TimePickerDialog(
//                context,
//                timeSetListener,
//                myCalendar.get(Calendar.HOUR_OF_DAY),
//                myCalendar.get(Calendar.MINUTE),
//                false
//            ).show()
//        }
//
//        binding.timezonePickerCard.setOnClickListener {
//            showTimezoneDialog()
//        }
//
//        binding.txtNext2.setOnClickListener {
//            // Check if date and time are filled in
//            val date = binding.datePickerActions.text.toString().trim()
//            val time = binding.timePicker.text.toString().trim()
//            var hasError = false
//
//            if (isInvalidDate(date)) {
//                // Only set the error text, no need to show the icon
//                binding.datePickerActions.error = getString(R.string.error_date_required)
//                hasError = true
//            } else {
//                binding.datePickerActions.error = null
//            }
//
//            if (isInvalidTime(time)) {
//                // Only set the error text, no need to show the icon
//                binding.timePicker.error = getString(R.string.error_time_required)
//                hasError = true
//            } else {
//                binding.timePicker.error = null
//            }
//
//            if (hasError) {
//                // Show toast message when validation fails, just like Fragment 1
//                Toast.makeText(requireContext(), getString(R.string.error_date_time_required), Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Proceed with the data saving and navigation if validation passes
//            sharedVisitViewModel.visitLog.whenVisitTime = time
//            sharedVisitViewModel.visitLog.timeZone = selectedTimezone
//
//            if (time.contains(":")) {
//                val splitTime = time.split(":")
//                val hour = splitTime[0].toIntOrNull()
//                val minute = splitTime.getOrNull(1)?.substring(0, 2)?.toIntOrNull()
//                val isPM = time.lowercase().contains("pm")
//
//                if (hour != null && minute != null) {
//                    val hour24 = if (isPM && hour < 12) hour + 12 else if (!isPM && hour == 12) 0 else hour
//
//                    // Compose user-entered ZonedDateTime in selected timezone
//                    val userZoneId = java.time.ZoneId.of(selectedTimezone)
//                    val localDateTime = java.time.LocalDateTime.of(
//                        myCalendar.get(Calendar.YEAR),
//                        myCalendar.get(Calendar.MONTH) + 1,
//                        myCalendar.get(Calendar.DAY_OF_MONTH),
//                        hour24,
//                        minute
//                    )
//
//                    val userZonedDateTime = java.time.ZonedDateTime.of(localDateTime, userZoneId)
//
//                    // Convert to New York time
//                    val nyZonedDateTime = userZonedDateTime.withZoneSameInstant(java.time.ZoneId.of("America/New_York"))
//
//                    // Save final NY-based date
//                    sharedVisitViewModel.visitLog.date = Date.from(nyZonedDateTime.toInstant())
//                }
//            }
//
//            findNavController().navigate(R.id.action_visitFormFragment2_to_visitFormFragment1)
//        }
//
//        binding.txtPrevious2.setOnClickListener {
//            findNavController().navigate(R.id.action_visitFormFragment2_to_nav_visit)
//        }
//    }
//
//    private fun displayDate(dateString: String) {
//        binding.datePickerActions.text = dateString
//    }
//
//    private fun updateTimeDisplay() {
//        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
//        val formattedTime = timeFormat.format(myCalendar.time)
//        binding.timePicker.text = formattedTime
//    }
//
//    private fun getRegionName(timezoneId: String): String {
//        return timezoneId.split("/").lastOrNull()?.replace("_", " ") ?: timezoneId
//    }
//
//    private fun showTimezoneDialog() {
//        val seenAbbreviations = mutableSetOf<String>()
//        val displayList = mutableListOf<String>()
//        val displayToZoneId = mutableMapOf<String, String>()
//
//        for (zoneId in TimeZone.getAvailableIDs()) {
//            if (zoneId.startsWith("Etc/") || zoneId.startsWith("SystemV") || !zoneId.contains("/")) continue
//            val tz = TimeZone.getTimeZone(zoneId)
//            val abbreviation = tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)
//
//            if (abbreviation in seenAbbreviations) continue
//            seenAbbreviations.add(abbreviation)
//            val city = zoneId.substringAfterLast("/").replace("_", " ")
//            val display = "$city ($abbreviation)"
//            displayList.add(display)
//            displayToZoneId[display] = zoneId
//        }
//
//        displayList.sort()
//        val listView = ListView(requireContext())
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
//        listView.adapter = adapter
//
//        val dialog = AlertDialog.Builder(requireContext())
//            .setTitle("Select Timezone")
//            .setView(listView)
//            .create()
//
//        listView.setOnItemClickListener { _, _, position, _ ->
//            val selectedDisplay = adapter.getItem(position) ?: return@setOnItemClickListener
//            selectedTimezone = displayToZoneId[selectedDisplay] ?: selectedTimezone
//            binding.timezoneText.text = selectedDisplay
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
//
//    private fun isInvalidDate(date: String): Boolean {
//        val placeholders = listOf(
//            getString(R.string.enter_date),
//            getString(R.string.enter_date) + "*",
//            getString(R.string.error_date_required)
//        )
//        return date.isEmpty() || placeholders.contains(date)
//    }
//
//    private fun isInvalidTime(time: String): Boolean {
//        val placeholders = listOf(
//            getString(R.string.enter_time),
//            getString(R.string.enter_time) + "*",
//            getString(R.string.error_time_required)
//        )
//        return time.isEmpty() || placeholders.contains(time)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        _binding = null
//    }
//}