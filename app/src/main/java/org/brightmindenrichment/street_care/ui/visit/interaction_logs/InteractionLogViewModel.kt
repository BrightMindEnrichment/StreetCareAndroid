package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import org.brightmindenrichment.street_care.ui.visit.data.IndividualInteraction
import java.util.Date

class InteractionLogViewModel : ViewModel() {

    // Interaction counter
    private val _interactionIndex = MutableLiveData(1)
    val interactionIndex: LiveData<Int> = _interactionIndex

    private val _interactionLog = MutableLiveData(InteractionLog())
    val interactionLog: LiveData<InteractionLog> = _interactionLog

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

    fun resetInteractionLog(forceReset: Boolean = true) {
        if (forceReset) {
            _interactionLog.value = InteractionLog()
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
