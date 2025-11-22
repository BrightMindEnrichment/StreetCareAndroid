package org.brightmindenrichment.street_care.ui.community

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.notification.ChangedType
import org.brightmindenrichment.street_care.ui.community.adapter.CommunityRecyclerAdapter
import org.brightmindenrichment.street_care.ui.community.data.CommunityData
import org.brightmindenrichment.street_care.ui.community.data.Event
import org.brightmindenrichment.street_care.ui.community.data.EventDataAdapter
import org.brightmindenrichment.street_care.ui.community.data.EventYear
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.ui.community.StickyHeaderItemDecorator
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDateTimeFromTimestamp
import org.brightmindenrichment.street_care.util.Extensions.Companion.dateParser
import org.brightmindenrichment.street_care.util.StateAbbreviation.getStateOrProvinceAbbreviation
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.brightmindenrichment.street_care.ui.user.getUserType
import org.brightmindenrichment.street_care.ui.user.verificationMark
import org.brightmindenrichment.street_care.util.DebouncingQueryTextListener
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.customGetSerializable
import org.brightmindenrichment.street_care.util.Extensions.Companion.getDayInMilliSec
import org.brightmindenrichment.street_care.util.Extensions.Companion.refreshNumOfInterest
import org.brightmindenrichment.street_care.util.Extensions.Companion.replaceButtonInterest
import org.brightmindenrichment.street_care.util.Extensions.Companion.setButtonInterest
import org.brightmindenrichment.street_care.util.Extensions.Companion.setRSVPButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setVerifiedAndRegistered
import org.brightmindenrichment.street_care.util.Extensions.Companion.toPx
import org.brightmindenrichment.street_care.util.Queries.getHelpRequestEventsQuery
import org.brightmindenrichment.street_care.util.Queries.getPastEventsQuery
import org.brightmindenrichment.street_care.util.Queries.getQueryToFilterEventsBeforeTargetDate
import org.brightmindenrichment.street_care.util.Queries.getQueryToFilterEventsAfterTargetDate
import org.brightmindenrichment.street_care.util.Queries.getQueryToFilterEventsByType
import org.brightmindenrichment.street_care.util.Queries.getUpcomingEventsQuery
import org.brightmindenrichment.street_care.util.Share
import org.brightmindenrichment.street_care.util.showLoginDialog
import java.util.Date


class CommunityEventFragment : Fragment(), AdapterView.OnItemSelectedListener {

    lateinit var buttonAdd: ImageButton
    private var scope = lifecycleScope
    private val eventDataAdapter = EventDataAdapter(scope)

    private var userInputText = ""
    private var selectedItemPos = -1
    //private var isPastEvents = true
    private var defaultQuery = getPastEventsQuery()
    private var communityPageName = CommunityPageName.UPCOMING_EVENTS
    private var helpRequestId: String? = null
    private var likedEventIds: List<String> = emptyList()
    
    // Date filter for liked events (client-side filtering)
    private enum class LikedDateFilter {
        ALL,
        LAST_7_DAYS,
        LAST_30_DAYS,
        NEXT_7_DAYS,
        NEXT_30_DAYS
    }
    private var likedDateFilter = LikedDateFilter.ALL

    //private lateinit var fragmentCommunityEventView: View
    private lateinit var bottomSheetView: ScrollView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ScrollView>
    private lateinit var searchView: SearchView
    private lateinit var menuItems: List<String>

    override fun onDestroy() {
        super.onDestroy()
        Log.d("syncWebApp", "Community Event Fragment onDestroy...")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("syncWebApp", "Community Event Fragment onCreate...")
        //Log.d("syncWebApp", "before, isPastEvents: $isPastEvents")
        arguments?.let {
            //isPastEvents = it.getBoolean("isPastEvents")
            it.customGetSerializable<CommunityPageName>("communityPageName")?.let{ name ->
                communityPageName = name
            }
            helpRequestId = it.getString("helpRequestId")
            likedEventIds = it.getStringArrayList("likedEventIds") ?: emptyList()
        }
        //Log.d("syncWebApp", "after, isPastEvents: $isPastEvents")

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(communityPageName == CommunityPageName.HELP_REQUESTS) {
                    // CHANGED: Navigate to publicEvent instead of communityHelpRequestFragment
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.publicEvent)
                }
                else {
                    findNavController().popBackStack()
                }
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d(ContentValues.TAG, "Community onCreateView")
        return inflater.inflate(R.layout.fragment_community_event, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCommunity)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter (using an empty controller initially if needed)
        var communityRecyclerAdapter =
            CommunityRecyclerAdapter(eventDataAdapter, communityPageName)
        recyclerView.adapter = communityRecyclerAdapter
        //fragmentCommunityEventView = view
        //if(!isPastEvents) defaultQuery = getUpcomingEventsQuery()
        when (communityPageName) {
            CommunityPageName.PAST_EVENTS -> {
                defaultQuery = getPastEventsQuery()
                setupEventsWithQuery(defaultQuery)
            }

            CommunityPageName.UPCOMING_EVENTS -> {
                defaultQuery = getUpcomingEventsQuery()
                setupEventsWithQuery(defaultQuery)
            }

            CommunityPageName.HELP_REQUESTS -> {
                defaultQuery = getHelpRequestEventsQuery(helpRequestId = helpRequestId!!)
                setupEventsWithQuery(defaultQuery)
            }

            CommunityPageName.LIKED_EVENTS -> {
                // If likedEventIds is empty, fetch from user document
                if (likedEventIds.isEmpty()) {
                    val currentUser = Firebase.auth.currentUser
                    if (currentUser == null) {
                        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
                        val tv = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
                        val rv = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)
                        progressBar?.visibility = View.GONE
                        rv?.visibility = View.GONE
                        tv?.visibility = View.VISIBLE
                        tv?.text = getString(R.string.no_liked_posts_yet)
                        return@onViewCreated
                    }
                    
                    val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
                    progressBar?.visibility = View.VISIBLE
                    
                    Firebase.firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { userDocument ->
                            val fetchedLikedIds = userDocument.get("likedOutreach") as? List<String> ?: emptyList()
                            likedEventIds = fetchedLikedIds
                            
                            if (fetchedLikedIds.isEmpty()) {
                                progressBar?.visibility = View.GONE
                                val tv = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
                                val rv = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)
                                rv?.visibility = View.GONE
                                tv?.visibility = View.VISIBLE
                                tv?.text = getString(R.string.no_liked_posts_yet)
                            } else {
                                defaultQuery = org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery(
                                    likedEventIds
                                )
                                setupEventsWithQuery(defaultQuery)
                            }
                        }
                        .addOnFailureListener {
                            progressBar?.visibility = View.GONE
                            val tv = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
                            val rv = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)
                            rv?.visibility = View.GONE
                            tv?.visibility = View.VISIBLE
                            tv?.text = getString(R.string.no_liked_posts_yet)
                        }
                } else {
                    defaultQuery = org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery(
                        likedEventIds
                    )
                    setupEventsWithQuery(defaultQuery)
                }
            }
        }

