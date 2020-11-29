package au.gov.health.covidsafe.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import au.gov.health.covidsafe.BuildConfig
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
    val collectionMessageVisible = MutableLiveData<Boolean>()
    val heraldUpgradeMessage = MutableLiveData<Boolean>()
    // Show = true and hide = false
    val turnCaseNumber = MutableLiveData<Boolean>()
    lateinit var context: Context
    var turnCaseAfterOpenPage = true

    val awsClient: AwsClient by lazy {
        RetrofitServiceGenerator.createService(AwsClient::class.java)
    }

    fun fetchGetCaseStatistics(lifecycle: Lifecycle) {
        context = getApplication() as Context
        turnCaseNumber.value = Preference.getTurnCaseNumber(context)
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

    fun getCollectionMessage() {
        val context = getApplication() as Context

        val latestVersion = Preference.getBuildNumber(context)
        // When We want to show disclaimer to user after update, minVersionShowPolicy should be as same as the current version
        val minVersionShowPolicy = 74
        val minVersionHeraldPolicy = 89
        val currentVersion = BuildConfig.VERSION_CODE
        if (latestVersion == 0) {
            collectionMessageVisible.value = true
            heraldUpgradeMessage.value = true
        } else {
            heraldUpgradeMessage.value = currentVersion <= minVersionHeraldPolicy && currentVersion > latestVersion
            collectionMessageVisible.value = currentVersion <= minVersionShowPolicy && currentVersion > latestVersion
        }
        Preference.putBuildNumber(context, currentVersion)
    }

    private fun showErrorMessage() {
        viewModelScope.launch {

            isRefreshing.value = false

            context = getApplication() as Context
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

    fun turnCaseNumber(turnoff: Boolean) {
        turnCaseAfterOpenPage = false
        context = getApplication() as Context
        Preference.setTurnCaseNumber(context, turnoff)
        turnCaseNumber.value = turnoff
    }

    fun getTurningCaseAfterOpenPage() = turnCaseAfterOpenPage
}