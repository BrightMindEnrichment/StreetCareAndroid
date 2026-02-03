package org.brightmindenrichment.street_care.ui.community.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import org.brightmindenrichment.street_care.databinding.BottomSheetCommunityHelpBinding
import org.brightmindenrichment.street_care.databinding.CommunityWantHelpItemBinding
import org.brightmindenrichment.street_care.ui.community.model.CommunityActivityHelp

private const val TAG = "CommunityWantHelpAdapter"

class CommunityWantHelpAdapter(
    private val onItemClicked: (CommunityActivityHelp) -> Unit,
    private val bottomSheet: View
) : RecyclerView.Adapter<CommunityWantHelpAdapter.ViewHolder>() {
    private var helpList = emptyList<CommunityActivityHelp>()

    inner class ViewHolder(
        private val binding: CommunityWantHelpItemBinding,
        private val bottomSheetBinding: BottomSheetCommunityHelpBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: CommunityActivityHelp) {
            //TODO: Add user
            binding.title.text = activity.title
            binding.description.text = activity.description
            //TODO: Need to edit the time text
            binding.timeLog.text = activity.time

//            val db = FirebaseFirestore.getInstance()
//            val user = //get uid
//            val userRef = db.collection("users").document(currentUser?.uid ?: "??")
//            userRef.get().addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val document = task.result
//                    if (document != null) {
//                        val username = document.getString("username")
//
//
//                    } else {
//                        Log.d(TAG, "No such document")
//                    }
//                } else {
//                    Log.d(TAG, "get failed with ", task.exception)
//                }
//            }

            binding.root.setOnClickListener {
                onItemClicked(activity)
                bottomSheetBinding.titleText.text = activity.title
                bottomSheetBinding.detailDescription.text = activity.description
                //TODO: Need to edit the time text
                bottomSheetBinding.timeLog.text = activity.time

                activity.contact?.let {
                    bottomSheetBinding.contactText.text = it
                }
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            CommunityWantHelpItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bottomSheetBinding = BottomSheetCommunityHelpBinding.bind(bottomSheet)
        return ViewHolder(binding, bottomSheetBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = helpList[position]
        holder.bind(activity)
    }

    fun submitList(newDataList: List<CommunityActivityHelp>) {
        helpList = newDataList
        this.notifyDataSetChanged()
    }

    override fun getItemCount() = helpList.size
}