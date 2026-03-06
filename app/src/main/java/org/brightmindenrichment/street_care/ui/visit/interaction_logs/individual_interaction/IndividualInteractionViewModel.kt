package org.brightmindenrichment.street_care.ui.visit.interaction_logs.individual_interaction

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    private val _currentInteraction = MutableLiveData<IndividualInteraction>(IndividualInteraction())
    val currentInteraction: LiveData<IndividualInteraction> get() = _currentInteraction

    private val _committedInteractions = MutableLiveData<List<IndividualInteraction>>(emptyList())
    val committedInteractions: LiveData<List<IndividualInteraction>> get() = _committedInteractions

    private var interactionLogId: String? = null

    private var listenerRegistration: ListenerRegistration? = null

    /** Non-null while an existing committed interaction is being edited; its index in the list. */
    var editingIndex: Int? = null
        private set

    /** Display name shown in the Q1-Q4 header during edit mode; null when creating a new II. */
    private var _editingHeaderText: String? = null

    /** Returns the header text to display during edit mode, or null if creating a new II. */
    fun editingHeaderText(): String? = _editingHeaderText

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
            date = date?.toString(),
            time = time?.toString(),
            interactionLogFirstName = firstName.trim(),
        )

        saveInteractions(updated)

        // Update LiveData
        _currentInteraction.value = updated


    }

    fun saveQ2(supportsProvided: List<String>) {
        val base = _currentInteraction.value ?: IndividualInteraction()
        _currentInteraction.value = base.copy(supportsProvided = supportsProvided)
    }

    fun saveQ3(furtherHelpNeeded: List<String>) {
        val base = _currentInteraction.value ?: IndividualInteraction()
        _currentInteraction.value = base.copy(furtherHelpNeeded = furtherHelpNeeded)
    }

    fun saveQ4(followUpDate: String?, followUpTime: String?, notes: String?) {
        val base = _currentInteraction.value ?: IndividualInteraction()
        val completed = base.copy(
            followUpDate = followUpDate,
            followUpTime = followUpTime,
            additionalDetails = notes?.takeUnless { it.isBlank() }
        )

        val current = _committedInteractions.value?.toMutableList() ?: mutableListOf()
        val idx = editingIndex
        if (idx != null) {
            current[idx] = completed
            editingIndex = null
            _editingHeaderText = null
        } else {
            current.add(completed)
        }
        _committedInteractions.value = current
        _currentInteraction.value = IndividualInteraction()
    }

    /** Loads an existing committed interaction into the current form for editing. */
    fun startEditing(index: Int) {
        editingIndex = index
        val interaction = _committedInteractions.value?.getOrNull(index) ?: IndividualInteraction()
        _currentInteraction.value = interaction
        _editingHeaderText = buildDisplayName(interaction, index)
    }

    /** Resets all in-session II state. Called when the user discards the IL/II workflow. */
    fun reset() {
        _committedInteractions.value = emptyList()
        _currentInteraction.value = IndividualInteraction()
        editingIndex = null
        _editingHeaderText = null
    }

    private fun buildDisplayName(interaction: IndividualInteraction, index: Int): String {
        if (interaction.firstName.isBlank()) return "IndividualInteraction${index + 1}"
        val lastInitial = interaction.lastName?.firstOrNull()?.let { "${it}." }.orEmpty()
        return "Interaction with ${interaction.firstName}${if (lastInitial.isNotEmpty()) " $lastInitial" else ""}"
    }

    /** Removes a committed interaction from the local list (no Firestore call needed before submission). */
    fun deleteCommittedInteraction(item: IndividualInteraction) {
        val current = _committedInteractions.value?.toMutableList() ?: return
        current.remove(item)
        _committedInteractions.value = current
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
                    IndividualInteraction(
                        interactionLogDocId = interactionId,
                        helpRequestId = "",
                        firstName = ""
                    )
                }

                _interactions.value = items ?: emptyList()
            }
    }

    fun deleteInteraction(interaction: IndividualInteraction) {
        val batch: WriteBatch = db.batch()

        interaction.helpRequestId ?: return

        if (interactionLogId != null){
            val interactionLogRef =
                db.collection(COLLECTION_INTERACTION_LOG_DEV).document(interactionLogId!!)
            batch.update(
                interactionLogRef, FIELD_HELP_REQUEST_DOC_IDS,
                FieldValue.arrayRemove(interaction.helpRequestId)
            )


            val helpRequestRef =
                db.collection(COLLECTION_HELP_REQUEST_DEV).document(interaction.helpRequestId!!)
            batch.delete(helpRequestRef)


            batch.commit()
                .addOnFailureListener { e ->
                    Log.w("BME", "Error deleting document: $e")
                }
        }
        Log.w("BME", "Error InteractionLog Doc Id is missing.")
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