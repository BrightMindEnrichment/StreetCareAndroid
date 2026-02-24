package org.brightmindenrichment.street_care.ui.visit.data

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime

data class IndividualInteraction(
    val helpRequestId: String? = null,                   // IndividualInteraction document ID
    val interactionLogDocId: String,                     // Related InteractionLog document ID

    val firstName: String?,                              // Public
    val lastName: String? = null,                        // Private
    val locationLandmark: String? = null,                // General location or landmark
    val state: String? = null,
    val zip: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,

    val timestampOfInteraction: Timestamp? = null,

    val helpProvidedCategory: List<String>? = null, // create enum
    val furtherHelpCategory: List<String>? = null, // create enum

    val followUpTimestamp: Timestamp? = null,
    val additionalDetails: String? = null,               // Notes or extra info

    val interactionLogFirstName: String? = null,         // Denormalized first name from log
    val isPublic: Boolean = true,
    val status: String = "Pending",

    val lastModifiedTimestamp: Timestamp? = null,
    val lastActionPerformed: String? = null,             // Eg: "Edited", "Re-edited"

    val completedTimestamp: Timestamp? = null,           // When request marked as completed
    val isCompleted: Boolean = false
)