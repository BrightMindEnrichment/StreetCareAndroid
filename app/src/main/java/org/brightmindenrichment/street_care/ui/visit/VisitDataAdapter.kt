package org.brightmindenrichment.street_care.ui.visit

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.QuerySnapshot
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.ui.visit.data.Status
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import java.util.*


class VisitDataAdapter {

    var visits: MutableList<VisitLog> = mutableListOf()
    private var totalPeopleCount: Long = 0
    private var totalItemsDonated: Long = 0
    val size: Int
        get() {
            return visits.size
        }
    val getTotalPeopleCount: Long
        get() {
            return totalPeopleCount
        }
    val getTotalItemsDonated: Long
        get() {
            return totalItemsDonated
        }

    /***
     * Returns a visit log for a given position.
     *
     * If the position is not valid, null is returned.
     * */
    fun getVisitAtPosition(position: Int): VisitLog? {
        if ((position >= 0) && (position < visits.size)) {
            return visits[position]
        }
        return null
    }

    fun refreshAll(onComplete: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val allVisits = mutableListOf<VisitLog>()
        totalPeopleCount = 0
        totalItemsDonated = 0
        var completedFetches = 0

        fun checkAndFinish() {
            if (completedFetches == 3) {
                visits.clear()
                visits.addAll(allVisits.sortedByDescending { it.date })
                onComplete()
            }
        }


        val processOldDocuments: (QuerySnapshot) -> Unit = { result ->

            for (document in result) {
                val visit = VisitLog()
                visit.id = document.id

                try {

                    val platformType = document.getString("Type")


                    visit.typeofdevice = platformType?.toString() ?: "iOS"


                    val isIOS = platformType == null || !platformType.equals("Android", ignoreCase = true)

                    //Q11
                    (document.get("whenVisit") as? com.google.firebase.Timestamp)?.toDate()?.let {
                        visit.date = it
                    }

                    //Q2

                    visit.whereVisit = if (isIOS) {

                        document.getString("whereVisit") ?: ""
                 } else {

                        val loc = document.get("Location") as? Map<*, *>
                        val street = loc?.get("street") as? String ?: ""
                        val city = loc?.get("city") as? String ?: ""
                        val state = loc?.get("state") as? String ?: ""
                        val zip = loc?.get("zipcode") as? String ?: ""
                        listOf(street, city, state, zip).filter { it.isNotBlank() }.joinToString(", ")

                    }


                    // Q3

                    visit.peopleCount = if (isIOS) {
                        // iOS-specific field
                        document.get("peopleHelped") as? Long ?: 0L

                    } else {
                        // Android-specific field
                        document.get("NumberOfPeopleHelped") as? Long ?: 0L
                    }

                    // Q5

                    visit.number_of_items = if (isIOS) {
                        // iOS-specific field

                        document.get("itemQty") as? Long ?: 0L
                    } else {
                        // Android-specific field
                        document.get("number_of_items_donated") as? Long ?: 0L
                    }


                    // Q6

                    visit.experience = if (isIOS) {
                        (document.get("rating") as? Long ?: 0L).toInt()
                    } else {
                        (document.get("rating") as? Long ?: 0L).toInt()
                    }


                    // Q7

                    var totalHoursSpent = "0"
                    var totalMinutesSpent = "0"

                    if (isIOS) {
                        val hours = document.get("durationHours")?.toString()?.toLongOrNull() ?: 0L
                        val minutes = document.get("durationMinutes")?.toString()?.toLongOrNull() ?: 0L


                        totalHoursSpent = hours.toString()
                        totalMinutesSpent = minutes.toString()

                        visit.visitedHours = hours.toInt()
                        visit.visitedMinutes = minutes.toInt()

                        visit.helpTime = "$totalHoursSpent hr, $totalMinutesSpent min"
                    } else {
                        val hours = (document.get("total_hours_spent") as? Number)?.toLong() ?: 0L
                        val minutes = (document.get("total_minutes_spent") as? Number)?.toLong() ?: 0L

                        Log.d(TAG, "Android values — Hours: $hours, Minutes: $minutes")

                        totalHoursSpent = hours.toString()
                        totalMinutesSpent = minutes.toString()

                        visit.visitedHours = hours.toInt()
                        visit.visitedMinutes = minutes.toInt()

                        visit.helpTime = "$totalHoursSpent hr, $totalMinutesSpent min"
                    }


                    // Q8

                    visit.whoJoined = if (isIOS) {
                        (document.get("numberOfHelpers") as? Long ?: 0L).toInt()

                    } else {
                        (document.get("numberOfHelpers") as? Long ?: 0L).toInt()
                    }


                    // Q9

                    visit.stillNeedSupport = if (isIOS) {
                        // iOS-specific field
                        (document.get("peopleNeedFurtherHelp") as? Long ?: 0L).toInt()
                    } else {
                        // Android-specific field
                        (document.get("stillNeedSupport") as? Long ?: 0L).toInt()
                    }

                    // Q11

                    val timestamp = if (isIOS) {
                        document.get("followUpWhenVisit") as? com.google.firebase.Timestamp
                    } else {
                        document.get("followupDate") as? com.google.firebase.Timestamp
                    }

                    visit.followupDate = timestamp?.toDate()  // fallback to current time if null


                    //Q12
                    visit.futureNotes = if (isIOS) {
                        //new field - created with updation
                        document.get("futureNotes")?.toString() ?: "NA"
                    } else {
                        document.get("future_notes")?.toString() ?: "NA"
                    }


                    //Q13

                    visit.visitAgain = if (isIOS) {
                        when (val value = document.get("volunteerAgain")) {
                            is Long -> when (value) {
                                0L -> "No"
                                1L -> "Yes"
                                2L -> "Maybe"
                                else -> ""
                            }
                            is String -> value
                            else -> ""
                        }
                    } else {
                        when (val value = document.get("visitAgain")) {
                            is Long -> when (value) {
                                0L -> "No"
                                1L -> "Yes"
                                2L -> "Maybe"
                                else -> ""
                            }
                            is String -> value
                            else -> ""
                        }
                    }



                    allVisits.add(visit)
                } catch (e: Exception) {
                    Log.e("VisitLog", "Error parsing OLD document ${document.id}: $e")
                }
            }
        }


        val processNewDocuments: (QuerySnapshot) -> Unit = { result ->

            for (document in result) {
                val visit = VisitLog()
                visit.id = document.id

                try {
                    // Q1
                    (document.get("whenVisit") as? com.google.firebase.Timestamp)?.toDate()?.let {
                        visit.date = it
                    }

                    // Q2
                    visit.whereVisit = document.getString("whereVisit") ?: ""

                    // Q3
                    (document.get("peopleHelped") as? Long)?.let {
                        visit.peopleCount = it
                        totalPeopleCount += it
                    }

                    visit.peopleHelpedDescription = document.getString("peopleHelpedDescription") ?: ""


                    // Q4
                    visit.food_drink = (document.get("foodAndDrinks") as? String) == "Y"
                    visit.clothes = (document.get("clothes") as? String) == "Y"
                    visit.hygiene = (document.get("hygiene") as? String) == "Y"
                    visit.wellness = (document.get("wellness") as? String) == "Y"
                    visit.lawyerLegal = (document.get("legal") as? String) == "Y"
                    visit.medicalhelp = (document.get("medical") as? String) == "Y"
                    visit.socialWorker = (document.get("social") as? String) == "Y"
                    visit.other = (document.get("other") as? String) == "Y"


                    (document.get("whatGiven") as? List<*>)?.let { list ->
                        visit.whatGiven = list.filterIsInstance<String>().joinToString(", ")
                    }



                    //Q5

                    (document.get("itemQty") as? Long)?.let {
                        visit.number_of_items = it
                        totalItemsDonated += it
                    }

                    //Q6

                    (document.get("rating") as? Long)?.let {
                        visit.experience = it.toInt()
                    }


                    //Q7

                    document.get("durationHours")?.toString()?.let {
                        visit.visitedHours = it.toInt()
                    }

                    document.get("durationMinutes")?.toString()?.let {
                        visit.visitedMinutes = it.toInt()
                    }

                    visit.helpTime = "${visit.visitedHours} hr, ${visit.visitedMinutes} min"


                    //Q8

                    (document.get("numberOfHelpers") as? Long)?.let {
                        visit.whoJoined = it.toInt()
                    }

                    //Q9
                    (document.get("peopleNeedFurtherHelp") as? Long)?.let {
                        visit.stillNeedSupport = it.toInt()
                    }

                    //Q10

                    (document.get("whatGivenFurther") as? List<*>)?.let { list ->
                        visit.whatGivenFurther = list.filterIsInstance<String>().joinToString(", ")
                    }

                    //Q11
                    (document.get("followUpWhenVisit") as? com.google.firebase.Timestamp)?.toDate()?.let {
                        visit.followupDate = it
                    }

                    //Q12
                    document.get("futureNotes")?.toString()?.let {
                        visit.futureNotes = it
                    }

                    //Q13
                    document.get("volunteerAgain")?.toString()?.let {
                        visit.visitAgain = it
                    }

                    val isPublic = (document.getBoolean("isPublic"))
                    if (isPublic == true) {
                        (document.get("status") as? String)?.let { status ->
                            visit.status = when(status.lowercase())  {
                                "pending" -> Status.PENDING
                                "approved" -> Status.PUBLISHED
                                "rejected" -> Status.REJECTED
                                else -> Status.PRIVATE
                            }
                        }
                    }

                    // Count flags
                    if (visit.clothes == true) totalItemsDonated++
                    if (visit.food_drink == true) totalItemsDonated++
                    if (visit.hygiene == true) totalItemsDonated++
                    if (visit.wellness == true) totalItemsDonated++
                    if (visit.other == true) totalItemsDonated++

                    allVisits.add(visit)

                } catch (e: Exception) {
                    Log.e("VisitLog", "Error parsing NEW document ${document.id}: $e")
                }
            }
        }


        val processProdDocuments: (QuerySnapshot) -> Unit = { result ->

            for (document in result) {
                val visit = VisitLog()
                visit.id = document.id

                try {
                    val city = document.getString("city") ?: ""
                    val state = document.getString("stateAbbv") ?: ""
                    val street = document.getString("street") ?: ""
                    val zip = document.getString("zipcode") ?: ""

                    visit.whereVisit = listOf(street, city, state, zip).filter { it.isNotBlank() }.joinToString(", ")

                    (document.get("dateTime") as? com.google.firebase.Timestamp)?.toDate()?.let {
                        visit.date = it
                    }

                    visit.description = document.getString("description") ?: ""
                    visit.userId = document.get("uid").toString()

                    (document.get("whatGiven") as? List<*>)?.let { list ->
                        visit.whatGiven = list.filterIsInstance<String>().joinToString(", ")
                    }

                    (document.get("itemQty") as? Long)?.let {
                        visit.number_of_items = it
                    }
                    visit.peopleCount = document.getString("numberPeopleHelped")?.toLongOrNull() ?: 0L

                    visit.experience = (document.get("rating") as? Long ?: 0L).toInt()

                    visit.isPublic = document.getBoolean("public") ?: false
                    if (visit.isPublic) {
                        (document.get("status") as? String)?.let { status ->
                            visit.status = when (status.lowercase()) {
                                "approved" -> Status.PUBLISHED
                                "pending" -> Status.PENDING
                                "rejected" -> Status.REJECTED
                                else -> Status.PRIVATE
                            }
                        }
                    }

                    visit.isFlagged = document.getBoolean("isFlagged") ?: false
                    visit.flaggedByUser = document.getString("flaggedByUser") ?: ""

                    allVisits.add(visit)
                } catch (e: Exception) {
                    Log.e("VisitLog", "Error parsing prod doc: ${document.id}: $e")
                }
            }
        }
        // First fetch
        db.collection("VisitLogBook")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { result ->
                processOldDocuments(result)
                completedFetches++
                checkAndFinish()
            }
            .addOnFailureListener {
                Log.w("VisitLog", "Failed to fetch VisitLogBook: $it")
                completedFetches++
                checkAndFinish()
            }

        db.collection("VisitLogBook_New")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { result ->
                processNewDocuments(result)
                completedFetches++
                checkAndFinish()
            }
            .addOnFailureListener {
                Log.w("VisitLog", "Failed to fetch VisitLogBook_New: $it")
                completedFetches++
                checkAndFinish()
            }

        db.collection("visitLogWebProd")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { result ->
                processProdDocuments(result)
                Log.d(TAG, "processProdDocuments: called ")
                completedFetches++
                checkAndFinish()
            }
            .addOnFailureListener {
                Log.w("VisitLog", "Failed to fetch visitLogWebProd: $it")
                completedFetches++
                checkAndFinish()
            }

    }


