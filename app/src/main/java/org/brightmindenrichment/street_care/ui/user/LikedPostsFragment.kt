package org.brightmindenrichment.street_care.ui.user

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.StickyHeaderItemDecorator
import org.brightmindenrichment.street_care.ui.community.adapter.CommunityRecyclerAdapter
import org.brightmindenrichment.street_care.ui.community.StickyHeaderInterface
import org.brightmindenrichment.street_care.ui.community.data.Event
import org.brightmindenrichment.street_care.ui.community.data.EventDataAdapter
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.util.DebouncingQueryTextListener
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDayInMilliSec
import org.brightmindenrichment.street_care.util.Extensions.Companion.toPx
import org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery
import org.brightmindenrichment.street_care.util.StateAbbreviation.getStateOrProvinceAbbreviation
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*

/**
 * Fragment to display user's liked posts from the community
 */
class LikedPostsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var scope = lifecycleScope
    private val eventDataAdapter = EventDataAdapter(scope)
    private var defaultQuery = getLikedEventsQuery()
    private var userInputText = ""
    private var selectedItemPos = -1
    private var likedEventIds = emptyList<String>()
    
    // Date filtering state
    private var currentDateFilter = DateFilter.ALL
    private var currentInputText = ""

    // Search and filter components
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var spinner: Spinner
    private var menuItems = listOf<String>()

    // Bottom sheet components
    private lateinit var bottomSheetView: ScrollView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ScrollView>

    // Date filter enum
    // Note: Date filtering is limited for liked posts due to Firestore constraints
    // (cannot mix whereIn on document IDs with inequality operators on dates)
    // Currently shows all liked events up to 10 (Firestore whereIn limit)
    enum class DateFilter {
        ALL, NEXT_7_DAYS, NEXT_30_DAYS, NEXT_60_DAYS, NEXT_90_DAYS, OTHER_UPCOMING
    }
    private var selectedDateFilter = DateFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_liked_posts, container, false)
    }

    @SuppressLint("ClickableAccessibilityHint")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchAndFilter()
        setupBottomSheet()
        setupRecyclerView()
        loadLikedPosts()
    }

    override fun onResume() {
        super.onResume()
        loadLikedPosts()
    }

    private fun setupBottomSheet() {
        bottomSheetView = view!!.findViewById<ScrollView>(R.id.bottomLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val backgroundOverlay: FrameLayout = view!!.findViewById<FrameLayout>(R.id.backgroundOverlay)
        val mask = view!!.findViewById<LinearLayout>(R.id.ll_mask)

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        mask.visibility = View.VISIBLE
                        backgroundOverlay.visibility = View.VISIBLE
                    }
                    else -> {
                        mask.visibility = View.GONE
                        backgroundOverlay.visibility = View.GONE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                backgroundOverlay.visibility = View.VISIBLE
                backgroundOverlay.alpha = slideOffset
            }
        })
    }

    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewLikedPosts)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        val communityRecyclerAdapter =
            LikedPostsRecyclerAdapter(eventDataAdapter, CommunityPageName.UPCOMING_EVENTS)
        recyclerView?.adapter = communityRecyclerAdapter

        // Setup click listener for bottom sheet
        communityRecyclerAdapter.setClickListener(object : CommunityRecyclerAdapter.ClickListener {
            @SuppressLint("ResourceAsColor")
            override fun onClick(event: Event, position: Int) {
                setupBottomSheetClickHandlers(event, position, communityRecyclerAdapter, recyclerView!!)
            }
        })
    }

    private fun setupBottomSheetClickHandlers(
        event: Event,
        position: Int,
        communityRecyclerAdapter: LikedPostsRecyclerAdapter,
        recyclerView: RecyclerView
    ) {
        // Get bottom sheet UI elements
        val bsTextViewTitle: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTitle)
        val bsTextViewCommunityLocation: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityLocation)
        val bsTextViewCommunityTime: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTime)
        val bsTextViewCommunityDesc: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityDesc)
        val bsButtonShare: ImageButton = bottomSheetView.findViewById(R.id.btnShare)
        val bsButtonLike: ImageButton = bottomSheetView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = bottomSheetView.findViewById(R.id.tvLikeCount)
        val bsButtonClose: AppCompatButton = bottomSheetView.findViewById(R.id.buttonClose)

        // Share button logic
        bsButtonShare.setOnClickListener {
            // Share functionality - can be implemented later
        }

        // Like button - no click listener needed for liked posts
        // Just display the current like state and count
        bsButtonLike.setImageResource(
            if (event.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        tvLikeCount.text = event.likeCount.toString()
        
        // Make like button non-clickable to prevent crashes
        bsButtonLike.isClickable = false
        bsButtonLike.isFocusable = false

        // Close button logic
        bsButtonClose.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // Populate bottom sheet with event data
        bsTextViewTitle.text = event.title
        bsTextViewCommunityLocation.text = if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
            "${event.street}, ${event.city}, ${event.state} ${event.zipcode}"
        } else {
            event.location.orEmpty()
        }
        bsTextViewCommunityTime.text = event.time
        val eventDesc = event.description?.takeIf { it.isNotBlank() }
        bsTextViewCommunityDesc.text = eventDesc

        // Show the bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }



    private fun setupSearchAndFilter() {
        // Setup search view
        searchView = view!!.findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                userInputText = query ?: ""
                refreshEvents(eventDataAdapter, resources, defaultQuery, userInputText)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                userInputText = newText ?: ""
                refreshEvents(eventDataAdapter, resources, defaultQuery, userInputText)
                return false
            }
        })

        // Setup filter spinner
        spinner = Spinner(requireContext())
        spinner.onItemSelectedListener = this
        spinner.dropDownHorizontalOffset = (-130).toPx()
        spinner.dropDownVerticalOffset = 40.toPx()
        spinner.dropDownWidth = 180.toPx()
        spinner.background =
            ResourcesCompat.getDrawable(resources, R.drawable.filter_layer, null)
        // Add right padding so spinner doesn't stick to the edge
        spinner.setPadding(0, 0, 16.toPx(), 0)

        // Menu items
        menuItems = listOf(
            getString(R.string.select),
            getString(R.string.next_7_days),
            getString(R.string.next_30_days),
            getString(R.string.next_60_days),
            getString(R.string.next_90_days),
            getString(R.string.other_upcoming_events),
            getString(R.string.reset)
        )

        // Custom adapter so spinner text doesn't show in toolbar
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            menuItems
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = "" // hide label in toolbar
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return super.getDropDownView(position, convertView, parent)
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        setupToolbarMenu()
    }

    private fun setupToolbarMenu() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.clear()

                val itemEventsFilter = menu.add(Menu.NONE, 0, 1, "events filter").apply {
                    actionView = spinner
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun loadLikedPosts() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            showEmptyState()
            return
        }

        // Check if the user has liked any events first
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarLoading)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewLikedPosts)
        
        progressBar?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
        textView?.visibility = View.GONE

        Firebase.firestore
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDocument ->
                likedEventIds = userDocument.get("likedOutreach") as? List<String> ?: emptyList()
                
                android.util.Log.d("LikedPostsFragment", "Liked event IDs count: ${likedEventIds.size}")
                android.util.Log.d("LikedPostsFragment", "Liked event IDs: $likedEventIds")

                if (likedEventIds.isEmpty()) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                // Create query with the liked event IDs
                defaultQuery = getLikedEventsQuery(likedEventIds)
                
                // Use query-based approach with the liked event IDs
                android.util.Log.d("LikedPostsFragment", "About to refresh events with query")
                refreshEvents(
                    eventDataAdapter,
                    resources,
                    defaultQuery,
                    ""
                )

                android.util.Log.d("LikedPostsFragment", "Setting up search")
                searchEvents(
                    eventDataAdapter,
                    resources,
                    defaultQuery
                )
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("LikedPostsFragment", "Error loading user liked events", exception)
                showEmptyState()
            }
    }

    private fun refreshEvents(
        eventDataAdapter: EventDataAdapter,
        resources: Resources,
        query: Query,
        inputText: String
    ) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarLoading)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewLikedPosts)

        eventDataAdapter.refresh(
            inputText = inputText,
            query = query,
            showProgressBar = {
                progressBar?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                textView?.visibility = View.GONE
            },
            onNoResults = {
                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.GONE
                textView?.visibility = View.VISIBLE
                textView?.text = getString(R.string.no_results_were_found)
            }
        ) {
            // Success - data loaded
            android.util.Log.d("LikedPostsFragment", "Events loaded successfully, count: ${eventDataAdapter.size}")
            textView?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            recyclerView?.layoutManager = LinearLayoutManager(view?.context)

            val communityRecyclerAdapter = LikedPostsRecyclerAdapter(
                eventDataAdapter,
                CommunityPageName.UPCOMING_EVENTS
            )
            recyclerView?.adapter = communityRecyclerAdapter

            // Setup click listener for bottom sheet
            communityRecyclerAdapter.setClickListener(object : CommunityRecyclerAdapter.ClickListener {
                @SuppressLint("ResourceAsColor")
                override fun onClick(event: Event, position: Int) {
                    setupBottomSheetClickHandlers(event, position, communityRecyclerAdapter, recyclerView!!)
                }
            })

            val stickyHeaderItemDecorator = StickyHeaderItemDecorator(communityRecyclerAdapter)
            recyclerView?.addItemDecoration(stickyHeaderItemDecorator)
            
            android.util.Log.d("LikedPostsFragment", "RecyclerView setup complete")
        }
    }

    private fun refreshEventsWithDateFilter(
        eventDataAdapter: EventDataAdapter,
        resources: Resources,
        query: Query,
        inputText: String,
        dateFilter: DateFilter
    ) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarLoading)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewLikedPosts)

        progressBar?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
        textView?.visibility = View.GONE

        // Fetch all events and apply client-side filtering
        query.get()
            .addOnSuccessListener { result ->
                android.util.Log.d("LikedPostsFragment", "Successfully fetched ${result.size()} events for date filtering")

                scope.launch {
                    val filteredEvents = mutableListOf<org.brightmindenrichment.street_care.ui.community.data.CommunityData>()
                    var prevMonth: String? = null
                    var prevDay: String? = null
                    
                    for (document in result) {
                        yield()
                        var event = Event()
                        event.title = document.get("title")?.toString() ?: "Unknown"
                        event.consentBox = (document.get("consentStatus") ?: false) as Boolean
                        event.contactNumber = document.get("contactNumber")?.toString() ?: "Unknown"
                        event.email = document.get("emailAddress")?.toString() ?: "Unknown"
                        event.description = document.get("description")?.toString() ?: "Unknown"
                        event.isFlagged = (document.get("isFlagged") ?: false) as Boolean
                        event.flaggedByUser = document.get("flaggedByUser")?.toString()
                        
                        val location = document.get("location") as? Map<*, *>
                        if (location != null) {
                            val stateName = location["state"] ?: ""
                            val stateAbbr = getStateOrProvinceAbbreviation(stateName.toString())
                            event.city = location["city"]?.toString()
                            event.state = stateAbbr
                            event.street = location["street"]?.toString()
                            event.zipcode = location["zipcode"]?.toString()
                            event.location = "${location["street"]}, ${location["city"]}, $stateAbbr ${location["zipcode"]}"
                        } else {
                            event.location = "Unknown"
                        }

                        event.eventId = document.id
                        event.uid = document.get("uid").toString()
                        event.time = org.brightmindenrichment.street_care.util.Extensions.getDateTimeFromTimestamp(document.get("eventDate")).split("at ")[1]
                        
                        document.get("interests")?.let {
                            try {
                                event.interest = it.toString().toInt()
                            } catch (e: Exception) {
                                event.interest = 0
                            }
                        }

                        event.eventStartTime = document.get("eventStartTime").toString()
                        event.eventEndTime = document.get("eventEndTime").toString()
                        event.createdAt = document.get("createdAt").toString()
                        event.helpRequest = (document.get("helpRequest") as? ArrayList<String>) ?: arrayListOf()
                        event.helpType = document.get("helpType").toString()
                        event.participants = (document.get("participants") as? ArrayList<String>) ?: arrayListOf()
                        event.skills = (document.get("skills") as? ArrayList<String>) ?: arrayListOf()
                        event.approved = (document.get("approved") ?: false) as Boolean
                        event.totalSlots = document.get("totalSlots")?.toString()?.toIntOrNull()

                        val user = Firebase.auth.currentUser
                        val likes = document.get("likes") as? List<*>
                        event.likeCount = likes?.size ?: 0
                        event.likedByMe = user != null && likes?.contains(user.uid) == true

                        // Apply text search filter
                        if (!checkQuery(event, inputText)) continue

                        // Apply date filter
                        val eventDate = document.get("eventDate") as? com.google.firebase.Timestamp
                        if (eventDate != null && !isEventInDateRange(eventDate, dateFilter)) continue

                        // Process date formatting and layout (same as EventDataAdapter)
                        val date: String = document.get("eventDate")?.toString() ?: "Unknown"
                        if (date != "Unknown") {
                            val localDateTime = org.brightmindenrichment.street_care.util.Extensions.dateParser(date)
                            val month = localDateTime?.month ?: "Unknown"
                            val dayOfMonth = localDateTime?.dayOfMonth?.toString() ?: "NA"
                            val dayOfWeek = localDateTime?.dayOfWeek?.toString() ?: "NA"
                            val year = localDateTime?.year ?: "Unknown"
                            val monthName = month.toString()

                            if (dayOfWeek.length > 3) {
                                event.day = dayOfWeek.substring(0, 3)
                            }

                            event.date = dayOfMonth
                            event.year = "$monthName $year"

                            // Handle month headers and day layout (same as original EventDataAdapter)
                            if (prevMonth != null) {
                                if (!month.toString().equals(prevMonth)) {
                                    prevMonth = month.toString()
                                    event.layoutType = org.brightmindenrichment.street_care.util.Extensions.TYPE_NEW_DAY
                                    val eventYear = org.brightmindenrichment.street_care.ui.community.data.EventYear()
                                    eventYear.year = "$monthName $year"
                                    val community = org.brightmindenrichment.street_care.ui.community.data.CommunityData(eventYear, org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH)
                                    filteredEvents.add(community)
                                } else {
                                    if (dayOfMonth != prevDay) {
                                        prevDay = dayOfMonth
                                        event.layoutType = org.brightmindenrichment.street_care.util.Extensions.TYPE_NEW_DAY
                                    } else {
                                        event.layoutType = org.brightmindenrichment.street_care.util.Extensions.TYPE_DAY
                                    }
                                }
                            } else {
                                prevMonth = month.toString()
                                prevDay = dayOfMonth
                                event.layoutType = org.brightmindenrichment.street_care.util.Extensions.TYPE_NEW_DAY
                                val eventYear = org.brightmindenrichment.street_care.ui.community.data.EventYear()
                                eventYear.year = "$monthName $year"
                                val community = org.brightmindenrichment.street_care.ui.community.data.CommunityData(eventYear, org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH)
                                filteredEvents.add(community)
                            }

                            val communityEvent = org.brightmindenrichment.street_care.ui.community.data.CommunityData(event, event.layoutType!!)
                            filteredEvents.add(communityEvent)
                        }
                    }

                    android.util.Log.d("LikedPostsFragment", "Filtered events count: ${filteredEvents.size}")

                    if (filteredEvents.isNotEmpty()) {
                        textView?.visibility = View.GONE
                        progressBar?.visibility = View.GONE
                        recyclerView?.visibility = View.VISIBLE
                        recyclerView?.layoutManager = LinearLayoutManager(view?.context)

                        // Create a new EventDataAdapter and populate it with filtered events
                        val filteredEventDataAdapter = EventDataAdapter(scope)
                        
                        // We need to populate the filteredEventDataAdapter with our filtered events
                        // Since we can't access the private communityDataList, we'll use a workaround
                        // by creating a custom adapter that can handle the filtered data
                        val customAdapter = FilteredCommunityRecyclerAdapter(filteredEvents, scope)
                        recyclerView?.adapter = customAdapter

                        // Setup click listener for bottom sheet
                        customAdapter.setClickListener(object : CommunityRecyclerAdapter.ClickListener {
                            @SuppressLint("ResourceAsColor")
                            override fun onClick(event: Event, position: Int) {
                                setupBottomSheetClickHandlers(event, position, customAdapter, recyclerView!!)
                            }
                        })

                        val stickyHeaderItemDecorator = StickyHeaderItemDecorator(customAdapter)
                        recyclerView?.addItemDecoration(stickyHeaderItemDecorator)
                    } else {
                        progressBar?.visibility = View.GONE
                        recyclerView?.visibility = View.GONE
                        textView?.visibility = View.VISIBLE
                        textView?.text = getString(R.string.no_results_were_found)
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("LikedPostsFragment", "Error loading filtered events", exception)
                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.GONE
                textView?.visibility = View.VISIBLE
                textView?.text = getString(R.string.no_results_were_found)
            }
    }

    private fun checkQuery(event: Event, inputText: String): Boolean {
        val title = event.title.lowercase().trim()
        val description = event.description?.lowercase()?.trim() ?: "unknown"
        val location = event.location?.lowercase()?.trim() ?: "unknown"
        val skills = event.skills?.map { it.lowercase() } ?: emptyList()
        var checkSkills = false
        for (skill in skills) {
            if (skill.contains(inputText.lowercase().trim())) {
                checkSkills = true
                break
            }
        }
        return inputText.isEmpty() ||
                title.contains(inputText.lowercase().trim()) ||
                description.contains(inputText.lowercase().trim()) ||
                location.contains(inputText.lowercase().trim()) ||
                checkSkills
    }

    private fun isEventInDateRange(eventDate: com.google.firebase.Timestamp, dateFilter: DateFilter): Boolean {
        if (dateFilter == DateFilter.ALL) return true

        val currentTime = System.currentTimeMillis()
        val eventTime = eventDate.toDate().time

        return when (dateFilter) {
            DateFilter.NEXT_7_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(7)
                eventTime >= currentTime && eventTime <= targetDate
            }
            DateFilter.NEXT_30_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(30)
                eventTime >= currentTime && eventTime <= targetDate
            }
            DateFilter.NEXT_60_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(60)
                eventTime >= currentTime && eventTime <= targetDate
            }
            DateFilter.NEXT_90_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(90)
                eventTime >= currentTime && eventTime <= targetDate
            }
            DateFilter.OTHER_UPCOMING -> {
                val targetDate = currentTime + getDayInMilliSec(90)
                eventTime > targetDate
            }
            else -> true
        }
    }

    private fun searchEvents(
        eventDataAdapter: EventDataAdapter,
        resources: Resources,
        query: Query
    ) {
        searchView.setOnQueryTextListener(
            DebouncingQueryTextListener(lifecycle) { inputText ->
                inputText?.let {
                    userInputText = it
                    if (selectedDateFilter != DateFilter.ALL) {
                        refreshEventsWithDateFilter(
                            eventDataAdapter,
                            resources,
                            query,
                            it,
                            selectedDateFilter
                        )
                    } else {
                        refreshEvents(
                            eventDataAdapter,
                            resources,
                            query,
                            it
                        )
                    }
                }
            }
        )
    }

    private fun showEmptyState() {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarLoading)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewLikedPosts)
        
        progressBar?.visibility = View.GONE
        recyclerView?.visibility = View.GONE
        textView?.visibility = View.VISIBLE
        textView?.text = getString(R.string.no_liked_posts_yet)
    }

    private fun applyDateFilter(filter: DateFilter) {
        if (likedEventIds.isEmpty()) {
            showEmptyState()
            return
        }

        // Note: Firestore doesn't allow mixing whereIn() on document IDs with date inequality operators
        // So we fetch all liked events and filter them client-side
        selectedDateFilter = filter
        defaultQuery = getLikedEventsQuery(likedEventIds)
        refreshEventsWithDateFilter(eventDataAdapter, resources, defaultQuery, userInputText, selectedDateFilter)
        searchEvents(eventDataAdapter, resources, defaultQuery)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedItem = parent?.getItemAtPosition(position)?.toString() ?: return

                when (selectedItem) {
            getString(R.string.select) -> return
            getString(R.string.next_7_days) -> {
                selectedDateFilter = DateFilter.NEXT_7_DAYS
                applyDateFilter(selectedDateFilter)
            }
            getString(R.string.next_30_days) -> {
                selectedDateFilter = DateFilter.NEXT_30_DAYS
                applyDateFilter(selectedDateFilter)
            }
            getString(R.string.next_60_days) -> {
                selectedDateFilter = DateFilter.NEXT_60_DAYS
                applyDateFilter(selectedDateFilter)
            }
            getString(R.string.next_90_days) -> {
                selectedDateFilter = DateFilter.NEXT_90_DAYS
                applyDateFilter(selectedDateFilter)
            }
            getString(R.string.other_upcoming_events) -> {
                selectedDateFilter = DateFilter.OTHER_UPCOMING
                applyDateFilter(selectedDateFilter)
            }
            getString(R.string.reset) -> {
                selectedDateFilter = DateFilter.ALL
                userInputText = ""
                searchView.setQuery("", false)
                spinner.setSelection(0)
                defaultQuery = getLikedEventsQuery(likedEventIds)
                refreshEvents(eventDataAdapter, resources, defaultQuery, "")
                searchEvents(eventDataAdapter, resources, defaultQuery)
            }
        }

        selectedItemPos = position
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        selectedItemPos = -1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventDataAdapter.cleanupFlagStatusListeners()
    }
}

