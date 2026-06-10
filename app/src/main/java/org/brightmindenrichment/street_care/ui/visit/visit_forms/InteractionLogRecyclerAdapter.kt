package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.VisitLogListLayoutBinding
import org.brightmindenrichment.street_care.ui.visit.InteractionLogDataAdapter
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import org.brightmindenrichment.street_care.ui.visit.visit_forms.InteractionDetailsButtonClickListener
import org.brightmindenrichment.street_care.ui.visit.visit_forms.InteractionLogRecyclerAdapter

class InteractionLogRecyclerAdapter(
    private val context: Context,
    private val controller: InteractionLogDataAdapter,
    private val clickListener: InteractionDetailsButtonClickListener
) : RecyclerView.Adapter<InteractionLogRecyclerAdapter.ViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy | h:mma", Locale.getDefault())

    inner class ViewHolder(
        private val binding: VisitLogListLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InteractionLog, position: Int, size: Int) {
            binding.textViewDetails.text = item.formattedLocation().ifBlank { "Location" }

            binding.textViewDate.text = item.startTimestamp
                ?.toDate()
                ?.let { dateTimeFormat.format(it) }
                ?: "Date unavailable"

            val status = item.displayStatus()

            if (item.isPublic && status.isNotBlank()) {
                binding.statusTextView.visibility = View.VISIBLE
                binding.statusTextView.text = status

                (binding.statusTextView.background as? GradientDrawable)?.setColor(
                    ContextCompat.getColor(context, item.statusColor())
                )
            } else {
                binding.statusTextView.visibility = View.GONE
            }

            binding.detailsButton.setOnClickListener {
                Log.d("DETAILS_TEST", "InteractionLog details clicked")
                clickListener.onClick(item)
            }

            when (position) {
                0 -> {
                    binding.timelineLine.visibility = View.GONE
                    binding.timelineLineHalfUp.visibility = View.GONE
                    binding.timelineLineHalfDown.visibility = View.VISIBLE
                }

                size - 1 -> {
                    binding.timelineLine.visibility = View.GONE
                    binding.timelineLineHalfUp.visibility = View.VISIBLE
                    binding.timelineLineHalfDown.visibility = View.GONE
                }

                else -> {
                    binding.timelineLine.visibility = View.VISIBLE
                    binding.timelineLineHalfUp.visibility = View.GONE
                    binding.timelineLineHalfDown.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = VisitLogListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        controller.getInteractionAtPosition(position)?.let { interactionLog ->
            holder.bind(interactionLog, position, itemCount)
        }
    }

    override fun getItemCount(): Int {
        return controller.size
    }
}

private fun InteractionLog.formattedLocation(): String {
    return listOf(addr1, addr2, city, state, zipcode)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

private fun InteractionLog.displayStatus(): String {
    return when (status.trim().lowercase(Locale.US)) {
        "approved", "published" -> "PUBLISHED"
        "pending" -> "PENDING"
        "rejected" -> "REJECTED"
        else -> status.uppercase(Locale.US)
    }
}

private fun InteractionLog.statusColor(): Int {
    return when (status.trim().lowercase(Locale.US)) {
        "approved", "published" -> R.color.status_green
        "rejected" -> R.color.status_red
        "pending" -> R.color.status_amber
        else -> R.color.black
    }
}