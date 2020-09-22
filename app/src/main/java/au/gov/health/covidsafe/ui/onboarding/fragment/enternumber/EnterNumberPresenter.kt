package au.gov.health.covidsafe.ui.onboarding.fragment.enternumber


import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.extensions.isInternetAvailable
import au.gov.health.covidsafe.factory.NetworkFactory
import au.gov.health.covidsafe.interactor.usecase.GetOnboardingOtp
import au.gov.health.covidsafe.interactor.usecase.GetOnboardingOtpException
import au.gov.health.covidsafe.interactor.usecase.GetOtpParams

const val AUSTRALIA_CALLING_CODE = 61
const val AUSTRALIA_MOBILE_NUMBER_LENGTH = 9
const val AUSTRALIA_MOBILE_NUMBER_PREFIX_DIGIT = "0"
const val NORFOLK_ISLAND_CALLING_CODE = 672
const val NORFOLK_ISLAND_MOBILE_NUMBER_LENGTH = 5
const val NORFOLK_ISLAND_MOBILE_PREFIX_DIGIT = "3"

class EnterNumberPresenter(private val enterNumberFragment: EnterNumberFragment) : LifecycleObserver {

    private lateinit var getOnboardingOtp: GetOnboardingOtp

    init {
        enterNumberFragment.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        getOnboardingOtp = GetOnboardingOtp(NetworkFactory.awsClient, enterNumberFragment.lifecycle)
    }

    fun requestOTP(callingCode: Int, phoneNumber: String) {
        val prefixZeroRemovedPhoneNumber =
                adjustPrefixForAustralianAndNorfolkPhoneNumber(callingCode, phoneNumber)

        makeOTPCall(callingCode, prefixZeroRemovedPhoneNumber)
    }

    /**
     * if [callingCode] is 61 for Australia, then [phoneNumber] should be a prefix removed
     * Australian phone number: 9 digits, doesn't start with 0.
     * Otherwise [phoneNumber] can be any number and the validation should be done in the backend
     */
    private fun makeOTPCall(callingCode: Int, phoneNumber: String) {
        enterNumberFragment.activity?.let {
            enterNumberFragment.disableContinueButton()
            enterNumberFragment.showLoading()

            val context = enterNumberFragment.requireContext()

            getOnboardingOtp.invoke(
                    GetOtpParams(
                            countryCode = "+$callingCode",
                            phoneNumber = phoneNumber,
                            deviceId = Preference.getDeviceID(context),
                            postCode = Preference.getPostCode(context),
                            age = Preference.getAge(context),
                            name = Preference.getName(context)
                    ),
                    onSuccess = {
                        enterNumberFragment.navigateToOTPPage(
                                it.session,
                                it.challengeName,
                                callingCode,
                                phoneNumber)
                    },
                    onFailure = {
                        when {
                            it is GetOnboardingOtpException.GetOtpInvalidNumberException -> {
                                enterNumberFragment.showInvalidPhoneNumberPrompt(R.string.invalid_phone_number)
                            }
                            context.isInternetAvailable() -> {
                                enterNumberFragment.showGenericError()
                            }
                            else -> {
                                enterNumberFragment.showCheckInternetError()
                            }
                        }

                        enterNumberFragment.hideLoading()
                        enterNumberFragment.enableContinueButton()
                    })
        }
    }

    fun validatePhoneNumber(callingCode: Int, phoneNumber: String): Pair<Boolean, Int> {
        val isNumberValid = when (callingCode) {
            AUSTRALIA_CALLING_CODE -> {
                if (phoneNumber.startsWith(AUSTRALIA_MOBILE_NUMBER_PREFIX_DIGIT)) {
                    phoneNumber.length == AUSTRALIA_MOBILE_NUMBER_LENGTH + 1
                } else {
                    phoneNumber.length == AUSTRALIA_MOBILE_NUMBER_LENGTH
                }
            }
            NORFOLK_ISLAND_CALLING_CODE -> {
                if (phoneNumber.startsWith(NORFOLK_ISLAND_MOBILE_PREFIX_DIGIT)) {
                    phoneNumber.length == NORFOLK_ISLAND_MOBILE_NUMBER_LENGTH + 1
                } else {
                    phoneNumber.length == NORFOLK_ISLAND_MOBILE_NUMBER_LENGTH
                }
            }
            else -> true
        }

        val errorMessageResID = when (callingCode) {
            AUSTRALIA_CALLING_CODE -> R.string.invalid_australian_phone_number_error_prompt
            NORFOLK_ISLAND_CALLING_CODE -> R.string.invalid_norfolk_island_phone_number_error_prompt
            else -> 0
        }

        return Pair(isNumberValid, errorMessageResID)
    }

    private fun adjustPrefixForAustralianAndNorfolkPhoneNumber(callingCode: Int, phoneNumber: String): String {
        return when (callingCode) {
            AUSTRALIA_CALLING_CODE -> phoneNumber.removePrefix(AUSTRALIA_MOBILE_NUMBER_PREFIX_DIGIT)
            NORFOLK_ISLAND_CALLING_CODE -> {
                if (phoneNumber.length == NORFOLK_ISLAND_MOBILE_NUMBER_LENGTH) {
                    "$NORFOLK_ISLAND_MOBILE_PREFIX_DIGIT$phoneNumber"
                } else {
                    phoneNumber
                }
            }
            else -> phoneNumber
        }
    }

}