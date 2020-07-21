package au.gov.health.covidsafe.scheduler

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.Preference
import au.gov.health.covidsafe.TracerApp
import au.gov.health.covidsafe.factory.NetworkFactory.Companion.awsClient
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.MessagesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private const val TAG = "GetMessagesScheduler"
private const val GET_MESSAGES_JOB_ID = 1
private const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * 60 * 60 * 1000L

// for testing only
//private const val TWENTY_FOUR_HOURS_IN_MILLIS = 16 * 60 * 1000L

class GetMessagesJobSchedulerService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        CentralLog.d(TAG, "onStartJob()")
        GetMessagesScheduler.getMessages(this, params)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        CentralLog.d(TAG, "onStopJob()")
        return true
    }
}

object GetMessagesScheduler {
    fun scheduleGetMessagesJob() {
        CentralLog.d(TAG, "scheduleGetMessagesJob()")

        val context = TracerApp.AppContext

        (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?)
                ?.let {
                    CentralLog.d(TAG, "JobScheduler available")

                    it.schedule(
                            JobInfo.Builder(GET_MESSAGES_JOB_ID,
                                    ComponentName(context, GetMessagesJobSchedulerService::class.java))
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                    .setPersisted(true)
                                    .setPeriodic(TWENTY_FOUR_HOURS_IN_MILLIS)
                                    .build()
                    )
                }
    }

    fun getMessages(jobService: JobService? = null, params: JobParameters? = null) {
        val context = TracerApp.AppContext

        val jwtToken = Preference.getEncrypterJWTToken(context)
        val os = "android-${android.os.Build.VERSION.SDK_INT}"
        val appVersion = "${BuildConfig.VERSION_CODE}"
        val token = Preference.getFirebaseInstanceID(context)

        val messagesCall: Call<MessagesResponse> = awsClient.getMessages(
                "Bearer $jwtToken",
                os,
                appVersion,
                token
        )

        CentralLog.d(TAG, "getMessages() to be called with InstanceID = $token")

        messagesCall.enqueue(object : Callback<MessagesResponse> {
            override fun onResponse(call: Call<MessagesResponse>, response: Response<MessagesResponse>) {
                val responseCode = response.code()
                if (responseCode == 200) {
                    CentralLog.d(TAG, "onResponse() got 200 response.")

                    response.body()?.let {
                        CentralLog.d(TAG, "onResponse() MessagesResponse = $it")
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