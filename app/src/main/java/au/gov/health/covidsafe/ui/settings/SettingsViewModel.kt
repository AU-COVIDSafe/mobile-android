package au.gov.health.covidsafe.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import au.gov.health.covidsafe.factory.RetrofitServiceGenerator
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.networking.request.ChangePostcodeRequest
import au.gov.health.covidsafe.networking.response.UploadPostcodeResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsViewModel(application: Application): AndroidViewModel(application) {

    val postcodeUpdated = MutableLiveData<Boolean>()
    val showSpinner = MutableLiveData<Boolean>()
    lateinit var context: Context

    val awsClient: AwsClient by lazy {
        RetrofitServiceGenerator.createService(AwsClient::class.java)
    }

    fun getCurrentPostCode() {
        context = getApplication() as Context
    }

    init {
        postcodeUpdated.value = false
        showSpinner.value = false
    }

    fun changePostcode(postcode: String) {
        showSpinner.value = true

        val token = Preference.getEncrypterJWTToken(getApplication() as Context)
        val authenticate = Preference.getAuthenticate(context)
        if (authenticate) {
            val changePstcode: Call<UploadPostcodeResponse> = awsClient.changePostcode("Bearer $token", ChangePostcodeRequest(postcode))
            changePstcode.enqueue(object : Callback<UploadPostcodeResponse> {
                override fun onFailure(call: Call<UploadPostcodeResponse>, t: Throwable) {
                    onError()
                    showSpinner.value = false
                }

                override fun onResponse(call: Call<UploadPostcodeResponse>, response: Response<UploadPostcodeResponse>) {
                    if (response.code() == 200) {
                        postcodeUpdated.value = true
                        showSpinner.value = false
                    } else {
                        showSpinner.value = false
                    }
                }
            })
        } else {
            showSpinner.value = false
            onError()
        }
    }

    private fun onError() {

    }
}