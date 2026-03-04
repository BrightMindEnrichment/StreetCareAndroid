package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ1Binding
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

    private val startCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val endCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 17)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

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

        binding.startDate.text = dateFormatter.format(startCalendar.time)
        binding.startTime.text = timeFormatter.format(startCalendar.time)
        binding.endDate.text = dateFormatter.format(endCalendar.time)
        binding.endTime.text = timeFormatter.format(endCalendar.time)
        binding.timezoneText.text = formatTimezone(selectedTimezone)

        setStartDatePicker()
        setStartTimePicker()
        setEndDatePicker()
        setEndTimePicker()
        setTimezonePicker()
        setNextButton()
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

                startCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )

                binding.startDate.text = dateFormatter.format(startCalendar.time)
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
            binding.timezoneText.text = formatTimezone(zone)
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

            findNavController().navigate(R.id.action_interactionQ1_to_interactionQ2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
