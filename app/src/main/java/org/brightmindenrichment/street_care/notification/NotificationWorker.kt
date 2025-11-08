package org.brightmindenrichment.street_care.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.firestore.ListenerRegistration
// import com.google.firebase.firestore.ktx.firestore MOD
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.brightmindenrichment.data.local.EventsDatabase
import org.brightmindenrichment.street_care.MainActivity
import org.brightmindenrichment.street_care.ui.community.CommunityEventFragment
import org.brightmindenrichment.street_care.ui.community.model.DatabaseEvent
import org.brightmindenrichment.street_care.util.DataStoreManager
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.addSnapshotListenerToCollection
import org.brightmindenrichment.street_care.util.Extensions.Companion.createDatabaseEvent
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDateTimeFromTimestamp
import org.brightmindenrichment.street_care.util.Extensions.Companion.showNotification
import kotlin.time.Duration


/*
     1. The application must be launched at least once to schedule work,
        installing the application is not enough.
     2. The phone needs to have completed the boot and been unlocked once.
        This because WorkManager is marked as not direct boot aware.
        Once the phone completed its boot, WorkManager will execute the scheduled work independently from having launched or not the application.
        The only exception is for application that have been force stopped.
        In this case all notification and all scheduled Jobs (as WorkManager relies to Android's JobScheduler) are cancelled till the application is launched by the user.
        Once the application is executed at least once,
        WorkManager will pick up all the Work and reschedule it.

 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val eventsDatabase: EventsDatabase,
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
): CoroutineWorker(appContext, workerParameters) {
    private val db = FirebaseFirestore.getInstance() // MOD
    private lateinit var dataStoreManager: DataStoreManager
    //private val databaseEvents: List<DatabaseEvent> = eventsDatabase.eventDao().getAllEventsDesc()

    override suspend fun doWork(): Result {
        Log.d("workManager", "do work...")
        dataStoreManager = DataStoreManager(appContext)
        if(FirebaseAuth.getInstance().currentUser != null) { // MOD
            Log.d("workManager", "user has logged in")
            CoroutineScope(IO).launch {
                val listenerRegistration = addSnapshotListenerToCollection(
                    db.collection("events"),
                    eventsDatabase,
                    applicationContext,
                    CoroutineScope(IO),
                    dataStoreManager,
                    true,
                )
                Log.d("workManager", "waiting for listenerRegistration to be removed")
                delay(30000L)
                Log.d("workManager", "listenerRegistration is removing")
                listenerRegistration.remove()
                Log.d("workManager", "listenerRegistration removed")
            }
            //checkUpdatesAndShowNotification()
        } else {
            Log.d("workManager", "no user login")
        }
        //if(databaseEvents.isEmpty()) return Result.retry()
        return Result.success()
    }
/*
    private fun checkUpdatesAndShowNotification() {
        val query = db.collection("events")
        val firebaseEventsMap = mutableMapOf<String, DatabaseEvent>()
        val databaseEventsMap = databaseEvents.associateBy({it.id}, {it})
        val scope = CoroutineScope(IO)
        //Log.d("workManager", "databaseEventsMap: ${databaseEventsMap.size}, $databaseEventsMap")
        query.get().addOnSuccessListener { result ->
            for (document in result) {
                var eventTitle = (document.get("title")?: "Unknown Event").toString()
                val timestamp = document.get("date")
                var eventDescription = (document.get("description")?: "No Description").toString()
                var eventLocation = (document.get("location")?: "No Location").toString()
                //Log.d("firebase", "timestamp: $timestamp")
                var eventMessage = "${getDateTimeFromTimestamp(timestamp)}/$eventDescription/$eventLocation"
                val databaseEvent = databaseEventsMap[document.id]
                val firebaseEvent = createDatabaseEvent(document)
                firebaseEventsMap[document.id] = firebaseEvent

                if(databaseEvent == null) {
                    // new event added
                    scope.launch {
                        eventsDatabase.eventDao().addEvent(firebaseEvent)
                    }
                    val notificationTitle = "New Event Added"
                    showNotification(
                        notificationTitle,
                        eventTitle,
                        eventMessage,
                        appContext,
                        ChangedType.Add.type,
                        document.id,
                    )
                    Log.d("workManager", "New event added: ${document.data}")
                }
                else {
                    // check if event has been modified
                    // only show the notification when title, location, date, description, status changed.
                    var shouldShowNotification = false
                    var hasModified = false
                    var notificationTitle = "Event Modified"
                    val originalEventDate = getDateTimeFromTimestamp(databaseEvent.date)
                    val newEventDate = getDateTimeFromTimestamp(firebaseEvent.date)

                    if(originalEventDate != newEventDate) {
                        Log.d("workManager", "originalEvent.timestamp: ${databaseEvent.date}")
                        Log.d("workManager", "new timestamp: ${firebaseEvent.date}")
                        databaseEvent.date = getDateTimeFromTimestamp(firebaseEvent.date)
                        shouldShowNotification = true
                        hasModified = true
                    }
                    if(databaseEvent.description != firebaseEvent.description) {
                        Log.d("modify", "originalEvent.description: ${databaseEvent.description}")
                        Log.d("modify", "new description: ${firebaseEvent.description}")
                        databaseEvent.description = (firebaseEvent.description ?: "no description") as String
                        shouldShowNotification = true
                        hasModified = true
                    }
                    if(databaseEvent.interest != firebaseEvent.interest) {
                        Log.d("modify", "originalEvent.interest: ${databaseEvent.interest}")
                        Log.d("modify", "new interest: ${firebaseEvent.interest}")
                        databaseEvent.interest = firebaseEvent.interest?: 0
                        hasModified = true
                    }
                    if(databaseEvent.location != firebaseEvent.location) {
                        Log.d("modify", "originalEvent.location: ${databaseEvent.location}")
                        Log.d("modify", "new location: ${firebaseEvent.location}")
                        databaseEvent.location = (firebaseEvent.location?: "no location") as String
                        shouldShowNotification = true
                        hasModified = true
                    }
                    if(databaseEvent.status != firebaseEvent.status) {
                        Log.d("modify", "originalEvent.status: ${databaseEvent.status}")
                        Log.d("modify", "new status: ${firebaseEvent.status}")
                        databaseEvent.status = (firebaseEvent.status?: "no status") as String
                        shouldShowNotification = true
                        hasModified = true
                        when(databaseEvent.status?.lowercase()) {
                            "pending" -> notificationTitle = "Event Removed"
                            "approved" -> notificationTitle = "Event Added"
                        }
                    }
                    if(databaseEvent.title != firebaseEvent.title) {
                        Log.d("modify", "originalEvent.title: ${databaseEvent.title}")
                        Log.d("modify", "new title: ${firebaseEvent.title}")
                        databaseEvent.title = (firebaseEvent.title?: "no title") as String
                        shouldShowNotification = true
                        hasModified = true
                    }

                    eventTitle = databaseEvent.title ?: "Unknown Title"
                    eventDescription = databaseEvent.description!!
                    eventLocation = databaseEvent.location!!
                    eventMessage = "${databaseEvent.date}/$eventDescription/$eventLocation"

                    if(shouldShowNotification) showNotification(
                        notificationTitle,
                        eventTitle,
                        eventMessage,
                        appContext,
                        ChangedType.Modify.type,
                        document.id,
                    )
                    if(hasModified) {
                        Log.d("workManager", "event has been modified: $hasModified")
                        scope.launch {
                            val updatedRow = eventsDatabase.eventDao().updateUsers(databaseEvent)
                            Log.d("workManager", "updatedRow: $updatedRow")
                        }
                        Log.d("workManager", "NotificationWorker, Modified event: ${document.data}")
                    }
                }
            }
            Log.d("workManager", "firebaseEventsMap: ${firebaseEventsMap.size}, $firebaseEventsMap")
            // check if event has been removed
            databaseEvents.forEach {
                val eventTitle = it.title.toString()
                val eventDescription = it.description
                val eventLocation = it.location
                //Log.d("firebase", "timestamp: $timestamp")
                val eventMessage = "${it.date}/$eventDescription/$eventLocation"
                if(firebaseEventsMap[it.id] == null) {
                    Log.d("workManager", "event has been removed.")
                    // event has been removed
                    var deleteStatus = -1
                    scope.launch {
                        deleteStatus = eventsDatabase.eventDao().deleteEvent(it.id)
                    }
                    val notificationTitle = "Event removed"
                    showNotification(
                        notificationTitle,
                        eventTitle,
                        eventMessage,
                        appContext,
                        ChangedType.Remove.type,
                        it.id,
                    )
                    Log.d("workManager", "Removed event: ${it}, deleteStatus: $deleteStatus")
                }
            }


        }.addOnFailureListener { exception ->
            Log.d("workManager", "initialize events database failed: $exception")
        }
    }

 */

    /*
    private suspend fun addSnapshotListenerToCollection(collectionRef: CollectionReference) {
        val scope = CoroutineScope(IO)
        listenerRegistration = collectionRef
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("workManager", "Listen failed.", e)
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
                    var eventTitle = (dc.document.data["title"]?: "Unknown Event").toString()
                    val timestamp = dc.document.data["date"]
                    var eventDescription = (dc.document.data["description"]?: "No Description").toString()
                    var eventLocation = (dc.document.data["location"]?: "No Location").toString()
                    //Log.d("firebase", "timestamp: $timestamp")
                    var eventMessage = "${Extensions.getDateTimeFromTimestamp(timestamp)}/$eventDescription/$eventLocation"
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val databaseEvent = createDatabaseEvent(dc)
                            scope.launch {
                                eventsDatabase.eventDao().addEvent(databaseEvent)
                            }
                            val notificationTitle = "New Event Added"
                            showNotification(
                                notificationTitle,
                                eventTitle,
                                eventMessage,
                                applicationContext,
                                ChangedType.Add.type,
                                dc.document.id,
                                activityContext
                            )
                            Log.d("workManager", "New event added: ${dc.document.data}")
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // only show the notification when title, location, date, description, status changed.
                            val originalEvent = eventsDatabase.eventDao().getEventById(dc.document.id)!!
                            var shouldShowNotification = false
                            var notificationTitle = "Event Modified"
                            val originalEventDate = Extensions.getDateTimeFromTimestamp(originalEvent.date)
                            val newEventDate = Extensions.getDateTimeFromTimestamp(dc.document.data["date"])
                            if(originalEventDate != newEventDate) {
                                Log.d("modify", "originalEvent.timestamp: ${originalEvent.date}")
                                Log.d("modify", "new timestamp: ${dc.document.data["date"]}")
                                originalEvent.date = Extensions.getDateTimeFromTimestamp(dc.document.data["date"])
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

                            eventTitle = originalEvent.title ?: "Unknown Title"
                            eventDescription = originalEvent.description!!
                            eventLocation = originalEvent.location!!
                            eventMessage = "${originalEvent.date}/$eventDescription/$eventLocation"

                            if(shouldShowNotification) showNotification(
                                notificationTitle,
                                eventTitle,
                                eventMessage,
                                applicationContext,
                                ChangedType.Modify.type,
                                dc.document.id,
                                activityContext
                            )
                            scope.launch { eventsDatabase.eventDao().updateUsers(originalEvent) }
                            Log.d("workManager", "Modified event: ${dc.document.data}")
                        }
                        DocumentChange.Type.REMOVED -> {
                            scope.launch {
                                eventsDatabase.eventDao().deleteEvent(dc.document.id)
                            }
                            val notificationTitle = "Event removed"
                            showNotification(
                                notificationTitle,
                                eventTitle,
                                eventMessage,
                                applicationContext,
                                ChangedType.Remove.type,
                                dc.document.id,
                                activityContext
                            )
                            Log.d("workManager", "Removed event: ${dc.document.data}")
                        }
                    }
                }
            }
    }

    private fun createDatabaseEvent(dc: DocumentChange): DatabaseEvent {
        return DatabaseEvent(
            id = dc.document.id,
            date = Extensions.getDateTimeFromTimestamp(dc.document.data["date"]),
            description = dc.document.data["description"] as? String,
            interest = ((dc.document.data["interest"] ?: 0) as Long).toInt(),
            location = dc.document.data["location"] as? String,
            status = dc.document.data["status"] as? String,
            title = dc.document.data["title"] as String,
        )
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

    private fun showNotification(
        notificationTitle: String,
        eventTitle: String,
        eventMessage: String,
        context: Context,
        changedType: String,
        eventId: String,
        activityContext: Context
    ) {

        // create pendingIntent to redirect to the event fragment when users click the notification
        val pendingIntent = createPendingIntent(changedType, eventId, eventTitle, activityContext)

        // Create a notification channel (required for Android Oreo and above)
        createNotificationChannel(context)
        val eventMessageChunk = eventMessage.split("/")
        val dateAndTime = eventMessageChunk[0]
        val description = eventMessageChunk[1]
        val location = eventMessageChunk[2]

        // Build the notification
        // https://developer.android.com/develop/ui/views/notifications/expanded
        val builder = NotificationCompat.Builder(context,
            EVENTS_NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.streetcare_logo) // Set your notification icon
            .setContentTitle(notificationTitle)
            .setContentText(eventTitle)
            .setContentIntent(pendingIntent)
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
            notify(2, builder.build()) // You can provide a unique notification ID
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Events Update Channel"
            val descriptionText = "Firebase Events Collection Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(EVENTS_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

     */


}

sealed class ChangedType(val type: String) {
    object Add: ChangedType("add"){}
    object Modify: ChangedType("modify"){}
    object Remove: ChangedType("remove"){}
}