/**
 * Custom adapter that works with filtered CommunityData and maintains UI consistency
 */
class FilteredCommunityRecyclerAdapter(
    private val filteredEvents: List<org.brightmindenrichment.street_care.ui.community.data.CommunityData>,
    private val scope: kotlinx.coroutines.CoroutineScope
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {
    
    private var clickListener: CommunityRecyclerAdapter.ClickListener? = null
    
    fun setClickListener(clickListener: CommunityRecyclerAdapter.ClickListener) {
        this.clickListener = clickListener
    }
    
    override fun getItemCount(): Int = filteredEvents.size
    
    override fun getItemViewType(position: Int): Int {
        return filteredEvents[position].layoutType ?: org.brightmindenrichment.street_care.util.Extensions.TYPE_DAY
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH -> {
                val view = inflater.inflate(R.layout.card_community_year, parent, false)
                YearViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.event_list_layout, parent, false)
                EventViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val communityData = filteredEvents[position]
        
        when (holder) {
            is YearViewHolder -> {
                communityData.eventYear?.let { eventYear ->
                    holder.bind(eventYear.year)
                }
            }
            is EventViewHolder -> {
                communityData.event?.let { event ->
                    holder.bind(event, position)
                    holder.itemView.setOnClickListener {
                        clickListener?.onClick(event, position)
                    }
                }
            }
        }
    }
    
    // StickyHeaderInterface implementation
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
        header?.let {
            it.findViewById<TextView>(R.id.textViewCommunityYear).text = filteredEvents[headerPosition].eventYear?.year
        }
    }
    
    override fun isHeader(itemPosition: Int): Boolean {
        return filteredEvents[itemPosition].layoutType == org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH
    }
    
    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewYear: TextView = itemView.findViewById(R.id.textViewCommunityYear)
        
        fun bind(year: String?) {
            textViewYear.text = year ?: ""
        }
    }
    
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewCommunityTitle)
        private val textViewLocation: TextView = itemView.findViewById(R.id.textViewCommunityLocation)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewCommunityTime)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
        
        fun bind(event: Event, position: Int) {
            textViewTitle.text = event.title
            textViewLocation.text = if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
                "${event.city}, ${event.state}"
            } else {
                event.location ?: ""
            }
            textViewTime.text = event.time ?: ""
            textViewDate.text = event.date ?: ""
            textViewDay.text = event.day ?: ""
        }
    }
}

