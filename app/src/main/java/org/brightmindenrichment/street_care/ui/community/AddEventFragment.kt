package org.brightmindenrichment.street_care.ui.community

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.firestore.FieldValue
// import com.google.firebase.firestore.ktx.firestore MOD
import com.google.firebase.ktx.Firebase
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.ui.user.ChapterMembershipFormOneAcitivity
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.customGetSerializable
import org.brightmindenrichment.street_care.util.Extensions.Companion.requiredSkills
import org.brightmindenrichment.street_care.util.StateAbbreviation.getStateOrProvinceAbbreviation
import org.brightmindenrichment.street_care.BuildConfig
import java.time.LocalDateTime
import java.util.*

class AddEventFragment : Fragment() {
    private lateinit var edtTitle: EditText
    private lateinit var edtEventStartDate: EditText
    private lateinit var edtEventEndDate: EditText
    private lateinit var edtEventStartTime: EditText
    private lateinit var edtEventEndTime: EditText
    private lateinit var edtHelpTypeRequired: EditText
    private lateinit var edtDesc: EditText
    private lateinit var edtStreet: EditText
    private lateinit var edtState: EditText
    private lateinit var edtCity: EditText
    private lateinit var edtZipcode: EditText
    private lateinit var edtMaxCapacity: EditText
    private lateinit var checkedItems: BooleanArray

    private val selectedItems = mutableListOf<String>()

    private var communityPageName: CommunityPageName? = null
    private var edtTitleText: String? = null
    private var street: String? = null
    private var city: String? = null
    private var state: String? = null
    private var zipcode: String? = null
    private var edtDescriptionText: String? = null
    private var helpRequestId: String? = null
    private var edtEmail: EditText? = null
    private var edtContactNumber: EditText? = null
    private var cbConsent: CheckBox? = null

