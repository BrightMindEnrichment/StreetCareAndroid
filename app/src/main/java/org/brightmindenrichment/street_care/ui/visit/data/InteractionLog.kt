package org.brightmindenrichment.street_care.ui.visit.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class InteractionLog(

    // ================= META =================
    var id: String = "",
    var userId: String = "",
    var outreachId: String = "",
    var status: String = "Pending",
    var isPublic: Boolean = false,
    var lastActionPerformed: String? = null,

    // ================= Q2 (USER INFO) =================
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var phoneNumber: String = "",

    // ================= DATE / TIME =================
    var startTimestamp: Timestamp? = null,
    var endTimestamp: Timestamp? = null,
    var timezone: String = "",

    // ================= LOCATION =================
    var addr1: String = "",
    var addr2: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = "USA",
    var zipcode: String = "",
    var landmark: String = "",

    // ================= SESSION STATS =================
    var carePackagesDistributed: Int = 0,
    var carePackageContents: List<String> = emptyList(),
    var numPeopleHelped: Int = 0,
    var numPeopleJoined: Int = 0,

    // ================= SESSION SUPPORT SUMMARY =================
    var listOfSupportsProvided: List<String> = emptyList(),

    val helpRequestCount: Int = 0,
    val helpRequestDocIds: List<String> = emptyList(),


    // ================= NESTED INDIVIDUAL INTERACTIONS =================
    var individualInteractions: List<IndividualInteraction> = emptyList(),

    // ================= SYSTEM =================
    var createdAt: Timestamp? = null,
    var lastModifiedTimestamp: Timestamp? = null,

    var wantsToProvideDetails: Boolean? = null,

    // ================= AUTOFILL TRACKING =================
    // Q2 and Q3 autofill from Firebase Auth and GPS; only count as filled if user edited them
    var q2WasUserEdited: Boolean = false,
    var q3WasUserEdited: Boolean = false

) : Parcelable {
    /**
     * True when no user-meaningful data has been entered beyond auto-filled defaults.
     * Q2 & Q3: only count as filled if user actually edited them (not just autofilled)
     * Excludes Q1 (auto-filled date/time) from the check.
     */
    val isPristine: Boolean get() =
        !q2WasUserEdited &&                          // Q2: autofilled but not edited
        !q3WasUserEdited &&                          // Q3: autofilled but not edited
        listOfSupportsProvided.isEmpty() &&          // Q4: no support selected
        numPeopleHelped <= 1 && numPeopleJoined == 0 && // Q5: at defaults (1 is fragment default)
        carePackagesDistributed == 0 && carePackageContents.isEmpty() && // Q6
        wantsToProvideDetails == null &&             // Q7: skipped/unanswered
        individualInteractions.isEmpty()             // IIs: none entered
}
