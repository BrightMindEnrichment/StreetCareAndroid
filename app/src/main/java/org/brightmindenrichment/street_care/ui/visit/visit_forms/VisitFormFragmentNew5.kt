import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.brightmindenrichment.street_care.R

class Question5Fragment : Fragment(R.layout.fragment_visit_form5_new) {

    private var helpedCount = 1
    private var joinedCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvQuestion = view.findViewById<TextView>(R.id.tv_question)
        val tvHelped = view.findViewById<TextView>(R.id.tv_count_helped)
        val tvJoined = view.findViewById<TextView>(R.id.tv_count_joined)

        val btnHelpedMinus = view.findViewById<View>(R.id.btn_decrease_helped)
        val btnHelpedPlus = view.findViewById<View>(R.id.btn_increase_helped)

        val btnJoinedMinus = view.findViewById<View>(R.id.btn_decrease_joined)
        val btnJoinedPlus = view.findViewById<View>(R.id.btn_increase_joined)

        val btnNext = view.findViewById<View>(R.id.btn_next)
        val btnPrevious = view.findViewById<View>(R.id.btn_previous)
        val btnSkip = view.findViewById<View>(R.id.btn_skip)

        // Set question label
        tvQuestion.text = "Question 5/7"

        // Initial values
        tvHelped.text = helpedCount.toString()
        tvJoined.text = joinedCount.toString()

        // Helped counter
        btnHelpedPlus.setOnClickListener {
            helpedCount++
            tvHelped.text = helpedCount.toString()
        }

        btnHelpedMinus.setOnClickListener {
            if (helpedCount > 0) {
                helpedCount--
                tvHelped.text = helpedCount.toString()
            }
        }

        // Joined counter
        btnJoinedPlus.setOnClickListener {
            joinedCount++
            tvJoined.text = joinedCount.toString()
        }

        btnJoinedMinus.setOnClickListener {
            if (joinedCount > 0) {
                joinedCount--
                tvJoined.text = joinedCount.toString()
            }
        }

        // Navigation
        btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_question5Fragment_to_question6Fragment)
        }

        btnPrevious.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_question5Fragment_to_question6Fragment)
        }
    }
}