    // Auto Populating addresses from 'Enter Address' field
    private lateinit var placesClient: PlacesClient
    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1
    }
    private lateinit var edtEnterAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Places.initialize(requireContext(), BuildConfig.API_KEY_PLACES)
        placesClient = Places.createClient(requireContext())

        arguments?.let {
            communityPageName = it.customGetSerializable<CommunityPageName>("communityPageName")
            checkedItems = it.getBooleanArray("skillsBooleanArray") ?: BooleanArray(requiredSkills.size)
            edtTitleText = it.getString("title")
            street = it.getString("street")
            city = it.getString("city")
            state = it.getString("state")
            zipcode = it.getString("zipcode")
            edtDescriptionText = it.getString("description")
            helpRequestId = it.getString("helpRequestId")
        }

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(communityPageName == null || communityPageName == CommunityPageName.HELP_REQUESTS) {
                    // Navigate directly to publicEvent instead of communityHelpRequestFragment
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.publicEvent)
                }
                else {
                    // This part handles navigation for regular events
                    val pageTitle = createPageTitle()
                    Log.d("debug", "communityPageName: $communityPageName")
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                        putString("pageTitle", pageTitle)
                        putSerializable("communityPageName", communityPageName)
                    })
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_event, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                // the action of back icon
                Log.d("menuItem.isVisible", "menuItem.isVisible: " + menuItem.itemId)
                Log.d("syncWebApp", "Selected Menu Item: ${menuItem.title}, id: ${menuItem.itemId}")
                Log.d("debug", "communityPageName: $communityPageName")
                if(communityPageName == null || communityPageName == CommunityPageName.HELP_REQUESTS) {
                    // Navigate to publicEvent instead of communityHelpRequestFragment
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.publicEvent)
                    return true
                }
                else {
                    val pageTitle = createPageTitle()
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                        putString("pageTitle", pageTitle)
                        putSerializable("communityPageName", communityPageName)
                    })
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val current = LocalDateTime.now()
        val currentDateInMillis = System.currentTimeMillis()

        edtTitle = view.findViewById<EditText>(R.id.edtTitle)
        edtTitle.filters = arrayOf(InputFilter.LengthFilter(50))
        edtEmail = view.findViewById<EditText>(R.id.edtEmail)
        edtContactNumber = view.findViewById<EditText>(R.id.edtContactNumber)
        cbConsent = view.findViewById<CheckBox>(R.id.cbConsent)
        edtEventStartDate = view.findViewById<EditText>(R.id.edtEventStartDate)
        edtEventEndDate = view.findViewById<EditText>(R.id.edtEventEndDate)
        edtEventStartTime = view.findViewById<EditText>(R.id.edtEventStartTime)
        edtEventEndTime = view.findViewById<EditText>(R.id.edtEventEndTime)
        edtDesc = view.findViewById<EditText>(R.id.edtDesc)
        edtStreet = view.findViewById<EditText>(R.id.edtStreet)
        edtEnterAddress = view.findViewById<EditText>(R.id.edtEnterAddress)
        // Autocompleting address based on 'Enter Address' field
        edtEnterAddress.setOnClickListener {
            launchPlacesAutocomplete()
        }
        edtState = view.findViewById<EditText>(R.id.edtState)
        edtCity = view.findViewById<EditText>(R.id.edtCity)
        edtZipcode = view.findViewById<EditText>(R.id.edtZipcode)
        edtHelpTypeRequired = view.findViewById<EditText>(R.id.edtHelpTypeRequired)
        edtMaxCapacity = view.findViewById<EditText>(R.id.edtMaxCapacity)

        val btnRequiredSkills = view.findViewById<Button>(R.id.btnRequiredSkills)
        val tvRequiredSkills = view.findViewById<TextView>(R.id.tvRequiredSkills)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnDiscard = view.findViewById<Button>(R.id.buttonDiscard)

        val startCalendar = Calendar.getInstance()
        val startHour = startCalendar.get(Calendar.HOUR)
        val startMinute = startCalendar.get(Calendar.MINUTE)
        val startYear = startCalendar.get(Calendar.YEAR)
        val startMonth = startCalendar.get(Calendar.MONTH)
        val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)

        val endCalendar = Calendar.getInstance()
        val endHour = startCalendar.get(Calendar.HOUR)
        val endMinute = startCalendar.get(Calendar.MINUTE)
        val endYear = startCalendar.get(Calendar.YEAR)
        val endMonth = startCalendar.get(Calendar.MONTH)
        val endDay = startCalendar.get(Calendar.DAY_OF_MONTH)

        edtTitleText?.let{ edtTitle.setText(it) }
        street?.let { edtStreet.setText(it) }
        city?.let { edtCity.setText(it) }
        state?.let {edtState.setText(it)}
        zipcode?.let { edtZipcode.setText(it) }
        edtDescriptionText?.let{ edtDesc.setText(it) }

        // show skills
        tvRequiredSkills.text = ""
        selectedItems.clear()
        for (i in checkedItems.indices) {
            if (checkedItems[i]) {
                tvRequiredSkills.text = "${tvRequiredSkills.text}${requiredSkills[i]}, "
                selectedItems.add(requiredSkills[i])
            }
        }
        if(tvRequiredSkills.text == "") tvRequiredSkills.visibility = View.GONE
        else {
            tvRequiredSkills.visibility = View.VISIBLE
            tvRequiredSkills.text = tvRequiredSkills.text.substring(0, tvRequiredSkills.text.length - 2)
        }

        // handle the Open Alert Dialog button
        btnRequiredSkills.setOnClickListener {
            val alertDialog = createRequiredSkillsDialog(
                tvRequiredSkills,
                checkedItems,
                requiredSkills,
            )
            alertDialog.show()
        }

        edtEventStartTime.setOnClickListener {
            val timePickerDialog = context?.let { it1 ->
                TimePickerDialog(context,
                    R.style.MyDatePickerDialogTheme,
                    { view, hourOfDay, minute ->
                        val minuteStr = if(minute < 10) "0$minute" else "$minute"
                        val hourStr = if(hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                        val timezone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
                        edtEventStartTime.setText(requireContext().getString(R.string.time_format,hourStr, minuteStr,timezone))
                        edtEventStartTime.error = null
                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        startCalendar.set(Calendar.MINUTE, minute)
                    },
                    startHour,
                    startMinute,
                    false
                )
            }
            timePickerDialog?.show()
        }

        edtEventEndTime.setOnClickListener {
            val timePickerDialog = context?.let { it1 ->
                TimePickerDialog(context,
                    R.style.MyDatePickerDialogTheme,
                    { view, hourOfDay, minute ->
                        val minuteStr = if(minute < 10) "0$minute" else "$minute"
                        val hourStr = if(hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                        val timezone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
                        edtEventEndTime.setText(requireContext().getString(R.string.time_format,hourStr, minuteStr,timezone))
                        edtEventEndTime.error = null
                        endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        endCalendar.set(Calendar.MINUTE, minute)
                    },
                    endHour,
                    endMinute,
                    false
                )
            }
            timePickerDialog?.show()
        }

        edtEventStartDate.setOnClickListener {
            val datePickerDialog = context?.let { it1 ->
                DatePickerDialog(
                    it1,
                    R.style.MyDatePickerDialogTheme,
                    { view, year, monthOfYear, dayOfMonth ->
                        val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                        Log.d("Event Month", "Event Month"+monthOfYear)
                        edtEventStartDate.setText(dat)
                        edtEventStartDate.error = null
                        startCalendar.set(Calendar.YEAR, year)
                        startCalendar.set(Calendar.MONTH, monthOfYear)
                        startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    },
                    startYear,
                    startMonth,
                    startDay
                )
            }
            datePickerDialog?.show()
        }

        edtEventEndDate.setOnClickListener {
            val datePickerDialog = context?.let { it1 ->
                DatePickerDialog(
                    it1,
                    R.style.MyDatePickerDialogTheme,
                    { view, year, monthOfYear, dayOfMonth ->
                        val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                        Log.d("Event Month", "Event Month"+monthOfYear)
                        edtEventEndDate.setText(dat)
                        edtEventEndDate.error = null
                        endCalendar.set(Calendar.YEAR, year)
                        endCalendar.set(Calendar.MONTH, monthOfYear)
                        endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    },
                    endYear,
                    endMonth,
                    endDay
                )
            }
            datePickerDialog?.show()
        }

        btnSubmit.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser == null) { // MOD
                context?.let { context ->
                    Extensions.showDialog(
                        context,
                        context.getString(R.string.alert),
                        context.getString(R.string.please_login_before_event),
                        context.getString(R.string.ok),
                        context.getString(R.string.cancel)
                    )
                }
            } else {
                val inputTitle = edtTitle.text.toString()
                val startDate = edtEventStartDate.text.toString()
                val startDateTimeStamp = Timestamp(Date(startCalendar.timeInMillis))
                Log.d("date", "submitted dateTimeStamp: $startDateTimeStamp")
                val startTime = edtEventStartTime.text.toString()
                val desc = edtDesc.text.toString()
                val street = edtStreet.text.toString()
                val endDate = edtEventEndDate.text.toString()
                val endDateTimeStamp = Timestamp(Date(endCalendar.timeInMillis))
                val endTime =  edtEventEndTime.text.toString()
                val helpTypeRequired = edtHelpTypeRequired.text.toString()
                val state = edtState.text.toString()
                val city = edtCity.text.toString()
                val zipcode = edtZipcode.text.toString()
                val currentDateTimestamp = Timestamp(Date(currentDateInMillis))
                val maxCapacity = edtMaxCapacity.text.toString()
                val consentGiven = cbConsent?.isChecked?: false

                //considering only upto 50 characters of title
                val title = if (inputTitle.length > 50) inputTitle.substring(0, 50) else inputTitle


                // Always get email and phone values
                val email = edtEmail?.text?.toString()?.trim() ?: ""
                val phone = edtContactNumber?.text?.toString()?.trim() ?: ""

                // Validate email and phone if they are not empty, regardless of consent
                var hasErrorEmailPhone = false
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    edtEmail?.error = getString(R.string.error_email_valid)
                    hasErrorEmailPhone = true
                } else{
                    edtEmail?.error = null
                }

                if (phone.isNotEmpty() && !isValidPhone(phone)) {
                    edtContactNumber?.error = getString(R.string.error_contact_valid)
                    hasErrorEmailPhone = true
                } else{
                    edtEmail?.error = null
                }
                if (hasErrorEmailPhone) {
                    // Show toast message when validation fails
                    showValidationAlert(getString(R.string.error_email_contact_valid))
                    //Toast.makeText(requireContext(), getString(R.string.error_email_contact_valid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Check if title is filled in
                var hasErrorTitle = false
                if (TextUtils.isEmpty(title)) {
                    edtTitle.error = it.context.getString(R.string.required)
                    hasErrorTitle = true
                } else {
                    edtTitle.error = null
                }
                if (hasErrorTitle) {
                    showValidationAlert(getString(R.string.error_title_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_title_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Check if date and time are filled in
                var hasErrorDate = false
                if (TextUtils.isEmpty(startDate)) {
                    edtEventStartDate.error = it.context.getString(R.string.required)
                    hasErrorDate = true
                } else{
                    edtEventStartDate.error = null
                }
                if (TextUtils.isEmpty(endDate)) {
                    edtEventEndDate.error = it.context.getString(R.string.required)
                    hasErrorDate = true
                } else{
                    edtEventEndDate.error = null
                }
                var hasErrorTime = false
                if (TextUtils.isEmpty(startTime)) {
                    edtEventStartTime.error = it.context.getString(R.string.required)
                    hasErrorTime = true
                } else{
                    edtEventStartTime.error = null
                }
                if (TextUtils.isEmpty(endTime)) {
                    edtEventEndTime.error = it.context.getString(R.string.required)
                    hasErrorTime = true
                } else{
                    edtEventEndTime.error = null
                }
                if (hasErrorDate || hasErrorTime) {
                    showValidationAlert(getString(R.string.error_date_time_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_date_time_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Check if city and state are filled in
                var hasErrorLoc = false
                if (TextUtils.isEmpty(state)) {
                    edtState.error = it.context.getString(R.string.required)
                    hasErrorLoc = true
                } else {
                    edtState.error = null
                }
                if (TextUtils.isEmpty(city)) {
                    edtCity.error = it.context.getString(R.string.required)
                    hasErrorLoc = true
                } else {
                    edtCity.error = null
                }
                if(hasErrorLoc) {
                    showValidationAlert(getString(R.string.error_city_state_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_city_state_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                //Check if Support Type field is filled in
                var hasErrorHelpType = false
                if (TextUtils.isEmpty(helpTypeRequired)) {
                    edtHelpTypeRequired.error = it.context.getString(R.string.required)
                    hasErrorHelpType = true
                } else {
                    edtHelpTypeRequired.error = null
                }
                if(hasErrorHelpType){
                    showValidationAlert(getString(R.string.error_helpType_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_helpType_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                var hasErrorDigits = false
                if (!TextUtils.isDigitsOnly(maxCapacity)) {
                    edtMaxCapacity.error = it.context.getString(R.string.digits_only)
                    hasErrorDigits = true
                } else {
                    edtMaxCapacity.error = it.context.getString(R.string.digits_only)
                }
                if(hasErrorDigits){
                    showValidationAlert(getString(R.string.digits_only))
                    //Toast.makeText(requireContext(), getString(R.string.digits_only), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                //Check if Total Participants is filled in
                var hasErrorMaxCapacity = false
                if (TextUtils.isEmpty(maxCapacity)) {
                    edtMaxCapacity.error = it.context.getString(R.string.required)
                    hasErrorMaxCapacity = true
                } else {
                    edtMaxCapacity.error = null
                }
                if(hasErrorMaxCapacity){
                    showValidationAlert(getString(R.string.error_maxCapacity_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_maxCapacity_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                //Check if event Description is filled in
                var hasErrorEventDesc = false
                if (TextUtils.isEmpty(desc)) {
                    edtDesc.error = it.context.getString(R.string.required)
                    hasErrorEventDesc = true
                } else {
                    edtMaxCapacity.error = null
                }
                if(hasErrorEventDesc){
                    showValidationAlert(getString(R.string.error_event_description_required))
                    //Toast.makeText(requireContext(), getString(R.string.error_event_description_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else {
                    addEvent(
                        title = title,
                        email = email,
                        contactNumber = phone,
                        description = desc,
                        startDate = startDateTimeStamp,
                        endDate = endDateTimeStamp,
                        street = street,
                        state = state,
                        city = city,
                        zipcode = zipcode,
                        helpTypeRequired = helpTypeRequired,
                        currentDateTimestamp = currentDateTimestamp,
                        maxCapacity = maxCapacity,
                        helpRequestId = helpRequestId,
                        consentGiven = consentGiven
                    )
                }
            }
        }
        btnDiscard.setOnClickListener{
            clearAllFields()
            findNavController().popBackStack()
            findNavController().navigate(R.id.nav_community)
        }
    }

    private fun launchPlacesAutocomplete() {
        val fields = listOf(
            Place.Field.ADDRESS,
            Place.Field.ADDRESS_COMPONENTS,
            Place.Field.LAT_LNG,
            Place.Field.VIEWPORT
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY, fields)
            .build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)

                        // Update the full address field
                        val completeStreetAddress = place.address?.split(',')?.firstOrNull()?.trim() ?: ""
                        edtEnterAddress.setText(completeStreetAddress)

                        // Extract just the street address
                        val streetAddress = place.address?.split(',')?.firstOrNull()?.trim() ?: ""
                        edtStreet.setText(streetAddress)

                        // Then parse components for city, state, zip
                        place.addressComponents?.asList()?.forEach { component ->
                            when {
                                component.types.contains("locality") -> {
                                    edtCity.setText(component.name)
                                    edtCity.error = null
                                }
                                component.types.contains("administrative_area_level_1") -> {
                                    edtState.setText(component.name)
                                    edtState.error = null
                                }
                                component.types.contains("postal_code") -> {
                                    edtZipcode.setText(component.name)
                                }
                            }
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.e("AddEventFragment", "Error: ${status.statusMessage}")
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // User canceled the operation
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createPageTitle(): String {
        return when(communityPageName) {
            CommunityPageName.UPCOMING_EVENTS -> view?.context?.getString(R.string.upcoming_events) ?: "Upcoming Events"
            CommunityPageName.PAST_EVENTS -> context?.getString(R.string.past_events) ?: "Past Events"
            CommunityPageName.HELP_REQUESTS -> view?.context?.getString(R.string.help_request) ?: "Help Requests"
            else -> ""
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        return cleanPhone.length == 10
    }

    private fun showValidationAlert(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Incomplete Form")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // just closes the dialog and returns to the form
            }
            .create()
            .show()
    }


    private fun createRequiredSkillsDialog(
        tvRequiredSkills: TextView,
        checkedItems:     BooleanArray,
        requiredSkills:   Array<String>,
    ): AlertDialog {
        // initialise the alert dialog builder
        val builder = AlertDialog.Builder(this.context).apply {
            // set the title for the alert dialog
            setTitle(context.getString(R.string.select_required_skills))

            // set the icon for the alert dialog
            setIcon(R.drawable.streetcare_logo)

            // now this is the function which sets the alert dialog for multiple item selection ready
            setMultiChoiceItems(requiredSkills, checkedItems) { dialog, which, isChecked ->
                checkedItems[which] = isChecked
                val currentItem = requiredSkills[which]
            }

            // alert dialog shouldn't be cancellable
            setCancelable(false)

            // handle the positive button of the dialog
            setPositiveButton(context.getString(R.string.done)) { dialog, which ->
                tvRequiredSkills.text = ""
                selectedItems.clear()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        tvRequiredSkills.text = "${tvRequiredSkills.text}${requiredSkills[i]}, "
                        selectedItems.add(requiredSkills[i])
                    }
                }
                if(tvRequiredSkills.text == "") tvRequiredSkills.visibility = View.GONE
                else {
                    tvRequiredSkills.visibility = View.VISIBLE
                    tvRequiredSkills.text = tvRequiredSkills.text.substring(0, tvRequiredSkills.text.length - 2)
                }
            }

            // handle the negative button of the alert dialog
            setNegativeButton("CANCEL") { dialog, which -> }

            // handle the neutral button of the dialog to clear the selected items boolean checkedItem
            setNeutralButton(context.getString(R.string.clear_all)) { dialog: DialogInterface?, which: Int ->
                Arrays.fill(checkedItems, false)
                tvRequiredSkills.text = null
                tvRequiredSkills.visibility = View.GONE
            }

        }

        // create the alert dialog with the alert dialog builder instance
        return builder.create()
    }

    private fun clearAllFields() {
        edtTitle.text.clear()
        edtEventStartDate.text.clear()
        edtEventEndDate.text.clear()
        edtEventStartTime.text.clear()
        edtEventEndTime.text.clear()
        edtDesc.text.clear()
        edtStreet.text.clear()
        edtState.text.clear()
        edtCity.text.clear()
        edtZipcode.text.clear()
        edtHelpTypeRequired.text.clear()
        edtMaxCapacity.text.clear()
        edtEmail?.text?.clear()
        edtContactNumber?.text?.clear()
        cbConsent?.isChecked = false
    }

    private fun addEvent(
        title: String,
        email: String,
        contactNumber: String,
        description: String,
        startDate: Timestamp,
        endDate: Timestamp,
        street: String,
        state: String,
        city: String,
        zipcode: String,
        helpTypeRequired: String,
        currentDateTimestamp: Timestamp,
        maxCapacity: String,
        helpRequestId: String?,
        consentGiven: Boolean
    ) {
        // make sure somebody is logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return // MOD
        val totalSlots = (maxCapacity.ifBlank { "-1" }).toInt()
        val helpRequest = if(helpRequestId == null) listOf() else listOf(helpRequestId)
        val stateAbbr = getStateOrProvinceAbbreviation(state)
        // create a map of event data so we can add to firebase

        val eventData = hashMapOf(
            "approved" to false,
            "isFlagged" to false,
            "flaggedByUser" to null,
            "createdAt" to currentDateTimestamp,
            "description" to description,
            "eventDate" to startDate,
            "eventEndTime" to endDate,
            "eventStartTime" to startDate,
            "helpRequest" to helpRequest, // array
            "helpType" to helpTypeRequired, // string
            "interests" to 1, // int
            "location" to mapOf(
                "street" to street,
                "state" to state,
                "city" to city,
                "zipcode" to zipcode
            ), // map: {city: String, state: String, street: String, zipcode: String
            "participants" to listOf<String>(user.uid), // array
            "skills" to selectedItems, // array
            "status" to "pending",
            "title" to title,
            "emailAddress" to email,
            "contactNumber" to contactNumber,
            "consentStatus" to consentGiven,
            "totalSlots" to totalSlots,
            "uid" to user.uid,
        )
        // save to firebase

        val db = FirebaseFirestore.getInstance() // MOD
        val usersDocRef = db.collection("users").document(user.uid)

        usersDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userType = document.getString("Type") ?: ""

                // Determine status based on user type
                val status = if (userType == "Chapter Leader" || userType == "Street Care Hub Leader") "approved" else "pending"

                val eventData = hashMapOf(
                    "approved" to false,
                    "createdAt" to currentDateTimestamp,
                    "description" to description,
                    "eventDate" to startDate,
                    "eventEndTime" to endDate,
                    "eventStartTime" to startDate,
                    "helpRequest" to helpRequest, // array
                    "helpType" to helpTypeRequired, // string
                    "interests" to 1, // int
                    "location" to mapOf(
                        "street" to street,
                        "state" to state,
                        "stateAbbv" to stateAbbr,
                        "city" to city,
                        "zipcode" to zipcode
                    ), // map: {city: String, state: String, street: String, zipcode: String
                    "participants" to listOf<String>(user.uid), // array
                    "skills" to selectedItems, // array
                    "status" to status,
                    "title" to title,
                    "emailAddress" to email,
                    "contactNumber" to contactNumber,
                    "totalSlots" to totalSlots,
                    "uid" to user.uid,
                    "consentStatus" to consentGiven
                )
                // save to firebase

                db.collection("outreachEventsDev")
                    .add(eventData)
                    .addOnSuccessListener { documentReference ->
                        Log.d("BME", "Saved with id ${documentReference.id}")

                        val message = getString(R.string.thank_you_post)
                        val approvalMessage = getString(R.string.approval_pending)
                        val learnMoreText = getString(R.string.streamline_experience)
                        val learnMoreLink = getString(R.string.learn_more)
                        val alreadyChapterMember = getString(R.string.already_chapter_member)

                        // Create a custom layout for the dialog
                        val dialogView = LayoutInflater.from(context).inflate(R.layout.chapter_membership_signup, null) // Assuming you have a custom_dialog_layout.xml
                        dialogView.findViewById<TextView>(R.id.textViewMessage).text = message
                        dialogView.findViewById<TextView>(R.id.approvalTextView).text = approvalMessage
                        dialogView.findViewById<TextView>(R.id.learnMoreTextView).text = learnMoreText
                        val usersDocRef1 = FirebaseFirestore.getInstance().collection("users").document(user.uid) // MOD

                        usersDocRef1.get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userType = document.getString("Type")
                                    val learnMoreTextView = dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView)

                                    if (userType == "Chapter Member") {
                                        learnMoreTextView.text = alreadyChapterMember
                                        learnMoreTextView.isClickable = false
                                        learnMoreTextView.isFocusable = false
                                        val grayColor = ContextCompat.getColor(dialogView.context, R.color.gray)
                                        learnMoreTextView.setTextColor(grayColor)

                                    } else {
                                        learnMoreTextView.text = learnMoreLink
                                        learnMoreTextView.isClickable = true
                                        learnMoreTextView.isFocusable = true
                                        learnMoreTextView.setOnClickListener{
                                            try {
                                                val activityContext = dialogView.context as? Activity ?: return@setOnClickListener
                                                val intent = Intent(activityContext, ChapterMembershipFormOneAcitivity::class.java)
                                                activityContext.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(dialogView.context, "Error opening sign-up page. Please try again.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(dialogView.context, "Error opening sign-up page. Please try again.", Toast.LENGTH_LONG).show()
                            }

                        // Create and show the dialog
                        val builder = AlertDialog.Builder(context)
                        builder.setView(dialogView)
                        builder.setCancelable(true) // Set to false if you don't want the dialog to be dismissed by tapping outside

                        val dialog = builder.create()
                        dialog.show()
                        dialogView.findViewById<ImageView>(R.id.closeIcon).setOnClickListener {
                            dialog.dismiss() // Dismiss the dialog
                        }
                        clearAllFields()

                        usersDocRef
                            .update("outreachEvents", FieldValue.arrayUnion(documentReference.id))
                            .addOnSuccessListener { Log.d("syncWebApp", "successfully updated!") }
                            .addOnFailureListener { e ->
                                Log.w(
                                    "syncWebApp",
                                    "Error updateOutreachEvents",
                                    e
                                )
                            }
                        Toast.makeText(
                            context,
                            context?.getString(R.string.successfully_registered),
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                        findNavController().navigate(R.id.nav_community)
                    }
                    .addOnFailureListener { exception ->
                        Log.w("BMR", "Error in addEvent ${exception.toString()}")
                        Toast.makeText(
                            context,
                            context?.getString(R.string.failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }.addOnFailureListener { e ->
            Log.w(
                "syncWebApp",
                "Error getting user details",
                e
            )
        }
    }
}