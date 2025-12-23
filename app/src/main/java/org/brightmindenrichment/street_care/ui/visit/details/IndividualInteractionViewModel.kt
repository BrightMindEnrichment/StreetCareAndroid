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

class IndividualInteractionViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Backing property for observing from Fragment
    private val _interactions = MutableLiveData<List<IndividualInteraction>>()
    val interactions: LiveData<List<IndividualInteraction>> get() = _interactions

    private val _currentInteraction = MutableLiveData<IndividualInteraction>()
    val currentInteraction: LiveData<IndividualInteraction> get() = _currentInteraction

    private lateinit var interactionLogId: String

    private var listenerRegistration: ListenerRegistration? = null

    fun saveQ1(firstName: String, lastName: String?, locationLandmark: String?, timestamp: Timestamp?) {

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
            timestampOfInteraction = timestamp,
            interactionLogFirstName = firstName.trim(),
            lastModifiedTimestamp = Timestamp.now()
        )

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