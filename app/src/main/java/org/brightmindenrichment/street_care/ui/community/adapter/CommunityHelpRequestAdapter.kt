package org.brightmindenrichment.street_care.ui.community.adapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.data.HelpRequest
import org.brightmindenrichment.street_care.ui.community.data.HelpRequestData
import org.brightmindenrichment.street_care.ui.community.data.HelpRequestDataAdapter
import org.brightmindenrichment.street_care.ui.user.getUserType
import org.brightmindenrichment.street_care.ui.user.verificationMark
import org.brightmindenrichment.street_care.util.Extensions.Companion.createSkillTextView
import org.brightmindenrichment.street_care.util.Extensions.Companion.setHelpRequestActionButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setHelpRequestActionButtonStyle



class CommunityHelpRequestAdapter(
    private val controller: HelpRequestDataAdapter,
    private val currentUserId: String,
    private val context: Context

    //private val isPastEvents: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val db = FirebaseFirestore.getInstance()

    interface ClickListener {
        fun onClick(helpRequest: HelpRequest, position: Int){}
    }
    private var clickListener:ClickListener?=null

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    private lateinit var refreshBottomSheet: (HelpRequest) -> Unit
    fun setRefreshBottomSheet(
        refreshBottomSheet: (HelpRequest) -> Unit
    ){
        this.refreshBottomSheet = refreshBottomSheet
    }


    fun getItemPosition(helpRequestId: String?): Int? {
        for (pos in 0 until controller.size) {
            val helpRequestData = controller.getHelpRequestAtPosition(pos)
            if(helpRequestData?.helpRequest?.id == helpRequestId) return pos
        }
        return null
    }

    fun getItemAtPosition(pos: Int): HelpRequestData? {
        return controller.getHelpRequestAtPosition(pos)
    }

    fun clickItem(helpRequest: HelpRequest, pos: Int) {
        clickListener!!.onClick(helpRequest, pos)
    }

    private var currentHeaderText: String? = null

    fun getCurrentHeaderText(): String? {
        return currentHeaderText
    }


    inner class HelpRequestViewHolder(private val helpRequestItemView: View) : RecyclerView.ViewHolder(helpRequestItemView) {
        private val tvHelpRequestStatus: TextView = helpRequestItemView.findViewById(R.id.tvHelpRequestStatus)
        private val tvHelpRequestTitle: TextView = helpRequestItemView.findViewById(R.id.tvHelpRequestTitle)
        private val tvHelpRequestLocation: TextView = helpRequestItemView.findViewById(R.id.tvHelpRequestLocation)
        private val tvHelpRequestHowToFind: TextView = helpRequestItemView.findViewById(R.id.tvHelpRequestHowToFind)
        private val tvHelpRequestDesc: TextView = helpRequestItemView.findViewById(R.id.tvHelpRequestDesc)
        private val btnAction: AppCompatButton = helpRequestItemView.findViewById(R.id.btnAction)
        private val helpRequestCardView:MaterialCardView = helpRequestItemView.findViewById(R.id.cardViewEvent)
        private val llButton: LinearLayout = helpRequestItemView.findViewById(R.id.llButton)
        private val flSkills: FlexboxLayout = helpRequestItemView.findViewById(R.id.flSkills)
        private val ivVerificationMark: ImageView = helpRequestItemView.findViewById(R.id.ivVerificationMark)


        init {
            helpRequestCardView.setOnClickListener{
                val position = bindingAdapterPosition
                clickListener?.let {
                    if(position != RecyclerView.NO_POSITION){
                        val helpRequestData = controller.getHelpRequestAtPosition(position)
                        helpRequestData?.helpRequest?.let{ helpRequest ->
                            clickListener!!.onClick(helpRequest, position)
                        }
                    }
                }
            }

            btnAction.setOnClickListener {

                val position = bindingAdapterPosition
                val helpRequestData = controller.getHelpRequestAtPosition(position)
                helpRequestData?.helpRequest?.let{ helpRequest->
                    // refreshNumOfInterestAndProfileImg(event)
                    // notifyDataSetChanged()
                    controller.setBtnAction(helpRequest) {
                        setHelpRequestActionButton(
                            helpRequest = helpRequest,
                            btnAction = btnAction,
                            tvHelpRequestStatus = tvHelpRequestStatus,
                            llButton = llButton,
                            currentUserId = currentUserId,
                            context = context
                        )
                        refreshBottomSheet(it)
                        notifyItemChanged(position)
                        Log.d("Liked Event Firebase Update", "Liked Event Firebase Update Success")
                    }
                }
            }

        }

        fun bind(pos: Int) {
            // Bind data to views
            // ...
            val helpRequestData = controller.getHelpRequestAtPosition(pos)
            helpRequestData?.helpRequest?.let{ helpRequest->
                tvHelpRequestStatus.text = helpRequest.status
                tvHelpRequestTitle.text = helpRequest.title
                tvHelpRequestLocation.text = helpRequest.location.orEmpty()
                tvHelpRequestDesc.text = helpRequest.description
                tvHelpRequestHowToFind.text = helpRequest.identification

                helpRequest.skills?.let { skills ->
                    flSkills.removeAllViews()
                    for(skill in skills) {
                        flSkills.addView(createSkillTextView(skill, context))
                    }
                }

                if (currentUserId.isEmpty() || currentUserId == "guest") {
                    btnAction.visibility = View.GONE
                } else {
                    btnAction.visibility = View.VISIBLE
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

                val uid = helpRequest.uid;  // Replace this with the actual UID

                var type :String? = "";

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
                        Log.e("FirestoreQuery", " Error getting documents: $exception" )
                    }


                /*
                when(helpRequest.status) {
                    HelpRequestStatus.NeedHelp.status -> {
                        btnAction.visibility = View.VISIBLE
                        //btnAction.text = Resources.getSystem().getString(R.string.can_help)
                        btnAction.text = context.getString(R.string.can_help)
                        tvHelpRequestStatus.text = HelpRequestStatus.NeedHelp.status
                    }
                    HelpRequestStatus.HelpOnTheWay.status -> {
                        btnAction.visibility = View.VISIBLE
                        //btnAction.text = Resources.getSystem().getString(R.string.help_received)
                        btnAction.text = context.getString(R.string.help_received)
                        tvHelpRequestStatus.text = HelpRequestStatus.HelpOnTheWay.status
                    }
                    HelpRequestStatus.HelpReceived.status -> {
                        tvHelpRequestStatus.text = HelpRequestStatus.HelpReceived.status
                        if(currentUserId == helpRequest.uid) {
                            btnAction.visibility = View.VISIBLE
                            //btnAction.text = Resources.getSystem().getString(R.string.reopen_help_request)
                            btnAction.text = context.getString(R.string.reopen_help_request)
                        }
                        else llButton.visibility = View.GONE
                    }
                }

                 */
            }
        }

    }

    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.help_requests_layout, parent, false)
        return HelpRequestViewHolder(view)
    }
    override fun getItemCount(): Int {
        return controller.size
    }

//    override fun getItemViewType(position: Int): Int {
//        val helpRequestData = controller.getHelpRequestAtPosition(position)
//        return helpRequestData?.layoutType ?:0
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HelpRequestViewHolder -> {
                holder.bind(position)
            }

        }
    }
}
