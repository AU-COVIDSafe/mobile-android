package au.gov.health.covidsafe.ui.onboarding.fragment.enternumber

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.NavigationRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.TracerApp
import au.gov.health.covidsafe.talkback.setHeading
import au.gov.health.covidsafe.ui.PagerChildFragment
import au.gov.health.covidsafe.ui.UploadButtonLayout
import au.gov.health.covidsafe.ui.onboarding.CountryCodeSelectionActivity
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_CALLING_CODE
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_CHALLENGE_NAME
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_DESTINATION_ID
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_PHONE_NUMBER
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_PROGRESS
import au.gov.health.covidsafe.ui.onboarding.fragment.enterpin.EnterPinFragment.Companion.ENTER_PIN_SESSION
import kotlinx.android.synthetic.main.fragment_enter_number.*

class EnterNumberFragment : PagerChildFragment() {

    companion object {
        const val ENTER_NUMBER_DESTINATION_ID = "destination_id"
        const val ENTER_NUMBER_PROGRESS = "progress"
    }

    override var step: Int? = 2

    private val enterNumberPresenter = EnterNumberPresenter(this)
    private var alertDialog: AlertDialog? = null

    @NavigationRes
    private var destinationId: Int? = null

    private lateinit var countryName: String
    private var callingCode: Int = 0
    private var nationalFlagResID: Int = 0

    private val errorTextColor = ContextCompat.getColor(TracerApp.AppContext, R.color.error)
    private val normalTextColor = ContextCompat.getColor(TracerApp.AppContext, R.color.slack_black)

    private fun updateSelectedCountry() {
        countryName = getString(Preference.getCountryNameResID(this.requireContext()))
        callingCode = Preference.getCallingCode(this.requireContext())
        nationalFlagResID = Preference.getNationalFlagResID(this.requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_enter_number, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            destinationId = it.getInt(ENTER_NUMBER_DESTINATION_ID)
            step = if (it.containsKey(ENTER_NUMBER_PROGRESS)) it.getInt(ENTER_PIN_PROGRESS) else null
        }
    }

    override fun onResume() {
        super.onResume()

        updateSelectedCountry()

        enter_number_phone_number.addTextChangedListener {
            enter_number_phone_number.setTextColor(normalTextColor)

            updateButtonState()
        }

        enter_number_phone_number.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER) {

                if (event == null || !event.isShiftPressed) {
                    // user has done typing.
                    updateButtonState()
                }
            }

            false // pass on to other listeners.
        }

        updateButtonState()
        displaySelectedCountryOrRegion()

        enter_number_page_headline.setHeading()
        enter_number_page_headline.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    @SuppressLint("SetTextI18n")
    private fun displaySelectedCountryOrRegion() {
        country_name_code.text = "$countryName(+$callingCode)"
        national_flag.setImageResource(nationalFlagResID)

        country_selection_box.setOnClickListener {
            startActivity(Intent(this.requireContext(), CountryCodeSelectionActivity::class.java))
        }
    }

    private fun hideInvalidPhoneNumberPrompt() {
        enter_number_headline.setTextColor(normalTextColor)
        enter_number_phone_number.background = context?.getDrawable(R.drawable.edittext_modified_states)
        enter_number_phone_number.setTextColor(normalTextColor)
        invalid_phone_number.visibility = GONE
    }

    fun showInvalidPhoneNumberPrompt(errorMessageResID: Int) {
        enter_number_headline.setTextColor(errorTextColor)
        enter_number_phone_number.background = context?.getDrawable(R.drawable.phone_number_invalid_background)
        enter_number_phone_number.setTextColor(errorTextColor)
        invalid_phone_number.visibility = VISIBLE
        invalid_phone_number.setText(errorMessageResID)
    }

    override fun updateButtonState() {
        val phoneNumberValidity = enterNumberPresenter.validatePhoneNumber(
                callingCode,
                enter_number_phone_number.text.toString().trim()
        )

        if (phoneNumberValidity.first) {
            enableContinueButton()

            hideInvalidPhoneNumberPrompt()
        } else {
            disableContinueButton()

            if (enter_number_phone_number.text.toString().isNotEmpty()) {
                showInvalidPhoneNumberPrompt(phoneNumberValidity.second)
            }
        }
    }

    fun showGenericError() {
        alertDialog?.dismiss()
        alertDialog = AlertDialog.Builder(activity)
                .setMessage(R.string.generic_error)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, null).show()
    }

    fun navigateToOTPPage(
            session: String,
            challengeName: String,
            callingCode: Int,
            phoneNumber: String) {
        val bundle = bundleOf(
                ENTER_PIN_SESSION to session,
                ENTER_PIN_CHALLENGE_NAME to challengeName,
                ENTER_PIN_CALLING_CODE to callingCode,
                ENTER_PIN_PHONE_NUMBER to phoneNumber,
                ENTER_PIN_DESTINATION_ID to destinationId).also { bundle ->
            step?.let {
                bundle.putInt(ENTER_PIN_PROGRESS, it + 1)
            }
        }
        navigateTo(R.id.action_enterNumberFragment_to_otpFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alertDialog?.dismiss()
        root.removeAllViews()
    }

    override fun getUploadButtonLayout() = UploadButtonLayout.ContinueLayout(R.string.enter_number_button) {
        enterNumberPresenter.requestOTP(callingCode, enter_number_phone_number.text.toString().trim())
    }

    fun showCheckInternetError() {
        alertDialog?.dismiss()
        alertDialog = AlertDialog.Builder(activity)
                .setMessage(R.string.generic_internet_error)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, null).show()
    }

}