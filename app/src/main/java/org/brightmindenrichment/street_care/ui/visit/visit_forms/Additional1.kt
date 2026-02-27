package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentAdditional1Binding
import org.brightmindenrichment.street_care.databinding.FragmentVisitForm5Binding
import java.text.SimpleDateFormat
import java.util.*

class Additional1 : Fragment() {
    private lateinit var _binding: FragmentAdditional1Binding
    private val binding get() = _binding!!
    private val sharedVisitViewModel: VisitViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdditional1Binding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.timePicker.setIs24HourView(true)
        binding.timePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                val A = SimpleDateFormat("HH:mm").format(cal.time).toLong()
                Log.d("BMEE", "A:$A")
                sharedVisitViewModel.visitLog.outreach = A
            }
            TimePickerDialog(context, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }
        binding.txtNextAdd1.setOnClickListener {

          //   sharedVisitViewModel.visitLog.outreach = binding.timePicker
            findNavController().navigate(R.id.action_additional1_to_additional2)
        }
        binding.txtPreviousAdd1.setOnClickListener {
            findNavController().navigate(R.id.action_additional1_to_visitFormFragmentNew5)
        }
        binding.txtSkipAdd1.setOnClickListener {
            findNavController().navigate(R.id.action_additional1_to_additional2)
        }
    }
}