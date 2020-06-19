package au.gov.health.covidsafe.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_RESULTS
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import au.gov.health.covidsafe.R
import kotlinx.android.synthetic.main.activity_country_code_selection.*

fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int = snapMode
        override fun getHorizontalSnapPreference(): Int = snapMode
    }

    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

const val VOICE_TO_TEXT_REQUEST_CODE = 2020

class CountryCodeSelectionActivity : Activity() {
    val countryListItem = CountryList.getCountryList()

    private fun setupToolbar() {
        countrySelectionToolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }

        countrySelectionToolbar.title = getString(R.string.select_country_or_region)
    }

    private fun setupCountryListRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this)
        countryListRecyclerView.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(
                this,
                linearLayoutManager.orientation
        )
        countryListRecyclerView.addItemDecoration(dividerItemDecoration)

        countryListRecyclerView.adapter = CountryListRecyclerViewAdapter(
                this,
                countryListItem
        ) {
            super.onBackPressed()
        }
    }

    private fun countryListScrollToPosition(positionOfLetter: Int) {
        countryListRecyclerView.scrollToPosition(positionOfLetter)

        Thread {
            Thread.sleep(100)
            runOnUiThread {
                countryListRecyclerView.smoothSnapToPosition(positionOfLetter)
            }
        }.start()
    }

    private fun setupSearchFunctions() {
        // text based search
        countryRegionNameEditText.setOnFocusChangeListener { _, hasFocus ->
            countrySearchImageView.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        countryRegionNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                fun getPositionOfCountryName(searchText: String): Int {
                    countryListItem.forEachIndexed { index, countryListItemInterface ->
                        if (countryListItemInterface is CountryGroupTitle) {
                            val groupTitle = getString(countryListItemInterface.titleResId)
                            if (groupTitle.startsWith(searchText, ignoreCase = true)
                            ) {
                                return index
                            }
                        } else if (countryListItemInterface is CountryListItem) {
                            val countryName = getString(countryListItemInterface.countryNameResId)
                            if (countryName.contains(searchText, ignoreCase = true)
                            ) {
                                return index
                            }

                            val callingCode = countryListItemInterface.callingCode
                            if ("$callingCode".startsWith(searchText, ignoreCase = true)
                            ) {
                                return index
                            }
                        }
                    }

                    return -1
                }

                s?.toString()?.let { enteredText ->
                    val positionOfCountryName = getPositionOfCountryName(enteredText)
                    if (positionOfCountryName != -1) {
                        countryListScrollToPosition(positionOfCountryName)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }

        })

        // voice to text search
        microphoneImageView.setOnClickListener {
            startActivityForResult(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                    VOICE_TO_TEXT_REQUEST_CODE
            )
        }
    }

    private fun setupInitialLetterRecyclerView() {
        val alphabet = ArrayList<String>()
        var letter = 'A'
        while (letter <= 'Z') {
            if (letter != 'W' && letter != 'X') {
                alphabet.add(letter.toString())
            }
            ++letter
        }

        countryInitialLetterRecyclerView.layoutManager = LinearLayoutManager(this)
        countryInitialLetterRecyclerView.adapter = CountryInitialLetterRecyclerViewAdapter(
                this,
                alphabet
        ) { letterClicked ->
            fun getPositionOfLetter(letter: String): Int {
                countryListItem.forEachIndexed { index, countryListItemInterface ->
                    if (countryListItemInterface is CountryGroupTitle) {
                        val groupTitle = getString(countryListItemInterface.titleResId)
                        if (groupTitle.startsWith(letter, ignoreCase = true)) {
                            return index
                        }
                    }
                }

                return 0
            }

            val positionOfLetter = getPositionOfLetter(letterClicked)

            countryListScrollToPosition(positionOfLetter)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_TO_TEXT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(EXTRA_RESULTS)?.let {
                countryRegionNameEditText.setText(it.first())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_code_selection)

        setupToolbar()
        setupCountryListRecyclerView()
        setupInitialLetterRecyclerView()

        // set up the search functions
        setupSearchFunctions()

    }


}

