package au.gov.health.covidsafe.ui.onboarding.fragment.personal

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import au.gov.health.covidsafe.ui.onboarding.fragment.enternumber.EnterNumberFragment
import kotlinx.android.synthetic.main.fragment_personal_details.*
import java.util.regex.Pattern


private const val TAG = "PersonalDetailsFragment"

private val POST_CODE_REGEX = Pattern.compile("^(?:(?:[2-8]\\d|9[0-7]|0?[28]|0?9(?=09))(?:\\d{2}))$")
private val NAME_REGEX = Pattern.compile("^[A-Za-z0-9\\u00C0-\\u017F][A-Za-z'0-9\\-\\u00C0-\\u017F ]{0,80}\$")

class PersonalDetailsFragment : PagerChildFragment() {

    private var alertDialog: AlertDialog? = null
    override var step: Int? = 1

    private lateinit var name: String
    private lateinit var postcode: String
    private var age: Int = -1

    private fun updatePersonalDetailsDataField() {
        name = personal_details_name.text.toString()
        postcode = personal_details_post_code.text.toString()
    }

    private fun isValidName() = NAME_REGEX.matcher(name).matches()

    private fun isValidAge() = age >= 0
    private fun isValidPostcode() = postcode.length == 4 && POST_CODE_REGEX.matcher(postcode).matches()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_personal_details, container, false)

    private fun checkAgeAndDisplayAgeError(): Boolean {
        val isValidAge = isValidAge()

        if (isValidAge) {
            personal_details_age_error.visibility = View.GONE
        } else {
            personal_details_age_error.visibility = View.VISIBLE
        }

        return isValidAge
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(personal_details_name.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()

        ArrayAdapter.createFromResource(
                requireContext(),
                R.array.age_range_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Apply the adapter to the spinner
            personal_details_age.adapter = adapter
        }

        personal_details_age.setSelectionCallback(object :
                SelectionCallbackSpinner.CustomSpinnerSelectionCallback {
            override fun onItemSelected(position: Int) {
                age = when (position) {
                    0 -> -1
                    1 -> 8
                    2 -> 22
                    3 -> 35
                    4 -> 45
                    5 -> 55
                    6 -> 65
                    7 -> 75
                    8 -> 85
                    9 -> 95
                    else -> -1
                }

                CentralLog.d(TAG, "age = $age")

                updateButtonState()

                if (checkAgeAndDisplayAgeError()) {
                    personal_details_post_code.requestFocus()
                }
            }
        })

        personal_details_name.setOnFocusChangeListener { _, hasFocus ->
            updatePersonalDetailsDataField()
            updateButtonState()

            personal_details_name_error.visibility = if (hasFocus || isValidName()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        personal_details_name.setOnEditorActionListener(OnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard()
                textView.clearFocus()
                personal_details_age.requestFocus()
                personal_details_age.performClick()
            }

            true
        })

        personal_details_post_code.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updatePersonalDetailsDataField()
                updateButtonState()
            }
        }

        personal_details_post_code.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER) {

                if (event == null || !event.isShiftPressed) {
                    // user has done typing.
                    updatePersonalDetailsDataField()
                    updateButtonState()

                    personal_details_post_code_error.visibility = if (isValidPostcode()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
            }

            false // pass on to other listeners.
        }

        personal_details_headline.setHeading()
        personal_details_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun onPause() {
        super.onPause()
        alertDialog?.dismiss()
    }

    override fun getUploadButtonLayout(): UploadButtonLayout = UploadButtonLayout.ContinueLayout(R.string.personal_details_button) {
        val context = this.requireContext()
        Preference.putName(context, name)
        Preference.putAge(context, "$age")
        Preference.putPostCode(context, postcode)

        navigateToNextPage(age < 16)
    }

    override fun updateButtonState() {
        updatePersonalDetailsDataField()

        if (isValidName() && isValidAge() && isValidPostcode()) {
            enableContinueButton()
        } else {
            disableContinueButton()
        }
    }

    private fun navigateToNextPage(isUnder16: Boolean) {
        if (isUnder16) {
            navigateTo(PersonalDetailsFragmentDirections.actionPersonalDetailsToUnderSixteenFragment().actionId)
        } else {
            val bundle = bundleOf(
                    EnterNumberFragment.ENTER_NUMBER_DESTINATION_ID to R.id.action_otpFragment_to_permissionFragment,
                    EnterNumberFragment.ENTER_NUMBER_PROGRESS to 2)
            navigateTo(PersonalDetailsFragmentDirections.actionPersonalDetailsToEnterNumberFragment().actionId, bundle)
        }
    }
}