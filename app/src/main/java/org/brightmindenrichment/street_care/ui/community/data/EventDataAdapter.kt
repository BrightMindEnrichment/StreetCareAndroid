package org.brightmindenrichment.street_care.ui.community.data

import android.content.ContentValues
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDateTimeFromTimestamp
import org.brightmindenrichment.street_care.util.Extensions.Companion.requiredSkills
import org.brightmindenrichment.street_care.util.StateAbbreviation.getStateOrProvinceAbbreviation

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


class EventDataAdapter(private val scope: CoroutineScope) {
    //var events: MutableList<Event> = mutableListOf()
    private var communityDataList: MutableList<CommunityData> = mutableListOf()

    // track active listeners
    private val eventListeners = mutableMapOf<String, ListenerRegistration>()

    val storage = Firebase.storage
    val size: Int
        get() {
            return communityDataList.size
        }

    fun setupFlagStatusListeners(onFlagStatusChanged: (Event) -> Unit) {
        val db = Firebase.firestore

        for (listener in eventListeners.values) {
            listener.remove()
        }
        eventListeners.clear()

        // listener for each event in list
        for (communityData in communityDataList) {
            communityData.event?.let { event ->
                event.eventId?.let { eventId ->
                    // listener for this specific event document
                    val listenerRegistration = db.collection("outreachEventsDev")
                        .document(eventId)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                Log.w("FlagListener", "Listen failed.", e)
                                return@addSnapshotListener
                            }

                            if (snapshot != null && snapshot.exists()) {
                                // Get the new flag status
                                val isFlagged = snapshot.getBoolean("isFlagged") ?: false
                                val flaggedByUser = snapshot.getString("flaggedByUser")

                                // If flag status has changed, update the event
                                if (event.isFlagged != isFlagged) {
                                    event.updateFlagStatus(isFlagged, flaggedByUser)
                                    onFlagStatusChanged(event)
                                }
                            }
                        }

                    // store the listener to remove it later
                    eventListeners[eventId] = listenerRegistration
                }
            }
        }
    }

    // clean up listeners when they're no longer needed
    fun cleanupFlagStatusListeners() {
        for (listener in eventListeners.values) {
            listener.remove()
        }
        eventListeners.clear()
    }


    fun getEventAtPosition(position: Int): CommunityData? {

        if ((position >= 0) && (position < communityDataList.size)) {
            return communityDataList[position]
        }

        return null
    }

    fun setLikedEvent(event: Event, onComplete: (Event) -> Unit) {

        // make sure somebody is logged in
        val user = Firebase.auth.currentUser
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Auth", "Signed in anonymously")
                    } else {
                        Log.w("Auth", "Anonymous sign-in failed", task.exception)
                    }
                }
        }

        val db = Firebase.firestore
        val doesLike: Boolean = event.signedUp
        val usersDocRef = user?.let { db.collection("users").document(it.uid) }
        val eventsDocRef = db.collection("outreachEventsDev").document(event.eventId!!)
        if (doesLike) {  // add a record if liked
            //val db = FirebaseFirestore.getInstance()
            var profileImageUrl : String
            if (usersDocRef != null) {
                usersDocRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val userData = document.data
                            if (userData != null) {
                                profileImageUrl = userData["photoUrl"].toString()

        //                            if(event.itemList.size < 3) {
        //                                event.addValue(profileImageUrl)
        //                            }
                                Log.d("loadProfileImg", "before, interest: ${event.itemList.size}")
                                event.addValue(profileImageUrl)
                                Log.d("loadProfileImg", "after, interest: ${event.itemList.size}")


                                event.interest = event.interest?.plus(1)
                                event.participants?.add(user.uid)

                                // update interests and participants in outreachEvents collection
                                val participants = event.participants?: listOf<String>(user.uid)
                                val updateInterestsAndParticipants = eventsDocRef
                                    .update("interests", event.interest,
                                        "participants", participants)
                                    .addOnSuccessListener { Log.d("syncWebApp", "successfully updated! event.interest: ${event.interest}, participants: ${participants.size}") }
                                    .addOnFailureListener { e -> Log.w("syncWebApp", "Error updateInterestsAndParticipants", e) }

                                //update outreachEvents(list) in users collection
                                //val outreachEvents = (userData["outreachEvents"]?: listOf<String>()) as ArrayList<String>
                                //outreachEvents.add(event.eventId!!)

                                val updateOutreachEvents = usersDocRef
                                    .update("outreachEvents", FieldValue.arrayUnion(event.eventId!!))
                                    .addOnSuccessListener { Log.d("syncWebApp", "successfully updated!") }
                                    .addOnFailureListener { e -> Log.w("syncWebApp", "Error updateOutreachEvents", e) }


                                val tasks = Tasks.whenAll(listOf(updateInterestsAndParticipants, updateOutreachEvents))

                                tasks.addOnCompleteListener {
                                    onComplete(event)
                                }

                            }
                        } else {
                            Log.d(ContentValues.TAG, "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        onComplete(event)
                        Log.d(ContentValues.TAG, "get failed with ", exception)
                    }
            }
            // create a map of the data to add to firebase

        } else {

            var profileImageUrl : String
            if (usersDocRef != null) {
                usersDocRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val userData = document.data
                            if (userData != null) {
                                document.get("photoUrl")?.let { profileImageUrl ->
                                    event.itemList.find {item ->
                                        item == profileImageUrl
                                    }?.let {
                                        Log.d("loadProfileImg", "before, interest: ${event.itemList.size}")
                                        event.itemList.remove(it)
                                        Log.d("loadProfileImg", "after, interest: ${event.itemList.size}")
                                    }
                                }
                                event.interest = event.interest?.minus(1)
                                event.participants?.remove(user.uid)

                                // update interests and participants in outreachEvents
                                // collection
                                val participants = event.participants?: listOf<String>()
                                val updateInterestsAndParticipants = eventsDocRef
                                    .update("interests", event.interest,
                                        "participants", participants)
                                    .addOnSuccessListener { Log.d("syncWebApp", "successfully updated! event.interest: ${event.interest}, participants: ${participants.size}") }
                                    .addOnFailureListener { e -> Log.w("syncWebApp", "Error updateInterestsAndParticipants", e) }

                                //update outreachEvents(list) in users collection
                                /*
                                    var outreachEvents = userData["outreachEvents"]
                                    if(outreachEvents != null) {
                                        (outreachEvents as ArrayList<String>).remove(event.eventId!!)
                                    }
                                    else outreachEvents = listOf<String>()
                                     */

                                val updateOutreachEvents = usersDocRef
                                    .update("outreachEvents", FieldValue.arrayRemove(event.eventId!!))
                                    .addOnSuccessListener { Log.d("syncWebApp", "successfully updated!") }
                                    .addOnFailureListener { e -> Log.w("syncWebApp", "Error updateOutreachEvents", e)}


                                val tasks = Tasks.whenAll(listOf(updateInterestsAndParticipants, updateOutreachEvents))

                                tasks.addOnCompleteListener {
                                    onComplete(event)
                                }

                            }
                        } else {
                            Log.d(ContentValues.TAG, "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        onComplete(event)
                        Log.d(ContentValues.TAG, "get failed with ", exception)
                    }
            }

        }
    }

    /*
   fun setLikedEvent(event: Event, onComplete: (Event) -> Unit) {

        // make sure somebody is logged in
        val user = Firebase.auth.currentUser ?: return

        val db = Firebase.firestore
        val doesLike: Boolean = event.signedUp
        if (doesLike) {  // add a record if liked

            //val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(user.uid)
            var profileImageUrl : String
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userData = document.data
                        if (userData != null) {
                            profileImageUrl = userData["profileImageUrl"].toString()

//                            if(event.itemList.size < 3) {
//                                event.addValue(profileImageUrl)
//                            }
                            Log.d("loadProfileImg", "before, interest: ${event.itemList.size}")
                            event.addValue(profileImageUrl)
                            Log.d("loadProfileImg", "after, interest: ${event.itemList.size}")


                            event.interest = event.interest?.plus(1)

                            val likedData = hashMapOf(
                                "uid" to user.uid,
                                "eventId" to event.eventId!!,
                                "profileImageUrl" to profileImageUrl
                            )

                            val setLikedData = db.collection("likedEvents").document()
                                .set(likedData)

                            val updateEventInterest = db.collection("events").document(event.eventId!!)
                                .update("interest", event.interest!!)

                            val tasks = Tasks.whenAll(listOf(setLikedData, updateEventInterest))

                            tasks.addOnCompleteListener {
                                onComplete(event)
                            }

                        }
                    } else {
                        Log.d(ContentValues.TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    onComplete(event)
                    Log.d(ContentValues.TAG, "get failed with ", exception)
                }
            // create a map of the data to add to firebase

        } else {
            // delete record of the like of this event for this user
            db.collection("likedEvents")
                .whereEqualTo("uid", user.uid)
                .whereEqualTo("eventId", event.eventId!!)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        document.get("profileImageUrl")?.let { profileImageUrl ->
                            event.itemList.find {item ->
                                item == profileImageUrl
                            }?.let {
                                Log.d("loadProfileImg", "before, interest: ${event.itemList.size}")
                                event.itemList.remove(it)
                                Log.d("loadProfileImg", "after, interest: ${event.itemList.size}")
                            }
                        }
                        event.interest = event.interest?.minus(1)

                        val deleteLikedEvent = db.collection("likedEvents").document(document.id).delete()

                        val updateEventInterest = db.collection("events").document(event.eventId!!)
                            .update("interest", event.interest!!)

                        val tasks = Tasks.whenAll(listOf(deleteLikedEvent, updateEventInterest))

                        tasks.addOnCompleteListener {
                            onComplete(event)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("BME", "Failed to save liked event ${exception.toString()}")
                    onComplete(event)
                }
        }
    }
    */

    fun setLikedOutreachEvent(eventId: String?, isLiked: Boolean, onComplete: (success: Boolean) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null || eventId.isNullOrEmpty()) {
            onComplete(false)
            return
        }

        val userId = currentUser.uid
        val db = Firebase.firestore
        val eventRef = db.collection("outreachEventsDev").document(eventId)
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            if (isLiked) {
                // Atomically add the user's ID to the event's 'likes' array
                transaction.update(eventRef, "likes", FieldValue.arrayUnion(userId))
                // Atomically add the event's ID to the user's 'likedOutreach' array
                transaction.update(userRef, "likedOutreach", FieldValue.arrayUnion(eventId))
            } else {
                // Atomically remove the user's ID from the event's 'likes' array
                transaction.update(eventRef, "likes", FieldValue.arrayRemove(userId))
                // Atomically remove the event's ID from the user's 'likedOutreach' array
                transaction.update(userRef, "likedOutreach", FieldValue.arrayRemove(eventId))
            }
            // Transaction must return a value. null is fine for write-only transactions.
            null
        }.addOnSuccessListener {
            Log.d("FirestoreLike", "Like/unlike transaction was successful.")
            onComplete(true)
        }.addOnFailureListener { e ->
            Log.e("FirestoreLike", "Like/unlike transaction failed.", e)
            onComplete(false)
        }
    }
    private fun checkQuery(event: Event, inputText: String): Boolean {
        val title = event.title.lowercase().trim()
        val description = event.description?.lowercase()?.trim() ?: "unknown"
        val location = event.location?.lowercase()?.trim() ?: "unknown"
        val skills = event.skills?.map { it.lowercase() } ?: emptyList()
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
                checkSkills
    }


    fun refresh(
        inputText: String,
        query: Query,
        showProgressBar: () -> Unit,
        onNoResults: () -> Unit,
        onComplete: () -> Unit,
    ) {
        showProgressBar()
        // make sure somebody is logged in
        // val user = Firebase.auth.currentUser ?: return
        var prevMonth: String? = null
        var prevDay: String? = null
        //val db = Firebase.firestore
        //val query = db.collection("events").orderBy("date", Query.Direction.DESCENDING)
        query.get()
            .addOnSuccessListener { result ->
                this.communityDataList.clear()
                Log.d("loadProfileImg", "before, communityDataList size: ${communityDataList.size}")
                Log.d("query", "successfully refresh: ${result.size()}")

                scope.launch {
                    for (document in result) {
                        yield()
                        var event = Event()
                        event.title = document.get("title")?.toString() ?: "Unknown"
                        event.consentBox = (document.get("consentStatus")?: false) as Boolean
                        event.contactNumber = document.get("contactNumber")?.toString()?: "Unknown"
                        event.email = document.get("emailAddress")?.toString()?: "Unknown"
                        event.description = document.get("description")?.toString() ?: "Unknown"
                        event.isFlagged = (document.get("isFlagged") ?: false) as Boolean
                        event.flaggedByUser = document.get("flaggedByUser")?.toString()
                        val location = document.get("location") as? Map<*, *>
                        if(location != null) {
                            val stateName = location["state"] ?: ""
                            val stateAbbr = getStateOrProvinceAbbreviation(stateName.toString()) // Get abbreviation or original state

                            // Set city and state separately
                            event.city = location["city"]?.toString()
                            event.state = stateAbbr
                            event.street = location["street"] ?.toString()
                            event.zipcode = location["zipcode"] ?.toString()

                            event.location = "${location["street"]}, ${location["city"]}, $stateAbbr ${location["zipcode"]}"
                        }
                        else event.location = "Unknown"

                        event.eventId = document.id
                        event.uid = document.get("uid").toString()
                        //event.time = document.get("time")?.toString() ?: "Unknown"
                        event.time = getDateTimeFromTimestamp(document.get("eventDate")).split("at ")[1]
                        document.get("interests")?.let {
                            try {
                                event.interest = it.toString().toInt()
                            } catch (e: Exception) {
                                event.interest = 0
                            }
                        }

                        // for outreachEventAndroid collection
                        event.eventStartTime = document.get("eventStartTime").toString()
                        event.eventEndTime = document.get("eventEndTime").toString()
                        event.createdAt = document.get("createdAt").toString()
                        event.helpRequest = (document.get("helpRequest") as? ArrayList<String>) ?: arrayListOf()  // List<String>
                        event.helpType = document.get("helpType").toString()
                        event.participants = (document.get("participants") as? ArrayList<String>) ?: arrayListOf() // List<String>
                        event.skills = (document.get("skills") as? ArrayList<String>) ?: arrayListOf() // List<String>
                        event.approved = (document.get("approved")?: false) as Boolean
                        event.totalSlots = document.get("totalSlots")?.toString()?.toIntOrNull()
                        event.skills?.forEach { skill ->
                            val index = requiredSkills.indexOf(skill)
                            if(index != -1) {
                                event.requiredSkillsBooleanArray[index] = true
                            }

                        }

                        val user = Firebase.auth.currentUser
                        val likes = document.get("likes") as? List<*>
                        event.likeCount = likes?.size ?: 0
                        event.likedByMe = user != null && likes?.contains(user.uid) == true

                        if(!checkQuery(event, inputText)) continue


                        //Log.d("Event date", "Event date"+event.date.toString())
                        val date:String = document.get("eventDate")?.toString()  ?: "Unknown"
                        //Log.d("date", "date: $date")

                        if(date != "Unknown"){
                            // Convert the Instant to a LocalDateTime in the system default time zone
                            val localDateTime = Extensions.dateParser(date)
// Extract the month from the LocalDateTime
                            val month = localDateTime?.month ?:"Unknown"
                            val dayOfMonth = localDateTime?.dayOfMonth?.toString() ?:"NA"
                            val dayOfWeek = localDateTime?.dayOfWeek?.toString() ?:"NA"
                            val year = localDateTime?.year ?:"Unknown"
// Get the month name as a string

                            val monthName = month.toString()
// Extract the month and date
                            if(dayOfWeek.length>3){
                                event.day = dayOfWeek.substring(0,3)
                            }

                            event.date = dayOfMonth
                            event.year = "$monthName $year"
                            if(prevMonth!=null){
                                if(!month.toString().equals(prevMonth)){
                                    prevMonth = month.toString()
                                    event.layoutType = Extensions.TYPE_NEW_DAY
                                    var eventYear = EventYear()
                                    eventYear.year = "$monthName $year"
                                    var community = CommunityData(eventYear, Extensions.TYPE_MONTH)
                                    this@EventDataAdapter.communityDataList.add(community)
                                }
                                else{
                                    if(dayOfMonth != prevDay){
                                        prevDay = dayOfMonth
                                        event.layoutType = Extensions.TYPE_NEW_DAY
                                    }
                                    else{
                                        event.layoutType = Extensions.TYPE_DAY
                                    }
                                }
                            }
                            else{
                                prevMonth = month.toString()
                                prevDay = dayOfMonth
                                event.layoutType = Extensions.TYPE_NEW_DAY
                                val eventYear = EventYear()
                                eventYear.year = "$monthName $year"
                                val community = CommunityData(eventYear,Extensions.TYPE_MONTH)
                                this@EventDataAdapter.communityDataList.add(community)
                            }

                            //this.events.add(event)
                            val communityEvent = CommunityData(event,event.layoutType!!)
                            this@EventDataAdapter.communityDataList.add(communityEvent)

                        }

                    }

                    Log.d("query", "communityData Size: ${communityDataList.size}")

                    /*
                    if(communityDataList.isNotEmpty()) {
                        refreshedLiked{
                            onComplete()
                        }
                    }

                     */

                    Log.d("loadProfileImg", "after, communityDataList size: ${communityDataList.size}")
//                if(communityDataList.size < 10) {
//                    for(communityData in communityDataList) {
//                        Log.d("loadProfileImg", "event: ${communityData.event?.title}, ${communityData.event?.eventId}")
//                    }
//                }

                    if(communityDataList.isNotEmpty()) {
                        refreshedLikedEvents(scope) {
                            onComplete()
                        }
                    }else {
                        onNoResults()
                    }
                }

            }.addOnFailureListener { exception ->
                Log.d("query", "refresh failed: $exception")
                onComplete()
            }
    }



   private fun refreshedLiked(onComplete: () -> Unit) {

        // make sure somebody is logged in
        val user = Firebase.auth.currentUser ?: return

        val db = Firebase.firestore

        /*db.collection("likedEvents").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { results ->

                for (document in results) {
                    for (event in this.events) {
                        if (event.eventId == document.get("eventId").toString()) {
                            event.liked = true
                        }
                    }
                }

                onComplete()
            }
            .addOnFailureListener { exceptioon ->
                onComplete()
            }*/
       db.collection("likedEvents").get()
           .addOnSuccessListener { results ->
               Log.d("query", "in refreshedLiked: communityData Size: ${communityDataList.size}")
               for(community in this.communityDataList) {
                   community.event?.let { event ->
                       event.interest = 0
                   }
               }
               for (document in results) {
                   for(community in this.communityDataList){
                       community.event?.let{ event->
                           if(event.eventId == document.get("eventId").toString()){
                               if (user.uid==document.get("uid").toString()) {
                                   event.signedUp = true
                                   event.interest = event.interest?.minus(1)
                               }
                               else{
                                   if(event.itemList.size<3)
                                       event.addValue(document.get("profileImageUrl").toString())
                               }
                               event.addInterest()
                           }
                       }
                   }
               }
               onComplete()
           }
           .addOnFailureListener { exception ->
               onComplete()
           }

    }

    private fun refreshedLikedEvents(
        scope: CoroutineScope,
        onComplete: () -> Unit,
    ) {

        // make sure somebody is logged in
        val user = Firebase.auth.currentUser

        if (user == null) {
            onComplete()
            return
        }
        val db = Firebase.firestore

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                Log.d("query", "in refreshedLiked: communityData Size: ${communityDataList.size}")
                scope.launch {
                    if (document != null) {
                        val userData = document.data
                        userData?.let {
                            it["outreachEvents"]?.let { outreachEventIDs ->
                                (outreachEventIDs as? ArrayList<String>)?.forEach { outreachEventID ->
                                    communityDataList.find { communityData ->
                                        communityData.event?.eventId == outreachEventID
                                    }?.event?.let { event ->
                                        event.signedUp = true
                                    }
                                }

                            }
                        }
                        Log.d("syncWebApp", "DocumentSnapshot data: ${document.data}")
                    } else {
                        Log.d("syncWebApp", "No such document")
                    }
                    /*
                    for (document in results) {
                        yield()
                        if(user.uid == document.get("uid").toString()) {
                            communityDataList.find { communityData ->
                                document.get("eventId").toString() == communityData.event?.eventId
                            }?.event?.let { event ->
                                event.signedUp = true
                            }
                        }

                        communityDataList.find { communityData ->
                            communityData.event?.eventId == document.get("eventId").toString()
                        }?.let { communityData ->
                            communityData.event?.let { event ->
                                event.interest?.let { numOfInterest ->
                                    /*
                                     Since multiple refreshedLikedEvents functions will be called concurrently
                                     on different threads, modifying communityData.event?.itemList,
                                     we must ensure that the size of itemList is not greater than event.interest
                                     before making any modifications/updates.
                                     For example, if the refresh is called twice,
                                     it will trigger the refreshedLikedEvents function twice as well.
                                     Without the `if(event.itemList.size < numOfInterest)` condition,
                                     the itemList will contain repetitive elements,
                                     such as [profileImg1, profileImg2, profileImg3, profileImg1, profileImg2, profileImg3],
                                     instead of the expected [profileImg1, profileImg2, profileImg3].
                                     This leads to repetitive results based on the number of times the refreshedLikedEvents function is called.

                                     */
                                    if(event.itemList.size < numOfInterest) {
                                        communityData.event?.addValue(document.get("profileImageUrl").toString())
                                    }
                                }
                            }
                        }

//                    val likedCommunityDataList = communityDataList.filter { communityData ->
//                        communityData.event?.eventId == document.get("eventId").toString()
//                    }
//
//                    for(likedCommunityData in likedCommunityDataList) {
//                        val index = communityDataList.indexOf(likedCommunityData)
//                        val event = communityDataList[index].event
////                        if(event != null) {
////                            if(event.itemList.size >= 3) break
////                            event.addValue(document.get("profileImageUrl").toString())
////                        }
//                        event?.addValue(document.get("profileImageUrl").toString())
//                    }
                    }
                    */

                    onComplete()
                }

            }
            .addOnFailureListener { exception ->
                onComplete()
            }


    }


} // end class