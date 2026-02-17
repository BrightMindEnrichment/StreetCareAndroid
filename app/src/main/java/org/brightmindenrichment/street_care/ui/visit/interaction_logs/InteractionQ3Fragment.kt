package org.brightmindenrichment.street_care.ui.visit.interaction_logs

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brightmindenrichment.street_care.R
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Filter
import android.widget.Filterable
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope

import org.brightmindenrichment.street_care.databinding.FragmentLogInteractionQ3Binding
import java.util.Locale
import kotlin.getValue


class InteractionQ3Fragment : Fragment() {

    private var _binding: FragmentLogInteractionQ3Binding? = null
    private val binding get() = _binding!!

    private var suggestions: List<Address> = emptyList()
    private val viewModel: InteractionLogViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()

                spokenText?.let {
                    binding.inputAddress.setText(it)
                    fetchSuggestions(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInteractionQ3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val log = viewModel.interactionLog.value

        binding.inputAddress.setText(log?.addr1.orEmpty())
        binding.inputCity.setText(log?.city.orEmpty())
        binding.inputState.setText(log?.state.orEmpty())
        binding.inputZip.setText(log?.zipcode.orEmpty())

        setupTypeAhead()
        setupClickListeners()
    }


    private lateinit var addressAdapter: ArrayAdapter<String>

    private fun setupTypeAhead() {

        addressAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )

        binding.inputAddress.setAdapter(addressAdapter)
        binding.inputAddress.threshold = 1

        binding.inputAddress.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                val query = s?.toString()?.trim() ?: return

                if (query.length < 2) {
                    addressAdapter.clear()
                    return
                }

                searchRunnable?.let { handler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    fetchSuggestions(query)
                }

                handler.postDelayed(searchRunnable!!, 350)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.inputAddress.setOnItemClickListener { _, _, position, _ ->

            if (position < suggestions.size) {

                val selectedAddress = suggestions[position]

                binding.inputAddress.setText(selectedAddress.getAddressLine(0))
                binding.inputAddress.setSelection(binding.inputAddress.text.length)

                fillFields(selectedAddress)
                hideKeyboard()
            }
        }
    }



    private fun setupClickListeners() {
        binding.iconMic.setOnClickListener { startVoiceInput() }

        binding.btnNext.setOnClickListener {

            val address = binding.inputAddress.text.toString().trim()
            val city = binding.inputCity.text.toString().trim()
            val state = binding.inputState.text.toString().trim()
            val zip = binding.inputZip.text.toString().trim()

            if (address.isEmpty()) {
                binding.inputAddress.error = "Enter address"
                binding.inputAddress.requestFocus()
                return@setOnClickListener
            }

            if (city.isEmpty()) {
                binding.inputCity.error = "Enter city"
                binding.inputCity.requestFocus()
                return@setOnClickListener
            }

            if (state.isEmpty()) {
                binding.inputState.error = "Enter state"
                binding.inputState.requestFocus()
                return@setOnClickListener
            }

            if (zip.isEmpty()) {
                binding.inputZip.error = "Enter zip"
                binding.inputZip.requestFocus()
                return@setOnClickListener
            }

            // Save to ViewModel
            viewModel.updateAddress(address)
            viewModel.updateCity(city)
            viewModel.updateState(state)
            viewModel.updateZipcode(zip)

            Log.d("Q3_DEBUG", "After Q3 Save: ${viewModel.interactionLog.value}")

            findNavController().navigate(R.id.action_q3_to_q4)
        }


        binding.btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCloseContainer.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        voiceLauncher.launch(intent)
    }

    private fun fetchSuggestions(query: String) {

        if (!Geocoder.isPresent()) {
            Log.e("Geocoder", "Geocoder backend not available")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {

                val geocoder = Geocoder(requireContext(), Locale.US)

                val results = geocoder.getFromLocationName(
                    query,
                    5,
                    24.396308, -124.848974,
                    49.384358, -66.93457
                )

                withContext(Dispatchers.Main) {

                    suggestions = results ?: emptyList()

                    val addressStrings = suggestions.map {
                        it.getAddressLine(0)
                    }

                    addressAdapter.clear()
                    addressAdapter.addAll(addressStrings)
                    addressAdapter.notifyDataSetChanged()

                    if (addressStrings.isNotEmpty()) {
                        binding.inputAddress.showDropDown()
                    }

                    Log.d("Geocoder", "Results size: ${addressStrings.size}")
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    addressAdapter.clear()
                }

                Log.e("Geocoder", "Error: ${e.message}")
            }
        }
    }



    override fun onDestroyView() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }
    private fun fillFields(address: Address) {

        binding.inputCity.setText("")
        binding.inputState.setText("")
        binding.inputZip.setText("")

        // City
        address.locality?.let {
            binding.inputCity.setText(it)
        } ?: run {
            address.subLocality?.let {
                binding.inputCity.setText(it)
            }
        }

        // State
        val rawState = address.adminArea ?: ""
        val stateMap = mapOf(
            "ALABAMA" to "AL", "ALASKA" to "AK", "ARIZONA" to "AZ",
            "CALIFORNIA" to "CA", "COLORADO" to "CO"
            // Add full map again if needed
        )

        val abbreviation = stateMap[rawState.uppercase(Locale.US)]

        if (abbreviation != null) {
            binding.inputState.setText(abbreviation)
        } else {
            binding.inputState.setText(
                if (rawState.length == 2) rawState.uppercase() else rawState
            )
        }

        // Zip
        address.postalCode?.let {
            binding.inputZip.setText(it)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

}
