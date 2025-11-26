package org.brightmindenrichment.street_care.ui.visit.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class InteractionLog(

    var id: String = "",
    var addr1: String = "",
    var addr2: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = "",
    var zipcode: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var isPublic: Boolean = false,
    var status: String = "Pending",

    var startTimestamp: Timestamp? = null,
    var endTimestamp: Timestamp? = null,
    var interactionDate: Timestamp? = null,
    var lastModifiedTimestamp: Timestamp? = null,

    var carePackageContents: String = "",
    var carePackagesDistributed: Int = 0,
    var helpRequestCount: Int = 0,
    var helpRequestDocIds: List<String> = emptyList(),
    var listOfSupportsProvided: List<String> = emptyList(),
    var numPeopleHelped: Int = 0,
    var numPeopleJoined: Int = 0,

    var outreachId: String = "",
    var userId: String = "",
    var lastActionPerformed: String? = null
) : Parcelable


