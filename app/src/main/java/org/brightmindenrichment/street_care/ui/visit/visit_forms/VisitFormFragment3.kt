//package org.brightmindenrichment.street_care.ui.visit.visit_forms
//
//
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.activityViewModels
//import androidx.navigation.fragment.findNavController
//import org.brightmindenrichment.street_care.R
//import org.brightmindenrichment.street_care.databinding.FragmentVisitForm3Binding
//
//
//class VisitFormFragment3 : Fragment() {
//    private var _binding: FragmentVisitForm3Binding? = null
//    private val binding get() = _binding!!
//    private val sharedVisitViewModel: VisitViewModel by activityViewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        _binding = FragmentVisitForm3Binding.inflate(inflater, container, false)
//        return binding.root
//    }
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.increaseNoOfPeople.setOnClickListener {
//            val count = sharedVisitViewModel.increment(sharedVisitViewModel.visitLog.peopleCount.toInt())
//
//            sharedVisitViewModel.visitLog.peopleCount = count.toLong()
//            binding.etNoOfPeople.setText(sharedVisitViewModel.visitLog.peopleCount.toString())
//        }
//        binding.decreaseNoOfPeople.setOnClickListener {
//            val count = sharedVisitViewModel.decrement(sharedVisitViewModel.visitLog.peopleCount.toInt())
//
//            sharedVisitViewModel.visitLog.peopleCount = count.toLong()
//            binding.etNoOfPeople.setText(sharedVisitViewModel.visitLog.peopleCount.toString())
//        }
//        binding.etNoOfPeople.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val input = s.toString().toIntOrNull() ?: 0
//                sharedVisitViewModel.visitLog.peopleCount = input.toLong()
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                /* if (s.isNullOrEmpty()) {
//                      binding.etNoOfPeople.hint = "0" // Show hint when empty
//                  } else {
//                      binding.etNoOfPeople.hint = ""  // Remove hint when user starts typing
//                  }*/
//            }
//        })
//        binding.txtNext3.setOnClickListener {
//          //  sharedVisitViewModel.visitLog.names = binding.e.text.toString()
//            sharedVisitViewModel.visitLog.peopleHelpedDescription = binding.descriptionExample.text.toString()
//            findNavController().navigate(R.id.action_visitFormFragment3_to_visitFormFragment4)
//        }
//        binding.txtPrevious3.setOnClickListener {
//            findNavController().navigate(R.id.action_visitFormFragment3_to_visitFormFragment1)
//        }
//        binding.txtSkip3.setOnClickListener {
//            findNavController().navigate(R.id.action_visitFormFragment3_to_visitFormFragment4)
//        }
//      /*  binding.descriptionExample.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val description = s.toString()
//                sharedVisitViewModel.visitLog.description = description
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//
//            }
//        })*/
//    }
//    override fun onResume() {
//        super.onResume()
//        binding.etNoOfPeople.setText(sharedVisitViewModel.visitLog.peopleCount.toString())
//    }
//
//    /*   binding.btnSubmitVisit.setOnClickListener
//       {
//           if (Firebase.auth.currentUser == null) {
//               Extensions.showDialog(
//                   requireContext(),
//                   "Anonymous",
//                   "Logging a visit without logging in may \n result in you, being unable to view your \n visit history.",
//                   "Ok"
//               )
//           } else {
//               sharedVisitViewModel.visitLog.comments = getUserComments()
//               sharedVisitViewModel.saveVisitLog()
//               Toast.makeText(context, "Log saved successfully ", Toast.LENGTH_SHORT).show()
//               sharedVisitViewModel.visitLog = VisitLog()
//           }
//           findNavController().navigate(R.id.action_visitFormFragment3_to_surveySubmittedFragment)
//       }
//
//
//       private fun getUserComments(): String {
//          return binding.commentsEditText.text.toString()
//       }
//
//   */
//    override fun onDestroy() {
//        super.onDestroy()
//        _binding = null
//    }
//}
//
