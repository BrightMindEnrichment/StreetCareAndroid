package org.brightmindenrichment.street_care.ui.visit.data

import com.google.firebase.Timestamp

data class FirestoreHelpRequest(
    val interactionLogDocId: String = "",
    val firstName: String = "",
    val lastName: String? = null,
    val locationLandmark: String? = null,
    val timestampOfInteraction: Timestamp? = null,
    val helpProvidedCategory: List<String> = emptyList(),
    val furtherHelpCategory: List<String> = emptyList(),
    val followUpTimestamp: Timestamp? = null,
    val additionalDetails: String? = null,
    val interactionLogFirstName: String = "",
    val isPublic: Boolean = true,
    val status: String = "pending",
    val lastModifiedTimestamp: Timestamp = Timestamp.now(),
    val lastActionPerformed: String? = null,
    val completedTimestamp: Timestamp? = null,
    val isCompleted: Boolean = false
)
