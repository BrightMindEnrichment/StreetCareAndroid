package org.brightmindenrichment.street_care.ui.visit.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog

class InteractionLogRepositoryImpl: InteractionLogRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collectionName = "InteractionLogDev"

    override fun saveInteractionLog(
        interactionLog: InteractionLog,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val data = hashMapOf(
            "addr1" to interactionLog.addr1,
            "addr2" to interactionLog.addr2,
            "city" to interactionLog.city,
            "state" to interactionLog.state,
            "country" to interactionLog.country,
            "zipcode" to interactionLog.zipcode,

            "firstName" to interactionLog.firstName,
            "lastName" to interactionLog.lastName,
            "email" to interactionLog.email,
            "phoneNumber" to interactionLog.phoneNumber,

            "isPublic" to interactionLog.isPublic,
            "status" to interactionLog.status,

            "startTimestamp" to interactionLog.startTimestamp,
            "endTimestamp" to interactionLog.endTimestamp,
            "interactionDate" to interactionLog.interactionDate,
            "lastModifiedTimestamp" to interactionLog.lastModifiedTimestamp,

            "carePackageContents" to interactionLog.carePackageContents,
            "carePackagesDistributed" to interactionLog.carePackagesDistributed,

            "helpRequestCount" to interactionLog.helpRequestCount,
            "helpRequestDocIds" to interactionLog.helpRequestDocIds,

            "listOfSupportsProvided" to interactionLog.listOfSupportsProvided,
            "numPeopleHelped" to interactionLog.numPeopleHelped,
            "numPeopleJoined" to interactionLog.numPeopleJoined,

            "outreachId" to interactionLog.outreachId,
            "userId" to interactionLog.userId, // optional, NOT mandatory
            "lastActionPerformed" to interactionLog.lastActionPerformed
        )

        // If ID exists → update; else → create new document
        if (interactionLog.id.isNotEmpty()) {
            db.collection(collectionName)
                .document(interactionLog.id)
                .set(data)
                .addOnSuccessListener {
                    Log.d("InteractionLogRepo", "Updated document ${interactionLog.id}")
                    onComplete(true, interactionLog.id)
                }
                .addOnFailureListener { e ->
                    Log.e("InteractionLogRepo", "Update failed", e)
                    onComplete(false, null)
                }
        } else {
            db.collection(collectionName)
                .add(data)
                .addOnSuccessListener { ref ->
                    Log.d("InteractionLogRepo", "Created document ${ref.id}")
                    onComplete(true, ref.id)
                }
                .addOnFailureListener { e ->
                    Log.e("InteractionLogRepo", "Create failed", e)
                    onComplete(false, null)
                }
        }
    }

    /**
     * Load single InteractionLog by DOCUMENT ID
     */
    override fun loadInteractionLogByDocumentId(
        documentId: String,
        onComplete: (InteractionLog?) -> Unit
    ) {
        db.collection(collectionName)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onComplete(null)
                    return@addOnSuccessListener
                }

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

                    onComplete(log)

                } catch (e: Exception) {
                    Log.e("InteractionLogRepo", "Parsing error", e)
                    onComplete(null)
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    /**
     * Load all PUBLIC interaction logs (no login required)
     */
    override fun loadPublicInteractionLogs(
        onComplete: (List<InteractionLog>) -> Unit
    ) {
        db.collection(collectionName)
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener { result ->
                val logs = mutableListOf<InteractionLog>()

                for (document in result) {
                    try {
                        logs.add(
                            InteractionLog(
                                id = document.id,
                                interactionDate = document.getTimestamp("interactionDate"),
                                firstName = document.getString("firstName") ?: "",
                                lastName = document.getString("lastName") ?: "",
                                city = document.getString("city") ?: "",
                                state = document.getString("state") ?: "",
                                isPublic = true
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("InteractionLogRepo", "Skipping bad document ${document.id}")
                    }
                }

                onComplete(logs.sortedByDescending { it.interactionDate })
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
}