/**
 * Custom adapter for liked posts that disables like button functionality
 */
class LikedPostsRecyclerAdapter(
    private val eventDataAdapter: EventDataAdapter,
    private val pageName: CommunityPageName
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {
    
    private var clickListener: CommunityRecyclerAdapter.ClickListener? = null
    
    fun setClickListener(clickListener: CommunityRecyclerAdapter.ClickListener) {
        this.clickListener = clickListener
    }
    
    override fun getItemCount(): Int = eventDataAdapter.size
    
    override fun getItemViewType(position: Int): Int {
        return eventDataAdapter.getEventAtPosition(position)?.layoutType ?: org.brightmindenrichment.street_care.util.Extensions.TYPE_DAY
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH -> {
                val view = inflater.inflate(R.layout.card_community_year, parent, false)
                YearViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.event_list_layout, parent, false)
                EventViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val communityData = eventDataAdapter.getEventAtPosition(position)
        
        when (holder) {
            is YearViewHolder -> {
                communityData?.eventYear?.let { eventYear ->
                    holder.bind(eventYear.year)
                }
            }
            is EventViewHolder -> {
                communityData?.event?.let { event ->
                    holder.bind(event, position)
                    holder.itemView.setOnClickListener {
                        clickListener?.onClick(event, position)
                    }
                }
            }
        }
    }
    
    // StickyHeaderInterface implementation
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
        header?.let {
            it.findViewById<TextView>(R.id.textViewCommunityYear).text = eventDataAdapter.getEventAtPosition(headerPosition)?.eventYear?.year
        }
    }
    
    override fun isHeader(itemPosition: Int): Boolean {
        return eventDataAdapter.getEventAtPosition(itemPosition)?.layoutType == org.brightmindenrichment.street_care.util.Extensions.TYPE_MONTH
    }
    
    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewYear: TextView = itemView.findViewById(R.id.textViewCommunityYear)
        
        fun bind(year: String?) {
            textViewYear.text = year ?: ""
        }
    }
    
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewCommunityTitle)
        private val textViewLocation: TextView = itemView.findViewById(R.id.textViewCommunityLocation)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewCommunityTime)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        
        fun bind(event: Event, position: Int) {
            textViewTitle.text = event.title
            textViewLocation.text = if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
                "${event.city}, ${event.state}"
            } else {
                event.location ?: ""
            }
            textViewTime.text = event.time ?: ""
            textViewDate.text = event.date ?: ""
            textViewDay.text = event.day ?: ""
            
            // Set like button state but disable clicking
            btnLike.setImageResource(
                if (event.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            tvLikeCount.text = event.likeCount.toString()
            
            // Disable like button click listener for liked posts
            btnLike.isClickable = false
            btnLike.isFocusable = false
        }
    }
}


