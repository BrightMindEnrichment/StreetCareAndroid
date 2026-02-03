package org.brightmindenrichment.street_care.ui.community.adapter

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.model.CommunityActivityObject
import org.brightmindenrichment.street_care.databinding.CommunityActivityItemBinding
import org.brightmindenrichment.street_care.ui.visit.VisitDataAdapter
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import org.brightmindenrichment.street_care.util.Extensions

class CommunityActivityAdapter(private val controller: VisitDataAdapter, private val context: Context) : RecyclerView.Adapter<CommunityActivityAdapter.ViewHolder>() {
    //private lateinit var activityList: List<CommunityActivityObject>
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val db = FirebaseFirestore.getInstance()
    inner class ViewHolder(private val binding: CommunityActivityItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VisitLog,onComplete: () -> Unit) {
            binding.activityDescription.text = item.location
            val localDateTime = Extensions.dateParser(item.whenVisit.toString())
            val docRef = db.collection("users").document(item.userId ?: "??")
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val user = document.data
                        val userName = user?.get("username")?: context.getString(R.string.someone)
                        if(item.location.isNullOrEmpty()){
                            binding.activityDescription.text = context.getString(R.string.completed_an_outreach, userName)
                        } else{
                            binding.activityDescription.text =
                                context.getString(R.string.in_completed_an_outreach, userName, item.location)
                        }

                        Picasso.get().load(user?.get("profileImageUrl").toString()).error(R.drawable.ic_profile).into(binding.avatar)
                        Log.d(ContentValues.TAG, "profileImageUrl: "+user?.get("profileImageUrl").toString())
                        binding.activityTime.text = context.getString(
                            R.string.month_year,
                            localDateTime?.month.toString().substring(0, 3),
                            localDateTime?.year.toString()
                        )
                        onComplete
                    } else {
                        Log.d(ContentValues.TAG, "No such document")

                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(ContentValues.TAG, "get failed with ", exception)

                }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CommunityActivityItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //val activity = activityList[position]
        val visit = controller.getVisitAtPosition(position)
        if (visit != null) {
            holder.bind(visit){
                return@bind
            }
        }
        //holder.bind(activity)
    }
    fun submitList(newDataList: List<CommunityActivityObject>) {
        //activityList = newDataList
        this.notifyDataSetChanged()
    }

    override fun getItemCount() = controller.size



}