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
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlag
import org.brightmindenrichment.street_care.util.featureflags.FeatureFlagManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class InteractionQ1Fragment : Fragment() {

    private var _binding: FragmentLogInteractionQ1Binding? = null
    private val binding get() = _binding!!

    private val viewModel: InteractionLogViewModel by activityViewModels()
    private val iiViewModel: IndividualInteractionViewModel by activityViewModels()

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a z", Locale.getDefault())

    private var selectedTimezone: TimeZone = TimeZone.getDefault()

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

        if (log?.startTimestamp != null) {
            restoreFromLog(log)
        } else if (preLoaded) {
            // Draft was pre-loaded from the Visit screen — restore directly without dialog
            if (log != null) {
                iiViewModel.restoreFromInteractionLog(log.individualInteractions)
                restoreFromLog(log)
            } else {
                refreshUI()
            }
        } else {
            // No in-memory state — restore from ViewModel if available
            if (log != null) {
                iiViewModel.restoreFromInteractionLog(log.individualInteractions)
                restoreFromLog(log)
            } else {
                refreshUI()
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
        binding.startTime.text = timeFormatter.format(startCalendar.time)
        binding.endDate.text = dateFormatter.format(endCalendar.time)
        binding.endTime.text = timeFormatter.format(endCalendar.time)
        binding.timezoneText.text = formatTimezone(selectedTimezone)
    }


    // ---------------- Start Date ----------------
    private fun setStartDatePicker() {
        binding.datePickerCard.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.MyDatePickerDialogTheme)
                .setTitleText("Select Start Date")
                .build()

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
                    binding.startTime.text = timeFormatter.format(startCalendar.time)
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
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.MyDatePickerDialogTheme)
                .setTitleText("Select End Date")
                .build()

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
                    binding.endTime.text = timeFormatter.format(endCalendar.time)
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

        val allZones = TimeZone.getAvailableIDs()
            .map { TimeZone.getTimeZone(it) }
            .sortedBy { it.rawOffset }

        val pad = (16 * density).toInt()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }

        val searchField = EditText(ctx).apply {
            hint = "Search timezone"
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

        val adapter = object : ArrayAdapter<TimeZone>(
            ctx,
            android.R.layout.simple_list_item_1,
            allZones.toMutableList()
        ) {
            private val fullList = allZones.toList()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                getItem(position)?.let { v.text = formatTimezone(it) }
                return v
            }

            override fun getFilter(): Filter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filtered = if (constraint.isNullOrBlank()) {
                        fullList
                    } else {
                        fullList.filter {
                            formatTimezone(it).contains(constraint, ignoreCase = true) ||
                                it.id.contains(constraint, ignoreCase = true)
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
                    addAll(results.values as List<TimeZone>)
                    notifyDataSetChanged()
                }
            }
        }

        listView.adapter = adapter

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Select Timezone")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .create()

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s)
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val zone = adapter.getItem(position) ?: return@setOnItemClickListener
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
            binding.startTime.text = timeFormatter.format(startCalendar.time)
            binding.endTime.text = timeFormatter.format(endCalendar.time)
            viewModel.updateTimezone(zone.id)
            viewModel.updateStartDate(startCalendar.time)
            viewModel.updateEndDate(endCalendar.time)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatTimezone(tz: TimeZone): String {
        val offsetMs = tz.rawOffset
        val sign = if (offsetMs >= 0) "+" else "-"
        val absMs = abs(offsetMs)
        val hours = absMs / 3_600_000
        val minutes = (absMs % 3_600_000) / 60_000
        val city = tz.id.substringAfterLast('/').replace('_', ' ')
        return "(UTC$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}) $city"
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
                .setTitle("Discard changes?")
                .setMessage("Your progress will be lost if you leave now.")
                .setPositiveButton("Discard") { _, _ -> clearAndNavigateBack() }
                .setNegativeButton("Keep editing", null)
                .show()
        } else {
            // Case 1: 2-button dialog — state can be preserved
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Leave form?")
                .setMessage("Save your progress and continue later, or keep editing?")
                .setPositiveButton("Save & Exit") { _, _ ->
                    viewModel.saveDraft()
                    if (isAdded) {
                        findNavController().popBackStack()
                    }
                }
                .setNegativeButton("Keep editing", null)
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

            viewModel.saveDraft()
            findNavController().navigate(R.id.action_interactionQ1_to_interactionQ2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
