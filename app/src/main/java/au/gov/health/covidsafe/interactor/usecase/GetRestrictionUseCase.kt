package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.request.GetRestrictionRequest
import au.gov.health.covidsafe.networking.response.RestrictionResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference

private const val TAG = "GetCaseStatisticsUseCase"

class GetRestrictionUseCase(private val awsClient: AwsClient, lifecycle: Lifecycle, private val context: Context?, val state: String) : UseCase<RestrictionResponse, String>(lifecycle) {

    override suspend fun run(params: String): Either<Exception, RestrictionResponse> {
        val token = Preference.getEncrypterJWTToken(context)
        return token?.let { jwtToken ->
            try {
                CentralLog.d(TAG, "GetCaseStatisticsUseCase run request")
                val response = retryRetrofitCall {
                    awsClient.getRestriction("Bearer $jwtToken", state).execute()
                }

                when {
                    response?.code() == 200 -> {
                        response.body()?.let { body ->
                            Success(body)
                        } ?: run {
                            CentralLog.d(TAG, "GetCaseStatistics Invalid response")
                            Failure(GetRestrictionUseCaseException.GetRestrictionUseCaseServiceException(response.code()))
                        }
                    }
                    else -> {
                        CentralLog.d(TAG, "GetCaseStatistics AWSAuthServiceError")
                        Failure(GetRestrictionUseCaseException.GetRestrictionUseCaseServiceException(response?.code()))
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

sealed class GetRestrictionUseCaseException : Exception() {
    class GetRestrictionUseCaseServiceException(val code: Int? = null) : GetRestrictionUseCaseException()
}


