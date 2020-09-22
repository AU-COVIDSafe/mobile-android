package au.gov.health.covidsafe.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import au.gov.health.covidsafe.factory.RetrofitServiceGenerator
import au.gov.health.covidsafe.interactor.usecase.GetCaseStatisticsUseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.CaseStatisticResponse
import au.gov.health.covidsafe.extensions.isInternetAvailable
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "HomeFragmentViewModel"

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val caseNumberDataState = MutableLiveData<CaseNumbersState>()
    val caseStatisticsLiveData = MutableLiveData<CaseStatisticResponse>()
    val isRefreshing = MutableLiveData<Boolean>()

    val awsClient: AwsClient by lazy {
        RetrofitServiceGenerator.createService(AwsClient::class.java)
    }

    fun fetchGetCaseStatistics(lifecycle: Lifecycle) {
        if(caseNumberDataState.value != CaseNumbersState.LOADING) {
            caseNumberDataState.value = CaseNumbersState.LOADING

            viewModelScope.launch(Dispatchers.IO) {

                GetCaseStatisticsUseCase(awsClient, lifecycle, getApplication()).invoke("",
                        onSuccess = {
                            updateOnSuccess(it)
                        },
                        onFailure = {
                            CentralLog.e(TAG, "On Failure: ${it.message}")
                            showErrorMessage()
                        }
                )
            }
        } else {
            isRefreshing.value = false
        }
    }

    private fun updateOnSuccess(caseStatisticResponse: CaseStatisticResponse) {
        viewModelScope.launch {
            isRefreshing.value = false

            CentralLog.d(TAG, "On Success: ${caseStatisticResponse.vic?.totalCases}")
            caseNumberDataState.value = CaseNumbersState.SUCCESS
            cacheCaseStatisticDataInPersistent(caseStatisticResponse)
            caseStatisticsLiveData.value = caseStatisticResponse
        }
    }

    private fun showErrorMessage() {
        viewModelScope.launch {

            isRefreshing.value = false

            val context = getApplication() as Context
            caseStatisticsLiveData.value = getCachedCaseStatisticDataFromPersistent(context)
            if (context.isInternetAvailable()) {
                caseNumberDataState.value = CaseNumbersState.ERROR_UNKNOWN
            } else {
                caseNumberDataState.value = CaseNumbersState.ERROR_NO_NETWORK
            }
        }
    }

    private fun cacheCaseStatisticDataInPersistent(caseStatisticResponse: CaseStatisticResponse) {
        Preference.putCaseStatisticData(getApplication(), Gson().toJson(caseStatisticResponse))
    }

    private fun getCachedCaseStatisticDataFromPersistent(context: Context): CaseStatisticResponse? {
        val caseStatisticString = Preference.getCaseStatisticData(context)
        return caseStatisticString?.let {
            val caseStatisticData = Gson().fromJson(it, CaseStatisticResponse::class.java)
            CentralLog.d(TAG, "On Preference: ${caseStatisticData.vic?.totalCases}")
            caseStatisticData
        }
    }
}