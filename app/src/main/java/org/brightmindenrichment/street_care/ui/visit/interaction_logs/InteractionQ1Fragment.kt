package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.datepicker.MaterialDatePicker
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ1Binding
import org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction.IndividualInteractionViewModel
import org.brightmindenrichment.street_care.ui.widget.StepState
import org.brightmindenrichment.street_care.ui.widget.StepValidator
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlag
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlagManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class InteractionQ1Fragment : Fragment(), StepValidator {

    private var _binding: FragmentLogInteractionQ1Binding? = null
    private val binding get() = _binding!!

    override val viewModel: InteractionLogViewModel by activityViewModels()
    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private var selectedTimezone: TimeZone = TimeZone.getDefault()
    private var wasSkipped = false
    private var isTouched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("Q1_DEBUG", "NEW Q1 FRAGMENT LOADED")

        setHasOptionsMenu(true)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showDiscardDialog()
        }

        setStartDatePicker()
        setStartTimePicker()
        setEndDatePicker()
        setEndTimePicker()
        setTimezonePicker()
        setNextButton()

        // Restore in-memory ViewModel state (same session, e.g. back-navigation from Q2)
        val log = viewModel.interactionLog.value
        val preLoaded = viewModel.draftPreLoaded
        if (preLoaded) viewModel.draftPreLoaded = false

        if (log != null) {
            // Always restore II state from the log
            iiViewModel.restoreFromInteractionLog(log.individualInteractions)
            restoreFromLog(log)
        } else {
            refreshUI()
        }

        // Set up progress bar with current step and click handler
        binding.progressBar.setCurrentStep(1)
        binding.progressBar.onDotClicked = { step ->
            saveCurrentState()
            viewModel.saveDraft {
                findNavController().popBackStack(DOT_DEST_IDS[step - 1], false)
            }
        }
    }

    private fun restoreFromLog(log: org.brightmindenrichment.street_care.ui.visit.data.InteractionLog) {
        log.startTimestamp?.let { startCalendar.timeInMillis = it.toDate().time }
        log.endTimestamp?.let { endCalendar.timeInMillis = it.toDate().time }
        if (log.timezone.isNotEmpty()) {
            selectedTimezone = TimeZone.getTimeZone(log.timezone)
        }
        refreshUI()
    }

    private fun refreshUI() {
        // Apply selected timezone to calendars for proper display and time calculations
        startCalendar.timeZone = selectedTimezone
        endCalendar.timeZone = selectedTimezone

        binding.startDate.text = dateFormatter.format(startCalendar.time)
        binding.startTime.text = formatTime(startCalendar)
        binding.endDate.text = dateFormatter.format(endCalendar.time)
        binding.endTime.text = formatTime(endCalendar)
        binding.timezoneText.text = formatTimezone(selectedTimezone)
    }

    /** Format time with DST-aware timezone abbreviation for the given calendar's date */
    private fun formatTime(calendar: Calendar): String {
        val abbrev = getTimezoneAbbreviation(selectedTimezone, calendar.timeInMillis)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = selectedTimezone
        return "${sdf.format(calendar.time)} $abbrev"
    }

    /** Get DST-aware timezone abbreviation for a specific timestamp */
    private fun getTimezoneAbbreviation(tz: TimeZone, timeInMillis: Long): String {
        return tz.getDisplayName(tz.inDaylightTime(java.util.Date(timeInMillis)), TimeZone.SHORT)
    }


    // ---------------- Start Date ----------------
    private fun setStartDatePicker() {
        binding.datePickerCard.setOnClickListener {
            val pickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.MyDatePickerDialogTheme)
                .setTitleText(getString(R.string.select_start_date))

            // Set date constraints: disable dates beyond 12 hours in the future
            val nowInTz = Calendar.getInstance(selectedTimezone)
            val maxFutureMillis = nowInTz.timeInMillis + (12 * 60 * 60 * 1000)
            val validator = com.google.android.material.datepicker.DateValidatorPointBackward.before(maxFutureMillis)
            val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(validator)
                .build()
            pickerBuilder.setCalendarConstraints(constraints)

            val picker = pickerBuilder.build()

            picker.addOnPositiveButtonClickListener { selection ->
                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCalendar.timeInMillis = selection

                startCalendar.timeZone = selectedTimezone
                startCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH)
                )

                binding.startDate.text = dateFormatter.format(startCalendar.time)
                binding.startTime.text = formatTime(startCalendar)
                viewModel.updateStartDate(startCalendar.time)
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
                    startCalendar.timeZone = selectedTimezone
                    startCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    startCalendar.set(Calendar.MINUTE, minute)
                    binding.startTime.text = formatTime(startCalendar)
                    viewModel.updateStartDate(startCalendar.time)
                },
                startCalendar.get(Calendar.HOUR_OF_DAY),
                startCalendar.get(Calendar.MINUTE),
                false
            )
            dialog.show()
        }
    }

    // ---------------- End Date ----------------
    private fun setEndDatePicker() {
        binding.datePickerCard1.setOnClickListener {
            val pickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.MyDatePickerDialogTheme)
                .setTitleText(getString(R.string.select_end_date))

            // Set date constraints: disable dates beyond 12 hours in the future
            val nowInTz = Calendar.getInstance(selectedTimezone)
            val maxFutureMillis = nowInTz.timeInMillis + (12 * 60 * 60 * 1000)
            val validator = com.google.android.material.datepicker.DateValidatorPointBackward.before(maxFutureMillis)
            val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(validator)
                .build()
            pickerBuilder.setCalendarConstraints(constraints)

            val picker = pickerBuilder.build()

            picker.addOnPositiveButtonClickListener { selection ->
                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCalendar.timeInMillis = selection

                endCalendar.timeZone = selectedTimezone
                endCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH)
                )

                binding.endDate.text = dateFormatter.format(endCalendar.time)
                binding.endTime.text = formatTime(endCalendar)
                viewModel.updateEndDate(endCalendar.time)
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
                    endCalendar.timeZone = selectedTimezone
                    endCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    endCalendar.set(Calendar.MINUTE, minute)
                    binding.endTime.text = formatTime(endCalendar)
                    viewModel.updateEndDate(endCalendar.time)
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
            showTimezonePickerDialog()
        }
    }

    private fun showTimezonePickerDialog() {
        val ctx = requireContext()
        val density = resources.displayMetrics.density

        // Filter to only proper IANA timezone IDs, exclude POSIX and special Etc/ timezones
        val ianaIds = TimeZone.getAvailableIDs()
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }

        // Group timezones by GMT offset and create a list with headers
        data class TimezoneItem(val isHeader: Boolean, val offset: Int = 0, val zone: TimeZone? = null)

        val groupedItems = mutableListOf<TimezoneItem>()
        val offsetGroups = mutableMapOf<Int, MutableList<TimeZone>>()

        // Group by current offset (DST-aware)
        for (id in ianaIds) {
            val tz = TimeZone.getTimeZone(id)
            val offset = tz.getOffset(System.currentTimeMillis())
            offsetGroups.getOrPut(offset) { mutableListOf() }.add(tz)
        }

        // Sort by offset and build items list with headers
        offsetGroups.entries
            .sortedBy { it.key }
            .forEach { (offset, zones) ->
                // Add header for this offset group
                val offsetHours = offset / 3_600_000
                val offsetMinutes = (abs(offset) % 3_600_000) / 60_000
                val sign = if (offset >= 0) "+" else "-"
                val headerText = "GMT$sign${abs(offsetHours).toString().padStart(2, '0')}:${offsetMinutes.toString().padStart(2, '0')}"
                groupedItems.add(TimezoneItem(isHeader = true, offset = offset))

                // Add zones in this group, sorted alphabetically by city
                zones.sortedBy { it.id.substringAfterLast('/') }
                    .forEach { zone ->
                        groupedItems.add(TimezoneItem(isHeader = false, zone = zone))
                    }
            }

        val pad = (16 * density).toInt()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }

        val searchField = EditText(ctx).apply {
            hint = getString(R.string.search_timezone_hint)
            setSingleLine(true)
            setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0
            )
        }
        layout.addView(
            searchField,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val listView = ListView(ctx)
        layout.addView(
            listView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (380 * density).toInt()
            )
        )

        val adapter = object : ArrayAdapter<TimezoneItem>(
            ctx,
            android.R.layout.simple_list_item_1,
            groupedItems.toMutableList()
        ) {
            private val fullList = groupedItems.toList()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)!!

                return if (item.isHeader) {
                    // Header view
                    val headerView = convertView as? TextView ?: TextView(ctx).apply {
                        setPadding(20, 16, 20, 8)
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.GRAY)
                    }
                    val offset = item.offset
                    val offsetHours = offset / 3_600_000
                    val offsetMinutes = (abs(offset) % 3_600_000) / 60_000
                    val sign = if (offset >= 0) "+" else "-"
                    headerView.text = "GMT$sign${abs(offsetHours).toString().padStart(2, '0')}:${offsetMinutes.toString().padStart(2, '0')}"
                    headerView
                } else {
                    // Regular timezone item - just show city name (offset is in header)
                    val v = super.getView(position, convertView, parent) as TextView
                    item.zone?.let { zone ->
                        val abbrev = getTimezoneAbbreviation(zone, System.currentTimeMillis())
                        val city = zone.id.substringAfterLast('/').replace('_', ' ')
                        v.text = "($abbrev) $city"
                        v.setPadding(40, 12, 20, 12)
                    }
                    v
                }
            }

            override fun getFilter(): Filter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filtered = if (constraint.isNullOrBlank()) {
                        fullList
                    } else {
                        fullList.filter { item ->
                            if (item.isHeader) {
                                false // Don't show headers when searching
                            } else {
                                item.zone?.let {
                                    formatTimezone(it).contains(constraint, ignoreCase = true) ||
                                        it.id.contains(constraint, ignoreCase = true)
                                } ?: false
                            }
                        }
                    }
                    return FilterResults().apply {
                        values = filtered
                        count = filtered.size
                    }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                    clear()
                    addAll(results.values as List<TimezoneItem>)
                    notifyDataSetChanged()
                }
            }
        }

        listView.adapter = adapter

        // Scroll to current timezone by default
        val currentTimezonePosition = groupedItems.indexOfFirst {
            !it.isHeader && it.zone?.id == selectedTimezone.id
        }
        if (currentTimezonePosition >= 0) {
            listView.setSelection(currentTimezonePosition)
        }

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.select_timezone_title))
            .setView(layout)
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .create()

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s)
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            if (item.isHeader) return@setOnItemClickListener  // Ignore header clicks

            val zone = item.zone ?: return@setOnItemClickListener
            selectedTimezone = zone

            // Preserve displayed time values by extracting them before timezone change
            val startYear = startCalendar.get(Calendar.YEAR)
            val startMonth = startCalendar.get(Calendar.MONTH)
            val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)
            val startHour = startCalendar.get(Calendar.HOUR_OF_DAY)
            val startMinute = startCalendar.get(Calendar.MINUTE)

            val endYear = endCalendar.get(Calendar.YEAR)
            val endMonth = endCalendar.get(Calendar.MONTH)
            val endDay = endCalendar.get(Calendar.DAY_OF_MONTH)
            val endHour = endCalendar.get(Calendar.HOUR_OF_DAY)
            val endMinute = endCalendar.get(Calendar.MINUTE)

            // Change timezone and restore the same displayed values
            startCalendar.timeZone = zone
            startCalendar.set(startYear, startMonth, startDay, startHour, startMinute)

            endCalendar.timeZone = zone
            endCalendar.set(endYear, endMonth, endDay, endHour, endMinute)

            binding.timezoneText.text = formatTimezone(zone)
            binding.startTime.text = formatTime(startCalendar)
            binding.endTime.text = formatTime(endCalendar)
            viewModel.updateTimezone(zone.id)
            viewModel.updateStartDate(startCalendar.time)
            viewModel.updateEndDate(endCalendar.time)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatTimezone(tz: TimeZone): String {
        val city = tz.id.substringAfterLast('/').replace('_', ' ')
        val abbrev = getTimezoneAbbreviation(tz, System.currentTimeMillis())
        return "($abbrev) $city"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                showDiscardDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDiscardDialog() {
        fun clearAndNavigateBack() {
            viewModel.resetInteractionLog {
                if (isAdded) {
                    findNavController().popBackStack()
                }
            }
        }

        if (FeatureFlagManager.isEnabled(FeatureFlag.CLEAR_FORM_ON_WORKFLOW_EXIT)) {
            // Case 2: simple 2-button dialog
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.discard_changes_title))
                .setMessage(getString(R.string.discard_changes_message))
                .setPositiveButton(getString(R.string.discard_changes_confirm)) { _, _ -> clearAndNavigateBack() }
                .setNegativeButton(getString(R.string.keep_editing), null)
                .show()
        } else {
            // Case 1: 2-button dialog — state can be preserved
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.leave_form_title))
                .setMessage(getString(R.string.leave_form_message))
                .setPositiveButton(getString(R.string.save_and_exit)) { _, _ ->
                    viewModel.saveDraft {
                        if (isAdded) {
                            findNavController().popBackStack()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.keep_editing), null)
                .show()
        }
    }

    // ---------------- Next ----------------
    private fun setNextButton() {
        binding.txtNext2.setOnClickListener {

            if (startCalendar.time.after(endCalendar.time)) {
                binding.dateErrorText.text = "End time must be after start time"
                binding.dateErrorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.updateStartDate(startCalendar.time)
            viewModel.updateEndDate(endCalendar.time)
            viewModel.updateTimezone(selectedTimezone.id)

            viewModel.saveDraft {
                findNavController().navigate(R.id.action_interactionQ1_to_interactionQ2)
            }
        }
    }

    override fun saveCurrentState() {
        isTouched = true
        viewModel.updateStartDate(startCalendar.time)
        viewModel.updateEndDate(endCalendar.time)
        viewModel.updateTimezone(selectedTimezone.id)
    }

    override fun getStepState(): StepState {
        return when {
            wasSkipped -> StepState.SKIPPED
            isCurrentStepValid() -> StepState.VALID
            isTouched -> StepState.TOUCHED
            else -> StepState.EMPTY
        }
    }

    private fun isCurrentStepValid(): Boolean {
        return startCalendar.time.before(endCalendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DOT_DEST_IDS = listOf(
            R.id.interactionQ1Fragment,
            R.id.interactionQ2Fragment,
            R.id.interactionQ3Fragment,
            R.id.interactionQ4Fragment,
            R.id.interactionQ5Fragment,
            R.id.interactionQ6Fragment,
            R.id.interactionQ7Fragment
        )
    }
}
