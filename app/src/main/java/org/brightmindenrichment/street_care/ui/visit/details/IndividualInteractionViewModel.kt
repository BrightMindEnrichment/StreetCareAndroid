package org.brightmindenrichment.street_care.ui.visit.details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.WriteBatch
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import java.time.LocalDate
import java.time.LocalTime

class IndividualInteractionViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Backing property for observing from Fragment
    private val _interactions = MutableLiveData<List<IndividualInteraction>>()
    val interactions: LiveData<List<IndividualInteraction>> get() = _interactions

    private val _currentInteraction = MutableLiveData<IndividualInteraction>()
    val currentInteraction: LiveData<IndividualInteraction> get() = _currentInteraction

    private var interactionLogId: String = "test"

    private var listenerRegistration: ListenerRegistration? = null

    fun saveQ1(firstName: String, lastName: String?, locationLandmark: String?, state: String?, zip: String?, date: LocalDate?, time: LocalTime?) {

        // Initialize base instance if none exists
        val base = _currentInteraction.value ?: IndividualInteraction(
            helpRequestId = null,
            interactionLogDocId = interactionLogId,
            firstName = firstName
        )

        // Copy updated values
        val updated = base.copy(
            firstName = firstName.trim(),
            lastName = lastName?.trim().takeUnless { it.isNullOrBlank() },
            locationLandmark = locationLandmark?.trim().takeUnless { it.isNullOrBlank() },
            state = state?.trim().takeUnless { it.isNullOrBlank() },
            zip = zip?.trim().takeUnless { it.isNullOrBlank() },
            date = date,
            time = time,
            interactionLogFirstName = firstName.trim(),
        )

        saveInteractions(updated)

        // Update LiveData
        _currentInteraction.value = updated


    }

    fun saveQ2() {
        // get the data
    }

    fun saveQ3() {
        // get the data
    }

    fun saveQ4(date: Int, time: Int, listener: SaveFormListener) {
        // get the data

        // call firebase, get the result


        //based on result call success or failure
        listener.onSaveFormSuccess()
    }

    interface SaveFormListener {
        fun onSaveFormSuccess()
        fun onSaveFormFailure(message: String)
    }

    fun saveInteractions(interaction: IndividualInteraction) {

        val helpRequestPayload: Map<String, Any?> = mapOf(
            // --- Required contextual fields ---
            "interactionLogDocId" to "dummyInteractionLogId123",

            // --- Public / personal info ---
            "firstName" to "John",
            //"lastName" to "Doe",
            "locationLandmark" to "Near Central Park",

            // --- Timestamps (dummy ISO strings or Firestore timestamp string equivalents) ---
            "timestampOfInteraction" to "2025-01-10T14:32:00Z",
            "followUpTimestamp" to "2025-01-20T15:00:00Z",
            "lastModifiedTimestamp" to "2025-01-10T14:35:00Z",
            "completedTimestamp" to "2025-02-01T10:00:00Z",

            // --- Help categories ---
            "helpProvidedCategory" to listOf("Food", "Housing Assistance"),
            "furtherHelpCategory" to listOf("Job Search Support", "Legal Aid"),

            // --- Additional info ---
            "additionalDetails" to "Provided food and temporary housing referral.",
            "interactionLogFirstName" to "John",
            "isPublic" to true,

            // --- Status fields ---
            "status" to "Approved",               // could be Pending / Approved / Rejected etc.
            "lastActionPerformed" to "Edited",    // using a placeholder instead of null
            "isCompleted" to false
        )



//        db.collection(COLLECTION_HELP_REQUEST_DEV).document().set(helpRequestPayload).addOnSuccessListener {
//            Log.d(
//                "TAG",
//                "saveInteractions: "
//            )  }.addOnFailureListener { e->
//            Log.d("TAG", "failure: ${e.message}")
//        }
    }

    fun fetchInteractions(interactionId: String) {
        this.interactionLogId = interactionId
        listenerRegistration = db.collection(COLLECTION_INTERACTION_LOG_DEV).document(interactionId)
            .addSnapshotListener { document, e ->
                if (document == null || e != null) {
                    _interactions.value = emptyList()
                    return@addSnapshotListener
                }

                val docs = document.get(FIELD_HELP_REQUEST_DOC_IDS) as? List<String>

                val items = docs?.map { doc ->
                    IndividualInteraction(interactionLogDocId = interactionId, helpRequestId = "", firstName = "")
                }

                _interactions.value = items ?: emptyList()
            }
    }

    fun deleteInteraction(interaction: IndividualInteraction) {
        val batch: WriteBatch = db.batch()

        interaction.helpRequestId ?: return

        val interactionLogRef =
            db.collection(COLLECTION_INTERACTION_LOG_DEV).document(interactionLogId)
        batch.update(
            interactionLogRef, FIELD_HELP_REQUEST_DOC_IDS,
            FieldValue.arrayRemove(interaction.helpRequestId)
        )

        val helpRequestRef =
            db.collection(COLLECTION_HELP_REQUEST_DEV).document(interaction.helpRequestId)
        batch.delete(helpRequestRef)

        batch.commit()
            .addOnFailureListener { e ->
              Log.w("BME", "Error deleting document: $e")
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }

    companion object {
        private const val COLLECTION_INTERACTION_LOG_DEV = "InteractionLogDev"
        private const val COLLECTION_HELP_REQUEST_DEV = "HelpRequestDev"
        private const val FIELD_HELP_REQUEST_DOC_IDS = "helpRequestDocIds"


    }
}