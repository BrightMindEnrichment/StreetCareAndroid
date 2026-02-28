//package org.brightmindenrichment.street_care.ui.visit.visit_forms
//
//import android.app.AlertDialog
//import android.content.Context
//import android.content.DialogInterface
//import android.graphics.Color
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ProgressBar
//import android.widget.RatingBar
//import android.widget.Toast
//import android.util.Log
//import androidx.fragment.app.activityViewModels
//import androidx.navigation.fragment.findNavController
//import com.google.firebase.auth.FirebaseAuth
//// import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
//import org.brightmindenrichment.street_care.R
//import org.brightmindenrichment.street_care.databinding.FragmentVisitForm5Binding
//import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
//import org.brightmindenrichment.street_care.util.Extensions
//
//class VisitFormFragment5 : Fragment() {
//    private lateinit var _binding: FragmentVisitForm5Binding
//    private val binding get() = _binding!!
//    private val sharedVisitViewModel: VisitViewModel by activityViewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        _binding = FragmentVisitForm5Binding.inflate(inflater, container, false)
//        return _binding.root
//    }
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val starViews = listOf(
//            binding.star1,
//            binding.star2,
//            binding.star3,
//            binding.star4,
//            binding.star5
//        )
//
//        // Set up click listeners for each star
//        starViews.forEachIndexed { index, starView ->
//            starView.setOnClickListener {
//                val newRating = index + 1
//                // Update the UI
//                starViews.forEachIndexed { i, star ->
//                    star.setImageResource(
//                        if (i < newRating) R.drawable.filled_star else R.drawable.empty_star
//                    )
//                }
//                // Update the model - CHANGED: Use rating instead of experience
//                sharedVisitViewModel.visitLog.experience = newRating
//            }
//        }
//
//
//        binding.txtNext5.setOnClickListener {
//            val notes = binding.edtcomment.text.toString()
//            sharedVisitViewModel.visitLog.comments = notes
//            Log.d("VisitForm", "User-entered rating notes: $notes")
//
//            if (FirebaseAuth.getInstance().currentUser == null) {
//                Extensions.showDialog(
//                    requireContext(),
//                    view.context.getString(R.string.anonymous_user_title),
//                    view.context.getString(R.string.anonymous_user_message),
//                    view.context.getString(R.string.ok),
//                    view.context.getString(R.string.cancel)
//                ) {
//                    findNavController().navigate(R.id.surveySubmittedFragment)
//                }
//            } else {
//                // Save comments before navigating
//
//                sharedVisitViewModel.visitLog.comments = binding.edtcomment.text.toString()
//
//
//                // Navigate directly to VisitForm7a
//                findNavController().navigate(R.id.action_visitFormFragment5_to_visitForm7a)
//                //showDialog(
//                //requireContext(),
//                //getString(R.string.additional_info),
//                //getString(R.string.would_you_like_to_answer_additional_questions),
//                //getString(R.string.yes), getString(R.string.no)
//                //)
//
//            }
//        }
//
//        binding.txtPrevious5.setOnClickListener {
//
//            findNavController().navigate(R.id.action_visitFormFragment5_to_visitFormFragment4)
//        }
//
//    }
//
//
//    fun showDialog(context: Context, title: String, message: String, textPositive: String, textNegative: String) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle(title)
//        builder.setMessage(message)
//            .setCancelable(false)
//            .setPositiveButton(textPositive, DialogInterface.OnClickListener { dialog, _ ->
//                sharedVisitViewModel.visitLog.comments = binding.edtcomment.text.toString()
//                findNavController().navigate(R.id.action_visitFormFragment5_to_visitForm7a)
//                dialog.dismiss()
//            })
//        builder.setNegativeButton(textNegative, DialogInterface.OnClickListener { dialog, _ ->
//            sharedVisitViewModel.visitLog.comments = binding.edtcomment.text.toString()
//            sharedVisitViewModel.saveVisitLog()
//            Toast.makeText(context, getString(R.string.log_saved_successfully), Toast.LENGTH_SHORT).show()
//            //sharedVisitViewModel.visitLog = VisitLog()
//            // Delay resetting visitLog
//            sharedVisitViewModel.resetVisitLogPage(forceReset = false)
//
//            binding.txtProgress.text= getString(R.string.completed)
//            findNavController().navigate(R.id.surveySubmittedFragment)
//            dialog.cancel()
//        })
//        val alert = builder.create()
//        alert.show()
//    }
//}