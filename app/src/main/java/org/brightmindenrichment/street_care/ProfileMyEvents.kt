package org.brightmindenrichment.street_care

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.ktx.Firebase
import android.graphics.Color
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.ktx.firestore MOD
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery
import java.text.SimpleDateFormat
import java.util.Date

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [nav_profileOutreach.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileMyEvents : Fragment(){



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view=  inflater.inflate(R.layout.fragment_profile_my_events, container, false)
        displayEvents(view)
        return view
    }


        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment nav_profileOutreach.
         */
        // TODO: Rename and change types and number of parameters



        private fun displayEvents( view: View) {

            //connecting to Firebase to get the current user detail
            val user = FirebaseAuth.getInstance().currentUser ?: return // MOD
            Log.d("BME current user", user.uid)

            if (user != null) {
                val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
                val container = view.findViewById<LinearLayout>(R.id.linearELayout)

                progressBar.visibility = View.VISIBLE
                container.removeAllViews()

                val query = getLikedEventsQuery()
                query.get()
                    .addOnSuccessListener { querySnapshot ->

                        progressBar.visibility = View.GONE

                        for (document in querySnapshot.documents) {
                            val data = document.data

                            val itemView = layoutInflater.inflate(R.layout.fragment_profile_my_events, container, false)

                            //container.visibility = View.VISIBLE

                                itemView.findViewById<TextView>(R.id.textViewCommunityEventTitle)?.text =
                                    data?.get("title") as? String

                                val location = data?.get("location") as? Map<*, *>
                                if (location != null) {
                                    itemView.findViewById<TextView>(R.id.textViewCommunityELocation)?.text =
                                        "${location["street"] as? String}, ${location["city"] as? String}, ${location["state"] as? String} ${location["zipcode"] as? String}"
                                } else itemView.findViewById<TextView>(R.id.textViewCommunityELocation)?.text =
                                    getString(R.string.unknown_location)

                                itemView.findViewById<TextView>(R.id.textViewCommunityETime)?.text =
                                    Extensions.getDateTimeFromTimestamp(
                                        data?.get("eventDate")
                                    ).split("at ")[1]

                                val Eventdate = Extensions.getDateTimeFromTimestamp(
                                    data?.get("eventDate")
                                ).split("at ")[0]
                                itemView.findViewById<TextView>(R.id.textViewEDate)?.text = Eventdate

                                itemView.findViewById<TextView>(R.id.tvHelpEType)?.text =
                                    data?.get("helpType") as? String

                                val currentTime = System.currentTimeMillis()
                                val currentDate =
                                    SimpleDateFormat("MMM dd, yyyy").format(Date(currentTime))
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy")

                                val isPastEvents =
                                    dateFormat.parse(Eventdate) < dateFormat.parse(currentDate)
                                if (!isPastEvents) {
                                    val editButton = itemView.findViewById<AppCompatButton>(R.id.btnEdit)
                                    editButton.setOnClickListener {
                                        // Handle "Edit" button click
                                        showConfirmationDialog(document.id)

                                    }
                                    itemView.findViewById<TextView>(R.id.textView)?.visibility = View.GONE
                                } else {

                                    itemView.findViewById<AppCompatButton>(R.id.btnEdit)?.visibility = View.GONE


                                }

                                container.addView(itemView)

                        }

                    }
                    .addOnFailureListener { exception ->
                        // Log any errors that occur during the query
                        Log.e("FirestoreQuery", "Error getting documents: $exception")
                    }

            }else {
                Log.d("BME", "not logged in")
            }

    }

    private fun showConfirmationDialog(documentId: String) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setMessage("Do you want to cancel your RSVP for this event?")
            .setPositiveButton("Yes") { _, _ ->
                // User confirmed, remove the entry from Firebase
                removeFromFirebase(documentId){
                    // Callback function to refresh UI
                    displayEvents(requireView())
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                // User cancelled, dismiss the dialog
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun removeFromFirebase(documentId: String, onSuccess: () -> Unit) {
        // Remove the document from the Firebase collection
        val user = FirebaseAuth.getInstance().currentUser ?: return // MOD
        val currentUserUid = user.uid
        val eventsCollection = FirebaseFirestore.getInstance().collection("outreachEventsDev") // MOD

        FirebaseFirestore.getInstance().runTransaction { transaction -> // MOD
            // Retrieve the current document snapshot
            val documentSnapshot = transaction.get(eventsCollection.document(documentId))

            // Get the current participants and interests values

            var currentInterests =  (documentSnapshot.getLong("interests") ?: 0).toInt()
            val participantsList = documentSnapshot.get("participants") as MutableList<String>?
            if (participantsList != null) {
                participantsList.remove(currentUserUid)
                if (participantsList.size != currentInterests) {
                    currentInterests = participantsList.size
                }
            }

            transaction.update(eventsCollection.document(documentId), "participants", participantsList)

                transaction.update(
                    eventsCollection.document(documentId),
                    "interests",
                    currentInterests
                )

            removeFromUsersCollection(documentId,currentUserUid )
        }.addOnSuccessListener {
            // Successfully removed from Firebase
            onSuccess.invoke()
            Log.d("FirestoreUpdate", "User $currentUserUid removed from participants")
        }.addOnFailureListener { exception ->
            // Failed to remove from Firebase, log the error
            Log.e("FirestoreUpdate", "Error removing user $currentUserUid from participants", exception)
        }
    }

    private fun removeFromUsersCollection(outreachEventdocumentId: String,currentUserUid: String) {
        val usersCollection = FirebaseFirestore.getInstance().collection("users") // MOD

        // Fetch all documents in the "users" collection
        usersCollection
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val uid = document.getString("uid")

                    // Check if the document uid matches with currentuserid
                    if (uid == currentUserUid) {
                        val documentId = document.id
                        usersCollection.document(documentId)
                            .update("outreachEvents", FieldValue.arrayRemove(outreachEventdocumentId))
                            .addOnSuccessListener {
                                // Successfully updated the document
                                println("Removed currentUser from participants in outreachEvents.")
                            }
                            .addOnFailureListener { e ->
                                // Handle failure
                                println("Error updating document: $e")
                            }
                    }

                }
            }.addOnFailureListener { e ->
                // Handle failure
                println("Error getting documents: $e")
            }

    }

}