package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import retrofit2.Response
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.ui.utils.Utils
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.BroadcastMessageResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import kotlin.math.pow

private const val TAG = "UpdateBroadcastMessage"
private const val RETRIES_LIMIT = 3
private const val GET_TEMP_ID_API_VERSION = 2

class UpdateBroadcastMessageAndPerformScanWithExponentialBackOff(private val awsClient: AwsClient,
                                                                 private val context: Context,
                                                                 lifecycle: Lifecycle) : UseCase<BroadcastMessageResponse, Void?>(lifecycle) {

    override suspend fun run(params: Void?): Either<Exception, BroadcastMessageResponse> {
        val token = Preference.getEncrypterJWTToken(context)
        val authenticate = Preference.getAuthenticate(context)
        if (!authenticate) {  return Failure(Exception()) }
        return token?.let { jwtToken ->
            var response = call(jwtToken)
            var retryCount = 0
            while ((response == null || !response.isSuccessful || response.body() == null) && retryCount < RETRIES_LIMIT) {
                val interval = 2.toDouble().pow(retryCount.toDouble()).toLong() * 1000
                delay(interval)
                response = call(jwtToken)
                retryCount++
            }

            if (response != null && response.isSuccessful) {
                response.body()?.let { broadcastMessageResponse ->
                    if (broadcastMessageResponse.tempId.isNullOrEmpty()) {
                        Failure(Exception())
                    } else {
                        val expiryTime = broadcastMessageResponse.expiryTime
                        val expiry = expiryTime?.toLongOrNull() ?: 0
                        Preference.putExpiryTimeInMillis(context, expiry * 1000)
                        val refreshTime = broadcastMessageResponse.refreshTime
                        val refresh = refreshTime?.toLongOrNull() ?: 0
                        Preference.putNextFetchTimeInMillis(context, refresh * 1000)
                        Utils.storeBroadcastMessage(context, broadcastMessageResponse.tempId)
                        Success(broadcastMessageResponse)
                    }
                } ?: run {
                    Failure(Exception())
                }
            } else {
                Failure(Exception())
            }
        } ?: run {
            return Failure(Exception())
        }
    }

    private fun call(jwtToken: String): Response<BroadcastMessageResponse>? {
        return try {
            awsClient.getTempId(
                    "Bearer $jwtToken",
                    GET_TEMP_ID_API_VERSION
            ).execute()
        } catch (e: Exception) {
            CentralLog.e(TAG, " awsClient.getTempId() failed.", e)
            null
        }
    }

}