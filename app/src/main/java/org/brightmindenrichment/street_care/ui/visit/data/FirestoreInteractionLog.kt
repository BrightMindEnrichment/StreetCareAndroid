package org.brightmindenrichment.street_care.ui.visit.data

import com.google.firebase.Timestamp

data class FirestoreInteractionLog(
    val userId: String = "",
    val outreachId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val interactionDate: String = "",          // "YYYY-MM-DD" derived from startTimestamp
    val startTimestamp: Timestamp? = null,
    val endTimestamp: Timestamp? = null,
    val listOfSupportsProvided: List<String> = emptyList(),
    val numPeopleHelped: Int = 0,
    val carePackagesDistributed: Int = 0,
    val carePackageContents: List<String> = emptyList(),
    val numPeopleJoined: Int = 0,
    val addr1: String = "",
    val addr2: String = "",
    val city: String = "",
    val state: String = "",
    val zipcode: String = "",
    val country: String = "USA",
    val helpRequestCount: Int = 0,
    val helpRequestDocIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
    val status: String = "pending",
    val lastModifiedTimestamp: Timestamp = Timestamp.now(),
    val lastActionPerformed: String = "submit",
    val isFlagged: Boolean = false,
    val flaggedByUser: String = ""
)
