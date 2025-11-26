package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class InteractionLogViewModel: ViewModel() {

    // -------------------------
    // Q1 – Person + Location
    // -------------------------
    val firstName = MutableLiveData("")
    val lastName = MutableLiveData("")
    val email = MutableLiveData("")
    val phoneNumber = MutableLiveData("")
    val location = MutableLiveData("")     // addr1
    val state = MutableLiveData("")
    val zipcode = MutableLiveData("")
    val interactionDate = MutableLiveData(Date())  // Contains both date+time

    // -------------------------
    // Q2 – Supports Provided
    // -------------------------
    val supportsProvided = MutableLiveData<MutableList<String>>(mutableListOf())

    fun toggleSupport(item: String, checked: Boolean) {
        val list = supportsProvided.value ?: mutableListOf()
        if (checked) list.add(item) else list.remove(item)
        supportsProvided.value = list
    }

    // -------------------------
    // Q3 – Counts
    // -------------------------
    val numPeopleHelped = MutableLiveData(0)
    val numPeopleJoined = MutableLiveData(0)

    val carePackagesDistributed = MutableLiveData(0)
    val carePackageContents = MutableLiveData("")

    // -------------------------
    // Q4 – Need further support?
    // -------------------------
    val needFollowup = MutableLiveData(false)
    val followupNotes = MutableLiveData("")

    // -------------------------
    // Build Firestore object
    // -------------------------
    fun buildInteractionLog(): Map<String, Any?> {
        return mapOf(
            "firstName" to firstName.value,
            "lastName" to lastName.value,
            "email" to email.value,
            "phoneNumber" to phoneNumber.value,
            "addr1" to location.value,
            "state" to state.value,
            "zipcode" to zipcode.value,
            "interactionDate" to interactionDate.value,

            "supportsProvided" to supportsProvided.value,

            "numPeopleHelped" to numPeopleHelped.value,
            "numPeopleJoined" to numPeopleJoined.value,

            "carePackagesDistributed" to carePackagesDistributed.value,
            "carePackageContents" to carePackageContents.value,

            "needFollowup" to needFollowup.value,
            "followupNotes" to followupNotes.value,

            "createdAt" to Date()
        )
    }

    // -------------------------
    // Submit to Firestore
    // -------------------------
    fun saveInteractionLog(onComplete: (Boolean) -> Unit) {
        val data = buildInteractionLog()
        val firestore = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            try {
                firestore.collection("InteractionLogDev")
                    .add(data)
                    .await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}
