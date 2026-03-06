package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import org.brightmindenrichment.street_care.databinding.FragmentIndividualInteractionListItemBinding
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import org.brightmindenrichment.street_care.util.formatTimeWithTimezone
import java.time.LocalTime

class IndividualInteractionAdapter(
    context: Context,
    private var items: List<IndividualInteraction>,
    private val listener: InteractionListener
) : ArrayAdapter<IndividualInteraction>(context, 0, items.toMutableList()) {

    fun updateList(newItems: List<IndividualInteraction>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: FragmentIndividualInteractionListItemBinding
        val view: View
        if (convertView == null) {
            binding = FragmentIndividualInteractionListItemBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as FragmentIndividualInteractionListItemBinding
        }
        val item = getItem(position) ?: return view

        val displayName = if (item.firstName.isNotBlank()) {
            val lastInitial = item.lastName?.firstOrNull()?.let { " $it." }.orEmpty()
            val name = "${item.firstName}$lastInitial"

            val details = listOfNotNull(
                name,
                item.locationLandmark?.takeIf { it.isNotBlank() },
                formatTimeWithTimezone(item.time)
            )

            details.joinToString(" | ")

            // Outputs:
            // "John K. | 123 Main St | 6:30 PM"
            // "John K. | 123 Main St"
            // "John K. | 6:30 PM"
            // "John K."
        } else {
            "IndividualInteraction${position + 1}"
        }
        binding.tvInteractionTitle.text = displayName

        binding.btnEdit.setOnClickListener {
            listener.onEditClicked(item, position)
        }

        binding.btnDelete.setOnClickListener {
            listener.onDeleteClicked(item)
        }
        return view
    }

    interface InteractionListener {
        fun onEditClicked(item: IndividualInteraction, position: Int)
        fun onDeleteClicked(item: IndividualInteraction)
    }
}