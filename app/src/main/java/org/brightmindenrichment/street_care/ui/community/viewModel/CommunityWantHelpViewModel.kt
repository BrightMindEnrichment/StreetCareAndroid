package org.brightmindenrichment.street_care.ui.community.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.ui.community.model.CommunityActivityHelp
import org.brightmindenrichment.street_care.ui.community.model.CommunityActivityRequest

private const val TAG = "CommunityHelpVM"

class CommunityWantHelpViewModel : ViewModel() {
    //    private val _helpListLiveData = MutableLiveData<List<CommunityActivityHelp>>()
//    // LiveData object to observe changes in the activities list
//    val helpListLiveData: LiveData<List<CommunityActivityHelp>> get() = _helpListLiveData
    val helpListLiveData: MutableLiveData<List<CommunityActivityHelp>> by lazy {
        MutableLiveData<List<CommunityActivityHelp>>().also {
            loadList()
        }
    }
    private val db = FirebaseFirestore.getInstance()


    fun loadList() {
        db.collection("communityHelp")
            .get()
            .addOnSuccessListener { documents ->
                val list = ArrayList<CommunityActivityHelp>()

                for (document in documents) {
                    val myObject =
                        document.toObject(CommunityActivityHelp::class.java)
                                as? CommunityActivityHelp
                    myObject?.let {
                        list.add(it)
                    }
                }

                helpListLiveData.value = list
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }


}