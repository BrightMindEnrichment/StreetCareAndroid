package org.brightmindenrichment.street_care.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.brightmindenrichment.data.local.EventsDatabase
import org.brightmindenrichment.street_care.MainActivity
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.notification.ChangedType
import org.brightmindenrichment.street_care.ui.community.data.Event
import org.brightmindenrichment.street_care.ui.community.data.HelpRequest
import org.brightmindenrichment.street_care.ui.community.data.HelpRequestStatus
import org.brightmindenrichment.street_care.ui.community.model.DatabaseEvent
import org.brightmindenrichment.street_care.util.Constants.DEFAULT_CAPACITY
import org.brightmindenrichment.street_care.util.Constants.INTENT_TYPE_NOTIFICATION
import java.io.Serializable
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Extensions {
    companion object{

        val requiredSkills = arrayOf(
            "Childcare",
            "Counseling and Support",
            "Clothing",
            "Education",
            "Personal Care",
            "Employment and Training",
            "Food and water",
            "Healthcare",
            "Chinese",
            "Spanish",
            "Language(please specify)",
            "Legal",
            "Shelter",
            "Transportation",
            "LGBTQ Support",
            "Technology Access",
            "Social Integration",
            "Pet Care"
        )

        @Suppress("DEPRECATION")
        inline fun <reified T : Serializable> Bundle.customGetSerializable(key: String): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getSerializable(key, T::class.java)
            } else {
                getSerializable(key) as? T
            }
        }

        fun createSkillTextView(text: String, context: Context): TextView {
            val textView = TextView(context)
            //setting height and width
            textView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 10, 10)
            }
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            textView.setTextColor(Color.GRAY)
            textView.setPadding(15, 5, 15, 5)
            textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textView.gravity = Gravity.CENTER_VERTICAL
            textView.isAllCaps = false
            textView.background = ResourcesCompat.getDrawable(context.resources, R.drawable.skill_text_view, null)

            return textView
        }

        fun setButtonInterest(
            buttonInterest: AppCompatButton,
            textId: Int,
            textColor: Int,
            backgroundColor: Int?,
        ) {
            buttonInterest.setText(textId)
            if(backgroundColor != null) {
                buttonInterest.backgroundTintList = ColorStateList.valueOf(
                    backgroundColor
                )
            }
            else buttonInterest.backgroundTintList = null
            buttonInterest.setTextColor(textColor)
        }

        fun replaceButtonInterest(
            buttonInterest: AppCompatButton,
            tvEventStatus: TextView,
            textId: Int,
        ) {
            buttonInterest.visibility = View.GONE
            tvEventStatus.setText(textId)
            tvEventStatus.visibility = View.VISIBLE
        }


        fun setHelpRequestActionButton(
            helpRequest: HelpRequest,
            btnAction: AppCompatButton,
            tvHelpRequestStatus: TextView,
            llButton: LinearLayout,
            currentUserId: String,
            context: Context
        ){
            when(helpRequest.status) {
                HelpRequestStatus.NeedHelp.status -> {
                    helpRequest.status = HelpRequestStatus.HelpOnTheWay.status
                }
                HelpRequestStatus.HelpOnTheWay.status -> {
                    if(currentUserId == helpRequest.uid) {
                        helpRequest.status = HelpRequestStatus.HelpReceived.status
                    }
                    else {
                        helpRequest.status = HelpRequestStatus.NeedHelp.status
                    }

                }
                HelpRequestStatus.HelpReceived.status -> {
                    helpRequest.status = HelpRequestStatus.HelpOnTheWay.status
                }
            }
//            if(helpRequest.title == "Employment Support Needed in NY") {
//                Log.d("check", "")
//            }
            setHelpRequestActionButtonStyle(
                helpRequest = helpRequest,
                btnAction = btnAction,
                tvHelpRequestStatus = tvHelpRequestStatus,
                llButton = llButton,
                currentUserId = currentUserId,
                context = context,
                textColor = Color.BLACK,
                backgroundColor = null
            )
        }

        fun setHelpRequestActionButtonStyle(
            helpRequest: HelpRequest,
            btnAction: AppCompatButton,
            tvHelpRequestStatus: TextView,
            llButton: LinearLayout,
            currentUserId: String,
            context: Context,
            textColor: Int,
            backgroundColor: Int?
        ) {
            Log.d("debug", "bind, helpRequest status: ${helpRequest.status}")
            when(helpRequest.status) {
                HelpRequestStatus.NeedHelp.status -> {
                    llButton.visibility = View.VISIBLE
                    //btnAction.text = Resources.getSystem().getString(R.string.can_help)
                    val color = context.resources.getColor(R.color.accent_yellow, null)
                    btnAction.backgroundTintList = ColorStateList.valueOf(
                        context.resources.getColor(R.color.dark_green, null)
                    )
                    btnAction.setTextColor(color)
                    btnAction.text = context.getString(R.string.can_help)
                    tvHelpRequestStatus.text = HelpRequestStatus.NeedHelp.status
                }
                HelpRequestStatus.HelpOnTheWay.status -> {
                    Log.d("debug", "bind, help on the way")
                    Log.d("debug", "bind, helpRequest: ${helpRequest.title}")
                    if(currentUserId != helpRequest.uid) {
                        //llButton.visibility = View.GONE
                        btnAction.text = context.getString(R.string.cancel_help)
                        btnAction.setTextColor(textColor)
                        if(backgroundColor != null) {
                            btnAction.backgroundTintList = ColorStateList.valueOf(
                                backgroundColor
                            )
                        }
                        else btnAction.backgroundTintList = null

                    }
                    else {
                        llButton.visibility = View.VISIBLE
                        //btnAction.text = Resources.getSystem().getString(R.string.help_received)

                        btnAction.text = context.getString(R.string.help_received)
                    }
                    tvHelpRequestStatus.text = HelpRequestStatus.HelpOnTheWay.status
                }
                HelpRequestStatus.HelpReceived.status -> {
                    tvHelpRequestStatus.text = HelpRequestStatus.HelpReceived.status
                    if(currentUserId == helpRequest.uid) {
                        llButton.visibility = View.VISIBLE
                        //btnAction.text = Resources.getSystem().getString(R.string.reopen_help_request)
                        val color = context.resources.getColor(R.color.accent_yellow, null)
                        btnAction.backgroundTintList = ColorStateList.valueOf(
                            context.resources.getColor(R.color.dark_green, null)
                        )
                        btnAction.setTextColor(color)
                        btnAction.text = context.getString(R.string.reopen_help_request)
                    }
                    else llButton.visibility = View.GONE
                }
            }
        }

        fun setRSVPButton(
            buttonRSVP: AppCompatButton,
            textId: Int,
            textColor: Int,
            backgroundColor: Int?,
        ) {
            buttonRSVP.setText(textId)
            if(backgroundColor != null) {
                buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                    backgroundColor
                )
            }
            else buttonRSVP.backgroundTintList = null
            buttonRSVP.setTextColor(textColor)
        }

        fun replaceRSVPButton(
            buttonRSVP: AppCompatButton,
            tvEventStatus: TextView,
            textId: Int,
        ) {
            buttonRSVP.visibility = View.GONE
            tvEventStatus.setText(textId)
            tvEventStatus.visibility = View.VISIBLE
        }

        fun setVerifiedAndRegistered(
            context: Context?,
            isVerified: Boolean,
            isRegistered: Boolean,
            isEventCard: Boolean,
            isPastEvents: Boolean,
            linearLayoutVerified: LinearLayout,
            linearLayoutVerifiedAndIcon: LinearLayout,
            textViewRegistered: TextView,
            cardViewEvent: MaterialCardView?,
            bottomSheetView: ScrollView?,
        ) {
            if(isVerified || (isRegistered && !isPastEvents)) linearLayoutVerified.visibility = View.VISIBLE
            else linearLayoutVerified.visibility = View.GONE

            if(isVerified) {
                linearLayoutVerifiedAndIcon.visibility = View.VISIBLE
                if(isEventCard) cardViewEvent?.strokeWidth = (1.5).toPx()
                else {
                    context?.let {
                        bottomSheetView?.background = ContextCompat.getDrawable(it, R.drawable.verified_shape)
                    }
                }
            }
            else {
                linearLayoutVerifiedAndIcon.visibility = View.INVISIBLE
                if(isEventCard) cardViewEvent?.strokeWidth = 0
                else {
                    context?.let{
                        bottomSheetView?.background = ContextCompat.getDrawable(it, R.drawable.round_corner)
                    }
                }
            }

            if(isRegistered && !isPastEvents) textViewRegistered.visibility = View.VISIBLE
            else textViewRegistered.visibility = View.INVISIBLE
        }

        fun refreshNumOfInterest(
            event: Event,
            textInterested: TextView,
            isPastEvent: Boolean,
            context: Context,
        ) {
            val numOfInterest: Int = event.participants?.size ?: 0
            val maxCapacity: Int? = event.totalSlots
            //val infinitySign = DecimalFormatSymbols.getInstance().infinity
            val defaultCapacity: Int = DEFAULT_CAPACITY
            Log.d("syncWebApp", "isPastEvent: $isPastEvent")
            if(isPastEvent) {
                textInterested.text = context.getString(R.string.participants, numOfInterest)
            }else {
                if(maxCapacity == null || maxCapacity == -1) {
                    textInterested.text =
                        context.getString(
                            R.string.participants_with_default_capacity, numOfInterest, defaultCapacity
                        )
                } else {
                    textInterested.text = context.getString(
                        R.string.participants_with_max_capacity,
                        numOfInterest,
                        maxCapacity
                    )
                }
            }
        }

        fun refreshNumOfInterestAndProfileImg(
            event: Event,
            textInterested: TextView,
            relativeLayoutImage: RelativeLayout,
            context: Context
        ) {
            val numOfInterest: Int = if(event.itemList.size > 3)
                event.itemList.size.minus(3)
            else 0

            if(numOfInterest > 0)
                textInterested.text =
                    context.getString(
                        R.string.number_of_interest,
                        numOfInterest,
                        Resources.getSystem().getString(R.string.plural_interested)
                    )
            else{
                when (event.itemList.size) {
                    0 -> {
                        textInterested.text = Resources.getSystem().getString(R.string.first_one_to_join)
                    }
                    1 -> {
                        textInterested.text = Resources.getSystem().getString(R.string.singular_interested)
                    }
                    else -> {
                        textInterested.text = Resources.getSystem().getString(R.string.plural_interested)
                    }
                }
            }

            relativeLayoutImage.removeAllViews()
            if(event.itemList.isNotEmpty()){
                for (i in event.itemList.indices){
                    if(i >= 3) break
                    val imageView = CircleImageView(relativeLayoutImage.context)
                    imageView.layoutParams = RelativeLayout.LayoutParams(80, 80)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    val layoutParams = imageView.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    layoutParams.marginStart = i * 40 // Adjust the spacing between images
                    imageView.borderWidth = 2 // Set border width
                    imageView.borderColor = Color.BLACK
                    Picasso.get().load(event.itemList[i]).error(R.drawable.ic_profile).into(imageView)
                    imageView.setCircleBackgroundColorResource(R.color.white)
                    relativeLayoutImage.addView(imageView)
                }
            }
            else{
                val imageView = CircleImageView(relativeLayoutImage.context)
                imageView.layoutParams = RelativeLayout.LayoutParams(80, 80)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                val layoutParams = imageView.layoutParams as RelativeLayout.LayoutParams
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                imageView.setImageResource(R.drawable.ic_profile)
                imageView.setBackgroundResource(R.drawable.dashed_border)
                relativeLayoutImage.addView(imageView)
            }
        }

        fun updateFieldInExistingCollection(
            db: FirebaseFirestore,
            existingCollection: String,
        ) {

            db.collection(existingCollection).get().addOnSuccessListener { documents ->
                for(doc in documents) {
                    val approved = doc.get("approved").toString()
                    val docRef = db.collection(existingCollection).document(doc.id)
                    if(approved == "Approved") {
                        docRef
                            .update("approved", true)
                            .addOnSuccessListener { Log.d("syncWebApp", "DocumentSnapshot successfully updated!") }
                            .addOnFailureListener { e -> Log.w("syncWebApp", "Error updating document", e) }
                    }
                }

            }

        }

        fun createNewCollectionFromExistingCollection(
            db: FirebaseFirestore,
            createData: (QueryDocumentSnapshot) -> Map<String, Any?>,
            existingCollection: String,
            newCollection: String
        ) {
            db.collection(existingCollection).get().addOnSuccessListener { documents ->
                for(doc in documents) {
                    val eventData = createData(doc)
                    db.collection(newCollection)
                        .add(eventData)
                        .addOnSuccessListener { documentReference ->
                            Log.d("syncWebApp", "Doc added from $existingCollection, ID: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("syncWebApp", "Error adding document", e)
                        }
                }

            }

        }

        fun createHelpRequestsData( doc: QueryDocumentSnapshot): Map<String, Any?> {
            return hashMapOf(
                "createdAt" to doc.get("createdAt"),
                "description" to doc.get("description"),
                "identification" to doc.get("identification"),
                "location" to doc.get("location"), // map: {city: String, state: String, street: String, zipcode: String
                "skills" to (doc.get("skills")?: listOf<String>()), // array
                "status" to doc.get("status"),
                "title" to doc.get("title"),
                "uid" to doc.get("uid"),
            )
        }

        fun createEventData( doc: QueryDocumentSnapshot): Map<String, Any?> {
            return hashMapOf(
                "approved" to (doc.get("approved")?: false),
                "createdAt" to doc.get("createdAt"),
                "description" to doc.get("description"),
                "eventDate" to doc.get("eventDate"),
                "eventEndTime" to doc.get("eventEndTime"),
                "eventStartTime" to doc.get("eventStartTime"),
                "helpRequest" to (doc.get("helpRequest")?: listOf<String>()), // array
                "helpType" to doc.get("helpType"), // string
                "interests" to doc.get("interests"), // int
                "location" to doc.get("location"), // map: {city: String, state: String, street: String, zipcode: String
                "participants" to (doc.get("participants")?: listOf<String>()), // array
                "skills" to (doc.get("skills")?: listOf<String>()), // array
                "title" to doc.get("title"),
                "totalSlots" to (doc.get("totalSlots")?: "0"),
                "uid" to doc.get("uid"),
            )
        }

        private fun onAdded(
            scope: CoroutineScope,
            databaseEvent: DatabaseEvent,
            eventsDatabase: EventsDatabase,
            eventTitle: String,
            eventMessage: String,
            appContext: Context,
            eventId: String
        ) {
            scope.launch(IO) {
                Log.d("workManager", "onAdded")
                val notificationTitle = "New Event Added"
                eventsDatabase.eventDao().addEvent(databaseEvent)
                showNotification(
                    notificationTitle,
                    eventTitle,
                    eventMessage,
                    appContext,
                    ChangedType.Add.type,
                    eventId,
                )
            }
        }

        private fun onRemoved(
            scope: CoroutineScope,
            eventsDatabase: EventsDatabase,
            eventTitle: String,
            eventMessage: String,
            appContext: Context,
            eventId: String
        ) {
            scope.launch(IO) {
                eventsDatabase.eventDao().deleteEvent(eventId)
                val notificationTitle = "Event removed"
                showNotification(
                    notificationTitle,
                    eventTitle,
                    eventMessage,
                    appContext,
                    ChangedType.Remove.type,
                    eventId,
                )
            }
        }

        private fun onModified(
            scope: CoroutineScope,
            eventsDatabase: EventsDatabase,
            appContext: Context,
            dc: DocumentChange
        ) {
            scope.launch(IO) {
                // only show the notification when title, location, date, description, status changed.
                val originalEvent = eventsDatabase.eventDao().getEventById(dc.document.id)!!
                var shouldShowNotification = false
                var notificationTitle = "Event Modified"
                val originalEventDate = getDateTimeFromTimestamp(originalEvent.date)
                val newEventDate = getDateTimeFromTimestamp(dc.document.data["date"])
                if(originalEventDate != newEventDate) {
                    Log.d("modify", "originalEvent.timestamp: ${originalEvent.date}")
                    Log.d("modify", "new timestamp: ${dc.document.data["date"]}")
                    originalEvent.date = getDateTimeFromTimestamp(dc.document.data["date"])
                    shouldShowNotification = true
                }
                if(originalEvent.description != dc.document.data["description"]) {
                    Log.d("modify", "originalEvent.description: ${originalEvent.description}")
                    Log.d("modify", "new description: ${dc.document.data["description"]}")
                    originalEvent.description = (dc.document.data["description"] ?: "no description") as String
                    shouldShowNotification = true
                }
                if(originalEvent.interest != dc.document.data["interest"]) {
                    Log.d("modify", "originalEvent.interest: ${originalEvent.interest}")
                    Log.d("modify", "new interest: ${dc.document.data["interest"]}")
                    originalEvent.interest = ((dc.document.data["interest"]?: 0) as Long).toInt()
                }
                if(originalEvent.location != dc.document.data["location"]) {
                    Log.d("modify", "originalEvent.location: ${originalEvent.location}")
                    Log.d("modify", "new location: ${dc.document.data["location"]}")
                    originalEvent.location = (dc.document.data["location"]?: "no location") as String
                    shouldShowNotification = true
                }
                if(originalEvent.status != dc.document.data["status"]) {
                    Log.d("modify", "originalEvent.status: ${originalEvent.status}")
                    Log.d("modify", "new status: ${dc.document.data["status"]}")
                    originalEvent.status = (dc.document.data["status"]?: "no status") as String
                    shouldShowNotification = true
                    when(originalEvent.status?.lowercase()) {
                        "pending" -> notificationTitle = "Event Removed"
                        "approved" -> notificationTitle = "Event Added"
                    }
                }
                if(originalEvent.title != dc.document.data["title"]) {
                    Log.d("modify", "originalEvent.title: ${originalEvent.title}")
                    Log.d("modify", "new title: ${dc.document.data["title"]}")
                    originalEvent.title = (dc.document.data["title"]?: "no title") as String
                    shouldShowNotification = true
                }

                val eventTitle = originalEvent.title ?: "Unknown Title"
                val eventDescription = originalEvent.description!!
                val eventLocation = originalEvent.location!!
                val eventMessage = "${originalEvent.date}/$eventDescription/$eventLocation"
                if(shouldShowNotification) showNotification(
                    notificationTitle,
                    eventTitle,
                    eventMessage,
                    appContext,
                    ChangedType.Modify.type,
                    dc.document.id,
                )
                Log.d("workManager", "Modified event: ${dc.document.data}")
                eventsDatabase.eventDao().updateUsers(originalEvent)
            }
        }
        suspend fun addSnapshotListenerToCollection(
            collectionRef: CollectionReference,
            eventsDatabase: EventsDatabase,
            appContext: Context,
            scope: CoroutineScope,
            dataStoreManager: DataStoreManager,
            isFromWorker: Boolean
        ): ListenerRegistration {
            val isFirstListener = AtomicBoolean(true)
            val listenerRegistration = collectionRef
                .addSnapshotListener { snapshots, e ->
                    scope.launch(IO) {
                        if (e != null) {
                            Log.w("workManager", "Listen failed.", e)
                            return@launch
                        }

                        Log.d("workManager", "isFirstListener: ${isFirstListener.get()}")

                        if(isFirstListener.get()) {
                            isFirstListener.set(false)
                            return@launch
                        }

                        val isAppOnBackground = dataStoreManager.getIsAppOnBackground().first()
                        Log.d("workManager", "isAppOnBackground: $isAppOnBackground")
                        Log.d("workManager", "isFromWorker: $isFromWorker")

                        if(isFromWorker && !isAppOnBackground) return@launch

                        for (dc in snapshots!!.documentChanges) {
                            val eventTitle = (dc.document.data["title"]?: "Unknown Event").toString()
                            val timestamp = dc.document.data["date"]
                            val eventDescription = (dc.document.data["description"]?: "No Description").toString()
                            val eventLocation = (dc.document.data["location"]?: "No Location").toString()
                            val eventMessage = "${getDateTimeFromTimestamp(timestamp)}/$eventDescription/$eventLocation"
                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    val databaseEvent = createDatabaseEvent(dc)
                                    onAdded(
                                        scope,
                                        databaseEvent,
                                        eventsDatabase,
                                        eventTitle,
                                        eventMessage,
                                        appContext,
                                        dc.document.id
                                    )
                                    Log.d("workManager", "New event added: ${dc.document.data}")
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    onModified(
                                        scope,
                                        eventsDatabase,
                                        appContext,
                                        dc
                                    )
                                }
                                DocumentChange.Type.REMOVED -> {
                                    onRemoved(
                                        scope,
                                        eventsDatabase,
                                        eventTitle,
                                        eventMessage,
                                        appContext,
                                        dc.document.id
                                    )
                                    Log.d("workManager", "event removed: ${dc.document.data}")
                                }
                            }
                        }
                    }
                }
            return listenerRegistration
        }


        private fun createDatabaseEvent(dc: DocumentChange): DatabaseEvent {
            return DatabaseEvent(
                id = dc.document.id,
                date = getDateTimeFromTimestamp(dc.document.data["date"]),
                description = dc.document.data["description"] as? String,
                interest = ((dc.document.data["interest"] ?: 0) as Long).toInt(),
                location = dc.document.data["location"] as? String,
                status = dc.document.data["status"] as? String,
                title = dc.document.data["title"] as String,
            )
        }

        fun createDatabaseEvent(document: QueryDocumentSnapshot): DatabaseEvent {
            return DatabaseEvent(
                id = document.id,
                date = getDateTimeFromTimestamp(document.get("date")),
                description = document.get("description")?.toString() ?: "Unknown Description",
                interest = ((document.get("interest") ?: 0) as Long).toInt(),
                location = document.get("location")?.toString() ?: "Unknown Location",
                status = document.get("status")?.toString() ?: "Unknown Status",
                title = document.get("title")?.toString() ?: "Unknown Title",
            )
        }

        private fun createPendingIntentByTaskStackBuilder(
            changedType: String,
            eventId: String,
            eventTitle: String,
            appContext: Context,
        ): PendingIntent {
            // Create an Intent for the activity you want to start.
            val resultIntent = Intent(appContext, MainActivity::class.java)
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            resultIntent.putExtra( "changedType", changedType)
            resultIntent.putExtra( "eventId", eventId)
            resultIntent.putExtra( "eventTitle", eventTitle)
            resultIntent.putExtra( "intentType", INTENT_TYPE_NOTIFICATION)
            // Create the TaskStackBuilder.
            return TaskStackBuilder.create(appContext).run {
                // Add the intent, which inflates the back stack.
                addNextIntentWithParentStack(resultIntent)
                // Get the PendingIntent containing the entire back stack.
                getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

        }

        private fun createPendingIntent(
            changedType: String,
            eventId: String,
            eventTitle: String,
            context: Context
        ): PendingIntent {
            return NavDeepLinkBuilder(context)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.nav_community)
                .setDestination(R.id.communityEventFragment)
                .setComponentName(MainActivity::class.java)
                .setArguments(
                    bundleOf(
                        "changedType" to changedType,
                        "eventId" to eventId,
                        "eventTitle" to eventTitle
                    )
                )
                .createPendingIntent()

            /*
            // Create an Intent for the activity you want to start.
            val resultIntent = Intent(context, MainActivity::class.java)
            // Create the TaskStackBuilder.
            val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                // Add the intent, which inflates the back stack.
                addNextIntentWithParentStack(resultIntent)
                // Get the PendingIntent containing the entire back stack.
                getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

             */

        }

        fun showNotification(
            notificationTitle: String,
            eventTitle: String,
            eventMessage: String,
            context: Context,
            changedType: String,
            eventId: String,
        ) {
            FirebaseAuth.getInstance().currentUser?.let { // MOD
                // create pendingIntent to redirect to the event fragment when users click the notification
                //val pendingIntent = createPendingIntentByTaskStackBuilder(changedType, eventId, eventTitle, context)
                val pendingIntent = createPendingIntent(changedType, eventId, eventTitle, context)

                // Create a notification channel (required for Android Oreo and above)
                createNotificationChannel(context)
                val eventMessageChunk = eventMessage.split("/")
                val dateAndTime = eventMessageChunk[0]
                val description = eventMessageChunk[1]
                val location = eventMessageChunk[2]

                // Build the notification
                // https://developer.android.com/develop/ui/views/notifications/expanded
                val builder = NotificationCompat.Builder(context,
                    Constants.EVENTS_NOTIFICATION_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.streetcare_logo) // Set your notification icon
                    .setContentTitle(notificationTitle)
                    .setContentText(eventTitle)
                    .setContentIntent(pendingIntent)
                    //.setGroup(EVENTS_NOTIFICATION)
                    .setStyle(
                        NotificationCompat.InboxStyle()
                            .addLine(eventTitle)
                            .addLine("location: $location")
                            .addLine(dateAndTime)
                            .addLine("description: $description")
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                // Show the notification
                with(NotificationManagerCompat.from(context)) {
                    if(checkPermission(Manifest.permission.POST_NOTIFICATIONS, context)) {
                        notify(NotificationID.getNotificationID(), builder.build()) // You can provide a unique notification ID
                    }
                }
            }

        }

        private fun createNotificationChannel(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Events Update Channel"
                val descriptionText = "Firebase Events Collection Channel"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(Constants.EVENTS_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                // Register the channel with the system
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun checkPermission(permission: String, context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }

        private fun showRationaleDialog(
            title: String,
            message: String,
            context: Context,
            requestPermissionLauncher: ActivityResultLauncher<String>
        ) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok") { dialog, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    dialog.dismiss()
                }

            builder.create().show()
        }

        fun askPermission(
            context: Context,
            permission: String,
            activity: Activity,
            requestPermissionLauncher: ActivityResultLauncher<String>,
            title: String,
            message: String,
        ) {
            if (checkPermission(permission, context)) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(activity, permission)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.

                showRationaleDialog(
                    title = title,
                    message = message,
                    context = context,
                    requestPermissionLauncher = requestPermissionLauncher
                )
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(permission)
            }
        }

        fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
        fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
        fun Double.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
        fun Double.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

        fun convertTimestampToDate(timestamp: Timestamp): Date {
            return timestamp.toDate()
        }

        fun convertTimestampToMilliSec(timestamp: Timestamp): Long {
            return timestamp.toDate().time
        }

        fun getDayInMilliSec(days: Int): Long {
            return days.toLong() * 24 * 60 * 60 * 1000
        }

        fun getDateTimeFromTimestamp(s: Any?): String {
            if(s == null) return "Unknown date and time"
            val timestamp = s as? Timestamp ?: return "Unknown date and time"
            val netDate = timestamp.toDate()
            //Log.d("firebase", "timestamp: ${timestamp.toDate()}")
            return try {
                // Jan/10/2023 at 15:08 CST
                val sdf =
                    android.icu.text.SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm zzz", Locale.US)
                //val netDate = Date(timestamp.toString().toLong() * 1000)
                sdf.format(netDate)
            } catch (e: Exception) {
                e.toString()
            }
        }

        fun floatToLong (value : Float) : Long{
            return value.toLong()
        }

