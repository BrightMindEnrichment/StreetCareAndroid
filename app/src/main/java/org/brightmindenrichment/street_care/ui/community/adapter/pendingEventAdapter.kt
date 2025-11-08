package org.brightmindenrichment.street_care.ui.community.adapter

import android.util.*
// import com.google.firebase.firestore.ktx.firestore MOD
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.ui.community.data.Event


class pendingEventAdapter {

    var events: MutableList<Event> = mutableListOf()
    val size: Int get() { return events.size }
    val db = FirebaseFirestore.getInstance() // MOD
    fun refresh(onComplete: () -> Unit) {


        db.collection("events").whereEqualTo("status", "pending").get().
        addOnSuccessListener { result ->
        this.events.clear()
            for (document in result) {
                var event = Event()
                event.eventId = document.id
                event.title = document.get("title").toString()
                event.description = document.get("description").toString()
                event.location=document.get("location").toString()
                event.time=document.get("time").toString()
                event.uid = document.get("uid").toString()
                event.date=document.get("date").toString()
              //  val dt = document.get("date") as com.google.firebase.Timestamp
               // if (dt != null) {
               //     event.date = dt.toDate()
              //  }
                Log.d("Heyy",event.title.toString())
                this.events.add(event)
            }

            onComplete()
        }.addOnFailureListener { exceptioon ->
            onComplete()
        }

    }

    fun getEventAtPosition(position: Int): Event? {

        if ((position >=0) && (position < events.size)) {
            return events[position]
        }

        return null
    }

    fun updateEventList(eventId: String ) {
        db.collection("events").document(eventId).update("status","Approved").addOnSuccessListener {
            this.events.clear()
            Log.d("BME", "Successfully Updated$eventId")


        }.addOnFailureListener {
            Log.w("BMEE","Fail Update$eventId")

        }
    }

    fun declineEventList(eventId: String) {
        db.collection("events").document(eventId).update("status","Declined").addOnSuccessListener {
            this.events.clear()
            Log.d("BME", "Successfully Updated$eventId")
        }.addOnFailureListener {
            Log.w("BMEE","Fail Update$eventId")
        }
    }

}