package org.brightmindenrichment.street_care.ui.visit.repository


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import java.util.Date

class VisitLogRepositoryImp : VisitLogRepository {

    var visits: MutableList<VisitLog> = mutableListOf()
    val visit = VisitLog()

    override fun saveVisitLog(visitLog: VisitLog) {
        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return
        Log.d("BME", user.uid)

        //Adding code to fix Date and Time issue in whenVisit and andWhenVisitTime
        //var tempDate: Date = visitLog.date
        //visitLog.date
        //val splitTemp = temp.split(" ")
        //val fixedDate = splitTemp[0] +" "+ splitTemp[1] +" "+ splitTemp[2] +" "+ visitLog.whenVisitTime.toString().uppercase() +" "+ splitTemp[4] +" "+ splitTemp[5]
        //visitLog.fixedDate = fixedDate

        // create a map of event data so we can add to firebase
        val visitData = hashMapOf(

            "whenVisit" to visitLog.date, //1
            //"whenVisitTime" to visitLog.whenVisitTime, //1
            "whereVisit" to visitLog.whereVisit, //2
            //"Location" to visitLog.locationmap, //2
            "locationDescription" to visitLog.locationDescription, //2
            "peopleHelped" to visitLog.peopleCount, //3
            "peopleHelpedDescription" to visitLog.peopleHelpedDescription, //3
            "foodAndDrinks" to visitLog.food_drink, //4
            "clothes" to visitLog.clothes, //4
            "hygiene" to visitLog.hygiene, //4
            "wellness" to visitLog.wellness, //4
            "medical" to visitLog.medicalhelp, //4
            "social" to visitLog.socialWorker, //4
            "legal" to visitLog.lawyerLegal, //4
            "other" to visitLog.other, //4
            "whatGiven" to visitLog.whattogive, //4
            "otherNotes" to visitLog.otherDetail, //4
            "itemQty" to visitLog.number_of_items, //5
            "itemQtyDescription" to visitLog.itemQtyDescription, //5
            "rating" to visitLog.experience, //6
            "ratingNotes" to visitLog.comments, //6

            "durationHours" to visitLog.visitedHours, //1A
            "durationMinutes" to visitLog.visitedMinutes, //1A
            "numberOfHelpers" to visitLog.whoJoined, //2A
            "numberOfHelpersComment" to visitLog.numberOfHelpersComment, //2A
            "peopleNeedFurtherHelp" to visitLog.stillNeedSupport, //3A
            "peopleNeedFurtherHelpComment" to visitLog.supportTypeNeeded, //3A
            "peopleNeedFurtherHelpLocation" to visitLog.peopleNeedFurtherHelpLocation, //3A
            "furtherFoodAndDrinks" to visitLog.add_food_drink, //4A
            "furtherClothes" to visitLog.add_clothes, //4A
            "furtherHygiene" to visitLog.add_hygine, //4A
            "furtherWellness" to visitLog.add_wellness, //4A
            "furtherMedical" to visitLog.add_medicalhelp, //4A
            "furtherSocial" to visitLog.add_socialWorker, //4A
            "furtherLegal" to visitLog.add_lawyerLegal, //4A
            "furtherOther" to visitLog.add_other, //4A
            "furtherOtherNotes" to visitLog.add_otherDetail, //4A
            "whatGivenFurther" to visitLog.whatrequired, //4A
            "followUpWhenVisit" to visitLog.followupDate, //5A
            "futureNotes" to visitLog.futureNotes, //6A
            "volunteerAgain" to visitLog.visitAgain, //7A

            "lastEdited" to visitLog.lastEditedTime,
            "type" to visitLog.typeofdevice,
            "timeStamp" to visitLog.createdTime,
            "uid" to user.uid,
            "isPublic" to visitLog.isPublic,
            "status"  to "",
            "isFlagged" to visitLog.isFlagged,
            "flaggedByUser" to visitLog.flaggedByUser
        )
        // save to firebase
        val db = FirebaseFirestore.getInstance()
        db.collection("VisitLogBook_New").add(visitData).addOnSuccessListener { documentReference ->
            Log.d("BME", "Saved with id ${documentReference.id}")
            // Store this ID somewhere – e.g., inside your visitLog object
            visitLog.documentId = documentReference.id
            //onComplete()
        }.addOnFailureListener { exception ->
            Log.w("BMR", "Error in Save VisitLog ${exception.toString()}")
            //onComplete()
        }
    }


