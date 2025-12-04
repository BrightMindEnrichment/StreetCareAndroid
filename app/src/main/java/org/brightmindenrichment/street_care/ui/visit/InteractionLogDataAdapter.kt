package org.brightmindenrichment.street_care.ui.visit

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog

class InteractionLogDataAdapter {


    private val db = FirebaseFirestore.getInstance()
    var interactions: MutableList<InteractionLog> = mutableListOf()

    val size: Int
        get() = interactions.size

    fun getInteractionAtPosition(position: Int): InteractionLog? {
        return if (position in 0 until interactions.size) interactions[position] else null
    }

    fun refreshAll(onComplete: () -> Unit) {
        val user = Firebase.auth.currentUser ?: return
        val allInteractions = mutableListOf<InteractionLog>()

        var completedFetches = 0

        fun checkAndFinish() {
            if (completedFetches == 3) {
                interactions.clear()
                interactions.addAll(allInteractions.sortedByDescending { it.interactionDate })
                onComplete()
            }
        }

        db.collection("InteractionLogDev")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                processDocuments(result, allInteractions)
                interactions.clear()
                interactions.addAll(allInteractions.sortedByDescending { it.interactionDate?.toDate() })
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("InteractionLogDataAdapter", "Error fetching logs: $e")
                onComplete()
            }

        // First fetch
//        db.collection("VisitLogBook")
//            .whereEqualTo("uid", user.uid)
//            .get()
//            .addOnSuccessListener { result ->
//                processOldDocuments(result)
//                completedFetches++
//                checkAndFinish()
//            }
//            .addOnFailureListener {
//                Log.w("VisitLog", "Failed to fetch VisitLogBook: $it")
//                completedFetches++
//                checkAndFinish()
//            }