//        fun dateToString(date: Date?, format: String): String {
//            val dateFormat = SimpleDateFormat(format, Locale.US)
//            return dateFormat.format(date)
//        }

        fun dateToString(date: Date?, pattern: String): String {
            return if (date == null) {
                ""
            } else {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.format(date)
            }
        }



        fun  showDialog(
            context: Context,
            title: String,
            message: String,
            textPositive: String,
            Cancel: String,
            callback: () -> Unit = {}
        ){
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(textPositive, DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                }
                )
            val alert = builder.create()
            alert.show()
            callback()
        }

        fun dateParser(date: String): LocalDateTime? {
            if(date.length>12) {
                val secondsStartIndex = date.indexOf("seconds=") + 8
                val secondsEndIndex = date.indexOf(",", secondsStartIndex)
                val secondsString = date.substring(secondsStartIndex, secondsEndIndex)
                val seconds = secondsString.toLong()

// Extract the nanoseconds value
                val nanosecondsStartIndex = date.indexOf("nanoseconds=") + 12
                val nanosecondsEndIndex = date.indexOf(")", nanosecondsStartIndex)
                val nanosecondsString = date.substring(nanosecondsStartIndex, nanosecondsEndIndex)
                val nanoseconds = nanosecondsString.toLong()
                val instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
// Convert the Instant to a LocalDateTime in the system default time zone
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            }
            return null
        }

        const val TYPE_MONTH = 1
        const val TYPE_NEW_DAY = 2
        const val TYPE_DAY = 3
    }





}