    override fun loadVisitLogs(onComplete: () -> Unit) {
        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return
        //val user = Firebase.auth.currentUser
        Log.d("BME", user.uid)
        val db = FirebaseFirestore.getInstance()
        //load old DB Collection records
        db.collection("VisitLogBook").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { result ->
                // we are going to reload the whole list, remove anything already cached
                this.visits.clear()
                for (document in result) {
                    val visit = VisitLog()

                    visit.date = document.get("whenVisit") as Date
                    visit.whereVisit = document.get("whereVisit").toString()
                    visit.visitAgain = document.get("volunteerAgain").toString()
                    visit.peopleCount = document.get("peopleHelped") as Long
                    visit.experience = document.get("rating") as Int
                    visit.comments = document.get("ratingNotes").toString()
                    visit.followupDate = document.get("followUpWhenVisit") as Date
                    //visit.date = document.get("whenVisit") as Date
                    visit.names = document.get("names(opt)").toString()

                    if(document.get("whenVisit")!=null)
                    {
                        val d = document.get("whenVisit") as com.google.firebase.Timestamp
                        visit.date = d.toDate()
                    }
                    this.visits.add(visit)
                }
                onComplete()
            }.addOnFailureListener { exception ->
                Log.w("BMR", "Error in Add VisitLog ${exception.toString()}")
                //onComplete()
                db.collection("VisitLogBook").whereEqualTo("share", true).get()
                    .addOnSuccessListener { result ->

                        // we are going to reload the whole list, remove anything already cached
                        this.visits.clear()

                        for (document in result) {
                            val visit = VisitLog()

                            visit.whereVisit = document.get("whereVisit").toString()
                            //visit.hours = document.get("hoursSpentOnOutreach") as Long
                            visit.visitAgain = document.get("volunteerAgain").toString()
                            visit.peopleCount = document.get("peopleHelped") as Long
                            visit.experience = document.get("rating") as Int
                            visit.comments = document.get("ratingNotes").toString()
                            if(document.get("whenVisit")!=null)
                            {
                                val d = document.get("whenVisit") as com.google.firebase.Timestamp
                                visit.date = d.toDate()
                            }

                            this.visits.add(visit)
                        }

                        onComplete()

                    }
            }
        //load new DB Collection logs VisitLogBook_New
        db.collection("VisitLogBook_New").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { result ->
                // we are going to reload the whole list, remove anything already cached
                this.visits.clear()
                for (document in result) {
                    val visit = VisitLog()

                    visit.date = document.get("whenVisit") as Date
                    visit.whereVisit = document.get("whereVisit").toString()
                    visit.visitAgain = document.get("volunteerAgain").toString()
                    visit.peopleCount = document.get("peopleHelped") as Long
                    visit.experience = document.get("rating") as Int
                    visit.comments = document.get("ratingNotes").toString()
                    visit.followupDate = document.get("followUpWhenVisit") as Date
                    //visit.date = document.get("whenVisit") as Date
                    visit.names = document.get("names(opt)").toString()

                    if(document.get("whenVisit")!=null)
                    {
                        val d = document.get("whenVisit") as com.google.firebase.Timestamp
                        visit.date = d.toDate()
                    }
                    this.visits.add(visit)
                }
                onComplete()
            }.addOnFailureListener { exception ->
                Log.w("BMR", "Error in Add VisitLog ${exception.toString()}")
                //onComplete()
                db.collection("VisitLogBook_New").whereEqualTo("share", true).get()
                    .addOnSuccessListener { result ->

                        // we are going to reload the whole list, remove anything already cached
                        this.visits.clear()

                        for (document in result) {
                            val visit = VisitLog()

                            visit.whereVisit = document.get("whereVisit").toString()
                            //visit.hours = document.get("hoursSpentOnOutreach") as Long
                            visit.visitAgain = document.get("volunteerAgain").toString()
                            visit.peopleCount = document.get("peopleHelped") as Long
                            visit.experience = document.get("rating") as Int
                            visit.comments = document.get("ratingNotes").toString()

                            if(document.get("whenVisit")!=null)
                            {
                                val d = document.get("whenVisit") as com.google.firebase.Timestamp
                                visit.date = d.toDate()
                            }

                            this.visits.add(visit)
                        }

                        onComplete()

                    }
            }
    }// end of class
}



