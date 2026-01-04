package org.brightmindenrichment.street_care.ui.visit.visit_forms

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.*
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.R
import java.util.*

class InteractionLogActivityq3 : AppCompatActivity() {

    private lateinit var inputAddress: AutoCompleteTextView
    private lateinit var inputCity: EditText
    private lateinit var inputState: EditText
    private lateinit var inputZip: EditText
    private lateinit var inputDescription: EditText
    private lateinit var iconSearch: ImageView
    private lateinit var iconMic: ImageView
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnClose: FrameLayout
    private lateinit var skipBtn: TextView

    private var suggestions: List<Address> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // State Mapping for cleaner UI (matching the "AZ" in screenshot)
    private val stateMap = mapOf(
        "ALABAMA" to "AL", "ALASKA" to "AK", "ARIZONA" to "AZ", "ARKANSAS" to "AR",
        "CALIFORNIA" to "CA", "COLORADO" to "CO", "CONNECTICUT" to "CT", "DELAWARE" to "DE",
        "FLORIDA" to "FL", "GEORGIA" to "GA", "HAWAII" to "HI", "IDAHO" to "ID",
        "ILLINOIS" to "IL", "INDIANA" to "IN", "IOWA" to "IA", "KANSAS" to "KS",
        "KENTUCKY" to "KY", "LOUISIANA" to "LA", "MAINE" to "ME", "MARYLAND" to "MD",
        "MASSACHUSETTS" to "MA", "MICHIGAN" to "MI", "MINNESOTA" to "MN", "MISSISSIPPI" to "MS",
        "MISSOURI" to "MO", "MONTANA" to "MT", "NEBRASKA" to "NE", "NEVADA" to "NV",
        "NEW HAMPSHIRE" to "NH", "NEW JERSEY" to "NJ", "NEW MEXICO" to "NM", "NEW YORK" to "NY",
        "NORTH CAROLINA" to "NC", "NORTH DAKOTA" to "ND", "OHIO" to "OH", "OKLAHOMA" to "OK",
        "OREGON" to "OR", "PENNSYLVANIA" to "PA", "RHODE ISLAND" to "RI", "SOUTH CAROLINA" to "SC",
        "SOUTH DAKOTA" to "SD", "TENNESSEE" to "TN", "TEXAS" to "TX", "UTAH" to "UT",
        "VERMONT" to "VT", "VIRGINIA" to "VA", "WASHINGTON" to "WA", "WEST VIRGINIA" to "WV",
        "WISCONSIN" to "WI", "WYOMING" to "WY"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_log_interaction_q3)
        supportActionBar?.hide()

        initViews()
        setupTypeAhead()
        setupClickListeners()
    }

    private fun initViews() {
        inputAddress = findViewById(R.id.input_address)
        inputCity = findViewById(R.id.input_city)
        inputState = findViewById(R.id.input_state)
        inputZip = findViewById(R.id.input_zip)
        inputDescription = findViewById(R.id.input_description)
        iconSearch = findViewById(R.id.icon_search)
        iconMic = findViewById(R.id.icon_mic)
        btnNext = findViewById(R.id.btn_next)
        btnPrevious = findViewById(R.id.btn_previous)
        btnClose = findViewById(R.id.btn_close_container)
        skipBtn = findViewById(R.id.skip_btn)
    }

    private fun setupTypeAhead() {
        // 1. We create a custom adapter to DISABLE the default filtering.
        // This ensures that when Geocoder returns results, they actually show up in the list.
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val filterResults = FilterResults()
                        // We return the suggestions list directly
                        filterResults.values = suggestions
                        filterResults.count = suggestions.size
                        return filterResults
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        inputAddress.setAdapter(adapter)
        inputAddress.threshold = 1 // Start searching after 1 character (good for street numbers)

        inputAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                // Reduced to 1 to allow number-only searches like "597"
                if (query.isNotEmpty()) {
                    searchRunnable?.let { handler.removeCallbacks(it) }
                    searchRunnable = Runnable { fetchSuggestions(query, adapter) }
                    handler.postDelayed(searchRunnable!!, 400)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputAddress.setOnItemClickListener { _, _, position, _ ->
            // Use a safe check for suggestions
            val adapterCount = adapter.count
            if (position < suggestions.size) {
                val selectedAddress = suggestions[position]
                // Set the text to the full address line and move cursor to end
                inputAddress.setText(selectedAddress.getAddressLine(0))
                inputAddress.setSelection(inputAddress.text.length)

                fillFields(selectedAddress)
                hideKeyboard(inputAddress)
            }
        }
    }

    private fun fetchSuggestions(query: String, adapter: ArrayAdapter<String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@InteractionLogActivityq3, Locale.US)

                // Define US Bounding Box (Southwest to Northeast)
                // Lower Left: ~24.39 (Florida/Texas south), -124.84 (Washington west)
                // Upper Right: ~49.38 (Washington north), -66.93 (Maine east)
                val results = geocoder.getFromLocationName(
                    query,
                    10,       // Max results
                    24.396308, -124.848974,
                    49.384358, -66.93457
                )

                withContext(Dispatchers.Main) {
                    if (!results.isNullOrEmpty()) {
                        suggestions = results
                        val addressStrings = results.map { it.getAddressLine(0) }

                        adapter.clear()
                        adapter.addAll(addressStrings)
                        adapter.notifyDataSetChanged()
                        inputAddress.showDropDown()
                    }
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Error: ${e.message}")
            }
        }
    }

    private fun fillFields(address: Address) {
        inputCity.setText("")
        inputState.setText("")
        inputZip.setText("")

        // 1. Fill City
        address.locality?.let { inputCity.setText(it) } ?: run {
            // Fallback for some sub-localities
            address.subLocality?.let { inputCity.setText(it) }
        }

        // 2. Fill State with improved Mapping
        val rawState = address.adminArea ?: ""
        val stateAbbreviation = stateMap[rawState.uppercase(Locale.US)]

        if (stateAbbreviation != null) {
            inputState.setText(stateAbbreviation)
        } else {
            // If it's already an abbreviation (2 chars), keep it, otherwise show raw
            inputState.setText(if (rawState.length == 2) rawState.uppercase() else rawState)
        }

        // 3. Fill Zip
        address.postalCode?.let { inputZip.setText(it) }
    }

    private fun setupClickListeners() {
        iconMic.setOnClickListener { startVoiceInput() }

        btnNext.setOnClickListener { navigateToNext() }

        skipBtn.setOnClickListener { navigateToNext() }

        btnPrevious.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnClose.setOnClickListener { finish() }
    }

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let {
                inputAddress.setText(it)
                // Trigger a search for the spoken text
                val adapter = inputAddress.adapter as ArrayAdapter<String>
                fetchSuggestions(it, adapter)
            }
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        voiceLauncher.launch(intent)
    }

    private fun navigateToNext() {
        val intent = Intent(this, InteractionLogActivityq4::class.java)
        // Pass data if needed: intent.putExtra("address", inputAddress.text.toString())
        startActivity(intent)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroy() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}





