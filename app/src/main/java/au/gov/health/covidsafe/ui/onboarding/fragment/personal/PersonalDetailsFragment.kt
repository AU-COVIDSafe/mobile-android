package au.gov.health.covidsafe.ui.onboarding.fragment.personal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import au.gov.health.covidsafe.ui.onboarding.fragment.enternumber.EnterNumberFragment
import kotlinx.android.synthetic.main.fragment_enter_number.*
import kotlinx.android.synthetic.main.fragment_personal_details.*
import java.util.regex.Pattern


private val POST_CODE_REGEX = Pattern.compile("^(?:(?:[2-8]\\d|9[0-7]|0?[28]|0?9(?=09))(?:\\d{2}))$")

class PersonalDetailsFragment : PagerChildFragment() {

    private var picker: NumberPicker? = null

    private var alertDialog: AlertDialog? = null
    override var stepProgress: Int? = 1
    override val navigationIcon: Int = R.drawable.ic_up

    private var ageSelected: Pair<Int, String> = Pair(-1, "")

    private lateinit var name: String
    private lateinit var postcode: String
    private var age: Int = -1

    private fun updatePersonalDetailsDataField() {
        name = personal_details_name.text.toString()
        postcode = personal_details_post_code.text.toString()
        age = ageSelected.first
    }

    private fun isFullName() = name.trim().length > 1
    private fun isValidAge() = age >= 0
    private fun isValidPostcode() = postcode.length == 4 && POST_CODE_REGEX.matcher(postcode).matches()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_personal_details, container, false)

    override fun onResume() {
        super.onResume()

        personal_details_age.setText(ageSelected.second)

        fun showAgePicker() {
            activity?.let { activity ->
                val ages = resources.getStringArray(R.array.personal_details_age_array).map {
                    it.split(":").let { split ->
                        (split[0]).toInt() to split[1]
                    }
                }

                var selected = ages.firstOrNull { it == ageSelected }?.let {
                    ages.indexOf(it)
                } ?: 0

                picker = NumberPicker(activity)
                picker?.minValue = 0
                picker?.maxValue = ages.size - 1
                picker?.displayedValues = ages.map { it.second }.toTypedArray()
                picker?.setOnValueChangedListener { _, _, newVal ->
                    selected = newVal
                }
                picker?.value = selected
                alertDialog?.dismiss()
                alertDialog = AlertDialog.Builder(activity)
                        .setTitle(R.string.personal_details_age_dialog_title)
                        .setView(picker)
                        .setPositiveButton(R.string.personal_details_dialog_ok) { _, _ ->
                            ageSelected = ages[selected]

                            personal_details_age.setText(ageSelected.second)

                            updatePersonalDetailsDataField()
                            updateButtonState()

                            personal_details_age_error.visibility =
                                    if (isValidAge()) {
                                        View.GONE
                                    } else {
                                        View.VISIBLE
                                    }

                            personal_details_post_code.requestFocus()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
            }
        }

        personal_details_name.setOnFocusChangeListener { _, hasFocus ->
            updatePersonalDetailsDataField()
            updateButtonState()

            personal_details_name_error.visibility = if (hasFocus || isFullName()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        personal_details_post_code.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus) {
                updatePersonalDetailsDataField()
                updateButtonState()

                personal_details_age_error.visibility =
                        if (isValidAge()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
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

                    personal_details_age_error.visibility =
                            if (isValidAge()) {
                                View.GONE
                            } else {
                                View.VISIBLE
                            }

                    personal_details_post_code_error.visibility = if (isValidPostcode()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
            }

            false // pass on to other listeners.
        }

        personal_details_age.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                showAgePicker()
            }
        }

        personal_details_age.setOnClickListener {
            showAgePicker()
        }

        // set accessibility focus to the title "Enter your details"
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

        if (isFullName() && isValidAge() && isValidPostcode()) {
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