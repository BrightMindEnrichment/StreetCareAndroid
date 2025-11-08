package org.brightmindenrichment.street_care

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.brightmindenrichment.street_care.ui.visit.VisitDataAdapter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.ktx.Firebase


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileBadges.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileBadges : Fragment() {
    // TODO: Rename and change types of parameter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayBadges(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_profile_badges, container, false)
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileBadges.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic

        //to set the image as black and white image
        private fun setBlackAndWhite(imageView: ImageView, isBlackAndWhite: Boolean) {
            if (isBlackAndWhite) {
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f) // Set saturation to 0 to achieve grayscale

                val colorFilter = ColorMatrixColorFilter(colorMatrix)
                imageView.colorFilter = colorFilter
            }else{
                imageView.colorFilter = null
            }
        }

        private fun displayBadges( view: View) {
            val visitDataAdapter = VisitDataAdapter()

            //storing image view of all badges for further process
            val neighborhood_all_star: ImageView= view.findViewById(R.id.neighborhood_all_star)
            val outreach_all_star: ImageView= view.findViewById(R.id.outreach_all_star)
            val benevolent_donor: ImageView= view.findViewById(R.id.benevolent_donor)

            //set the badges as black and white
            setBlackAndWhite(neighborhood_all_star,true)
            setBlackAndWhite(outreach_all_star, true)
            setBlackAndWhite(benevolent_donor, true)

            //connecting to Firebase to get the current user detail
            val user = FirebaseAuth.getInstance().currentUser?: return // MOD
            Log.d("BME current user", user.uid)


            if (user != null) {
                visitDataAdapter.refreshAll {
                    var totalItemsDonated = visitDataAdapter.getTotalItemsDonated
                    var totalOutreaches = visitDataAdapter.size.toInt()
                    var totalPeopleHelped = visitDataAdapter.getTotalPeopleCount

                    //assign the badges to user as per the milestone they achieved
                    if (totalOutreaches > 3) {
                        setBlackAndWhite(neighborhood_all_star, false)
                    }
                    if (totalItemsDonated.toInt() > 10) {
                        setBlackAndWhite(benevolent_donor, false)
                    }
                    if (totalOutreaches > 15 || totalPeopleHelped.toInt() > 60) {
                        setBlackAndWhite(outreach_all_star, false)
                    }
                }
            } else {
                Log.d("BME", "not logged in")
            }

        }

    }
}
