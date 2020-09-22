package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.CaseStatisticResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference

private const val TAG = "GetCaseStatisticsUseCase"

class GetCaseStatisticsUseCase(private val awsClient: AwsClient, lifecycle: Lifecycle, private val context: Context?) : UseCase<CaseStatisticResponse, String>(lifecycle) {

    override suspend fun run(params: String): Either<Exception, CaseStatisticResponse> {
        val token = Preference.getEncrypterJWTToken(context)
        return token?.let { jwtToken ->
             try {
                CentralLog.d(TAG, "GetCaseStatisticsUseCase run request")
                val response = retryRetrofitCall { awsClient.getCaseStatistics("Bearer $jwtToken").execute() }

                when {
                    response?.code() == 200 -> {
                        response.body()?.let { body ->
                            CentralLog.d(TAG, "GetCaseStatistics Success: ${body.national}")
                            Success(body)
                        } ?: run {
                            CentralLog.d(TAG, "GetCaseStatistics Invalid response")
                            Failure(GetCaseStatisticsException.GetGetCaseStatisticsServiceException(response.code()))
                        }
                    }
                    else -> {
                        CentralLog.d(TAG, "GetCaseStatistics AWSAuthServiceError")
                        Failure(GetCaseStatisticsException.GetGetCaseStatisticsServiceException(response?.code()))
                    }
                }
            } catch (e: Exception) {
                Failure(e)
            }
        } ?: run {
            return Failure(Exception())
        }
    }
}

sealed class GetCaseStatisticsException : Exception() {
    class GetGetCaseStatisticsServiceException(val code: Int? = null) : GetCaseStatisticsException()
}


