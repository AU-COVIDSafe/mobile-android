package au.gov.health.covidsafe.scheduler

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.extensions.isBatteryOptimizationDisabled
import au.gov.health.covidsafe.extensions.isBlueToothEnabled
import au.gov.health.covidsafe.extensions.isLocationEnabledOnDevice
import au.gov.health.covidsafe.extensions.isLocationPermissionAllowed
import au.gov.health.covidsafe.factory.NetworkFactory.Companion.awsClient
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.MessagesResponse
import au.gov.health.covidsafe.scheduler.GetMessagesScheduler.mostRecentRecordTimestamp
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecord
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


private const val TAG = "GetMessagesScheduler"
private const val GET_MESSAGES_JOB_ID = 1
private const val ONE_HOURS_IN_MILLIS = 60 * 60 * 1000L
private const val FOUR_HOURS_IN_MILLIS = 4 * ONE_HOURS_IN_MILLIS
private const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * ONE_HOURS_IN_MILLIS
private const val SEVEN_DAYS_IN_MILLIS = 7 * TWENTY_FOUR_HOURS_IN_MILLIS

private const val HEALTH_CHECK_RESULT_OK = "OK"
private const val HEALTH_CHECK_RESULT_POSSIBLE_ERROR = "POSSIBLE_ERROR"

// for testing only
//private const val FOUR_HOURS_IN_MILLIS = 16 * 60 * 1000L

class GetMessagesJobSchedulerService : JobService() {
    private val mostRecentRecordLiveData = StreetPassRecordDatabase.getDatabase(TracerApp.AppContext).recordDao().getMostRecentRecord()

    private val mostRecentRecordObserver = androidx.lifecycle.Observer<StreetPassRecord?> {
        mostRecentRecordTimestamp = it?.timestamp ?: 0
        CentralLog.d(TAG, "mostRecentRecordObserver updates mostRecentRecordTimestamp to $mostRecentRecordTimestamp")
    }

    override fun onStartJob(params: JobParameters): Boolean {
        CentralLog.d(TAG, "onStartJob()")

        mostRecentRecordLiveData.observeForever(mostRecentRecordObserver)

        GlobalScope.launch(Dispatchers.IO) {
            delay(1000)

            GlobalScope.launch(Dispatchers.Main) {
                GetMessagesScheduler.getMessages(this@GetMessagesJobSchedulerService, params)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        CentralLog.d(TAG, "onStopJob()")

        mostRecentRecordLiveData.removeObserver(mostRecentRecordObserver)

        return true
    }
}

object GetMessagesScheduler {
    var mostRecentRecordTimestamp: Long = 0

    var messagesResponseCallback: ((MessagesResponse) -> Unit)? = null

    fun scheduleGetMessagesJob(callback: (MessagesResponse) -> Unit) {
        GlobalScope.launch(Dispatchers.Default) {
            CentralLog.d(TAG, "scheduleGetMessagesJob()")

            messagesResponseCallback = callback

            val context = TracerApp.AppContext

            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?)
                    ?.let {
                        CentralLog.d(TAG, "JobScheduler available")
                        val schedulerInMillis = getRandomMinsInMillis() + FOUR_HOURS_IN_MILLIS
                        CentralLog.d(TAG, "Next scheduled Jon run in ${(schedulerInMillis / 1000) / 60} mins")

                        it.schedule(
                                JobInfo.Builder(GET_MESSAGES_JOB_ID,
                                        ComponentName(context, GetMessagesJobSchedulerService::class.java))
                                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                        .setPersisted(true)
                                        .setPeriodic(schedulerInMillis)
                                        .build()
                        )
                    }
        }
    }

    private fun getRandomMinsInMillis(): Long {
        //We don't want everyone calling at the same time eg 9 am, 1 pm, etc we want to add an offset between 0-30 minutes based upon some random number.
        val randomSecs = Random().nextInt(30 * 60)
        return randomSecs * 1000L
    }

    fun getMessages(jobService: JobService? = null, params: JobParameters? = null) {
        val context = TracerApp.AppContext

        val jwtToken = Preference.getEncrypterJWTToken(context)
        val os = "android-${android.os.Build.VERSION.SDK_INT}"
        val appVersion = "${BuildConfig.VERSION_CODE}"
        val token = Preference.getFirebaseInstanceID(context)

        val isBlueToothEnabled = context.isBlueToothEnabled() != false
        val isBatteryOptimizationDisabled = context.isBatteryOptimizationDisabled() != false
        val isLocationPermissionAllowed = context.isLocationPermissionAllowed() != false
        val isLocationEnabledOnDevice = context.isLocationEnabledOnDevice()
        val isLastRecordWithinSevenDays =
                (System.currentTimeMillis() - mostRecentRecordTimestamp) <= SEVEN_DAYS_IN_MILLIS

        CentralLog.d(TAG, "isBlueToothEnabled = $isBlueToothEnabled")
        CentralLog.d(TAG, "isBatteryOptimizationDisabled = $isBatteryOptimizationDisabled")
        CentralLog.d(TAG, "isLocationPermissionAllowed = $isLocationPermissionAllowed")
        CentralLog.d(TAG, "isLocationEnabledOnDevice = $isLocationEnabledOnDevice")
        CentralLog.d(TAG, "isLastRecordWithinSevenDays = $isLastRecordWithinSevenDays (mostRecentRecordTimestamp = $mostRecentRecordTimestamp)")

        val healthCheck = if (
                isBlueToothEnabled &&
                isBatteryOptimizationDisabled &&
                isLocationPermissionAllowed &&
                isLocationEnabledOnDevice
        ) {
            HEALTH_CHECK_RESULT_OK
        } else {
            HEALTH_CHECK_RESULT_POSSIBLE_ERROR
        }

        val encountersHealth = if (isLastRecordWithinSevenDays) {
            HEALTH_CHECK_RESULT_OK
        } else {
            HEALTH_CHECK_RESULT_POSSIBLE_ERROR
        }

        CentralLog.d(TAG, "healthCheck = $healthCheck")

        val preferredLanguages = Locale.getDefault().language

        val messagesCall: Call<MessagesResponse> = awsClient.getMessages(
                "Bearer $jwtToken",
                os,
                appVersion,
                token,
                healthCheck,
                encountersHealth,
                preferredLanguages
        )

        CentralLog.d(TAG, "getMessages() to be called with InstanceID = $token")

        messagesCall.enqueue(object : Callback<MessagesResponse> {
            override fun onResponse(call: Call<MessagesResponse>, response: Response<MessagesResponse>) {
                val responseCode = response.code()

                if (responseCode == 200) {
                    CentralLog.d(TAG, "onResponse() got 200 response.")

                    response.body()?.let {
                        CentralLog.d(TAG, "onResponse() MessagesResponse = $it")

                        messagesResponseCallback?.invoke(it)
                    }
                } else {
                    CentralLog.w(TAG, "onResponse() got error response code = $responseCode.")
                }

                jobService?.jobFinished(params, false)
            }

            override fun onFailure(call: Call<MessagesResponse>, t: Throwable) {
                CentralLog.e(TAG, "onResponse() failed $t")

                jobService?.jobFinished(params, false)
            }
        })
    }
}