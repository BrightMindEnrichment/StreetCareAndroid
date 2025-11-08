package org.brightmindenrichment.street_care.ui.user

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.ktx.auth MOD
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentForgetPassBinding
import org.brightmindenrichment.street_care.databinding.FragmentVisitBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ForgetPassFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ForgetPassFragment : Fragment() {

    private var _binding: FragmentForgetPassBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentForgetPassBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendBtn.setOnClickListener{
            val email = binding.editTextForgetEmailAddress.text.toString()
            if (TextUtils.isEmpty(email))
            {
                Toast.makeText(context, getString(R.string.please_enter_email), Toast.LENGTH_LONG).show()
            }   else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context,
                    getString(R.string.please_enter_valid_email), Toast.LENGTH_LONG).show()
            }
            else{
                beginRecovery(email)
            }
        }

    }
    private fun beginRecovery(mText: String) {
        auth = FirebaseAuth.getInstance() // MOD
        auth.sendPasswordResetEmail(mText)
            .addOnCompleteListener(OnCompleteListener<Void?> { task ->

                if (task.isSuccessful) {
                    // if isSuccessful then done message will be shown
                    // and you can change the password
                    Toast.makeText(context, getString(R.string.done_sent), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_LONG).show()
                }
            }).addOnFailureListener(OnFailureListener {

                Toast.makeText(context, getString(R.string.error_failed), Toast.LENGTH_LONG).show()
            })
    }


}