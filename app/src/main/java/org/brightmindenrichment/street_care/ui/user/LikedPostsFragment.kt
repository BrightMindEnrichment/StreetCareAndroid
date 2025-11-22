package org.brightmindenrichment.street_care.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName

/**
 * Fragment to display menu for accessing liked posts
 */
class LikedPostsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_liked_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val outreachRow = view.findViewById<LinearLayout>(R.id.row_outreach_events)

        outreachRow.setOnClickListener {
            val bundle = Bundle().apply {
                putString("pageTitle", getString(R.string.liked_posts))
                putSerializable("communityPageName", CommunityPageName.LIKED_EVENTS)
            }
            findNavController().navigate(R.id.communityEventFragment, bundle)
        }
    }
}
