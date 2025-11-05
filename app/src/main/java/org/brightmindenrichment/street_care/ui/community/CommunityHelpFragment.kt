package org.brightmindenrichment.street_care.ui.community

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentCommunityHelpBinding
import org.brightmindenrichment.street_care.ui.community.viewModel.CommunityHelpViewModel

class CommunityHelpFragment : Fragment() {

    private lateinit var helpBtn: Button
    private lateinit var requestBtn: Button
    private var requestFlag: String = "Request"
    private var helpFlag: String = "Help"
    private var _binding: FragmentCommunityHelpBinding? = null
    private var fragmentFlag: String = "Request"
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = CommunityHelpFragment()
    }

    private lateinit var viewModel: CommunityHelpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCommunityHelpBinding.inflate(inflater, container, false)
        helpBtn = binding.helpBtn
        requestBtn = binding.requestBtn

        helpBtn.setOnClickListener {
            startWantHelpFragment()
        }

        requestBtn.setOnClickListener {
            startRequestHelpFragment()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CommunityHelpViewModel::class.java)
        //by passing string in the bundle and if else to see which fragment to start
        arguments?.let {
            initSubFragment(it)
        }

    }

    private fun initSubFragment(args: Bundle) {
        val fragVal = args.getString(NavigationUtil.FRAGMENT_KEY)
        if (fragVal == NavigationUtil.FRAGMENT_HELP) {
            startWantHelpFragment()
        } else if (fragVal == NavigationUtil.FRAGMENT_REQUEST) {
            startRequestHelpFragment()
        }
    }

    private fun startWantHelpFragment() {

        helpBtn.setTextAppearance(R.style.RightRoundButtonActivate)
        helpBtn.setBackgroundResource(R.drawable.green_right_round_button)
        requestBtn.setTextAppearance(R.style.LeftRoundButtonStyle)
        requestBtn.setBackgroundResource(R.drawable.left_round_button)

        val wantHelpFragment = CommunityWantHelpFragment()
        fragmentFlag = helpFlag
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, wantHelpFragment)
            .commit()
    }

    private fun startRequestHelpFragment() {
        helpBtn.setTextAppearance(R.style.RightRoundButtonStyle)
        helpBtn.setBackgroundResource(R.drawable.right_round_button)
        requestBtn.setTextAppearance(R.style.LeftRoundButtonActivate)
        requestBtn.setBackgroundResource(R.drawable.green_left_round_button)

        val requestHelpFragment = CommunityNeedHelpFragment()
        fragmentFlag = requestFlag
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, requestHelpFragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        enableMyPost()
    }

    override fun onStop() {
        super.onStop()
        disableMyPost()
    }

    private fun enableMyPost() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            val myPostText: TextView? = activity?.findViewById(R.id.toolbar_title_text)
            myPostText?.let {
                myPostText.visibility = View.VISIBLE
                myPostText.setOnClickListener {
                    findNavController().navigate(
                        R.id.communityMyPostFragment,
                        bundleOf(
                            "name" to fragmentFlag
                        )
                    )
                    Log.d("BME", "Add")
                    disableMyPost()
                }
            }
        }
    }

    private fun disableMyPost() {
        val myPostText: TextView? = activity?.findViewById(R.id.toolbar_title_text)
        myPostText?.let {
            myPostText.visibility = View.GONE
            myPostText.setOnClickListener(null)
        }
    }
}