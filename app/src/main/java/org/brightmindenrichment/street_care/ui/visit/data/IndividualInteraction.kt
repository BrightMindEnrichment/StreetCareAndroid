package org.brightmindenrichment.street_care.ui.visit.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class IndividualInteraction(

    // --- Firebase metadata ---
    var helpRequestId: String? = null,
    var interactionLogDocId: String? = null,

    // --- Person Info ---
    var firstName: String = "",
    var lastName: String? = null,
    var state: String? = null,
    var zip: String? = null,
    var locationLandmark: String? = null,

    // --- Interaction timing ---
    var date: String? = null,
    var time: String? = null,

    // --- Help Provided ---
    var supportsProvided: List<String> = emptyList(),

    // --- Further Help ---
    var furtherHelpNeeded: List<String> = emptyList(),

    // --- Additional ---
    var additionalDetails: String? = null,

    var interactionLogFirstName: String? = null


) : Parcelable
