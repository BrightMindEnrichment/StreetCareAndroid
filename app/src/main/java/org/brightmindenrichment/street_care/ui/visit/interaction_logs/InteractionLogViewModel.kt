package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import org.brightmindenrichment.street_care.util.DataStoreManager
import org.brightmindenrichment.street_care.util.InteractionLogDraftSerializer
import java.util.Date

class InteractionLogViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    // Interaction counter
    private val _interactionIndex = MutableLiveData(1)
    val interactionIndex: LiveData<Int> = _interactionIndex

    private val _interactionLog = MutableLiveData(InteractionLog())
    val interactionLog: LiveData<InteractionLog> = _interactionLog

    /** Set to true when a draft is pre-loaded before navigating to Q1 so Q1 can skip the resume dialog. */
    var draftPreLoaded = false

    fun nextInteraction() {
        val cur = _interactionIndex.value ?: 1
        _interactionIndex.value = cur + 1
    }

    fun resetInteractions() {
        _interactionIndex.value = 1
    }

    fun resetInteractionLog() {
        _interactionLog.value = InteractionLog()
        resetInteractions()
        clearDraft()
    }

    // =========================================================
    // -------------------- Q1 (Session Time) ------------------
    // =========================================================

    fun updateStartDate(date: Date) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(
            startTimestamp = Timestamp(date)
        )
    }

    fun updateEndDate(date: Date) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(
            endTimestamp = Timestamp(date)
        )
    }

    fun updateTimezone(timezone: String) {
        val current = interactionLog.value ?: return
        _interactionLog.value = current.copy(timezone = timezone)
    }
    // =========================================================
    // -------------------- Q2 (User) ----------------------
    // =========================================================
    fun updateFirstName(name: String) {
        val current = interactionLog.value ?: return
        _interactionLog.value = current.copy(firstName = name)
    }

    fun updateLastName(name: String) {
        val current = interactionLog.value ?: return
        _interactionLog.value = current.copy(lastName = name)
    }

    fun updateEmail(email: String) {
        val current = interactionLog.value ?: return
        _interactionLog.value = current.copy(email = email)
    }

    fun updatePhone(phone: String) {
        val current = interactionLog.value ?: return
        _interactionLog.value = current.copy(phoneNumber = phone)
    }



    // =========================================================
    // -------------------- Q3 (Location) ----------------------
    // =========================================================

    fun updateAddress(addr1: String) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(addr1 = addr1)
    }

    fun updateCity(city: String) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(city = city)
    }

    fun updateState(state: String) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(state = state)
    }

    fun updateZipcode(zip: String) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(zipcode = zip)
    }

    // =========================================================
    // -------------------- Q4 (Session Supports) --------------
    // =========================================================

    fun toggleSupport(item: String, checked: Boolean) {
        val current = interactionLog.value!!
        val updated = current.listOfSupportsProvided.toMutableList()

        if (checked) {
            if (!updated.contains(item)) updated.add(item)
        } else {
            updated.remove(item)
        }

        _interactionLog.value = current.copy(
            listOfSupportsProvided = updated
        )
    }

    // =========================================================
    // -------------------- Q5 (Counts) ------------------------
    // =========================================================

    fun updatePeopleHelped(count: Int) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(numPeopleHelped = count)
    }

    fun updatePeopleJoined(count: Int) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(numPeopleJoined = count)
    }

    // =========================================================
    // -------------------- Q6 (Care Packages) -----------------
    // =========================================================

    fun updateCarePackages(count: Int) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(carePackagesDistributed = count)
    }

    fun updateCarePackageContents(contents: String) {
        val current = interactionLog.value!!
        _interactionLog.value = current.copy(carePackageContents = contents)
    }

    // =========================================================
    // -------------------- NESTED INDIVIDUAL LOGS ------------
    // =========================================================

    fun addIndividualInteraction(interaction: IndividualInteraction) {
        val current = interactionLog.value!!
        val updatedList = current.individualInteractions.toMutableList()

        updatedList.add(interaction)

        _interactionLog.value = current.copy(
            individualInteractions = updatedList
        )
    }

    fun removeIndividualInteraction(index: Int) {
        val current = interactionLog.value!!
        val updatedList = current.individualInteractions.toMutableList()

        if (index in updatedList.indices) {
            updatedList.removeAt(index)
        }

        _interactionLog.value = current.copy(
            individualInteractions = updatedList
        )
    }

    fun replaceIndividualInteraction(index: Int, interaction: IndividualInteraction) {
        val current = interactionLog.value ?: return
        val updatedList = current.individualInteractions.toMutableList()
        if (index in updatedList.indices) updatedList[index] = interaction
        _interactionLog.value = current.copy(individualInteractions = updatedList)
    }

    // =========================================================
    // -------------------- DRAFT PERSISTENCE ------------------
    // =========================================================

    fun saveDraft() {
        viewModelScope.launch {
            val json = InteractionLogDraftSerializer.serialize(_interactionLog.value ?: return@launch)
            dataStoreManager.saveILDraft(json)
        }
    }

    fun clearDraft() {
        viewModelScope.launch {
            dataStoreManager.clearILDraft()
        }
    }

    /** Returns true if a persisted draft exists in DataStore. */
    suspend fun hasDraft(): Boolean = dataStoreManager.getILDraft().map { it != null }.first()

    /**
     * Loads the DataStore draft into [_interactionLog]. Calls [onLoaded] with true if a valid
     * draft was found and restored, false otherwise.
     */
    fun loadDraft(onLoaded: (Boolean) -> Unit) {
        viewModelScope.launch {
            val json = dataStoreManager.getILDraft().first()
            if (json != null) {
                val log = InteractionLogDraftSerializer.deserialize(json)
                if (log != null) {
                    _interactionLog.value = log
                    draftPreLoaded = true
                    onLoaded(true)
                    return@launch
                }
            }
            onLoaded(false)
        }
    }

    // =========================================================
    // -------------------- SAVE TO FIRESTORE ------------------
    // =========================================================

    fun saveInteractionLog(onComplete: (Boolean) -> Unit) {

        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        val log = interactionLog.value!!.copy(
            userId = userId,
            lastModifiedTimestamp = Timestamp(Date())
        )

        viewModelScope.launch {
            try {
                val docRef = firestore
                    .collection("InteractionLogDev")
                    .add(log)
                    .await()

                android.util.Log.d("FIRESTORE", "Saved ID: ${docRef.id}")
                onComplete(true)

            } catch (e: Exception) {
                android.util.Log.e("FIRESTORE", "Save failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun setSupportsProvided(supports: List<String>) {
        val current = _interactionLog.value ?: return
        _interactionLog.value = current.copy(
            listOfSupportsProvided = supports
        )
    }

    fun updateCounts(helped: Int, joined: Int) {
        val current = _interactionLog.value ?: return
        _interactionLog.value = current.copy(
            numPeopleHelped = helped,
            numPeopleJoined = joined
        )
    }

    fun updateCarePackage(count: Int, notes: String) {
        val current = _interactionLog.value ?: return
        _interactionLog.value = current.copy(
            carePackagesDistributed = count,
            carePackageContents = notes
        )
    }

    fun updateQ7Answer(answer: Boolean?) {
        val current = interactionLog.value ?: InteractionLog()
        _interactionLog.value = current.copy(
            wantsToProvideDetails = answer
        )
    }
}
