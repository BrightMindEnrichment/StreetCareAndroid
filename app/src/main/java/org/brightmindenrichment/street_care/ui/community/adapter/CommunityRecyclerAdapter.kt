package org.brightmindenrichment.street_care.ui.community.adapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.StickyHeaderInterface
import org.brightmindenrichment.street_care.ui.community.data.CommunityData
import org.brightmindenrichment.street_care.ui.community.data.Event
import org.brightmindenrichment.street_care.ui.community.data.EventDataAdapter
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.ui.user.getUserType
import org.brightmindenrichment.street_care.ui.user.verificationMark
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.refreshNumOfInterest
import org.brightmindenrichment.street_care.util.Extensions.Companion.replaceRSVPButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setRSVPButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setVerifiedAndRegistered
import org.brightmindenrichment.street_care.util.Share
import org.brightmindenrichment.street_care.util.showLoginDialog


class CommunityRecyclerAdapter(
    private val controller: EventDataAdapter,
    private val communityPageName: CommunityPageName,
    //private val isPastEvents: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    // real-time synchronization from webapp
    private var currentBottomSheetEvent: Event? = null

    fun getCurrentBottomSheetEvent(): Event? {
        return currentBottomSheetEvent
    }

    fun setCurrentBottomSheetEvent(event: Event) {
        currentBottomSheetEvent = event
    }

    interface ClickListener {
        fun onClick(event: Event, position: Int) {}
    }

    private var clickListener: ClickListener? = null

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    private lateinit var refreshBottomSheet: (Event) -> Unit
    fun setRefreshBottomSheet(
        refreshBottomSheet: (Event) -> Unit
    ) {
        this.refreshBottomSheet = refreshBottomSheet
    }


    fun getItemPosition(eventId: String?): Int? {
        for (pos in 0 until controller.size) {
            val communityData = controller.getEventAtPosition(pos)
            if (communityData?.event?.eventId == eventId) return pos
        }
        return null
    }

    fun getItemAtPosition(pos: Int): CommunityData? {
        return controller.getEventAtPosition(pos)
    }

    fun clickItem(event: Event, pos: Int) {
        clickListener!!.onClick(event, pos)
    }

    private var currentHeaderText: String? = null

    fun getCurrentHeaderText(): String? {
        return currentHeaderText
    }


    inner class EventViewHolder(private val communityItemView: View) :
        RecyclerView.ViewHolder(communityItemView) {

        private val textViewTitle: TextView =
            communityItemView.findViewById<TextView>(R.id.textViewCommunityTitle)
        private val textViewCommunityLocation: TextView =
            communityItemView.findViewById<TextView>(R.id.textViewCommunityLocation)
        private val textViewCommunityTime: TextView =
            communityItemView.findViewById<TextView>(R.id.textViewCommunityTime)
        private val textViewDate: TextView =
            communityItemView.findViewById<TextView>(R.id.textViewDate)
        private val textViewDay: TextView =
            communityItemView.findViewById<TextView>(R.id.textViewDay)
        private val buttonRSVP: AppCompatButton =
            communityItemView.findViewById<AppCompatButton>(R.id.btnRSVP)
        private val relativeLayoutImage: RelativeLayout =
            communityItemView.findViewById<RelativeLayout>(R.id.relativeLayoutImage)
        private val textInterested: TextView =
            communityItemView.findViewById<TextView>(R.id.textInterested)
        private val cardViewEvent: MaterialCardView =
            communityItemView.findViewById<MaterialCardView>(R.id.cardViewEvent)
        private val linearLayoutVerified: LinearLayout =
            communityItemView.findViewById<LinearLayout>(R.id.llVerifiedAndRegistered)
        private val textHelpType: TextView =
            communityItemView.findViewById<TextView>(R.id.tvHelpType)
        private val linearLayoutVerifiedAndIcon: LinearLayout =
            communityItemView.findViewById(R.id.llVerifiedAndIcon)
        private val textViewRegistered: TextView = communityItemView.findViewById(R.id.tvRegistered)
        private val textViewEventStatus: TextView =
            communityItemView.findViewById(R.id.tvEventStatus)
        private val ivVerificationMark: ImageView =
            communityItemView.findViewById(R.id.ivVerificationMark)
        private val ivFlag: ImageView = communityItemView.findViewById<ImageView>(R.id.ivFlag)

        private val btnLike: ImageButton = communityItemView.findViewById(R.id.btnLike)
        private val textViewlikeCount: TextView = communityItemView.findViewById(R.id.tvLikeCount)

        private val btnShare: ImageButton = communityItemView.findViewById(R.id.btnShare)

        init {
            cardViewEvent.setOnClickListener {
                val position = bindingAdapterPosition
                clickListener?.let {
                    if (position != RecyclerView.NO_POSITION) {
                        val communityData = controller.getEventAtPosition(position)
                        communityData?.event?.let { event ->
                            clickListener!!.onClick(event, position)
                        }
                    }
                }
            }

            buttonRSVP.setOnClickListener {

                val position = bindingAdapterPosition
                val communityData = controller.getEventAtPosition(position)
                if (communityData != null) {
                    communityData.event?.let { event ->
                        //val isFavorite = event.liked
                        event.signedUp = !event.signedUp

//                        if(isFavorite){
//                            imageViewUnFav.setImageResource(R.drawable.ic_unfav)
//                            //event.interest = event.interest?.minus(1)
//
//                        }
//                        else{
//                            imageViewUnFav.setImageResource(R.drawable.ic_favorite)
//                            //event.interest = event.interest?.plus(1)
//                        }

                        // refreshNumOfInterestAndProfileImg(event)
                        // notifyDataSetChanged()
                        controller.setLikedEvent(event) {
                            refreshBottomSheet(it)
                            notifyItemChanged(position)
                            Log.d(
                                "Liked Event Firebase Update",
                                "Liked Event Firebase Update Success"
                            )
                        }
                    }

                }
            }
            btnLike.setOnClickListener {
                if (Firebase.auth.currentUser == null) {
                    // User is not logged in, show the dialog and stop further execution
                    showLoginDialog(itemView.context)
                    return@setOnClickListener
                }
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val communityData =
                    controller.getEventAtPosition(position) ?: return@setOnClickListener
                val event = communityData.event ?: return@setOnClickListener

                // Toggle like state
                event.likedByMe = !event.likedByMe
                event.likeCount += if (event.likedByMe) 1 else -1
                notifyItemChanged(position)
                refreshBottomSheet(event)

                // Call the controller to update Firestore and handle the result
                controller.setLikedOutreachEvent(event.eventId, event.likedByMe) { success ->
                    if (success) {
                        Log.d("LikeUpdate", "Firestore updated successfully.")
                    } else {
                        Log.w("LikeUpdate", "Firestore update failed. Reverting UI.")

                        // On failure, revert the local data and UI state
                        event.likedByMe = !event.likedByMe
                        event.likeCount += if (event.likedByMe) 1 else -1
                        notifyItemChanged(position)
                        refreshBottomSheet(event)
                        Toast.makeText(
                            itemView.context,
                            "Action failed. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }

            // SHARE
            btnShare.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val communityData =
                    controller.getEventAtPosition(position) ?: return@setOnClickListener
                val event = communityData.event ?: return@setOnClickListener

                Share.shareEvent(itemView.context, event.eventId)
            }

        }

        fun bind(pos: Int, context: Context) {
            // Bind data to views
            // ...
            val communityData = controller.getEventAtPosition(pos)
            communityData?.event?.let { event ->
                textViewTitle.text = event.title
                textViewCommunityLocation.text =
                    if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
                        "${event.city}, ${event.state}"
                    } else {
                        event.location.orEmpty()
                    }
                textViewCommunityTime.text = event.time.orEmpty()
                textViewDate.text = event.date.orEmpty()
                textViewDay.text = event.day.orEmpty()

                Log.d("query", "event.interest: ${event.interest}")
                Log.d("query", "event.itemList.size: ${event.itemList.size}")

                val approved = event.approved!!
                val isSignedUp = event.signedUp
                // numOfInterest = event.interest?.minus(event.itemList.size)
                when (communityPageName) {
                    CommunityPageName.PAST_EVENTS -> {
                        if (!event.signedUp) {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.completed,
                            )
                            /*
                            buttonRSVP.setText(R.string.completed)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        } else {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.attended,
                            )
                            /*
                            buttonRSVP.setText(R.string.attended)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        }
                    }

                    CommunityPageName.UPCOMING_EVENTS, CommunityPageName.HELP_REQUESTS -> {
                        if (isSignedUp) {
                            setRSVPButton(
                                buttonRSVP = buttonRSVP,
                                textId = R.string.deregister,
                                textColor = Color.BLACK,
                                backgroundColor = null
                            )
                            /*
                            buttonRSVP.setText(R.string.unregister)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                             */
                        } else {
                            if (event.totalSlots == null || event.totalSlots == -1 || (event.participants?.size
                                    ?: 0) < event.totalSlots!!
                            ) {
                                setRSVPButton(
                                    buttonRSVP = buttonRSVP,
                                    textId = R.string.rsvp,
                                    textColor = Color.parseColor("#FEED00"),
                                    backgroundColor = Color.parseColor("#002925")
                                )
                                /*
                                val color = Color.parseColor("#FEED00")
                                buttonRSVP.setText(R.string.rsvp)
                                buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                                    Color.parseColor("#002925")
                                )
                                buttonRSVP.setTextColor(color)
                                 */
                            } else {
                                replaceRSVPButton(
                                    buttonRSVP = buttonRSVP,
                                    tvEventStatus = textViewEventStatus,
                                    textId = R.string.event_full,
                                )
                                /*
                                buttonRSVP.setText(R.string.event_full)
                                buttonRSVP.backgroundTintList = null
                                buttonRSVP.setTextColor(Color.BLACK)
                                buttonRSVP.isEnabled = false
                                 */
                            }
                        }
                    }
                }
                /*
                if(!isPastEvents) {
                    if (isSignedUp) {
                        setRSVPButton(
                            buttonRSVP = buttonRSVP,
                            textId = R.string.deregister,
                            textColor = Color.BLACK,
                            backgroundColor = null
                        )
                        /*
                        buttonRSVP.setText(R.string.unregister)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                         */
                    } else {
                        if(event.totalSlots == null || event.totalSlots == -1 || (event.participants?.size ?: 0) < event.totalSlots!!) {
                            setRSVPButton(
                                buttonRSVP = buttonRSVP,
                                textId = R.string.rsvp,
                                textColor = Color.parseColor("#FEED00"),
                                backgroundColor = Color.parseColor("#002925")
                            )
                            /*
                            val color = Color.parseColor("#FEED00")
                            buttonRSVP.setText(R.string.rsvp)
                            buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                                Color.parseColor("#002925")
                            )
                            buttonRSVP.setTextColor(color)
                             */
                        }
                        else {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.event_full,
                            )
                            /*
                            buttonRSVP.setText(R.string.event_full)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        }
                    }
                }
                else {
                    if(!event.signedUp) {
                        replaceRSVPButton(
                            buttonRSVP = buttonRSVP,
                            tvEventStatus = textViewEventStatus,
                            textId = R.string.completed,
                        )
                        /*
                        buttonRSVP.setText(R.string.completed)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                        buttonRSVP.isEnabled = false
                         */
                    }
                    else {
                        replaceRSVPButton(
                            buttonRSVP = buttonRSVP,
                            tvEventStatus = textViewEventStatus,
                            textId = R.string.attended,
                        )
                        /*
                        buttonRSVP.setText(R.string.attended)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                        buttonRSVP.isEnabled = false
                         */
                    }
                }
                 */

                Log.d("syncWebApp", "approved: $approved")
                val isPastEvents = communityPageName == CommunityPageName.PAST_EVENTS
                setVerifiedAndRegistered(
                    context = null,
                    isVerified = approved,
                    isRegistered = isSignedUp,
                    isEventCard = true,
                    linearLayoutVerified = linearLayoutVerified,
                    linearLayoutVerifiedAndIcon = linearLayoutVerifiedAndIcon,
                    textViewRegistered = textViewRegistered,
                    cardViewEvent = cardViewEvent,
                    bottomSheetView = null,
                    isPastEvents = isPastEvents,
                )


                // reflect like state on each bind
                val liked = event.likedByMe
                btnLike.setImageResource(
                    if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                btnLike.tag = if (liked) "liked" else "unliked"
                textViewlikeCount.text = event.likeCount.toString()
                
                /*
                if(approved) {
                    linearLayoutVerified.visibility = View.VISIBLE
                    cardViewEvent.strokeWidth = (1.5).toPx()
                    //cardViewEvent.strokeColor = Color.parseColor("#007AFF")
                }
                else {
                    linearLayoutVerified.visibility = View.GONE
                    //cardViewEvent.strokeWidth = 0
                }

                 */

                textHelpType.text = event.helpType ?: "Help Type Required"

                // Get the help type from event
                val helpType = event.helpType ?: "Help Type Required"

                // Replace " and " with "," (case-insensitive)
                val cleanHelpType = helpType.replace(" and ", ",", ignoreCase = true)

                // Split into list after cleaning
                val helpTypeList =
                    cleanHelpType.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                // Display logic
                val displayedHelpType = when {
                    helpTypeList.size > 2 -> helpTypeList.take(2).joinToString(", ") + "..."
                    helpTypeList.size == 2 -> helpTypeList.joinToString(", ") // ✅ ensures both words show
                    else -> helpTypeList.firstOrNull() ?: "Help Type Required"
                }

                // Set processed text to TextView
                textHelpType.text = displayedHelpType


                when (event.layoutType) {
                    Extensions.TYPE_DAY -> {
                        textViewDate.visibility = View.INVISIBLE
                        textViewDay.visibility = View.INVISIBLE
                    }

                }

                //refreshNumOfInterestAndProfileImg(event, textInterested, relativeLayoutImage)

                // refreshNumOfInterest
                refreshNumOfInterest(event, textInterested, isPastEvents, context)

                // Initialize Flag Icon Status
                val isFlagged = event.isFlagged == true

                ivFlag.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        if (isFlagged) R.color.red else R.color.gray
                    )
                )

                ivFlag.setOnClickListener {
                    val currentUser = Firebase.auth.currentUser ?: return@setOnClickListener
                    val db = Firebase.firestore
                    val eventRef = db.collection("outreachEventsDev").document(event.eventId!!)
                    val currentUserId = currentUser.uid

                    // Check if event is already flagged
                    if (event.isFlagged == true) {
                        // Only allow unflagging if current user is the one who flagged it
                        if (event.flaggedByUser == currentUserId) {
                            // User can unflag and update local event object
                            event.updateFlagStatus(false, null)

                            // Update Firestore
                            val updates = mapOf(
                                "isFlagged" to false,
                                "flaggedByUser" to null
                            )

                            eventRef.update(updates)
                                .addOnSuccessListener {
                                    ivFlag.setColorFilter(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.gray
                                        )
                                    )
                                    notifyItemChanged(pos)
                                    refreshBottomSheet(event)
                                    Log.d(
                                        "FlagDebug",
                                        "Successfully unflagged event by user ${currentUser.email}"
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FlagDebug", "Error updating flag status: ", e)
                                }
                        } else {
                            // User is not allowed to unflag
                            Toast.makeText(
                                context,
                                "Only the user who flagged this event or a Street Care Hub Leader can unflag it.",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.d(
                                "FlagDebug",
                                "User ${currentUser.email} attempted to unflag event flagged by another user"
                            )
                        }
                    } else {
                        // Event is not flagged, anyone can flag it - Update local event object
                        event.updateFlagStatus(true, currentUserId)

                        // Update Firestore
                        val updates = mapOf(
                            "isFlagged" to true,
                            "flaggedByUser" to currentUserId
                        )

                        eventRef.update(updates)
                            .addOnSuccessListener {
                                ivFlag.setColorFilter(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.red
                                    )
                                )
                                notifyItemChanged(pos)
                                refreshBottomSheet(event)
                                Log.d(
                                    "FlagDebug",
                                    "Successfully flagged event by user ${currentUser.email}"
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e("FlagDebug", "Error updating flag status: ", e)
                            }
                    }
                }

                val uid = event.uid;

                var type: String? = "";
                val db = Firebase.firestore
                db.collection("users").whereEqualTo("uid", uid.toString())
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (document in querySnapshot.documents) {
                                type += document.getString("Type")  // Replace "Type" with the correct field name
                                println(type)  // Do something with the retrieved Type
                            }
                        } else {
                            println("No user found with that uid")
                        }
                        verificationMark(getUserType(type.toString()), ivVerificationMark)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirestoreQuery", " Error getting documents: $exception")
                    }
            }
        }

    }

    inner class YearViewHolder(yearItemView: View) : RecyclerView.ViewHolder(yearItemView) {
        private val textViewYear: TextView =
            yearItemView.findViewById<TextView>(R.id.textViewCommunityYear)

        fun bind(pos: Int) {
            // Bind data to views
            // ...
            val communityData = controller.getEventAtPosition(pos)
            communityData?.eventYear?.let { eventYear ->
                textViewYear.text = eventYear.year
                currentHeaderText = eventYear.year
            }
            //Log.d("stickyHeader", "pos: $pos, textViewYear: ${textViewYear.text}")
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == Extensions.TYPE_MONTH) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_community_year, parent, false)
            YearViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.event_list_layout, parent, false)
            EventViewHolder(view)
        }


    }

    override fun getItemCount(): Int {
        return controller.size
    }


    //commenting out and changing this method to remove default cards
   override fun getItemViewType(position: Int): Int {
    val communityData = controller.getEventAtPosition(position)
    return if (communityData?.event == null) Extensions.TYPE_MONTH else Extensions.TYPE_DAY
     }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> {
                holder.bind(position, holder.itemView.context)
            }
            is YearViewHolder -> {
                holder.bind(position)
            }
        }
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = 0
        var theItemPosition = itemPosition
        do {
            if (isHeader(theItemPosition)) {
                headerPosition = theItemPosition
                break
            }
            theItemPosition -= 1
        } while (theItemPosition >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.card_community_year
    }

    override fun bindHeaderData(header: View?, headerPosition: Int) {
        header?.let{
            it.findViewById<TextView>(R.id.textViewCommunityYear).text = controller.getEventAtPosition(headerPosition)?.eventYear?.year
            it.findViewById<TextView>(R.id.textViewCommunityYear).setBackgroundColor(Color.WHITE)
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return controller.getEventAtPosition(itemPosition)?.layoutType == Extensions.TYPE_MONTH
    }

}