        //val user = Firebase.auth.currentUser
        if (user == null) {
            Log.e("InteractionLogDev", "No authenticated user — cannot fetch.")
        } else {

//            db.collection("InteractionLogDev")
//                .whereEqualTo(FieldPath.documentId(), "AB342F2E-28E5-4D19-B8B5-E68DDAD8D032")
//                .get()
//                .addOnSuccessListener { result ->
//                    Log.d("FetchTest", "Fetch complete. Count = ${result.size()}")
//
//                    val allInteractions = mutableListOf<InteractionLog>()
//                    processDocuments(result, allInteractions)
//                    interactions.clear()
//                    interactions.addAll(allInteractions.sortedByDescending { it.interactionDate?.toDate() })
//
//                    // Display the details of each fetched interaction
//                    for ((index, interaction) in allInteractions.withIndex()) {
//                        Log.d("FetchTest", "[$index] $interaction"
//                            //"[${index}] ID=${interaction.id}, Email=${interaction.email}, UserId=${interaction.userId}, FirstName=${interaction.firstName}, LastName=${interaction.lastName}, NumPeopleHelped=${interaction.numPeopleHelped}, CarePackages=${interaction.carePackagesDistributed}"
//                        )
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("FetchTest", "Fetch failed: $e")
//                }
        }

    }


    fun getPublicInteractionLogs(onComplete: () -> Unit) {
        db.collection("InteractionLogDev")
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener { result ->
                val publicInteractions = mutableListOf<InteractionLog>()
                processDocuments(result, publicInteractions)
                interactions.clear()
                interactions.addAll(publicInteractions.sortedByDescending { it.interactionDate?.toDate() })
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("InteractionLogDataAdapter", "Error fetching public logs: $e")
                onComplete()
            }
    }

    fun fetchByDocumentId(
        documentId: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("InteractionLogDev")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                interactions.clear()

                if (document.exists()) {
                    try {
                        val log = InteractionLog(
                            id = document.id,

                            addr1 = document.getString("addr1") ?: "",
                            addr2 = document.getString("addr2") ?: "",
                            city = document.getString("city") ?: "",
                            state = document.getString("state") ?: "",
                            country = document.getString("country") ?: "",
                            zipcode = document.getString("zipcode") ?: "",

                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            email = document.getString("email") ?: "",
                            phoneNumber = document.getString("phoneNumber") ?: "",

                            isPublic = document.getBoolean("isPublic") ?: false,
                            status = document.getString("status") ?: "Pending",

                            startTimestamp = document.getTimestamp("startTimestamp"),
                            endTimestamp = document.getTimestamp("endTimestamp"),
                            interactionDate = document.getTimestamp("interactionDate"),
                            lastModifiedTimestamp = document.getTimestamp("lastModifiedTimestamp"),

                            carePackageContents = document.getString("carePackageContents") ?: "",
                            carePackagesDistributed = (document.getLong("carePackagesDistributed") ?: 0L).toInt(),

                            helpRequestCount = (document.getLong("helpRequestCount") ?: 0L).toInt(),
                            helpRequestDocIds = document.get("helpRequestDocIds") as? List<String> ?: emptyList(),

                            listOfSupportsProvided = document.get("listOfSupportsProvided") as? List<String> ?: emptyList(),
                            numPeopleHelped = (document.getLong("numPeopleHelped") ?: 0L).toInt(),
                            numPeopleJoined = (document.getLong("numPeopleJoined") ?: 0L).toInt(),

                            outreachId = document.getString("outreachId") ?: "",
                            userId = document.getString("userId") ?: "",

                            lastActionPerformed = document.getString("lastActionPerformed")
                        )

                        interactions.add(log)
                        onComplete(true)

                    } catch (e: Exception) {
                        Log.e("InteractionLogAdapter", "Parsing failed", e)
                        onComplete(false)
                    }
                } else {
                    Log.w("InteractionLogAdapter", "Document not found: $documentId")
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("InteractionLogAdapter", "Fetch failed", e)
                onComplete(false)
            }
    }




    /** Convert Firestore documents -> InteractionLog objects */
    private fun processDocuments(result: QuerySnapshot, targetList: MutableList<InteractionLog>) {
        for (document in result) {
            try {
                val log = InteractionLog(
                    id = document.id,

                    addr1 = document.getString("addr1") ?: "",
                    addr2 = document.getString("addr2") ?: "",
                    city = document.getString("city") ?: "",
                    state = document.getString("state") ?: "",
                    country = document.getString("country") ?: "",
                    zipcode = document.getString("zipcode") ?: "",

                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    email = document.getString("email") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",

                    isPublic = document.getBoolean("isPublic") ?: false,
                    status = document.getString("status") ?: "Pending",

                    startTimestamp = document.getTimestamp("startTimestamp"),
                    endTimestamp = document.getTimestamp("endTimestamp"),
                    interactionDate = document.getTimestamp("interactionDate"),
                    lastModifiedTimestamp = document.getTimestamp("lastModifiedTimestamp"),

                    carePackageContents = document.getString("carePackageContents") ?: "",
                    carePackagesDistributed = (document.getLong("carePackagesDistributed") ?: 0L).toInt(),

                    helpRequestCount = (document.getLong("helpRequestCount") ?: 0L).toInt(),
                    helpRequestDocIds = document.get("helpRequestDocIds") as? List<String> ?: emptyList(),

                    listOfSupportsProvided = document.get("listOfSupportsProvided") as? List<String> ?: emptyList(),
                    numPeopleHelped = (document.getLong("numPeopleHelped") ?: 0L).toInt(),
                    numPeopleJoined = (document.getLong("numPeopleJoined") ?: 0L).toInt(),

                    outreachId = document.getString("outreachId") ?: "",
                    userId = document.getString("userId") ?: "",

                    lastActionPerformed = document.getString("lastActionPerformed")
                )

                targetList.add(log)

            } catch (e: Exception) {
                Log.e("InteractionLogDataAdapter", "Error parsing document ${document.id}: $e")
            }
        }
    }
}

