package org.brightmindenrichment.street_care.ui.visit.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class IndividualInteraction(

    // ========== PERSON INFO ==========
    var firstName: String = "",
    var lastName: String = "",
    var state: String = "",
    var zipcode: String = "",
    var locationOrLandmark: String = "",

    // ========== HELP PROVIDED ==========
    var supportsProvided: List<String> = emptyList(),

    // ========== FURTHER HELP NEEDED ==========
    var furtherHelpNeeded: List<String> = emptyList(),

    // ========== FOLLOW UP ==========
    var followUpDate: Timestamp? = null,
    var additionalDetails: String = ""

) : Parcelable
