package org.brightmindenrichment.street_care.ui.community.data

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.navigation.NavController
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.util.Extensions.Companion.requiredSkills
import org.brightmindenrichment.street_care.util.StateAbbreviation.getStateOrProvinceAbbreviation
import java.util.concurrent.atomic.AtomicInteger

/**
// example addEvent
//eventDataAdapter.addEvent("Food for Androids", "Feed all the android of the world.", Date()) {
//    Log.d("BME", "Event added")
//}

//example setLiked
//eventDataAdapter.setLikedEvent("2r9Z5TKnQYFC96iMAB1i", true) {
//    Log.d("BME", "done")
//}

// example refresh
//eventDataAdapter.refresh {
//    for (event in this.eventDataAdapter.events) {
//        Log.d("BME", "${event.title} ${event.date} ${event.liked}")
//    }
//}
 * */


class HelpRequestDataAdapter(
    private val scope: CoroutineScope,
    private val context: Context,
    private val navController: NavController,
) {
    //var events: MutableList<Event> = mutableListOf()
    private var helpRequestDataList: MutableList<HelpRequestData> = mutableListOf()
    val storage = FirebaseStorage.getInstance()
    val size: Int
        get() {
            return helpRequestDataList.size
        }

    fun getHelpRequestAtPosition(position: Int): HelpRequestData? {

        if ((position >= 0) && (position < helpRequestDataList.size)) {
            return helpRequestDataList[position]
        }

        return null
    }

    private fun checkQuery(helpRequest: HelpRequest, inputText: String): Boolean {
        val title = helpRequest.title?.lowercase()?.trim() ?: "unknown"
        val description = helpRequest.description?.lowercase()?.trim() ?: "unknown"
        val location = helpRequest.location?.lowercase()?.trim() ?: "unknown"
        val identification = helpRequest.identification?.lowercase()?.trim() ?: "unknown"
        val skills = helpRequest.skills?.map { it.lowercase() } ?: emptyList()
        var checkSkills = false
        for(skill in skills) {
            if(skill.contains(inputText.lowercase().trim())) {
                checkSkills = true
                break
            }
        }
        return  inputText.isEmpty() ||
                title.contains(inputText.lowercase().trim()) ||
                description.contains(inputText.lowercase().trim()) ||
                location.contains(inputText.lowercase().trim()) ||
                checkSkills ||
                identification.contains(inputText.lowercase().trim())
    }


    fun refresh(
        inputText: String,
        query: Query,
        showProgressBar: () -> Unit,
        onNoResults: () -> Unit,
        onComplete: () -> Unit,
    ) {
        Log.d("debug", "helpRequests refresh")
        showProgressBar()
        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        if (user == null) {
            // Guest (logged out) version: Simply fetch help requests without user-specific logic.
            query.get()
                .addOnSuccessListener { result ->
                    Log.d("debug", "helpRequests, guest fetch success: ${result.size()}")
                    scope.launch(Dispatchers.IO) {
                        val newHelpRequestDataList = mutableListOf<HelpRequestData>()
                        for (document in result) {
                            yield()
                            val helpRequest = HelpRequest().apply {
                                id = document.id
                                title = document.get("title")?.toString() ?: "Unknown"
                                description = document.get("description")?.toString() ?: "Unknown"
                                // If location is stored as a map, format it (or set a default)
                                val locationData = document.get("location") as? Map<*, *>
                                val locationStr = if (locationData != null) {
                                    val stateAbbr = getStateOrProvinceAbbreviation(locationData["state"]?.toString() ?: "")
                                    "${locationData["street"]}, ${locationData["city"]}, $stateAbbr ${locationData["zipcode"]}"
                                } else {
                                    "Unknown"
                                }
                                location = locationStr
                                identification = document.get("identification").toString()
                                status = document.get("status").toString()
                                uid = document.get("uid").toString()
                                createdAt = document.get("createdAt").toString()
                                skills = document.get("skills") as? ArrayList<String>
                                // Additional processing if needed…
                            }

                            if (!checkQuery(helpRequest, inputText)) continue
                            // No guest-specific update: We simply add the help request.
                            val helpRequestData = HelpRequestData(helpRequest)
                            newHelpRequestDataList.add(helpRequestData)
                        }

                        withContext(Dispatchers.Main) {
                            this@HelpRequestDataAdapter.helpRequestDataList.clear()
                            this@HelpRequestDataAdapter.helpRequestDataList.addAll(newHelpRequestDataList)
                            if (helpRequestDataList.isEmpty()) onNoResults() else onComplete()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("query", "guest refresh failed: $exception")
                    onComplete()
                }
        } else {
            //val query = db.collection("events").orderBy("date", Query.Direction.DESCENDING)
            val usersDocRef = db.collection("users").document(user.uid)
            usersDocRef.get()
                .addOnSuccessListener { userDoc ->
                    Log.d("debug", "helpRequests, get users collection")
                    val userData = userDoc.data
                    userData?.let { uData ->
                        val outreachEvents: List<*> =
                            uData["outreachEvents"] as? List<*> ?: emptyList<String>()
                        val userHelpRequests = mutableListOf<String>()
                        val count = AtomicInteger(0)

                        if (outreachEvents.isEmpty()) {
                            Log.d("helpRequestDebug", "outreach events is empty")
                            getHelpRequestsFromFirebase(
                                query = query,
                                inputText = inputText,
                                user = user,
                                userHelpRequests = userHelpRequests,
                                onNoResults = onNoResults,
                                onComplete = onComplete
                            )
                        }

                        for (eventId in outreachEvents) {
                            Log.d("debug", "original, count: $count")
                            val outreachEventsDocRef =
                                db.collection("outreachEventsDev").document(eventId.toString())
                            outreachEventsDocRef.get()
                                .addOnSuccessListener { eventDoc ->
                                    Log.d("debug", "helpRequests, get outreach Events")
                                    val eventData = eventDoc.data
                                    eventData?.let { eData ->
                                        val helpRequests: List<*> =
                                            eData["helpRequest"] as? List<*> ?: emptyList<String>()
                                        for (helpRequest in helpRequests) {
                                            userHelpRequests.add(helpRequest.toString())
                                        }
                                    }
                                    Log.d("debug", "before, count: ${count.toInt()}")
                                    count.incrementAndGet()
                                    Log.d("debug", "after, count: ${count.toInt()}")
                                    if (count.toInt() == outreachEvents.size) {
                                        Log.d("helpRequestDebug", "outreach events is not empty")
                                        getHelpRequestsFromFirebase(
                                            query = query,
                                            inputText = inputText,
                                            user = user,
                                            userHelpRequests = userHelpRequests,
                                            onNoResults = onNoResults,
                                            onComplete = onComplete
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    onComplete()
                                }

                        }
                    }
                }
                .addOnFailureListener {
                    onComplete()
                }
        }
    }

    fun getHelpRequestsFromFirebase(
        query: Query,
        inputText: String,
        user: FirebaseUser,
        userHelpRequests: MutableList<String>,
        onNoResults: () -> Unit,
        onComplete: () -> Unit,
    ) {
        query.get()
            .addOnSuccessListener { result ->
                Log.d("debug", "helpRequests, get help Requests")
                this.helpRequestDataList.clear()
                Log.d("loadProfileImg", "before, communityDataList size: ${helpRequestDataList.size}")
                Log.d("query", "successfully refresh: ${result.size()}")

                scope.launch(Dispatchers.IO) {
                    val newHelpRequestDataList = mutableListOf<HelpRequestData>()

                    for (document in result) {
                        yield()
                        val helpRequest = HelpRequest()
                        helpRequest.id = document.id
                        helpRequest.title = document.get("title")?.toString() ?: "Unknown"
                        helpRequest.description = document.get("description")?.toString() ?: "Unknown"
                        val location = document.get("location") as? Map<*, *>
                        if(location != null) {
                            val stateName = location["state"] ?: ""
                            val stateAbbr = getStateOrProvinceAbbreviation(stateName.toString()) // Get abbreviation or original state

                            helpRequest.location = "${location["street"]}, ${location["city"]}, $stateAbbr ${location["zipcode"]}"
                            helpRequest.street = "${location["street"]}"
                            helpRequest.city = "${location["city"]}"
                            helpRequest.state = "${location["state"]}"
                            helpRequest.zipcode = "${location["zipcode"]}"
                        }
                        else helpRequest.location = "Unknown"

                        helpRequest.identification = document.get("identification").toString()
                        helpRequest.status = document.get("status").toString()
                        helpRequest.uid = document.get("uid").toString()
                        helpRequest.createdAt = document.get("createdAt").toString()
                        helpRequest.skills = document.get("skills") as ArrayList<String> // List<String>
                        helpRequest.skills?.forEach { skill ->
                            val index = requiredSkills.indexOf(skill)
                            if(index != -1) {
                                helpRequest.skillsBooleanArray[index] = true
                            }

                        }
                        Log.d("debug", "userHelpRequests2: $userHelpRequests")

                        if(!checkQuery(helpRequest, inputText)) continue

                        if(helpRequest.status == HelpRequestStatus.NeedHelp.status &&
                            (user.uid == helpRequest.uid) || (userHelpRequests.contains(helpRequest.id))) {
                            helpRequest.status = HelpRequestStatus.HelpOnTheWay.status
                            if(userHelpRequests.contains(helpRequest.id)) {
                                Log.d("debug", "helpRequest title: ${helpRequest.title}")
                                Log.d("debug", "helpRequest status: ${helpRequest.status}")
                            }
                        }

                        val helpRequestData = HelpRequestData(helpRequest)
                        newHelpRequestDataList.add(helpRequestData)
                    }

                    withContext(Dispatchers.Main) {
                        this@HelpRequestDataAdapter.helpRequestDataList.clear()
                        this@HelpRequestDataAdapter.helpRequestDataList.addAll(newHelpRequestDataList)

                        if (helpRequestDataList.isEmpty()) onNoResults() else onComplete()
                    }
                }

            }.addOnFailureListener { exception ->
                Log.d("query", "refresh failed: $exception")
                onComplete()
            }
    }

    fun setBtnAction(
        helpRequest: HelpRequest,
        onComplete: (HelpRequest) -> Unit,
    ) {

        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val db = FirebaseFirestore.getInstance()
        val helpRequestStatus = helpRequest.status!!
        val helpRequestsDocRef = db.collection("helpRequests").document(helpRequest.id!!)
        when(helpRequestStatus) {
            HelpRequestStatus.NeedHelp.status -> {
                // show alert dialog, either
                // create a new outreach event or
                // RSVP existing outreach related to the help request
                val helpRequestDialog = createHelpRequestDialog(helpRequest)
                helpRequestDialog.show()

                helpRequestDialog.apply {
                    getButton(Dialog.BUTTON_POSITIVE).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP,15.0f)
                        backgroundTintList = ColorStateList.valueOf(
                            context.resources.getColor(R.color.dark_green, null)
                        )
                        setTextColor(context.resources.getColor(R.color.accent_yellow, null))
                    }
                    getButton(Dialog.BUTTON_NEGATIVE).apply {
                        backgroundTintList = ColorStateList.valueOf(
                            context.resources.getColor(R.color.dark_green, null)
                        )
                        setTextColor(context.resources.getColor(R.color.accent_yellow, null))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f)
                    }
                    getButton(Dialog.BUTTON_NEUTRAL).apply {
                        backgroundTintList = ColorStateList.valueOf(
                            context.resources.getColor(R.color.dark_green, null)
                        )
                        setTextColor(context.resources.getColor(R.color.accent_yellow, null))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f)
                    }
                }
            }
            HelpRequestStatus.HelpOnTheWay.status -> {
                // only for the user who created this help request:
                // set the value of status field in firebase to "Help Received"
                if(user.uid == helpRequest.uid) {
                    val updateHelpRequestStatus = helpRequestsDocRef.update("status", HelpRequestStatus.HelpReceived.status)
                        .addOnSuccessListener {
                            onComplete(helpRequest)
                            Log.d("debug", "HelpRequestStatus successfully updated! status: ${helpRequest.status}")
                        }
                        .addOnFailureListener { e -> Log.w("debug", "Error update HelpRequestStatus", e) }
                }
                else {
                    // find a outreach event created by this help request

                    val usersDocRef = db.collection("users").document(user.uid)
                    usersDocRef.get()
                        .addOnSuccessListener { userDoc ->
                            Log.d("debug", "helpRequests, get users collection")
                            val userData = userDoc.data
                            userData?.let { uData ->
                                val outreachEvents: List<*> = uData["outreachEvents"] as? List<*> ?: emptyList<String>()
                                val helpRequestEventIDs = mutableListOf<String>()
                                val count = AtomicInteger(0)
                                for(outreachEventId in outreachEvents) {
                                    Log.d("debug", "original, count: $count")
                                    val outreachEventsDocRef = db.collection("outreachEventsDev").document(outreachEventId.toString())
                                    outreachEventsDocRef.get()
                                        .addOnSuccessListener { eventDoc ->
                                            Log.d("debug", "helpRequests, get outreach Events")
                                            val eventData = eventDoc.data
                                            eventData?.let { eData ->
                                                val helpRequests: List<*> = eData["helpRequest"] as? List<*> ?: emptyList<String>()
                                                if(helpRequests.contains(helpRequest.id)) {
                                                    helpRequestEventIDs.add(outreachEventId.toString())
                                                }

                                            }
                                            Log.d("debug", "before, count: ${count.toInt()}")
                                            count.incrementAndGet()
                                            Log.d("debug", "after, count: ${count.toInt()}")
                                            if(count.toInt() == outreachEvents.size) {
                                                for(eventId in helpRequestEventIDs) {
                                                    val eventsDocRef = db.collection("outreachEventsDev").document(eventId)
                                                    eventsDocRef.get()
                                                        .addOnSuccessListener { document ->
                                                            val event = document.data
                                                            event?.let {
                                                                Log.d("debug", "userId: ${user.uid}")
                                                                var interests = event["interests"] as? Int ?: 0
                                                                Log.d("debug", "before, interests: $interests")
                                                                var participants = event["participants"] as? MutableList<*> ?: emptyList<String>()
                                                                Log.d("debug", "before, participants: $participants")
                                                                --interests
                                                                Log.d("debug", "after, interests: $interests")
                                                                participants = participants.filter { it.toString() != user.uid }
                                                                Log.d("debug", "after, participants: $participants")

                                                                val updateInterestsAndParticipantsOrDelete =
                                                                    if(interests <= 0 && participants.isEmpty()) {
                                                                        eventsDocRef.delete()
                                                                    } else {
                                                                        eventsDocRef
                                                                            .update("interests", interests,
                                                                                "participants", participants)
                                                                            .addOnSuccessListener { Log.d("debug", "successfully updated! event.interest: ${interests}, participants: ${participants.size}") }
                                                                            .addOnFailureListener { e -> Log.w("debug", "Error updateInterestsAndParticipants", e) }

                                                                    }

                                                                val updateOutreachEventsInUsersCollection = usersDocRef
                                                                    .update("outreachEvents", FieldValue.arrayRemove(eventId))
                                                                    .addOnSuccessListener { Log.d("debug", "successfully updated!") }
                                                                    .addOnFailureListener { e -> Log.w("debug", "Error updateOutreachEvents", e) }

                                                                val tasks = Tasks.whenAll(listOf(updateInterestsAndParticipantsOrDelete, updateOutreachEventsInUsersCollection))

                                                                tasks
                                                                    .addOnSuccessListener {
                                                                        onComplete(helpRequest)
                                                                    }
                                                                    .addOnFailureListener {
                                                                        e -> Log.w("debug", "Error: ", e)
                                                                    }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                                e -> Log.w("debug", "Error: ", e)
                                                        }
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                                e -> Log.w("debug", "Error: ", e)
                                        }

                                }
                            }
                        }
                        .addOnFailureListener {
                                e -> Log.w("debug", "Error: ", e)
                        }
                }
            }
            HelpRequestStatus.HelpReceived.status -> {
                // only for the user who created this help request:
                // reopen the help request
                if(user.uid == helpRequest.uid) {
                    val updateHelpRequestStatus = helpRequestsDocRef.update("status", HelpRequestStatus.NeedHelp.status)
                        .addOnSuccessListener {
                            onComplete(helpRequest)
                            Log.d("debug", "HelpRequestStatus successfully updated! status: ${helpRequest.status}")
                        }
                        .addOnFailureListener { e -> Log.w("debug", "Error update HelpRequestStatus", e) }
                }
            }
        }

    }

    private fun createHelpRequestDialog(
        helpRequest: HelpRequest,
    ): AlertDialog {
        // initialise the alert dialog builder
        val builder = AlertDialog.Builder(this.context).apply {
            // set the title for the alert dialog
            setTitle("Make sure you are not going alone")

            setMessage("Group presence offers security and effectiveness in engaging with unfamiliar situations and individuals, benefiting both volunteers and the homeless.\nHow outreach on Street Care works?\nWe post the outreach for you and other volunteers can sign up to go with you.")

            // set the icon for the alert dialog
            //setIcon(R.drawable.streetcare_logo)

            // now this is the function which sets the alert dialog for multiple item selection ready
            /*
            setMultiChoiceItems(requiredSkills, checkedItems) { dialog, which, isChecked ->
                checkedItems[which] = isChecked
                val currentItem = requiredSkills[which]
            }
             */

            // alert dialog shouldn't be cancellable
            setCancelable(true)

            // handle the positive button of the dialog
            setPositiveButton(context.getString(R.string.rsvp_existing_outreach)) { dialog, which ->
                navController.popBackStack()
                navController.navigate(R.id.communityEventFragment, Bundle().apply {
                    putString("pageTitle", context.getString(R.string.upcoming_events))
                    putString("helpRequestId", helpRequest.id)
                    putSerializable("communityPageName", CommunityPageName.HELP_REQUESTS)
                })
            }

            // handle the negative button of the alert dialog
            setNegativeButton("CANCEL") { dialog, which -> }

            // handle the neutral button of the dialog to clear the selected items boolean checkedItem
            setNeutralButton("Create an Outreach") { dialog: DialogInterface?, which: Int ->
                navController.popBackStack()
                navController.navigate(R.id.nav_add_event, Bundle().apply {
                    putSerializable("communityPageName", CommunityPageName.HELP_REQUESTS)
                    putString("helpRequestId", helpRequest.id)
                    putString("title", helpRequest.title?:"unknown")
                    putString("street", helpRequest.street?:"street")
                    putString("city", helpRequest.city?:"city")
                    putString("state", helpRequest.state?:"state")
                    putString("zipcode", helpRequest.zipcode?:"zipcode")
                    putBooleanArray("skillsBooleanArray", helpRequest.skillsBooleanArray)
                    putString("description", helpRequest.description?:"unknown")
                })
            }

        }

        // create the alert dialog with the alert dialog builder instance
        return builder.create()
    }


} // end class

sealed class HelpRequestStatus(val status: String) {
    object NeedHelp: HelpRequestStatus("Need Help")
    object HelpOnTheWay: HelpRequestStatus("Help is on the way")
    object HelpReceived: HelpRequestStatus("Help Received")
}