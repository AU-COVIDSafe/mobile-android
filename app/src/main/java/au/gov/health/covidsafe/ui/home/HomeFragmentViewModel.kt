package au.gov.health.covidsafe.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.extensions.isInternetAvailable
import au.gov.health.covidsafe.factory.RetrofitServiceGenerator
import au.gov.health.covidsafe.interactor.usecase.GetCaseStatisticsUseCase
import au.gov.health.covidsafe.interactor.usecase.IssueInitialRefreshTokenUseCase
import au.gov.health.covidsafe.interactor.usecase.ReIssueAuth
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.CaseDetailsData
import au.gov.health.covidsafe.networking.response.CaseStatisticResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "HomeFragmentViewModel"

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val caseNumberDataState = MutableLiveData<CaseNumbersState>()
    val caseStatisticsLiveData = MutableLiveData<CaseStatisticResponse>()
    val caseStateStatisticsLiveData = MutableLiveData<CaseStatisticResponse>()
    val isRefreshing = MutableLiveData<Boolean>()
    val collectionMessageVisible = MutableLiveData<Boolean>()
    val heraldUpgradeMessage = MutableLiveData<Boolean>()
    // Show = true and hide = false
    val turnCaseNumber = MutableLiveData<Boolean>()
    lateinit var context: Context
    var turnCaseAfterOpenPage = true
    val visibleSelectStateLayout = MutableLiveData<Boolean>()
    val newCase = MutableLiveData<Int>()
    val localCase = MutableLiveData<String>()
    val overseaCase = MutableLiveData<String>()
    val activeCase = MutableLiveData<Int>()
    val totalDeaths = MutableLiveData<String>()
    val titleOfNumber = MutableLiveData<String>()
    val hotSpotTitle = MutableLiveData<String>()
    val hotSpotLink = MutableLiveData<String>()
    val locallyAquired = MutableLiveData<String>()
    val aquiredOversea = MutableLiveData<String>()
    val totalyDeathe = MutableLiveData<String>()
    val isV2Available = MutableLiveData<Boolean>()
    val reIssueFail = MutableLiveData<Boolean>(false)
    val reIssueSuccess = MutableLiveData<Boolean>(false)
    val getStatisticSuccessfull = MutableLiveData<Boolean>(false)
    var reIssueFailOnRefreshToken = false

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val awsClient: AwsClient by lazy {
        RetrofitServiceGenerator.createService(AwsClient::class.java)
    }

    fun fetchGetCaseStatistics(lifecycle: Lifecycle, afterReIssue: Boolean = false) {
        isV2Available.value = false
        context = getApplication() as Context
        titleOfNumber.value = context.getString(R.string.national_numbers)
        turnCaseNumber.value = Preference.getTurnCaseNumber(context)
        if((caseNumberDataState.value != CaseNumbersState.LOADING) || afterReIssue) {
            caseNumberDataState.value = CaseNumbersState.LOADING

            viewModelScope.launch(Dispatchers.IO) {

                GetCaseStatisticsUseCase(awsClient, lifecycle, getApplication()).invoke("",
                        onSuccess = {
                            getStatisticSuccessfull.value = true
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

    fun getRefreshToken(lifecycle: Lifecycle) {
        context = getApplication() as Context
        viewModelScope.launch(Dispatchers.IO) {

            IssueInitialRefreshTokenUseCase(awsClient, lifecycle, getApplication()).invoke("",
                    onSuccess = {
                        it.refreshToken.let {
                            Preference.putEncryptRefreshToken(context, it)
                        }
                        it.token.let {
                            Preference.putEncrypterJWTToken(context, it)
                        }
                        fetchGetCaseStatistics(lifecycle, true)
                        Preference.setAuthenticate(context, true)
                        reIssueSuccess.value = true
                    },
                    onFailure = {
                        reIssueFailOnRefreshToken = true
                        reIssueFail.value = true
                        Preference.setAuthenticate(context, false)
                        CentralLog.e(TAG, "On Failure: ${it.message}")
                    }
            )
        }
    }

    fun getReissueAuth(lifecycle: Lifecycle) {
        if (!Preference.getAuthenticate(context)) {
            reIssueFail.value = true
            return
        }
        Preference.setAuthenticate(context, false)
        var subject: String? = null
        context = getApplication() as Context

        val token = Preference.getEncrypterJWTToken(context)
        val refreshToken = Preference.getEncryptRefreshToken(context)

        val tokenSeparate = token?.split(".")
        var subjectItem: String? = null
        if (tokenSeparate?.size !=null && tokenSeparate.size >= 3) {
            subjectItem = tokenSeparate.let {
                it[1]
            }
        }

        var subjectByte: ByteArray? = null
        subjectItem?.let { subjectByte =  android.util.Base64.decode(subjectItem, android.util.Base64.DEFAULT)}
        val charset = Charsets.UTF_8

        subjectByte?.let {
            val jsonModel = String(it, charset)
            val jsonObj = JSONObject(jsonModel)
            subject = jsonObj.get("sub").toString()
        }
        if (subject.isNullOrEmpty()) {
            reIssueFail.value = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {

                ReIssueAuth(awsClient, lifecycle, getApplication(), subject, refreshToken).invoke("",
                        onSuccess = {
                            it.refreshToken.let {
                                Preference.putEncryptRefreshToken(context, it)
                            }
                            it.token.let {
                                Preference.putEncrypterJWTToken(context, it)
                            }
                            Preference.setAuthenticate(context, true)
                            reIssueSuccess.value = true
                            fetchGetCaseStatistics(lifecycle, true)
                        },
                        onFailure = {
                            Preference.setAuthenticate(context, false)
                            reIssueFail.value = true
                        }
                )
            }
        }
    }

    fun getReissueOnRefreshToken(): Boolean{
        return reIssueFailOnRefreshToken
    }
    private fun updateOnSuccess(caseStatisticResponse: CaseStatisticResponse) {
        viewModelScope.launch {
            isRefreshing.value = false

            CentralLog.d(TAG, "On Success: ${caseStatisticResponse.vic?.totalCases}")
            caseNumberDataState.value = CaseNumbersState.SUCCESS
            cacheCaseStatisticDataInPersistent(caseStatisticResponse)
            caseStatisticsLiveData.value = caseStatisticResponse

            if ( caseStatisticResponse.version != null) {
                isV2Available.value = true
            }
            caseStateStatisticsLiveData.value = caseStatisticResponse
            selectState()
        }
    }

    fun getCollectionMessage() {
        val context = getApplication() as Context

        val latestVersion = Preference.getBuildNumber(context)
        // When We want to show disclaimer to user after update, minVersionShowPolicy should be as same as the current version
        val minVersionShowPolicy = 74
        val minVersionHeraldPolicy = 94
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

    fun showSelectSate(visibleLayout: Boolean) {
        visibleSelectStateLayout.value = visibleLayout
    }

    fun getTurningCaseAfterOpenPage() = turnCaseAfterOpenPage
    fun setSelectedState(state: String) {
        Preference.putSelectedState(context, state)
    }

    fun selectState() {
        visibleSelectStateLayout.value = false
        val stateResponse: CaseDetailsData?
        locallyAquired.value = context.getString(R.string.locally_acquired).replace("%@", "")
        aquiredOversea.value = context.getString(R.string.overseas_acquired).replace("%@", "")
        totalyDeathe.value = context.getString(R.string.total_deaths).replace("%@", "")

        when (Preference.getSelectedState(context).toString()) {
           "Australia" -> {
               stateResponse = caseStateStatisticsLiveData.value?.national
               recordStateData(stateResponse, "National")
           }
            "Australian Capital Territory" -> {
                stateResponse = caseStateStatisticsLiveData.value?.act
                recordStateData(stateResponse, "ACT")
                hotSpotLink.value = context.getString(R.string.hotspot_link_ACT)
            }
            "New South Wales" -> {
                stateResponse = caseStateStatisticsLiveData.value?.nsw
                recordStateData(stateResponse, "NSW")
                hotSpotLink.value = context.getString(R.string.hotspot_link_NSW)
            }
            "Northern Territory" -> {
                stateResponse = caseStateStatisticsLiveData.value?.nt
                recordStateData(stateResponse, "NT")
                hotSpotLink.value = context.getString(R.string.hotspot_link_NT)
            }
            "Queensland" -> {
                stateResponse = caseStateStatisticsLiveData.value?.qld
                recordStateData(stateResponse, "QLD")
                hotSpotLink.value = context.getString(R.string.hotspot_link_QLD)
            }
            "South Australia" -> {
                stateResponse = caseStateStatisticsLiveData.value?.sa
                recordStateData(stateResponse, "SA")
                hotSpotLink.value = context.getString(R.string.hotspot_link_SA)
            }
            "Tasmania" -> {
                stateResponse = caseStateStatisticsLiveData.value?.tas
                recordStateData(stateResponse, "TAS")
                hotSpotLink.value = context.getString(R.string.hotspot_link_TAS)
            }
            "Victoria" -> {
                stateResponse = caseStateStatisticsLiveData.value?.vic
                recordStateData(stateResponse, "VIC")
                hotSpotLink.value = context.getString(R.string.hotspot_link_VIC)
            }
            "Western Australia" -> {
                stateResponse = caseStateStatisticsLiveData.value?.wa
                recordStateData(stateResponse, "WA")
                hotSpotLink.value = context.getString(R.string.hotspot_link_WA)
            }
            else ->  {
            stateResponse = caseStateStatisticsLiveData.value?.national
            recordStateData(stateResponse, "National")
            }
        }
    }

    private fun recordStateData(stateResponse: CaseDetailsData?, stateName: String?) {
        newCase.value = if (stateResponse?.newCases != null) { stateResponse.newCases} else { 0 }
        localCase.value = if (stateResponse?.locallyAcquired != null) { stateResponse.newLocallyAcquired.toString()} else { "0" }
        overseaCase.value = if (stateResponse?.overseasAcquired != null) { stateResponse.newOverseasAcquired.toString()} else { "0" }
        activeCase.value = if (stateResponse?.activeCases != null) { stateResponse.activeCases} else { 0 }
        totalDeaths.value = if (stateResponse?.deaths != null) { stateResponse.deaths.toString()} else { "0" }
        titleOfNumber.value = "$stateName numbers"
        stateName?.let {
            val hotSpot = context.getString(R.string.hotspots_state_territory)
            hotSpotTitle.value = hotSpot.replace("%@", it, false)
        }
    }
}