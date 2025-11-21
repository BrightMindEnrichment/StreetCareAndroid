package org.brightmindenrichment.street_care.ui.visit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.ItemInteractionBinding
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction


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
        val binding: ItemInteractionBinding
        val view: View
        if (convertView == null) {
            binding = ItemInteractionBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemInteractionBinding
        }
        val item = getItem(position) ?: return view
        binding.tvInteractionTitle.text = "Individual Interaction ${position + 1}"


        binding.btnEdit.setOnClickListener {
//            listener.onEditClicked(item)
        }

        binding.btnDelete.setOnClickListener {
            listener.onDeleteClicked(item)
        }
        return view
    }

    interface InteractionListener {
        fun onEditClicked(item: IndividualInteraction)
        fun onDeleteClicked(item: IndividualInteraction)
    }
}