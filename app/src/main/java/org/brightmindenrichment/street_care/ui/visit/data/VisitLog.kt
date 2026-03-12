package org.brightmindenrichment.street_care.ui.visit.data

import java.util.Calendar.getInstance
import java.util.Date
import android.os.Parcelable
import androidx.annotation.ColorRes
import kotlinx.parcelize.Parcelize
import org.brightmindenrichment.street_care.R
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
data class VisitLog(

    var id: String = "",
    var isPublic: Boolean = false,
//    var status: String = "pending",
    var location: String = "",
    var date: Date = getInstance().time,
    var lastEditedTime: Date = getInstance().time,
    var createdTime: Date = getInstance().time,
    var food_drink: Boolean = false,
    var clothes: Boolean = false,
    var hygiene: Boolean = false,
    var names: String = "NA",
    var peopleHelpedDescription: String = "",
    var description:String = "",
    var itemQtyDescription:String  ="",
    var wellness: Boolean = false,
    var other: Boolean = false,
    var otherDetail:String = "",
    var peopleCount: Long = 0L,
    var experience: Int = 0,
    var comments: String = "",
    var visitAgain: String ="",
    var outreach:Long = 0L,
    var numberOfHelpersComment: String ="",
    var share: Boolean=false,
    var timeZone: String? = TimeZone.getDefault().id,

    var isFlagged: Boolean?= false,
    var flaggedByUser: String?= null,
    // var comments: String = ""
    var whereVisit: String = "",
    var whenVisit: String? = null,
    var whenVisitTime: String? = null,
    var userId: String? = null,

    var helpTime: String? = "NA",
    var followupDate: Date?=null,
    var addnames: String = "NA",
    var address: String = "NA",

    var add_food_drink: Boolean?= false,
    var add_clothes: Boolean?= false,
    var add_hygine: Boolean?= false,
    var add_wellness: Boolean?= false,
    var add_medicalhelp: Boolean?= false,
    var add_socialWorker: Boolean?= false,
    var add_lawyerLegal: Boolean?= false,
    var add_other: Boolean?= false,
    var add_otherDetail:String = "",
    var add_volunteerDetail: String = "NA",
    var number_of_items: Long = 0L,

    var medicalhelp: Boolean = false,
    var socialWorker: Boolean = false,
    var lawyerLegal: Boolean = false,
    var whattogive: ArrayList<String> = arrayListOf(),
    var whatrequired: ArrayList<String> = arrayListOf(),
    //var locationmap: HashMap<String, String> = hashMapOf(),
    var typeofdevice: String = "Android",
    var outreachHours: Int = 0,
    var outreachMinutes: Int =0,
    var locationDescription: String = "",
    var visitedHours: Int =0,
    var visitedMinutes: Int=0,
    var whoJoined: Int=0,
    var stillNeedSupport: Int=0,
    var supportTypeNeeded: String ="",
    var peopleNeedFurtherHelpLocation: String ="",
    var peopleJoined: Int = 0,
    var futureNotes: String ="",
    //document ID for updating
    var documentId: String? = null,
    var status: Status = Status.PRIVATE,

    var peopleHelped: Int = 0,
    var whatGiven: String? = null,
    var whatGivenFurther: String? = null,
    var whoJoinedDescription: String? = null,
    var carePackagesGiven: Int? = 0,
    var carePackageNotes: String? = ""



    ) : Parcelable
enum class Status(@ColorRes val color: Int) {
    PRIVATE(R.color.black),
    PENDING(R.color.status_amber),
    PUBLISHED(R.color.status_green),
    REJECTED(R.color.status_red)

}