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
import org.brightmindenrichment.street_care.ui.visit.data.FirestoreInteractionLog
import org.brightmindenrichment.street_care.ui.visit.data.FirestoreHelpRequest
import org.brightmindenrichment.street_care.util.DataStoreManager
import org.brightmindenrichment.street_care.util.InteractionLogDraftSerializer
import org.brightmindenrichment.street_care.util.FirestoreCollections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.Instant
import java.time.ZonedDateTime
import java.time.LocalTime
import java.time.LocalDate
import java.time.ZoneId

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

    fun resetInteractionLog(onCleared: (() -> Unit)? = null) {
        _interactionLog.value = InteractionLog()
        resetInteractions()
        clearDraft(onCleared)
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
        val list = contents.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        _interactionLog.value = current.copy(carePackageContents = list)
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

    fun clearDraft(onCleared: (() -> Unit)? = null) {
        viewModelScope.launch {
            dataStoreManager.clearILDraft()
            onCleared?.invoke()
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
                    .collection(FirestoreCollections.INTERACTION_LOG)
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

    fun saveWithIIs(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val log = _interactionLog.value ?: run { onComplete(false); return@launch }
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val db = FirebaseFirestore.getInstance()

            val ilRef = db.collection(FirestoreCollections.INTERACTION_LOG).document()
            val iis = log.individualInteractions
            val hrRefs = iis.map { db.collection(FirestoreCollections.HELP_REQUEST).document() }
            val hrIds = hrRefs.map { it.id }

            val interactionDate = log.startTimestamp?.toDate()?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                    .format(it)
            } ?: ""

            val ilDoc = FirestoreInteractionLog(
                userId = uid,
                firstName = log.firstName,
                lastName = log.lastName,
                email = log.email,
                phoneNumber = log.phoneNumber,
                interactionDate = interactionDate,
                startTimestamp = log.startTimestamp,
                endTimestamp = log.endTimestamp,
                listOfSupportsProvided = log.listOfSupportsProvided,
                numPeopleHelped = log.numPeopleHelped,
                numPeopleJoined = log.numPeopleJoined,
                carePackagesDistributed = log.carePackagesDistributed,
                carePackageContents = log.carePackageContents,
                addr1 = log.addr1, addr2 = log.addr2,
                city = log.city, state = log.state, zipcode = log.zipcode, country = log.country,
                helpRequestCount = hrIds.size,
                helpRequestDocIds = hrIds
            )

            val batch = db.batch()
            batch.set(ilRef, ilDoc)

            iis.forEachIndexed { i, ii ->
                // Parse interaction time with timezone context
                val timestampOfInteraction = ii.time?.let { t ->
                    runCatching {
                        // Try to parse as ZonedDateTime (includes timezone)
                        val zdt = ZonedDateTime.parse(t)
                        Timestamp(zdt.toInstant().epochSecond, zdt.toInstant().nano)
                    }.getOrNull() ?:
                    // Fallback: Try parsing as simple time string without timezone
                    runCatching {
                        Instant.parse(t).let { Timestamp(it.epochSecond, it.nano) }
                    }.getOrNull()
                } ?: log.startTimestamp

                // Parse follow-up time with timezone context
                val followUpTimestamp = ii.followUpTime?.let { ft ->
                    runCatching {
                        // Try to parse as ZonedDateTime (includes timezone)
                        val zdt = ZonedDateTime.parse(ft)
                        Timestamp(zdt.toInstant().epochSecond, zdt.toInstant().nano)
                    }.getOrNull() ?:
                    // Fallback: Try parsing as simple time string without timezone
                    runCatching {
                        Instant.parse(ft).let { Timestamp(it.epochSecond, it.nano) }
                    }.getOrNull()
                }

                val hrDoc = FirestoreHelpRequest(
                    interactionLogDocId = ilRef.id,
                    firstName = ii.firstName,
                    lastName = ii.lastName,
                    locationLandmark = ii.locationLandmark,
                    timestampOfInteraction = timestampOfInteraction,
                    helpProvidedCategory = ii.supportsProvided,
                    furtherHelpCategory = ii.furtherHelpNeeded,
                    followUpTimestamp = followUpTimestamp,
                    additionalDetails = ii.additionalDetails,
                    interactionLogFirstName = log.firstName
                )
                batch.set(hrRefs[i], hrDoc)
            }

            try {
                batch.commit().await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
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
        val list = notes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        _interactionLog.value = current.copy(
            carePackagesDistributed = count,
            carePackageContents = list
        )
    }

    fun updateQ7Answer(answer: Boolean?) {
        val current = interactionLog.value ?: InteractionLog()
        _interactionLog.value = current.copy(
            wantsToProvideDetails = answer
        )
    }
}