//        val pageTitle = if(isPastEvents) "Past Events" else "Upcoming Events"
//        findNavController().currentDestination?.label = pageTitle

        val menuHost: MenuHost = requireActivity()
        Log.d("notification", "associated activity: $menuHost")
        searchView = view.findViewById(R.id.search_view)
        //if(communityPageName == CommunityPageName.HELP_REQUESTS) searchView.visibility = View.GONE
        val spinner: Spinner = view.findViewById(R.id.events_filter)
        spinner.onItemSelectedListener = this
        spinner.dropDownHorizontalOffset = (-130).toPx()
        spinner.dropDownVerticalOffset = 40.toPx()
        spinner.dropDownWidth = 180.toPx()
        when(communityPageName) {
            CommunityPageName.PAST_EVENTS -> {
                menuItems = listOf(
                    getString(R.string.select),
                    getString(R.string.last_7_days),
                    getString(R.string.last_30_days),
                    getString(R.string.last_60_days),
                    getString(R.string.last_90_days),
                    getString(R.string.other_past_events),
                    getString(R.string.reset)
                )
            }
            CommunityPageName.UPCOMING_EVENTS -> {
                menuItems = listOf(
                    getString(R.string.select),
                    getString(R.string.next_7_days),
                    getString(R.string.next_30_days),
                    getString(R.string.next_60_days),
                    getString(R.string.next_90_days),
                    getString(R.string.other_upcoming_events),
                    getString(R.string.reset)
                )
            }
            CommunityPageName.LIKED_EVENTS -> {
                menuItems = listOf(
                    getString(R.string.select),
                    getString(R.string.last_7_days),
                    getString(R.string.last_30_days),
                    getString(R.string.next_7_days),
                    getString(R.string.next_30_days),
                    getString(R.string.reset)
                )
            }
            else -> menuItems = emptyList()
        }
        /*
        if(isPastEvents) {
            menuItems = listOf(
                "Select...",
                "Last 7 days",
                "Last 30 days",
                "Last 60 days",
                "Last 90 days",
                "Other past events",
                "Reset"
            )
        }
        else {
            menuItems = listOf(
                "Select...",
                "Next 7 days",
                "Next 30 days",
                "Next 60 days",
                "Next 90 days",
                "Other upcoming events",
                "Reset"
            )
        }
         */

        val dataAdapter: ArrayAdapter<String> =
            object : ArrayAdapter<String>(this.requireContext(), android.R.layout.simple_spinner_item, menuItems) {
                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    //var v: View? = null
                    val v = super.getDropDownView(position, null, parent)
                    // If this is the selected item position
                    if (position == selectedItemPos && selectedItemPos != 0 && selectedItemPos != menuItems.size - 1) {
                        v.setBackgroundColor(resources.getColor(R.color.item_selected_black, null))
                    } else {
                        // for other views
                        v.setBackgroundColor(Color.WHITE)
                    }
                    return v
                }
            }
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = dataAdapter

        /*
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            this.requireContext()!!,
            R.array.events_filter,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinner.adapter = adapter
        }

         */

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
                super.onPrepareMenu(menu)
                when(communityPageName) {
                    CommunityPageName.UPCOMING_EVENTS -> {
                        val itemEventsFilter = menu.add(Menu.NONE, 0, 0, "events filter").apply {
                            //setIcon(R.drawable.filter_layer)
                            actionView = spinner
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

                        val itemAddNew = menu.add(Menu.NONE, 1, 1, "add new").apply {
                            setIcon(R.drawable.ic_menu_add)
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

                        Log.d("filter", "itemEventsFilterId: " + itemEventsFilter.itemId)
                        Log.d("filter", "itemAddNewId: " + itemAddNew.itemId)
                    }

                    CommunityPageName.PAST_EVENTS -> {

//                        val itemAddNew = menu.add(Menu.NONE, 1, 0, "add new").apply {
//                            setIcon(R.drawable.ic_menu_add)
//                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//                        }
                        spinner.background = ResourcesCompat.getDrawable(resources, R.drawable.filter_layer_past_events, null)
                        spinner.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
                        val itemEventsFilter = menu.add(Menu.NONE, 0, 1, "events filter").apply {
                            //setIcon(R.drawable.filter_layer)
                            actionView = spinner
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

//                        Log.d("filter", "itemEventsFilterId: " + itemEventsFilter.itemId)
//                        Log.d("filter", "itemAddNewId: " + itemAddNew.itemId)
                    }

                    CommunityPageName.LIKED_EVENTS -> {
                        // Same spinner behavior as UPCOMING_EVENTS, but no "add new" button
                        spinner.background = ResourcesCompat.getDrawable(resources, R.drawable.filter_layer, null)

                        val itemEventsFilter = menu.add(Menu.NONE, 0, 0, "events filter").apply {
                            actionView = spinner
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }
                    }
                    else -> Unit
                }


            }
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                Log.d("menuItem.isVisible", "menuItem.isVisible: " + menuItem.itemId)
                Log.d("filter", "Selected Menu Item: ${menuItem.title}, id: ${menuItem.itemId}")

                when(menuItem.itemId) {
                    0 -> {

                    }
                    1-> {
                        if(Firebase.auth.currentUser!=null) {
                            findNavController().popBackStack()
                            findNavController().navigate(R.id.nav_add_event, Bundle().apply {
                                //putBoolean("isPastEvents", isPastEvents)
                                putSerializable("communityPageName", communityPageName)
                            })
                        } else{
                            Extensions.showDialog(
                                requireContext(),
                                requireContext().getString(R.string.alert),
                                requireContext().getString(R.string.events_can_only_be_logged_by_logged_in_users),
                                requireContext().getString(R.string.ok),
                                requireContext().getString(R.string.cancel)
                            )
                        }

                    }
                    else -> {
                        if(communityPageName == CommunityPageName.HELP_REQUESTS) {
                            // CHANGED: Navigate to publicEvent instead of communityHelpRequestFragment
                            findNavController().popBackStack()
                            findNavController().navigate(R.id.publicEvent)
                        }
                        else requireActivity().onBackPressedDispatcher.onBackPressed()
                        //requireActivity().onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)


        /*val community_toolbar: Toolbar = view.findViewById<Toolbar>(R.id.community_toolbar)
        community_toolbar.inflateMenu(R.menu.community_toolbar_menu)


        val addButton: LinearLayout = community_toolbar.findViewById<LinearLayout>(R.id.action_button)
        addButton.setOnClickListener {
            findNavController().navigate(R.id.nav_add_event)
        }*/

        setUpSearchView(searchView)

        bottomSheetView = view.findViewById<ScrollView>(R.id.bottomLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val backgroundOverlay: FrameLayout = view.findViewById<FrameLayout>(R.id.backgroundOverlay)
        val mask = view.findViewById<LinearLayout>(R.id.ll_mask)

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


    private fun setupEventsWithQuery(query: Query) {
        refreshEvents(
            eventDataAdapter,
            this@CommunityEventFragment.resources,
            query,
            ""
        ) {
            eventDataAdapter.setupFlagStatusListeners { updatedEvent ->
                // triggered when a flag status changes in real-time
                val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)
                val adapter = recyclerView?.adapter as? CommunityRecyclerAdapter

                adapter?.let { communityAdapter ->
                    // position of the updated event
                    val position = communityAdapter.getItemPosition(updatedEvent.eventId)

                    // Update the item in the RecyclerView
                    position?.let {
                        communityAdapter.notifyItemChanged(it)

                        // update the bottom sheet event too
                        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                            val bottomSheetEvent = adapter.getCurrentBottomSheetEvent()
                            if (bottomSheetEvent?.eventId == updatedEvent.eventId) {
                                val ivFlag: ImageView = bottomSheetView.findViewById(R.id.ivFlag)
                                ivFlag.post {
                                    ivFlag.setColorFilter(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            if (updatedEvent.isFlagged == true) R.color.red else R.color.gray
                                        )
                                    )
                                }
                                val bsRelativeLayoutImage: RelativeLayout = bottomSheetView.findViewById(R.id.relativeLayoutImage)
                                val bsTextInterested: TextView = bottomSheetView.findViewById(R.id.textInterested)
                                val bsButtonRSVP: AppCompatButton = bottomSheetView.findViewById(R.id.btnRSVP)
                                val bsButtonInterested: AppCompatButton = bottomSheetView.findViewById(R.id.buttonInterested)
                                val bsTextViewEventStatus: TextView = bottomSheetView.findViewById(R.id.tvEventStatus)
                                val bsLinearLayoutVerified: LinearLayout = bottomSheetView.findViewById(R.id.llVerifiedAndRegistered)
                                val bsLinearLayoutVerifiedAndIcon: LinearLayout = bottomSheetView.findViewById(R.id.llVerifiedAndIcon)
                                val bsTextViewRegistered: TextView = bottomSheetView.findViewById(R.id.tvRegistered)
                                val isPastEvents = communityPageName == CommunityPageName.PAST_EVENTS
                                val bsFlexboxLayoutSkills: FlexboxLayout = bottomSheetView.findViewById(R.id.flSkills)
                                val bsButtonLiked: ImageButton = bottomSheetView.findViewById(R.id.btnLike)
                                val tvLikeCount: TextView = bottomSheetView.findViewById(R.id.tvLikeCount)

                                refreshBottomSheet(
                                    updatedEvent,
                                    bsRelativeLayoutImage,
                                    bsTextInterested,
                                    bsButtonRSVP,
                                    bsButtonInterested,
                                    bsTextViewEventStatus,
                                    bsLinearLayoutVerified,
                                    bsLinearLayoutVerifiedAndIcon,
                                    bsTextViewRegistered,
                                    isPastEvents,
                                    bsFlexboxLayoutSkills,
                                    bsButtonLiked,
                                    tvLikeCount
                                )
                            }
                        }
                    }
                }
            }
            searchEvents(
                eventDataAdapter,
                this@CommunityEventFragment.resources,
                query
            )
        }
    }

    private fun createTextView(text: String): TextView {
        val textView = TextView(context)
        //setting height and width
        textView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textView.text = text
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        textView.setTextColor(Color.GRAY)
        textView.setPadding(20, 20, 20, 20)
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.isAllCaps=false
        return textView
    }

    private fun setUpSearchView(searchView: SearchView) {
        searchView.setIconifiedByDefault(false)
        searchView.isSubmitButtonEnabled = true
        searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchView.queryHint = getString(R.string.search)

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
                    // For LIKED_EVENTS, re-apply client-side filter with search text
                    if (communityPageName == CommunityPageName.LIKED_EVENTS && likedDateFilter != LikedDateFilter.ALL) {
                        applyLikedEventsFilter(likedDateFilter)
                    } else {
                        requestQuery(
                            it,
                            eventDataAdapter,
                            view,
                            bottomSheetView,
                            bottomSheetBehavior,
                            resources,
                            query
                        )
                    }
                }
            }
        )

        /*
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(inputText: String?): Boolean {
                requestQuery(
                    inputText,
                    eventDataAdapter,
                    view,
                    bottomSheetView,
                    bottomSheetBehavior,
                    resources
                )
                return false
            }

            override fun onQueryTextChange(inputText: String?): Boolean {
                Log.d("query", "textChanged: $inputText")
                requestQuery(
                    inputText,
                    eventDataAdapter,
                    view,
                    bottomSheetView,
                    bottomSheetBehavior,
                    resources
                )
                return false
            }
        })

         */

    }

    private fun requestQuery(
        inputText: String,
        eventDataAdapter: EventDataAdapter,
        view: View?,
        bottomSheetView: ScrollView,
        bottomSheetBehavior: BottomSheetBehavior<ScrollView>,
        resources: Resources,
        query: Query
    ) {
//            val query = if(it.isNotEmpty()) Firebase.firestore
//                .collection("events")
//                .where(Filter.or(
//                    Filter.equalTo("description", it),
//                    Filter.equalTo("location", it),
//                    Filter.equalTo("title", it),
//                ))
//                .orderBy("date", Query.Direction.DESCENDING)
//            else defaultQuery

        refreshEvents(
            eventDataAdapter = eventDataAdapter,
            resources = resources,
            query = query,
            inputText = inputText
        )
    }


    private fun refreshEvents(
        eventDataAdapter: EventDataAdapter,
        resources: Resources,
        query: Query,
        inputText: String,
        onComplete: (() -> Unit)? = null
    ) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        eventDataAdapter.refresh(
            inputText = inputText,
            query = query,
            showProgressBar = {
                progressBar?.visibility = View.VISIBLE
            },
            onNoResults = {
                progressBar?.visibility = View.GONE
                textView?.visibility = View.VISIBLE
                textView?.text = getString(R.string.no_results_were_found)
                onComplete?.invoke()
                /*
                val layout = view?.findViewById<LinearLayout>(R.id.root)
                val textView = createTextView("No results were found")
                val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)
                recyclerView?.visibility = View.GONE
                layout?.addView(textView)

                 */
            }
        ) {
            textView?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)!!
            val communityRecyclerAdapter = CommunityRecyclerAdapter(eventDataAdapter, communityPageName)
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(view?.context)
            recyclerView.adapter = communityRecyclerAdapter
            recyclerView.addItemDecoration(LinePaint())

            val stickyHeaderItemDecorator = StickyHeaderItemDecorator(communityRecyclerAdapter)
            recyclerView.addItemDecoration(stickyHeaderItemDecorator)

            val bsTextViewTitle: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTitle)
            val bsTextViewCommunityLocation: TextView =bottomSheetView.findViewById<TextView>(R.id.textViewCommunityLocation)
            val bsTextViewCommunityTime: TextView =bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTime)
            val bsTextViewCommunityDesc: TextView =bottomSheetView.findViewById<TextView>(R.id.textViewCommunityDesc)
            val bsRelativeLayoutImage: RelativeLayout = bottomSheetView.findViewById<RelativeLayout>(R.id.relativeLayoutImage)
            val bsButtonRSVP: AppCompatButton = bottomSheetView.findViewById<AppCompatButton>(R.id.btnRSVP)
            val bsTextInterested:TextView = bottomSheetView.findViewById<TextView>(R.id.textInterested)
            val bsButtonInterested: AppCompatButton = bottomSheetView.findViewById<AppCompatButton>(R.id.buttonInterested)
            val bsButtonClose: AppCompatButton = bottomSheetView.findViewById<AppCompatButton>(R.id.buttonClose)
            val bsLinearLayoutVerified: LinearLayout = bottomSheetView.findViewById<LinearLayout>(R.id.llVerifiedAndRegistered)
            val bsTextHelpType:TextView = bottomSheetView.findViewById<TextView>(R.id.tvHelpType)
            val bsLinearLayoutVerifiedAndIcon: LinearLayout = bottomSheetView.findViewById(R.id.llVerifiedAndIcon)
            val bsTextViewRegistered: TextView = bottomSheetView.findViewById(R.id.tvRegistered)
            val bsTextViewEventStatus: TextView = bottomSheetView.findViewById(R.id.tvEventStatus)
            val bsFlexboxLayoutSkills: FlexboxLayout = bottomSheetView.findViewById(R.id.flSkills)
            val isPastEvents = communityPageName == CommunityPageName.PAST_EVENTS
            val bsImageViewVerification: ImageView = bottomSheetView.findViewById(R.id.ivVerificationMark)
            val ivFlag: ImageView = bottomSheetView.findViewById(R.id.ivFlag)
            val bsLinearLayoutEmail: LinearLayout = bottomSheetView.findViewById<LinearLayout>(R.id.linearLayoutEmail)
            val bsTextViewEmail: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewEmail)
            val bsLinearLayoutContact: LinearLayout = bottomSheetView.findViewById<LinearLayout>(R.id.linearLayoutContact)
            val bsTextViewContact: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewContact)
            val bsLinearLayoutEventDesc: LinearLayout = bottomSheetView.findViewById<LinearLayout>(R.id.linearLayoutEventDesc)
            val bsButtonShare: ImageButton = bottomSheetView.findViewById(R.id.btnShare)
            val bsButtonLike: ImageButton = bottomSheetView.findViewById(R.id.btnLike)
            val tvLikeCount: TextView = bottomSheetView.findViewById(R.id.tvLikeCount)


            (recyclerView?.adapter as CommunityRecyclerAdapter).setRefreshBottomSheet { event ->
                refreshBottomSheet(
                    event = event,
                    relativeLayoutImage = bsRelativeLayoutImage,
                    textInterested = bsTextInterested,
                    buttonRSVP = bsButtonRSVP,
                    buttonInterested = bsButtonInterested,
                    textViewEventStatus = bsTextViewEventStatus,
                    linearLayoutVerified = bsLinearLayoutVerified,
                    linearLayoutVerifiedAndIcon = bsLinearLayoutVerifiedAndIcon,
                    textViewRegistered = bsTextViewRegistered,
                    isPastEvents = isPastEvents,
                    flexboxLayoutSkills = bsFlexboxLayoutSkills,
                    buttonLike = bsButtonLike,
                    tvLikeCount = tvLikeCount
                )
            }

            (recyclerView.adapter as CommunityRecyclerAdapter).setClickListener(object :
                CommunityRecyclerAdapter.ClickListener {
                @SuppressLint("ResourceAsColor")
                override fun onClick(event: Event, position: Int) {

                    //Shar button logic
                    bsButtonShare.setOnClickListener {
                        Share.shareEvent(requireContext(), event.eventId)
                    }

                    bsButtonLike.setOnClickListener {
                        if (Firebase.auth.currentUser == null) {
                            showLoginDialog(requireContext())
                            return@setOnClickListener
                        }
                        event.likedByMe = !event.likedByMe
                        if (event.likedByMe) {
                            event.likeCount++
                            bsButtonLike.setImageResource(R.drawable.ic_heart_filled)
                        } else {
                            event.likeCount--
                            bsButtonLike.setImageResource(R.drawable.ic_heart_outline)
                        }
                        tvLikeCount.text = event.likeCount.toString()

                        communityRecyclerAdapter.notifyItemChanged(position)
                        eventDataAdapter.setLikedOutreachEvent(event.eventId, event.likedByMe) { success ->
                            if (!success) {
                                event.likedByMe = !event.likedByMe
                                if (event.likedByMe) {
                                    event.likeCount++
                                    bsButtonLike.setImageResource(R.drawable.ic_heart_filled)
                                } else {
                                    event.likeCount--
                                    bsButtonLike.setImageResource(R.drawable.ic_heart_outline)
                                }
                                tvLikeCount.text = event.likeCount.toString()

                                communityRecyclerAdapter.notifyItemChanged(position)
                            }
                        }
                    }

                    (recyclerView.adapter as CommunityRecyclerAdapter).setCurrentBottomSheetEvent(event)
                    bsTextViewTitle.text = event.title
                    bsTextViewCommunityLocation.text = if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
                        "${event.street}, ${event.city}, ${event.state} ${event.zipcode}"
                    } else {
                        event.location.orEmpty()
                    }
                    bsTextViewCommunityTime.text = event.time
                    val eventDesc = event.description?.takeIf { it.isNotBlank()}
                    bsTextViewCommunityDesc.text = eventDesc
//                    if(eventDesc != null) {
//                        bsLinearLayoutEventDesc.visibility = View.VISIBLE
//                    } else {
//                        bsLinearLayoutEventDesc.visibility = View.GONE
//                    }

                    val consentGiven = event.consentBox
                    if (consentGiven == true) {
                        val email = event.email?.takeIf { it.isNotBlank()}
                        val contact = event.contactNumber?.takeIf { it.isNotBlank()}
                        if(email != null) {
                            bsTextViewEmail.text = email
                            bsLinearLayoutEmail.visibility = View.VISIBLE
                        } else{
                            bsLinearLayoutEmail.visibility = View.GONE
                        }
                        if(contact != null) {
                            bsTextViewContact.text = contact
                            bsLinearLayoutContact.visibility = View.VISIBLE
                        } else {
                            bsLinearLayoutContact.visibility = View.GONE
                        }
                    } else {
                        bsLinearLayoutEmail.visibility = View.GONE
                        bsLinearLayoutContact.visibility = View.GONE
                    }

                    val uid = event.uid;

                    var type :String? = "";
                    val db = Firebase.firestore
                    db.collection("users").whereEqualTo("uid", uid.toString())
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                for (document in querySnapshot.documents) {
                                    type = document.getString("Type") ?: "Unknown"
                                }
                            } else {
                                println("No user found with that uid")
                            }
                            verificationMark(getUserType(type.toString()), bsImageViewVerification)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirestoreQuery", " Error getting documents: $exception" )
                        }

                    val approved = event.approved!!

                    var isSignedUp = event.signedUp
                    //val numOfInterest = event.interest?.minus(event.itemList.size)
                    /*
                    if(!isPastEvents) {
                        if (isSignedUp) {
                            buttonRSVP.setText(R.string.unregister)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = true

                            buttonInterested.backgroundTintList = null
                            buttonInterested.text = resources.getString(R.string.unregister)
                            buttonInterested.setTextColor(Color.BLACK)
                            buttonInterested.isEnabled = true
                        } else {
                            buttonRSVP.setText(R.string.rsvp)
                        }
                    }
                    else {
                        buttonRSVP.setText(R.string.expired)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                        buttonRSVP.isEnabled = false

                        buttonInterested.backgroundTintList = null
                        buttonInterested.text = resources.getString(R.string.expired)
                        buttonInterested.setTextColor(Color.BLACK)
                        buttonInterested.isEnabled = false
                    }
                     */
                    /*
                    if(approved) {
                        linearLayoutVerified.visibility = View.VISIBLE
                        bottomSheetView.background = ContextCompat.getDrawable(this@CommunityEventFragment.requireContext(), R.drawable.verified_shape)
                    }
                    else {
                        linearLayoutVerified.visibility = View.GONE
                        bottomSheetView.background = ContextCompat.getDrawable(this@CommunityEventFragment.requireContext(), R.drawable.round_corner)
                    }
                     */
                    setVerifiedAndRegistered(
                        context = this@CommunityEventFragment.requireContext(),
                        isVerified = approved,
                        isRegistered = isSignedUp,
                        isEventCard = false,
                        isPastEvents = isPastEvents,
                        linearLayoutVerified = bsLinearLayoutVerified,
                        linearLayoutVerifiedAndIcon = bsLinearLayoutVerifiedAndIcon,
                        textViewRegistered = bsTextViewRegistered,
                        cardViewEvent = null,
                        bottomSheetView = bottomSheetView,
                    )
                    // Initialize Flag Button Color
                    val isFlagged = event.isFlagged == true // Directly use isFlagged field
                    ivFlag.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isFlagged) R.color.red else R.color.gray
                        )
                    )

                    ivFlag.setOnClickListener {
                        val currentUser = Firebase.auth.currentUser ?: return@setOnClickListener
                        val eventRef = db.collection("outreachEventsDev").document(event.eventId!!)
                        val currentUserId = currentUser.uid

                        // Check if event is already flagged
                        if (event.isFlagged == true) {
                            // Only allow unflagging if current user is the one who flagged it
                            if (event.flaggedByUser == currentUserId) {
                                // User can unflag- Update local event object
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
                                                requireContext(),
                                                R.color.gray
                                            )
                                        )
                                        (recyclerView.adapter as CommunityRecyclerAdapter).notifyItemChanged(position)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FlagDebug", "Error updating flag status: ", e)
                                    }
                            } else {
                                // User is not allowed to unflag
                                Toast.makeText(
                                    requireContext(),
                                    "Only the user who flagged this event or a Street Care Hub Leader can unflag it.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                Log.d("FlagDebug", "User ${currentUser.email} attempted to unflag event flagged by another user")
                            }
                        } else {
                            // Event is not flagged, anyone can flag it- update local event object
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
                                            requireContext(),
                                            R.color.red
                                        )
                                    )
                                    (recyclerView.adapter as CommunityRecyclerAdapter).notifyItemChanged(position)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FlagDebug", "Error updating flag status: ", e)
                                }
                        }
                    }


                    bsTextHelpType.text = event.helpType?: context?.getString(R.string.help_type_required_with_star)

                    Log.d("query", "event.interest: ${event.interest}")
                    Log.d("query", "event.itemList.size: ${event.itemList.size}")

                    refreshBottomSheet(
                        event,
                        bsRelativeLayoutImage,
                        bsTextInterested,
                        bsButtonRSVP,
                        bsButtonInterested,
                        bsTextViewEventStatus,
                        bsLinearLayoutVerified,
                        bsLinearLayoutVerifiedAndIcon,
                        bsTextViewRegistered,
                        isPastEvents,
                        bsFlexboxLayoutSkills,
                        bsButtonLike,
                        tvLikeCount
                    )

                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    bsButtonInterested.setOnClickListener {
                        bsButtonRSVP.performClick()
                    }

                    bsButtonRSVP.setOnClickListener {
                        isSignedUp = event.signedUp
                        event.signedUp=!event.signedUp
                        if(isSignedUp){
                            setRSVPButton(
                                buttonRSVP = bsButtonRSVP,
                                textId = R.string.rsvp,
                                textColor = resources.getColor(R.color.accent_yellow, null),
                                backgroundColor = resources.getColor(R.color.dark_green, null)
                            )
                            /*
                            val color = resources.getColor(R.color.accent_yellow, null)
                            buttonRSVP.setText(R.string.rsvp)
                            buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                                resources.getColor(R.color.dark_green, null)
                            )
                            buttonRSVP.setTextColor(color)
                             */
                            setButtonInterest(
                                buttonInterest = bsButtonInterested,
                                textId = R.string.sign_up,
                                textColor = resources.getColor(R.color.accent_yellow, null),
                                backgroundColor = resources.getColor(R.color.dark_green, null)
                            )
                            /*
                            buttonInterested.text = resources.getString(R.string.sign_up)
                            buttonInterested.backgroundTintList = ColorStateList.valueOf(
                                resources.getColor(R.color.dark_green, null)
                            )
                            buttonInterested.setTextColor(color)
                             */

                        }
                        else{
                            setRSVPButton(
                                buttonRSVP = bsButtonRSVP,
                                textId = R.string.deregister,
                                textColor = Color.BLACK,
                                backgroundColor = null
                            )
                            /*
                            buttonRSVP.setText(R.string.unregister)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                             */
                            setButtonInterest(
                                buttonInterest = bsButtonInterested,
                                textId = R.string.deregister,
                                textColor = Color.BLACK,
                                backgroundColor = null
                            )
                            /*
                            buttonInterested.text = resources.getString(R.string.unregister)
                            buttonInterested.backgroundTintList = null
                            buttonInterested.setTextColor(Color.BLACK)
                             */
                            Log.d("interestedBtn", "${bsButtonInterested.text}, ${bsButtonInterested.backgroundTintList}, ${bsButtonInterested.currentTextColor}")
                        }
                        //(recyclerView?.adapter as CommunityRecyclerAdapter).notifyDataSetChanged()


                        eventDataAdapter.setLikedEvent(event){ event ->
                            refreshBottomSheet(
                                event,
                                bsRelativeLayoutImage,
                                bsTextInterested,
                                bsButtonRSVP,
                                bsButtonInterested,
                                bsTextViewEventStatus,
                                bsLinearLayoutVerified,
                                bsLinearLayoutVerifiedAndIcon,
                                bsTextViewRegistered,
                                isPastEvents,
                                bsFlexboxLayoutSkills,
                                bsButtonLike,
                                tvLikeCount
                            )
                            (recyclerView.adapter as CommunityRecyclerAdapter).notifyItemChanged(position)
                            Log.d("Liked Event Firebase Update", "Liked Event Firebase Update Success")
                        }
                    }

                    bsButtonClose.setOnClickListener{
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }

                }
            })

            //recyclerView!!.addItemDecoration(LinePaint())
            var changedType: String? = null
            var eventId: String? = null
            var eventTitle: String = getString(R.string.unknown_event)

            arguments?.let {
                changedType = it.getString("changedType")
                eventId = it.getString("eventId")
                eventTitle = it.getString("eventTitle")?:getString(R.string.unknown_event)
            }

            Log.d("workManager", "changedType: $changedType")
            Log.d("workManager", "eventId: $eventId")
            Log.d("workManager", "eventTitle: $eventTitle")

            if(changedType != null && eventId != null) {
                if(changedType == ChangedType.Add.type || changedType == ChangedType.Modify.type) {
                    val adapter = recyclerView.adapter as CommunityRecyclerAdapter
                    val pos = adapter.getItemPosition(eventId)
                    Log.d("getItemPosition", "In Fragment, pos: $pos")
                    pos?.let {
                        recyclerView.scrollToPosition(it)
                        val communityData = adapter.getItemAtPosition(it)
                        communityData?.event?.let{ event ->
                            adapter.clickItem(event, pos)
                        }
                    }
                }
                else if(changedType == ChangedType.Remove.type) {
                    Toast.makeText(activity?.applicationContext, "$eventTitle ${getString(R.string.event_removed)}", Toast.LENGTH_LONG).show()
                }
                else {
                    Toast.makeText(activity?.applicationContext, "$eventTitle ${getString(R.string.no_event)}", Toast.LENGTH_LONG).show()
                }
            }
            onComplete?.invoke()
        }

    }

    private fun filterEventsBySkill(skill: String, isPastEvents: Boolean) {

        // Refresh the event list with the filtered query
        refreshEvents(
            eventDataAdapter,
            this@CommunityEventFragment.resources,
            getQueryToFilterEventsByType(skill, isPastEvents),
            userInputText
        )
        searchEvents(
            eventDataAdapter,
            this@CommunityEventFragment.resources,
            getQueryToFilterEventsByType(skill, isPastEvents),
        )
    }



    private fun refreshBottomSheet(
        event: Event,
        relativeLayoutImage: RelativeLayout,
        textInterested: TextView,
        buttonRSVP: AppCompatButton,
        buttonInterested: AppCompatButton,
        textViewEventStatus: TextView,
        linearLayoutVerified: LinearLayout,
        linearLayoutVerifiedAndIcon: LinearLayout,
        textViewRegistered: TextView,
        isPastEvents: Boolean,
        flexboxLayoutSkills: FlexboxLayout,
        buttonLike: ImageButton,
        tvLikeCount: TextView,
    ) {

        val isSignedUp = event.signedUp
        //val numOfInterest = event.interest?.minus(event.itemList.size)

        setVerifiedAndRegistered(
            context = this@CommunityEventFragment.requireContext(),
            isVerified = event.approved!!,
            isRegistered = isSignedUp,
            isEventCard = false,
            isPastEvents = isPastEvents,
            linearLayoutVerified = linearLayoutVerified,
            linearLayoutVerifiedAndIcon = linearLayoutVerifiedAndIcon,
            textViewRegistered = textViewRegistered,
            cardViewEvent = null,
            bottomSheetView = bottomSheetView,
        )

        buttonLike.setImageResource(if (event.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        tvLikeCount.text = event.likeCount.toString()


        event.skills?.let { skills ->
            flexboxLayoutSkills.removeAllViews()
            val bsFlexboxLayoutSkills: FlexboxLayout = bottomSheetView.findViewById(R.id.flSkills)

            val skillsList = mutableListOf<String>()

            for (i in 0 until bsFlexboxLayoutSkills.childCount) {
                val childView = bsFlexboxLayoutSkills.getChildAt(i)
                if (childView is TextView) {
                    skillsList.add(childView.text.toString())
                }
            }

            for (skill in skills) {
                val chip = Chip(requireContext())
                chip.text = skill
                chip.isClickable = true
                chip.isCheckable = false

                chip.setOnClickListener {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    filterEventsBySkill(skill, isPastEvents)
                }

                bsFlexboxLayoutSkills.addView(chip)
            }
        }

        if(!isPastEvents) {
            if (isSignedUp) {
                /*
                buttonRSVP.setText(R.string.unregister)
                buttonRSVP.backgroundTintList = null
                buttonRSVP.setTextColor(Color.BLACK)
                buttonRSVP.isEnabled = true
                 */
                setButtonInterest(
                    buttonInterest = buttonInterested,
                    textId = R.string.deregister,
                    textColor = Color.BLACK,
                    backgroundColor = null
                )
                /*
                buttonInterested.backgroundTintList = null
                buttonInterested.text = resources.getString(R.string.unregister)
                buttonInterested.setTextColor(Color.BLACK)
                buttonInterested.isEnabled = true
                 */
            } else {
                if(event.totalSlots == null || event.totalSlots == -1 || (event.participants?.size ?: 0) < event.totalSlots!!) {
                    val textColor = resources.getColor(R.color.accent_yellow, null)
                    /*
                    buttonRSVP.setText(R.string.rsvp)
                    buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                        resources.getColor(R.color.dark_green, null)
                    )
                    buttonRSVP.setTextColor(textColor)
                     */

                    setButtonInterest(
                        buttonInterest = buttonInterested,
                        textId = R.string.sign_up,
                        textColor = textColor,
                        backgroundColor = resources.getColor(R.color.dark_green, null)
                    )
                    /*
                    buttonInterested.text = resources.getString(R.string.sign_up)
                    buttonInterested.backgroundTintList = ColorStateList.valueOf(
                        resources.getColor(R.color.dark_green, null)
                    )
                    buttonInterested.setTextColor(textColor)
                     */
                }
                else {
                    /*
                    buttonRSVP.setText(R.string.event_full)
                    buttonRSVP.backgroundTintList = null
                    buttonRSVP.setTextColor(Color.BLACK)
                    buttonRSVP.isEnabled = false
                     */

                    replaceButtonInterest(
                        buttonInterest = buttonInterested,
                        tvEventStatus = textViewEventStatus,
                        textId = R.string.event_full
                    )
                    /*
                    buttonInterested.backgroundTintList = null
                    buttonInterested.text = resources.getString(R.string.event_full)
                    buttonInterested.setTextColor(Color.BLACK)
                    buttonInterested.isEnabled = false
                     */
                }

            }
        }
        else {
            if(!event.signedUp) {
                /*
                buttonRSVP.setText(R.string.completed)
                buttonRSVP.backgroundTintList = null
                buttonRSVP.setTextColor(Color.BLACK)
                buttonRSVP.isEnabled = false
                 */
                replaceButtonInterest(
                    buttonInterest = buttonInterested,
                    tvEventStatus = textViewEventStatus,
                    textId = R.string.completed
                )
                /*
                buttonInterested.backgroundTintList = null
                buttonInterested.text = resources.getString(R.string.completed)
                buttonInterested.setTextColor(Color.BLACK)
                buttonInterested.isEnabled = false
                 */
            }
            else {
                /*
                buttonRSVP.setText(R.string.attended)
                buttonRSVP.backgroundTintList = null
                buttonRSVP.setTextColor(Color.BLACK)
                buttonRSVP.isEnabled = false
                 */
                replaceButtonInterest(
                    buttonInterest = buttonInterested,
                    tvEventStatus = textViewEventStatus,
                    textId = R.string.attended
                )
                /*
                buttonInterested.backgroundTintList = null
                buttonInterested.text = resources.getString(R.string.attended)
                buttonInterested.setTextColor(Color.BLACK)
                buttonInterested.isEnabled = false
                 */
            }
        }
        /*
        if(event.signedUp){
            buttonRSVP.setText(R.string.unregister)
            buttonRSVP.backgroundTintList = null
            buttonRSVP.setTextColor(Color.BLACK)

            buttonInterested.text = resources.getString(R.string.unregister)
            buttonInterested.backgroundTintList = null
            buttonInterested.setTextColor(Color.BLACK)
            Log.d("interestedBtn", "${buttonInterested.text}, ${buttonInterested.backgroundTintList}, ${buttonInterested.currentTextColor}")

        }
        else{
            val color = resources.getColor(R.color.accent_yellow, null)
            buttonRSVP.setText(R.string.rsvp)
            buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                resources.getColor(R.color.dark_green, null)
            )
            buttonRSVP.setTextColor(color)

            buttonInterested.text = resources.getString(R.string.sign_up)
            buttonInterested.backgroundTintList = ColorStateList.valueOf(
                resources.getColor(R.color.dark_green, null)
            )
            buttonInterested.setTextColor(color)

        }
        */
        //refreshNumOfInterestAndProfileImg(event, textInterested, relativeLayoutImage)

        // refreshNumOfInterest
        context?.let { refreshNumOfInterest(event, textInterested, isPastEvents, it) }

    }

    private fun pastEventsItemSelected(parent: AdapterView<*>, pos: Int, isPastEvents: Boolean) {
        var shouldUpdateSelectedItemPos = true
        val selectedItem = parent.getItemAtPosition(pos)
        when(selectedItem.toString()) {
            getString(R.string.select) -> {
                shouldUpdateSelectedItemPos = false
            }
            getString(R.string.last_7_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() - getDayInMilliSec(7)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                )
            }
            getString(R.string.last_30_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() - getDayInMilliSec(30)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                )
            }
            getString(R.string.last_60_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() - getDayInMilliSec(60)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                )
            }
            getString(R.string.last_90_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() - getDayInMilliSec(90)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents),
                )
            }
            getString(R.string.other_past_events) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() - getDayInMilliSec(90)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents),
                )
            }
            getString(R.string.reset) -> {
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    defaultQuery,
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    defaultQuery,
                )
            }
        }
        if(shouldUpdateSelectedItemPos) selectedItemPos = pos
        Log.d("filter", "selectedItem: $selectedItem")
    }

    private fun upcomingEventsItemSelected(parent: AdapterView<*>, pos: Int, isPastEvents: Boolean) {
        var shouldUpdateSelectedItemPos = true
        val selectedItem = parent.getItemAtPosition(pos)
        when(selectedItem.toString()) {
            getString(R.string.select) -> {
                shouldUpdateSelectedItemPos = false
            }
            getString(R.string.next_7_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() + getDayInMilliSec(7)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents,Query.Direction.ASCENDING),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                )
            }
            getString(R.string.next_30_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() + getDayInMilliSec(30)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                )
            }
            getString(R.string.next_60_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() + getDayInMilliSec(60)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                )
            }
            getString(R.string.next_90_days) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() + getDayInMilliSec(90)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsBeforeTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                )
            }
            getString(R.string.other_upcoming_events) -> {
                val targetDate = Timestamp(Date(System.currentTimeMillis() + getDayInMilliSec(90)))
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    getQueryToFilterEventsAfterTargetDate(targetDate, isPastEvents, Query.Direction.ASCENDING),
                )
            }
            getString(R.string.reset) -> {
                refreshEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    defaultQuery,
                    userInputText
                )
                searchEvents(
                    eventDataAdapter,
                    this@CommunityEventFragment.resources,
                    defaultQuery,
                )
            }
        }
        if(shouldUpdateSelectedItemPos) selectedItemPos = pos
        Log.d("filter", "selectedItem: $selectedItem")
    }

    private fun likedEventsItemSelected(parent: AdapterView<*>, pos: Int) {
        var shouldUpdateSelectedItemPos = true
        val selectedItem = parent.getItemAtPosition(pos).toString()

        when (selectedItem) {
            getString(R.string.select) -> {
                shouldUpdateSelectedItemPos = false
            }
            getString(R.string.last_7_days) -> {
                applyLikedEventsFilter(LikedDateFilter.LAST_7_DAYS)
            }
            getString(R.string.last_30_days) -> {
                applyLikedEventsFilter(LikedDateFilter.LAST_30_DAYS)
            }
            getString(R.string.next_7_days) -> {
                applyLikedEventsFilter(LikedDateFilter.NEXT_7_DAYS)
            }
            getString(R.string.next_30_days) -> {
                applyLikedEventsFilter(LikedDateFilter.NEXT_30_DAYS)
            }
            getString(R.string.reset) -> {
                likedDateFilter = LikedDateFilter.ALL
                userInputText = ""
                searchView.setQuery("", false)
                view?.findViewById<Spinner>(R.id.events_filter)?.setSelection(0)
                defaultQuery = org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery(likedEventIds)
                setupEventsWithQuery(defaultQuery)
            }
        }

        if (shouldUpdateSelectedItemPos) {
            selectedItemPos = pos
        }
    }

    private fun applyLikedEventsFilter(filter: LikedDateFilter) {
        likedDateFilter = filter

        // If filter is ALL, use normal query path
        if (filter == LikedDateFilter.ALL) {
            defaultQuery = org.brightmindenrichment.street_care.util.Queries.getLikedEventsQuery(likedEventIds)
            setupEventsWithQuery(defaultQuery)
            return
        }

        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        val textView = view?.findViewById<LinearLayout>(R.id.root)?.findViewById<TextView>(R.id.text_view)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerCommunity)

        progressBar?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
        textView?.visibility = View.GONE

        // defaultQuery in LIKED_EVENTS should already be getLikedEventsQuery(likedEventIds)
        defaultQuery.get()
            .addOnSuccessListener { result ->
                scope.launch {
                    val filteredEvents = mutableListOf<CommunityData>()
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
                        event.time = getDateTimeFromTimestamp(document.get("eventDate")).split("at ")[1]

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
                        if (!checkQuery(event, userInputText)) continue

                        // Apply date filter
                        val eventDate = document.get("eventDate") as? Timestamp
                        if (eventDate != null && !isLikedEventInDateRange(eventDate, filter)) continue

                        // Process date formatting and layout (same as EventDataAdapter)
                        val date: String = document.get("eventDate")?.toString() ?: "Unknown"
                        if (date != "Unknown") {
                            val localDateTime = dateParser(date)
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

                            // Handle month headers and day layout
                            if (prevMonth != null) {
                                if (!month.toString().equals(prevMonth)) {
                                    prevMonth = month.toString()
                                    event.layoutType = Extensions.TYPE_NEW_DAY
                                    val eventYear = EventYear()
                                    eventYear.year = "$monthName $year"
                                    val community = CommunityData(eventYear, Extensions.TYPE_MONTH)
                                    filteredEvents.add(community)
                                } else {
                                    if (dayOfMonth != prevDay) {
                                        prevDay = dayOfMonth
                                        event.layoutType = Extensions.TYPE_NEW_DAY
                                    } else {
                                        event.layoutType = Extensions.TYPE_DAY
                                    }
                                }
                            } else {
                                prevMonth = month.toString()
                                prevDay = dayOfMonth
                                event.layoutType = Extensions.TYPE_NEW_DAY
                                val eventYear = EventYear()
                                eventYear.year = "$monthName $year"
                                val community = CommunityData(eventYear, Extensions.TYPE_MONTH)
                                filteredEvents.add(community)
                            }

                            val communityEvent = CommunityData(event, event.layoutType!!)
                            filteredEvents.add(communityEvent)
                        }
                    }

                    if (filteredEvents.isNotEmpty()) {
                        textView?.visibility = View.GONE
                        progressBar?.visibility = View.GONE
                        recyclerView?.visibility = View.VISIBLE
                        recyclerView?.layoutManager = LinearLayoutManager(view?.context)

                        // Create a new EventDataAdapter and populate it with filtered events using reflection
                        val filteredEventDataAdapter = EventDataAdapter(scope)
                        try {
                            val field = EventDataAdapter::class.java.getDeclaredField("communityDataList")
                            field.isAccessible = true
                            val list = field.get(filteredEventDataAdapter) as MutableList<CommunityData>
                            list.clear()
                            list.addAll(filteredEvents)
                        } catch (e: Exception) {
                            Log.e("FilteredEvents", "Failed to populate filtered adapter", e)
                            // Fallback: use original adapter
                        }
                        val communityRecyclerAdapter = CommunityRecyclerAdapter(filteredEventDataAdapter, communityPageName)
                        recyclerView?.adapter = communityRecyclerAdapter

                        // Setup click listener for bottom sheet - reuse existing refreshBottomSheet logic
                        communityRecyclerAdapter.setRefreshBottomSheet { event ->
                            refreshBottomSheet(
                                event,
                                bottomSheetView.findViewById(R.id.relativeLayoutImage),
                                bottomSheetView.findViewById(R.id.textInterested),
                                bottomSheetView.findViewById(R.id.btnRSVP),
                                bottomSheetView.findViewById(R.id.buttonInterested),
                                bottomSheetView.findViewById(R.id.tvEventStatus),
                                bottomSheetView.findViewById(R.id.llVerifiedAndRegistered),
                                bottomSheetView.findViewById(R.id.llVerifiedAndIcon),
                                bottomSheetView.findViewById(R.id.tvRegistered),
                                false, // isPastEvents
                                bottomSheetView.findViewById(R.id.flSkills),
                                bottomSheetView.findViewById(R.id.btnLike),
                                bottomSheetView.findViewById(R.id.tvLikeCount)
                            )
                        }

                        communityRecyclerAdapter.setClickListener(object : CommunityRecyclerAdapter.ClickListener {
                            @SuppressLint("ResourceAsColor")
                            override fun onClick(event: Event, position: Int) {
                                // Reuse existing bottom sheet click handler setup
                                val bsTextViewTitle: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTitle)
                                val bsTextViewCommunityLocation: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityLocation)
                                val bsTextViewCommunityTime: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityTime)
                                val bsTextViewCommunityDesc: TextView = bottomSheetView.findViewById<TextView>(R.id.textViewCommunityDesc)
                                val bsButtonShare: ImageButton = bottomSheetView.findViewById(R.id.btnShare)
                                val bsButtonLike: ImageButton = bottomSheetView.findViewById(R.id.btnLike)
                                val tvLikeCount: TextView = bottomSheetView.findViewById(R.id.tvLikeCount)
                                val bsButtonClose: AppCompatButton = bottomSheetView.findViewById(R.id.buttonClose)

                                bsButtonShare.setOnClickListener {
                                    Share.shareEvent(requireContext(), event.eventId)
                                }

                                bsButtonLike.setOnClickListener {
                                    if (Firebase.auth.currentUser == null) {
                                        showLoginDialog(requireContext())
                                        return@setOnClickListener
                                    }
                                    event.likedByMe = !event.likedByMe
                                    if (event.likedByMe) {
                                        event.likeCount++
                                        bsButtonLike.setImageResource(R.drawable.ic_heart_filled)
                                    } else {
                                        event.likeCount--
                                        bsButtonLike.setImageResource(R.drawable.ic_heart_outline)
                                    }
                                    tvLikeCount.text = event.likeCount.toString()

                                    communityRecyclerAdapter.notifyItemChanged(position)
                                    eventDataAdapter.setLikedOutreachEvent(event.eventId, event.likedByMe) { success ->
                                        if (!success) {
                                            event.likedByMe = !event.likedByMe
                                            if (event.likedByMe) {
                                                event.likeCount++
                                                bsButtonLike.setImageResource(R.drawable.ic_heart_filled)
                                            } else {
                                                event.likeCount--
                                                bsButtonLike.setImageResource(R.drawable.ic_heart_outline)
                                            }
                                            tvLikeCount.text = event.likeCount.toString()
                                            communityRecyclerAdapter.notifyItemChanged(position)
                                        }
                                    }
                                }

                                bsButtonClose.setOnClickListener {
                                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                                }

                                bsTextViewTitle.text = event.title
                                bsTextViewCommunityLocation.text = if (!event.city.isNullOrEmpty() && !event.state.isNullOrEmpty()) {
                                    "${event.street}, ${event.city}, ${event.state} ${event.zipcode}"
                                } else {
                                    event.location.orEmpty()
                                }
                                bsTextViewCommunityTime.text = event.time
                                val eventDesc = event.description?.takeIf { it.isNotBlank() }
                                bsTextViewCommunityDesc.text = eventDesc

                                bsButtonLike.setImageResource(
                                    if (event.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                                )
                                tvLikeCount.text = event.likeCount.toString()

                                communityRecyclerAdapter.setCurrentBottomSheetEvent(event)
                                refreshBottomSheet(
                                    event,
                                    bottomSheetView.findViewById(R.id.relativeLayoutImage),
                                    bottomSheetView.findViewById(R.id.textInterested),
                                    bottomSheetView.findViewById(R.id.btnRSVP),
                                    bottomSheetView.findViewById(R.id.buttonInterested),
                                    bottomSheetView.findViewById(R.id.tvEventStatus),
                                    bottomSheetView.findViewById(R.id.llVerifiedAndRegistered),
                                    bottomSheetView.findViewById(R.id.llVerifiedAndIcon),
                                    bottomSheetView.findViewById(R.id.tvRegistered),
                                    false, // isPastEvents
                                    bottomSheetView.findViewById(R.id.flSkills),
                                    bsButtonLike,
                                    tvLikeCount
                                )

                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            }
                        })

                        val stickyHeaderItemDecorator = StickyHeaderItemDecorator(communityRecyclerAdapter)
                        recyclerView?.addItemDecoration(stickyHeaderItemDecorator)
                        
                        // Setup search listener for filtered liked events
                        searchView.setOnQueryTextListener(
                            DebouncingQueryTextListener(lifecycle) { inputText ->
                                inputText?.let {
                                    userInputText = it
                                    // Re-apply filter with new search text
                                    applyLikedEventsFilter(likedDateFilter)
                                } ?: run {
                                    userInputText = ""
                                    applyLikedEventsFilter(likedDateFilter)
                                }
                            }
                        )
                    } else {
                        progressBar?.visibility = View.GONE
                        recyclerView?.visibility = View.GONE
                        textView?.visibility = View.VISIBLE
                        textView?.text = getString(R.string.no_results_were_found)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LikedEventsFilter", "Error loading filtered events", exception)
                progressBar?.visibility = View.GONE
                recyclerView?.visibility = View.GONE
                textView?.visibility = View.VISIBLE
                textView?.text = getString(R.string.no_results_were_found)
            }
    }

    private fun isLikedEventInDateRange(eventDate: Timestamp, dateFilter: LikedDateFilter): Boolean {
        if (dateFilter == LikedDateFilter.ALL) return true

        val currentTime = System.currentTimeMillis()
        val eventTime = eventDate.toDate().time

        return when (dateFilter) {
            LikedDateFilter.LAST_7_DAYS -> {
                val targetDate = currentTime - getDayInMilliSec(7)
                eventTime >= targetDate && eventTime <= currentTime
            }
            LikedDateFilter.LAST_30_DAYS -> {
                val targetDate = currentTime - getDayInMilliSec(30)
                eventTime >= targetDate && eventTime <= currentTime
            }
            LikedDateFilter.NEXT_7_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(7)
                eventTime >= currentTime && eventTime <= targetDate
            }
            LikedDateFilter.NEXT_30_DAYS -> {
                val targetDate = currentTime + getDayInMilliSec(30)
                eventTime >= currentTime && eventTime <= targetDate
            }
            else -> true
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

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item is selected.
        // You can retrieve the selected item using parent.getItemAtPosition(pos).
        when(communityPageName) {
            CommunityPageName.PAST_EVENTS -> pastEventsItemSelected(parent, pos, true)
            CommunityPageName.UPCOMING_EVENTS -> upcomingEventsItemSelected(parent, pos, false)
            CommunityPageName.LIKED_EVENTS -> likedEventsItemSelected(parent, pos)
            else -> Unit
        }

    }



    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventDataAdapter.cleanupFlagStatusListeners()
    }
}
