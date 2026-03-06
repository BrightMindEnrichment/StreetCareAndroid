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

    var wantsToProvideDetails: Boolean? = null

) : Parcelable