    fun getPublicVisitLog(onComplete: () -> Unit) {
        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return
        Log.d("BME", user.uid)
        val db = FirebaseFirestore.getInstance()
        db.collection("VisitLogBook").whereEqualTo("share", true).get()
            .addOnSuccessListener { result ->
                // we are going to reload the whole list, remove anything already cached
                this.visits.clear()
                for (document in result) {
                    var visit = VisitLog()
                    visit.location = document.get("whereVisit").toString()
                    visit.whenVisit = document.get("whenVisit").toString()
                    visit.userId = document.get("uid").toString()
                    if(document.get("whenVisit")!=null)
                    {
                        val d = document.get("whenVisit") as com.google.firebase.Timestamp
                        if (d != null) {
                            visit.date = d.toDate()
                        }
                    }
                    if (visit.userId != user.uid) {
                        this.visits.add(visit)
                    }
                }
                this.visits.sortByDescending { it.date }
                onComplete()
            }
    }
}

fun addVisit(location: String, hours: Long, visitAgain: String, peopleCount: Long, experience: String, comments: String, date: Date, onComplete: () -> Unit) {

    // make sure somebody is logged in
    val user = FirebaseAuth.getInstance().currentUser ?: return

    // create a map of event data so we can add to firebase
    val visitData = hashMapOf(
        "location" to location,
        "hoursSpentOnOutreach" to hours,
        "willPerformOutreachAgain" to visitAgain,
        "helpers" to peopleCount,
        "rating" to experience,
        "ratingNotes" to comments,
        "uid" to user.uid
    )

    // save to firebase
    val db = FirebaseFirestore.getInstance()
    db.collection("surveys").add(visitData).addOnSuccessListener { documentReference ->
        Log.d("BME", "Saved with id ${documentReference.id}")
        onComplete()
    } .addOnFailureListener { exception ->
        Log.w("BMR", "Error in addEvent ${exception.toString()}")
        onComplete()
    }
}
// end class