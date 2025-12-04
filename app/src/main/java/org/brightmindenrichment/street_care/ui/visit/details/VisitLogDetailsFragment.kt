package org.brightmindenrichment.street_care.ui.visit.details

import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentVisitLogDetailsBinding
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import org.brightmindenrichment.street_care.util.Extensions
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class VisitLogDetailsFragment : Fragment() {

    lateinit var binding: FragmentVisitLogDetailsBinding
    private var googleMap: GoogleMap? = null
    private val viewModel: VisitLogDetailsViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVisitLogDetailsBinding.inflate(inflater)

        (requireActivity() as AppCompatActivity)

        try {
            val visitLog = requireArguments().getParcelable<VisitLog>("visitLog")!!
            viewModel.setVisitLog(visitLog)

            cacheVisitLogData(visitLog)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_LONG).show()
            return binding.root
        }

        setupObservers()
        setupClickListeners()
        setupMap(savedInstanceState)

        return binding.root
    }

    private val TAG = "VisitLogDetails"

    private fun cacheVisitLogData(visitLog: VisitLog) {
        // Only assign if current cache is still default/empty (not updated yet)
        if (cachedVisitDate == null) cachedVisitDate = visitLog.date
        if (cachedWhereVisit == "") cachedWhereVisit = visitLog.whereVisit ?: ""
        if (cachedNumberOfPeople == 0) cachedNumberOfPeople = (visitLog.peopleCount ?: 0).toInt()
        if (cachedWhatGiven == "") cachedWhatGiven = visitLog.whatGiven ?: ""
        if (cachedNumberOfItems == 0) cachedNumberOfItems = (visitLog.number_of_items ?: 0).toInt()
        if (cachedRating == 0) cachedRating = visitLog.experience ?: 0
        if (cachedHelpTime == "") cachedHelpTime = visitLog.helpTime ?: ""
        if (cachedWhoJoined == 0) cachedWhoJoined = visitLog.whoJoined ?: 0
        if (cachedStillNeedSupport == 0) cachedStillNeedSupport = visitLog.stillNeedSupport ?: 0
        if (cachedWhatGivenFurther == "") cachedWhatGivenFurther = visitLog.whatGivenFurther ?: ""
        if (cachedFollowUpDate == null) cachedFollowUpDate = visitLog.followupDate
        if (cachedFutureNotes == "") cachedFutureNotes = visitLog.futureNotes ?: ""
        if (cachedVisitAgain == "") cachedVisitAgain = visitLog.visitAgain ?: ""
    }

    // Local cache of visit data

    var cachedVisitDate: Date? = null
    var cachedWhereVisit: String = ""
    var cachedNumberOfPeople: Int = 0
    var cachedWhatGiven: String = ""
    var cachedNumberOfItems: Int = 0
    var cachedRating: Int = 0
    var cachedHelpTime: String = ""
    var cachedWhoJoined: Int = 0
    var cachedStillNeedSupport: Int = 0
    var cachedWhatGivenFurther: String = ""
    var cachedFollowUpDate: Date? = null
    var cachedFutureNotes: String = ""
    var cachedVisitAgain: String = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        parentFragmentManager.setFragmentResultListener(
            "visit_updated",
            viewLifecycleOwner
        ) { _, result ->
            val updated = result.getBoolean("updated", false)


            if (updated) {
                val formatter = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
                formatter.timeZone = TimeZone.getTimeZone("UTC")

                if (result.containsKey("visitDate")) {
                    val visitDate = result.getString("visitDate")?.let {
                        try {
                            formatter.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    cachedVisitDate = visitDate
                }

                if (result.containsKey("whereVisit")) {
                    cachedWhereVisit = result.getString("whereVisit") ?: cachedWhereVisit
                }

                if (result.containsKey("numberOfPeople")) {
                    cachedNumberOfPeople = result.getInt("numberOfPeople", cachedNumberOfPeople)
                }

                if (result.containsKey("whatGiven")) {
                    cachedWhatGiven = result.getString("whatGiven") ?: cachedWhatGiven
                }

                if (result.containsKey("numberOfItems")) {
                    cachedNumberOfItems = result.getInt("numberOfItems", cachedNumberOfItems)
                }

                if (result.containsKey("rating")) {
                    cachedRating = result.getInt("rating", cachedRating)
                }

                if (result.containsKey("helpTime")) {
                    cachedHelpTime = result.getString("helpTime") ?: cachedHelpTime
                }

                if (result.containsKey("whoJoined")) {
                    cachedWhoJoined = result.getInt("whoJoined", cachedWhoJoined)
                }

                if (result.containsKey("stillNeedSupport")) {
                    cachedStillNeedSupport =
                        result.getInt("stillNeedSupport", cachedStillNeedSupport)
                }

                if (result.containsKey("whatGivenFurther")) {
                    cachedWhatGivenFurther =
                        result.getString("whatGivenFurther") ?: cachedWhatGivenFurther
                }

                if (result.containsKey("followUpDate")) {
                    val followUpDate = result.getString("followUpDate")?.let {
                        try {
                            formatter.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    cachedFollowUpDate = followUpDate
                }

                if (result.containsKey("futureNotes")) {
                    cachedFutureNotes = result.getString("futureNotes") ?: cachedFutureNotes
                }

                if (result.containsKey("visitAgain")) {
                    cachedVisitAgain = result.getString("visitAgain") ?: cachedVisitAgain
                }

                // Now update your ViewModel with all current cached values
                viewModel.updateWhenVisit(cachedVisitDate)
                viewModel.updateWhereVisit(cachedWhereVisit)
                viewModel.updatePeopleHelped(cachedNumberOfPeople)
                viewModel.updateWhatGiven(cachedWhatGiven)
                viewModel.updateItemsDonated(cachedNumberOfItems)
                viewModel.updateRating(cachedRating)
                viewModel.updateHelpTime(cachedHelpTime)
                viewModel.updateWhoJoined(cachedWhoJoined)
                viewModel.updateStillNeedSupport(cachedStillNeedSupport)
                viewModel.updateWhatGivenFurther(cachedWhatGivenFurther)
                viewModel.updateFollowupDate(cachedFollowUpDate)
                viewModel.updateFutureNotes(cachedFutureNotes)
                viewModel.updateVisitAgain(cachedVisitAgain)

                Log.d(TAG, "Updated ViewModel using merged cached + updated fields")
            }
        }

    }



    fun String.cleanAddress(): String {
        return this.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }


    private fun setupObservers() {
        viewModel.visitLog.observe(viewLifecycleOwner) { visitLog ->
            binding.visitLogAddressTV.text = visitLog.whereVisit
//            binding.numberOfPeopleHelped.text = visitLog.peopleCount.toString()
            binding.ratingBar.rating = visitLog.experience.toFloat()
//            binding.commentsContent.text = visitLog.comments

            binding.whereWasYourInteractionAnswer.text = visitLog.whereVisit?.cleanAddress() ?: "Not specified"
            binding.howManyYouHelpedAnswer.text= visitLog.peopleCount.toString() ?: "Not specified"
            binding.howManyItemsDonatedAnswer.text= visitLog.number_of_items.toString()
            binding.howMuchTimeSpentAnswer.text= visitLog.helpTime.toString()
            binding.whoJoinedYouAnswer.text= visitLog.whoJoined.toString()
            binding.howManyNeedHelpAnswer.text= visitLog.stillNeedSupport.toString()
            binding.whatSupportNeededAnswer.text= visitLog.whatGivenFurther
            binding.nextPlannedDateAnswer.text= Extensions.dateToString(
                visitLog.followupDate, "dd MMM yyyy 'at' hh:mm a z"
            )
            binding.whatVolunteersShouldKnowAnswer.text= visitLog.futureNotes.toString()
            binding.wouldYouVolunteerAnswer.text= visitLog.visitAgain.toString()
            binding.whatSupportYouProvidedAnswer.text= visitLog.whatGiven

        }

        viewModel.formattedDateTime.observe(viewLifecycleOwner) { formattedDateTime ->
            binding.whenWasYourInteractionAnswer.text = formattedDateTime
        }

        viewModel.formattedDate.observe(viewLifecycleOwner) { formattedDate ->
            binding.visitLogDateTV.text = formattedDate
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Entry deleted successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to delete entry. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    fun showShareConfirmationPopup(visitLog: VisitLog) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_popup_shared_comm_visit_log, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // This removes the black border and makes corners visible
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        val btnOK = dialogView.findViewById<TextView>(R.id.ok_btn)
        val cancel_btn = dialogView.findViewById<TextView>(R.id.cancel_btn)


        btnOK.setOnClickListener {
            shareVisitLogToWeb(visitLog)
            dialog.dismiss()

        }
        cancel_btn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun shareVisitLogToWeb(visitLog: VisitLog) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        if (user != null) {
            val userDocRef = db.collection("users").document(user.uid)

            userDocRef.get().addOnSuccessListener { document ->
                val userType = document.getString("Type") ?: ""
                val proposedStatus = if (userType == "Chapter Leader" || userType == "Street Care Hub Leader") "approved" else "pending"

                db.collection("VisitLogBook_New").document(visitLog.id).get()
                    .addOnSuccessListener { visitDoc ->
                        val currentStatus = visitDoc.getString("status") ?: ""
                        val finalStatus = if (currentStatus == "approved") "approved" else proposedStatus

                        val updateMap = mapOf(
                            "status" to finalStatus,
                            "isPublic" to true
                        )

                        db.collection("VisitLogBook_New").document(visitLog.id).update(updateMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Interaction log published.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Older logs cannot be published.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to check current status.", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "User type check failed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupClickListeners() {

        binding.editInteractionButton.setOnClickListener {
            try {
                Log.d("VisitLogDetailsFragment", "Attempting to navigate to Edit Fragment")
                val currentDest = findNavController().currentDestination
                Log.d("NavDebug", "Current Destination: ${currentDest?.id}, Name: ${currentDest?.label}")

                val bundle1 = Bundle().apply {
                    val visitId = viewModel.visitLog.value?.id
                    val visitDate = viewModel.visitLog.value?.date
                    val deviceType=viewModel.visitLog.value?.typeofdevice
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                    val formattedDate = visitDate?.let { sdf.format(it) }

                    Log.d("VisitLogDetails", "visitId: $visitId")
                    putString("visitId", visitId)        // ID of this visit
                    putString("fieldName1", formattedDate)
                    putString("fieldName0", deviceType)


                }

                findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit1,bundle1)


            } catch (e: IllegalArgumentException) {
                Log.e("VisitLogDetailsFragment", "Navigation failed: Action ID not found", e)
                Toast.makeText(requireContext(), "Navigation failed: Action not found", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("VisitLogDetailsFragment", "Navigation failed: ${e.message}", e)
                Toast.makeText(requireContext(), "Navigation failed: Unexpected error", Toast.LENGTH_SHORT).show()
            }
        }

        binding.editInteractionButton2.setOnClickListener {
            val bundle2 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice
                val visitLocation = viewModel.visitLog.value?.location
                val visitLocationDescrption = viewModel.visitLog.value?.locationDescription

                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", visitLocation.toString())
                putString("fieldName2", visitLocationDescrption.toString())


            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit2,bundle2)
        }

        binding.editInteractionButton3.setOnClickListener {
            val bundle3 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val peopleHelped = viewModel.visitLog.value?.peopleCount
                val PeopleHelpedDescription = viewModel.visitLog.value?.peopleHelpedDescription
                Log.d("VisitLogDetails", "visitId: $visitId")
                Log.d("VisitLogDetails", "visitId: $PeopleHelpedDescription")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", peopleHelped.toString())
                putString("fieldName2", PeopleHelpedDescription)// The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit3,bundle3)
        }

        binding.editInteractionButton4.setOnClickListener {

            val bundle4 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)
                putString("fieldName0", deviceType)


            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit4,bundle4)
        }


        binding.editInteractionButton5.setOnClickListener {
            val bundle5 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val itemQty = viewModel.visitLog.value?.number_of_items
                val itemDesc = viewModel.visitLog.value?.description
                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", itemQty.toString())
                putString("fieldName2", itemDesc)  // The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit5,bundle5)
        }

        binding.editInteractionButton6.setOnClickListener {
            val bundle6 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val rating = viewModel.visitLog.value?.experience
                val comments = viewModel.visitLog.value?.comments
                Log.d("VisitLogDetails", "visitId: $visitId")
                Log.d("VisitLogDetails1", "rating: $rating")
                Log.d("VisitLogDetails2", "comments: $comments")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", rating.toString())
                putString("fieldName2", comments)// The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit6,bundle6)
        }

        binding.editInteractionButton7.setOnClickListener {
            val bundle7 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val visitedHours = viewModel.visitLog.value?.visitedHours
                val visitedMinutes = viewModel.visitLog.value?.visitedMinutes
                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", visitedHours.toString())
                putString("fieldName2", visitedMinutes.toString())// The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit7,bundle7)
        }

        binding.editInteractionButton8.setOnClickListener {
            val bundle8 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val whoJoined = viewModel.visitLog.value?.peopleCount
                val numberOfHelpersComment = viewModel.visitLog.value?.whoJoinedDescription
                Log.d("VisitLogDetails", "visitId: $visitId")
                Log.d("VisitLogDetails", "nc: $whoJoined")
                Log.d("VisitLogDetails", "nc: $numberOfHelpersComment")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", whoJoined.toString())
                putString("fieldName2", numberOfHelpersComment)// The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit8,bundle8)
        }

        binding.editInteractionButton9.setOnClickListener {
            val bundle9 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val stillNeedSupport = viewModel.visitLog.value?.stillNeedSupport
                val supportTypeNeeded = viewModel.visitLog.value?.supportTypeNeeded
                val peopleNeedFurtherHelpLocation = viewModel.visitLog.value?.peopleNeedFurtherHelpLocation
                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", stillNeedSupport.toString())
                putString("fieldName2", supportTypeNeeded)// The field to edit
                putString("fieldName3", peopleNeedFurtherHelpLocation)// The field to edit
            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit9,bundle9)
        }

        binding.editInteractionButton10.setOnClickListener {
            val bundle10 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)

            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit10,bundle10)
        }

        binding.editInteractionButton11.setOnClickListener {
            val bundle11 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)

            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit11,bundle11)
        }

        binding.editInteractionButton12.setOnClickListener {
            val bundle12 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val futureNotes = viewModel.visitLog.value?.futureNotes

                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)        // ID of this visit
                putString("fieldName0", deviceType)
                putString("fieldName1", futureNotes)

            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit12,bundle12)
        }

        binding.editInteractionButton13.setOnClickListener {
            val bundle13 = Bundle().apply {
                val visitId = viewModel.visitLog.value?.id
                val deviceType=viewModel.visitLog.value?.typeofdevice

                val visitAgain = viewModel.visitLog.value?.visitAgain
                Log.d("VisitLogDetails", "visitId: $visitId")
                putString("visitId", visitId)
                putString("fieldName0", deviceType)
                putString("fieldName1", visitAgain)

            }
            findNavController().navigate(R.id.action_visitLogDetailsFragment_to_visitFormFragmentEdit13,bundle13)
        }

        binding.shareBtn.setOnClickListener {
            viewModel.visitLog.value?.let { visitLog ->
                showShareConfirmationPopup(visitLog)
            }
        }

        binding.removeBtn.setOnClickListener {
            showAlertDialog()
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            googleMap = map
            viewModel.visitLog.value?.let { visitLog ->
                updateMapLocation(visitLog.whereVisit)
            }
        }
    }

    private fun updateMapLocation(address: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses?.isNotEmpty() == true) {
                val location = LatLng(addresses[0].latitude, addresses[0].longitude)
                googleMap?.addMarker(MarkerOptions().position(location).title("Visit Location"))
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity()).apply {
            setTitle("Delete Visit Log")
            setMessage("This will permanently delete your Visit Log. This can't be undone.")
            setPositiveButton("Confirm") { _, _ -> viewModel.deleteVisitLog() }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            create().show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null // Avoid memory leaks
    }
}