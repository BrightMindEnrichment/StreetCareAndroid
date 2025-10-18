package org.brightmindenrichment.street_care

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.data.local.EventsDatabase
import org.brightmindenrichment.street_care.databinding.ActivityMainBinding
import org.brightmindenrichment.street_care.notification.NotificationWorker
import org.brightmindenrichment.street_care.ui.community.model.DatabaseEvent
import org.brightmindenrichment.street_care.ui.user.UserSingleton
import org.brightmindenrichment.street_care.ui.user.UserRepository
import org.brightmindenrichment.street_care.ui.visit.InteractionLogDialog
import org.brightmindenrichment.street_care.util.Constants.NOTIFICATION_WORKER
import org.brightmindenrichment.street_care.util.DataStoreManager
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.addSnapshotListenerToCollection
import org.brightmindenrichment.street_care.util.Extensions.Companion.askPermission
import org.brightmindenrichment.street_care.util.Extensions.Companion.createHelpRequestsData
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDateTimeFromTimestamp
import org.brightmindenrichment.street_care.util.Extensions.Companion.updateFieldInExistingCollection
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavView : BottomNavigationView
    private lateinit var listenerRegistration: ListenerRegistration
    private lateinit var workManager: WorkManager
    private lateinit var dataStoreManager: DataStoreManager

    private val db = Firebase.firestore

    @Inject
    lateinit var eventsDatabase: EventsDatabase

    private val scope = lifecycleScope

    // Use 'hasInitialized' to avoid receiving notifications when the user first opens the app.
    //private var hasInitialized = false
    //private val eventsMap: MutableMap<String, Event> = mutableMapOf() // <id : Event>

    companion object {
        private const val TAG = "MainActivity" // Provide a meaningful tag
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUI()


        /*
        // Create a New Collection From Existing Collection
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("syncWebApp", "start adding documents to collection")
            // from "pastOutreachEvents" collection
            Extensions.createNewCollectionFromExistingCollection(
                db = db,
                createData = {doc ->
                    Extensions.createHelpRequestsData(doc)
                },
                existingCollection = "helpRequests",
                newCollection = "helpRequests"
            )

        }

         */


        Log.d("workManager", "onCreate")

//        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//               this@MainActivity.moveTaskToBack(true)
//            }
//        })

        dataStoreManager = DataStoreManager(this)

        scope.launch(IO) {
            val isInitialized = dataStoreManager.getRoomDBIsInitialized().first()
            Log.d("workManager", "isInitialized: $isInitialized")
            if(!isInitialized) {
                val query = db.collection("events")
                    .orderBy("date", Query.Direction.DESCENDING)
                launch(IO) {
                    syncFirebaseToRoomDB(query)
                }
            }
        }

        scope.launch(IO) {
            val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)

            workManager = WorkManager.getInstance(applicationContext)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1L, TimeUnit.HOURS)
                .setInitialDelay(1L, TimeUnit.HOURS)
                //.setBackoffCriteria(BackoffPolicy.LINEAR, 1L, TimeUnit.HOURS)
                //.setConstraints(constraints)
                .build()
            /*
            val oneTimeWorkRequest1 = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(1L, TimeUnit.MINUTES)
                .build()

            val oneTimeWorkRequest2 = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(3L, TimeUnit.MINUTES)
                .build()

             */

            // check if the events in firebase have been updated (added, modified, or removed)
            // one time work request
            //addWorkRequestToWorkManager(oneTimeWorkRequest1)
            //addWorkRequestToWorkManager(oneTimeWorkRequest2)

            // Create a periodic task triggered at specific intervals
            // to make sure active users get a notification even when the app is inactive or killed.
            addPeriodicWorkRequestToWorkManager(periodicWorkRequest)
        }
        // ask POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askPermission(
                context = this,
                permission = Manifest.permission.POST_NOTIFICATIONS,
                activity = this,
                requestPermissionLauncher = requestPermissionLauncher,
                title = "Street Care requires notification permission",
                message = "You won't receive a notification because the permission is denied",
            )
        }
        // it will be triggered if there is a change in the "events" collection
        //addSnapshotListenerToCollection(db.collection("events"))

        FirebaseMessaging.getInstance().subscribeToTopic("events")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully subscribed to FCM topic")
                } else {
                    Log.e(TAG, "Error subscribing to FCM topic", task.exception)
                }
            }

        /* FirebaseMessaging.getInstance().token.addOnCompleteListener{ task ->
             if (!task.isSuccessful) {
                 Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                 return@addOnCompleteListener
             }

             // Get new FCM registration token
             val token = task.result

             // Log and toast
             Log.d(TAG, "token: $token")
         }*/
     }

    private fun initUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        //val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavView.itemIconTintList = null
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.loginRedirectFragment, R.id.nav_community, R.id.nav_user, R.id.nav_interaction_log
            )
        )


        setupActionBarWithNavController(navController, appBarConfiguration)

      //  bottomNavView.setupWithNavController(navController)
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    true
                }

                R.id.loginRedirectFragment -> {
                    if (Firebase.auth.currentUser != null) {
                        navController.navigate(R.id.nav_interaction_log)
                    } else {
                        navController.navigate(R.id.loginVisitLogFragment)
                    }
                    true
                }

                R.id.nav_community -> {
                    navController.navigate(R.id.nav_community)
                    true
                }

                R.id.profile -> {
                    navController.navigate(R.id.profile)
                    true
                }

                else -> false
            }

        }



        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home,R.id.loginVisitLogFragment,R.id.loginRedirectFragment, R.id.nav_visit, R.id.nav_community, R.id.nav_profile, R.id.nav_interaction_log -> {
                    bottomNavView.visibility = View.VISIBLE
                }
                else -> {
                    bottomNavView.visibility = View.GONE
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("workManager", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("workManager", "onResume, add listenerRegistration")
        scope.launch(IO) {
            dataStoreManager.setIsAppOnBackground(false)
            listenerRegistration = addSnapshotListenerToCollection(
                db.collection("events"),
                eventsDatabase,
                applicationContext,
                scope,
                dataStoreManager,
                false,
            )
            Log.d("workManager", "onResume, is initialized, ${this@MainActivity::listenerRegistration.isInitialized}.")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("workManager", "onPause, remove listenerRegistration")
        Log.d("workManager", "onPause, is initialized, ${this::listenerRegistration.isInitialized}.")
        scope.launch(IO) {
            dataStoreManager.setIsAppOnBackground(true)
            if(this@MainActivity::listenerRegistration.isInitialized) listenerRegistration.remove()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("workManager", "onStop")
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("workManager", "onDestroy")
        //listenerRegistration.remove()
    }


    private suspend fun syncFirebaseToRoomDB(query: Query) {
        Log.d("workManager", "syncFirebaseToRoomDB")
        val databaseEvents = mutableListOf<DatabaseEvent>()
        query.get().addOnSuccessListener { result ->
            scope.launch(IO) {
                for (document in result) {
                    val databaseEvent = DatabaseEvent(
                        id = document.id,
                        date = getDateTimeFromTimestamp(document.get("date")),
                        description = document.get("description")?.toString() ?: "Unknown Description",
                        interest = (document.get("interest") ?: 0) as? Int,
                        location = document.get("location")?.toString() ?: "Unknown Location",
                        status = document.get("status")?.toString() ?: "Unknown Status",
                        title = document.get("title")?.toString() ?: "Unknown Title",
                    )
                    databaseEvents.add(databaseEvent)
                }

                eventsDatabase.eventDao().addEvents(databaseEvents)
                dataStoreManager.setRoomDBIsInitialized(true)
                //Log.d("workManager", "eventsDatabase size: ${eventsDatabase.eventDao().getAllEvents().size}")
            }
        }.addOnFailureListener { exception ->
            Log.d("workManager", "initialize events database failed: $exception")
        }
    }



    private fun addWorkRequestToWorkManager(workRequest: WorkRequest) {
        Log.d("workManager", "addWorkRequestToWorkManager...")
        workManager.enqueue(workRequest)
    }

    private suspend fun addPeriodicWorkRequestToWorkManager(periodicWorkRequest: PeriodicWorkRequest) {
        Log.d("workManager", "addPeriodicWorkRequestToWorkManager...")
        workManager.enqueueUniquePeriodicWork(NOTIFICATION_WORKER, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest)
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return if (shouldInterceptUpNavigation()) {
            handleCustomUpNavigation(navController)
            true // Return true to indicate that you've handled the navigation
        } else {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }
    }

    private fun shouldInterceptUpNavigation(): Boolean {
        val isAuthUser = Firebase.auth.currentUser != null
        val isNavUserOnNavGraph =
            findNavController(R.id.nav_host_fragment_content_main).currentDestination?.id == R.id.nav_profile
        return isAuthUser && isNavUserOnNavGraph
    }

    private fun handleCustomUpNavigation(navController: NavController) {
        navController.popBackStack(R.id.nav_home, false)
    }

    /*
    private fun addSnapshotListenerToCollection(collectionRef: CollectionReference) {
        listenerRegistration = collectionRef
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("firebase", "Listen failed.", e)
                    return@addSnapshotListener
                }
                Log.d("firebase", "hasInitialized: $hasInitialized")
                if(hasInitialized) {
                    for (dc in snapshots!!.documentChanges) {
                        var eventTitle = dc.document.data["title"].toString() ?: "Unknown Event"
                        var timestamp = dc.document.data["date"]
                        var eventDescription = dc.document.data["description"].toString() ?: "No Description"
                        var eventLocation = dc.document.data["location"].toString() ?: "No Location"
                        //Log.d("firebase", "timestamp: $timestamp")
                        var eventMessage = "${getDateTimeFromTimestamp(timestamp)}/$eventDescription/$eventLocation"
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                eventsMap[dc.document.id] = createEvent(dc)
                                val notificationTitle = "New Event Added"
                                showNotification(
                                    notificationTitle,
                                    eventTitle,
                                    eventMessage,
                                    applicationContext,
                                    ChangedType.Add.type,
                                    dc.document.id
                                )
                                Log.d("firebase", "New event added: ${dc.document.data}")
                            }
                            DocumentChange.Type.MODIFIED -> {
                                // only show the notification when title, location, date, description, status changed.
                                val originalEvent = eventsMap[dc.document.id]
                                var shouldShowNotification = false
                                var notificationTitle = "Event Modified"
                                originalEvent?.let {
                                    if(originalEvent.timestamp != getDateTimeFromTimestamp(dc.document.data["date"])) {
                                        Log.d("modify", "originalEvent.timestamp: ${originalEvent.timestamp}")
                                        Log.d("modify", "new timestamp: ${getDateTimeFromTimestamp(dc.document.data["date"])}")
                                        originalEvent.timestamp = getDateTimeFromTimestamp(dc.document.data["date"])
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

                                    eventTitle = originalEvent.title
                                    eventDescription = originalEvent.description!!
                                    eventLocation = originalEvent.location!!
                                    eventMessage = "${originalEvent.timestamp}/$eventDescription/$eventLocation"
                                }

                                if(shouldShowNotification) showNotification(
                                    notificationTitle,
                                    eventTitle,
                                    eventMessage,
                                    applicationContext,
                                    ChangedType.Modify.type,
                                    dc.document.id
                                )
                                Log.d("firebase", "Modified event: ${dc.document.data}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                eventsMap.remove(dc.document.id)
                                val notificationTitle = "Event removed"
                                showNotification(
                                    notificationTitle,
                                    eventTitle,
                                    eventMessage,
                                    applicationContext,
                                    ChangedType.Remove.type,
                                    dc.document.id
                                )
                                Log.d("firebase", "Removed event: ${dc.document.data}")
                            }
                        }
                    }
                }
                else {
                    // initialize the eventsMap
                    for (dc in snapshots!!.documentChanges) {
                        eventsMap[dc.document.id] = createEvent(dc)
                    }
                    Log.d("notification_navigation", "eventsMapSize: ${eventsMap.size}")
                    Log.d("notification_navigation", "eventsMap: $eventsMap")

                }


                hasInitialized = true
            }
    }

    private fun createEvent( dc: DocumentChange): Event {
        val event = Event()
        event.timestamp = getDateTimeFromTimestamp(dc.document.data["date"])
        event.description = dc.document.data["description"] as? String
        event.interest = ((dc.document.data["interest"]?:0) as Long).toInt()
        event.location = dc.document.data["location"] as? String
        event.status = dc.document.data["status"] as? String
        event.title = dc.document.data["title"] as String
        return event
    }

    private fun createPendingIntent(
        changedType: String,
        eventId: String,
        eventTitle: String
    ): PendingIntent {
        val resultPendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.nav_community)
            .setDestination(R.id.communityEventFragment)
            .setArguments(bundleOf(
                "changedType" to changedType,
                "eventId" to eventId,
                "eventTitle" to eventTitle
            ))
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
        return resultPendingIntent
    }

    private fun showNotification(
        notificationTitle: String,
        eventTitle: String,
        eventMessage: String,
        context: Context,
        changedType: String,
        eventId: String,
    ) {

        // create pendingIntent to redirect to the event fragment when users click the notification
        val pendingIntent = createPendingIntent(changedType, eventId, eventTitle)

        // Create a notification channel (required for Android Oreo and above)
        createNotificationChannel(context)
        val eventMessageChunk = eventMessage.split("/")
        val dateAndTime = eventMessageChunk[0]
        val description = eventMessageChunk[1]
        val location = eventMessageChunk[2]

        // Build the notification
        // https://developer.android.com/develop/ui/views/notifications/expanded
        val builder = NotificationCompat.Builder(context, EVENTS_NOTIFICATION_CHANNEL_ID)
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
/*
sealed class ChangedType(val type: String) {
    object Add: ChangedType("add"){}
    object Modify: ChangedType("modify"){}
    object Remove: ChangedType("remove"){}
}

 */