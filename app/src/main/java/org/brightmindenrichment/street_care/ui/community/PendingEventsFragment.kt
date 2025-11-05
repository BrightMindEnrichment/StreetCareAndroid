package org.brightmindenrichment.street_care.ui.community

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.adapter.PendingEventsRecyclerAdapter
import org.brightmindenrichment.street_care.ui.community.adapter.pendingEventAdapter


class PendingEventsFragment : Fragment() {
    private val pendingEventAdapter = pendingEventAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pending_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser != null) {
             updateUI()
        }
        else {
            // TODO : some message to user
            Log.d("BME", "not logged in")
        }
    }

    private fun updateUI() {
        pendingEventAdapter.refresh {
            val recyclerView = view?.findViewById<RecyclerView>(R.id.pendingEventList)
            recyclerView?.layoutManager = LinearLayoutManager(view?.context)
            recyclerView?.adapter = PendingEventsRecyclerAdapter(pendingEventAdapter)
        }
    }


}