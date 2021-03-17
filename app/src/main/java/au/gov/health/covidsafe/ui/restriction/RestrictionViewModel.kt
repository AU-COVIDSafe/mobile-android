package au.gov.health.covidsafe.ui.restriction

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.factory.RetrofitServiceGenerator
import au.gov.health.covidsafe.interactor.usecase.GetRestrictionUseCase
import au.gov.health.covidsafe.networking.response.Activities
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class RestrictionViewModel(application: Application): AndroidViewModel(application) {

    val stateListVisible = MutableLiveData<Boolean>()
    val stateActivityListVisible = MutableLiveData<Boolean>()
    lateinit var context: Context
    val selectedState = MutableLiveData<String>()
    val selectedStateActivity = MutableLiveData<String>()
    val currentTime = MutableLiveData<String>()
    val linkVisible = MutableLiveData<Boolean>()
    val activityList = MutableLiveData<ArrayList<Activities>>()
    val errorLayout = MutableLiveData<Boolean>()

    val awsClient: AwsClient by lazy {
        RetrofitServiceGenerator.createService(AwsClient::class.java)
    }

    fun setup() {
        context = getApplication() as Context
        if (Preference.getSelectedRestrictionState(context) != "" && Preference.getSelectedRestrictionState(context) != null) {
            selectedState.value = Preference.getSelectedRestrictionState(context)
        }
    }
    init {
        stateListVisible.value = false
        stateActivityListVisible.value = false
        linkVisible.value = false
        errorLayout.value = false
    }

    fun setSelectedState(state: String, lifecycle: Lifecycle) {
        linkVisible.value = false
        stateListVisible.value = false
        selectedState.value = state
        Preference.putSelectedRestrictionState(context, state)
        selectedStateActivity.value = ""

        loadActivity(state, lifecycle)
    }

    fun loadActivity(state: String, lifecycle: Lifecycle) {
        errorLayout.value = false
        val stateAbrv = getState(state)
        if (stateAbrv.isEmpty()) {
            errorLayout.value = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {

                GetRestrictionUseCase(awsClient, lifecycle, getApplication(), stateAbrv).invoke("",
                        onSuccess = {
                            val list = ArrayList<Activities>()
                            it.activities?.forEach { activity ->
                                list.add(activity)
                            }
                            activityList.value = list
                        },
                        onFailure = {
                            errorLayout.value = true
                        }
                )
            }
        }
    }

    fun setSelectedStateActivity(activity: String?, activityTitle: String?, time: String?) {
        stateActivityListVisible.value = false
        selectedStateActivity.value = activityTitle
        linkVisible.value = true
        currentTime.value = time
    }

    fun tryAgain() {
        errorLayout.value = false
    }

    fun getState(state: String?): String {
        return when(state) {
            context.getString(R.string.australian_capital_territory) -> "act"
            context.getString(R.string.new_south_wales) -> "nsw"
            context.getString(R.string.northern_territory) -> "nt"
            context.getString(R.string.queensland) -> "qld"
            context.getString(R.string.south_australia) -> "sa"
            context.getString(R.string.tasmania) -> "tas"
            context.getString(R.string.victoria) -> "vic"
            context.getString(R.string.western_australia) -> "wa"
            else -> ""
        }
    }
}