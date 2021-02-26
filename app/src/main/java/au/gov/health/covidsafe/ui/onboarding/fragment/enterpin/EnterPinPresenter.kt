package au.gov.health.covidsafe.ui.onboarding.fragment.enterpin

import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.extensions.isInternetAvailable
import au.gov.health.covidsafe.factory.NetworkFactory
import au.gov.health.covidsafe.interactor.usecase.GetOnboardingOtp
import au.gov.health.covidsafe.interactor.usecase.GetOtpParams
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.request.AuthChallengeRequest
import au.gov.health.covidsafe.networking.response.AuthChallengeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EnterPinPresenter(private val enterPinFragment: EnterPinFragment,
                        private var session: String,
                        private var challengeName: String,
                        private val callingCode: Int,
                        private val phoneNumber: String) : LifecycleObserver {

    private val TAG = this.javaClass.simpleName

    private var awsClient = NetworkFactory.awsClient
    private lateinit var getOtp: GetOnboardingOtp

    init {
        enterPinFragment.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        getOtp = GetOnboardingOtp(awsClient, enterPinFragment.lifecycle)
    }

    internal fun resendCode() {
        enterPinFragment.activity?.let {
            val context = enterPinFragment.requireContext()

            getOtp.invoke(
                    GetOtpParams(
                            countryCode = "+$callingCode",
                            phoneNumber = phoneNumber,
                            deviceId = Preference.getDeviceID(context),
                            postCode = Preference.getPostCode(context),
                            age = Preference.getAge(context),
                            name = Preference.getName(context)
                    ),
                    onSuccess = {
                        session = it.session
                        challengeName = it.challengeName
                        enterPinFragment.resetTimer()
                    },
                    onFailure = {
                        if (context.isInternetAvailable()) {
                            enterPinFragment.showGenericError()
                        } else {
                            enterPinFragment.showCheckInternetError()
                        }
                    })
        }
    }

    internal fun validateOTP(otp: String) {
        if (TextUtils.isEmpty(otp) || otp.length != 6) {
            enterPinFragment.showErrorOtpMustBeSixDigits()
            return
        }

        enterPinFragment.disableContinueButton()
        enterPinFragment.showLoading()
        val authChallengeCall: Call<AuthChallengeResponse> = awsClient.respondToAuthChallenge(AuthChallengeRequest(session, otp))
        authChallengeCall.enqueue(object : Callback<AuthChallengeResponse> {
            override fun onResponse(call: Call<AuthChallengeResponse>, response: Response<AuthChallengeResponse>) {
                if (response.code() == 200) {
                    CentralLog.d(TAG, "code received")

                    val authChallengeResponse = response.body()

                    val handShakePin = authChallengeResponse?.pin
                    handShakePin?.let {
                        Preference.putHandShakePin(enterPinFragment.context, handShakePin)
                    }
                    val jwtToken = authChallengeResponse?.token
                    val refreshToken = authChallengeResponse?.refreshToken
                    jwtToken.let {
                        Preference.putEncrypterJWTToken(enterPinFragment.requireContext(), jwtToken)
                    }
                    refreshToken?.let {
                        Preference.putEncryptRefreshToken(enterPinFragment.requireContext(), refreshToken)
                    }
                    enterPinFragment.hideKeyboard()
                    enterPinFragment.navigateToNextPage()

                    val context = enterPinFragment.requireContext()
                    Preference.putBuildNumber(context, BuildConfig.VERSION_CODE)
                } else {
                    onError()
                }
            }

            override fun onFailure(call: Call<AuthChallengeResponse>, t: Throwable) {
                onError()
            }
        })
    }

    private fun onError() {
        enterPinFragment.enableContinueButton()
        enterPinFragment.hideLoading()
        enterPinFragment.hideKeyboard()
        enterPinFragment.showInvalidOtp()
    }
}