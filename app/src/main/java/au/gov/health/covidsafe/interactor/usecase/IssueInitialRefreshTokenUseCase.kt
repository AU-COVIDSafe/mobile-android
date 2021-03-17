package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.response.IssueInitialRefreshtokenResponse
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.preference.Preference

private const val TAG = "GetCaseStatisticsUseCase"

class IssueInitialRefreshTokenUseCase(private val awsClient: AwsClient, lifecycle: Lifecycle, private val context: Context) : UseCase<IssueInitialRefreshtokenResponse, String>(lifecycle) {

    override suspend fun run(params: String): Either<Exception, IssueInitialRefreshtokenResponse> {
        val token = Preference.getEncrypterJWTToken(context)
        return token?.let { jwtToken ->
             try {
                CentralLog.d(TAG, "GetCaseStatisticsUseCase run request")
                val response = awsClient.issueInitialRefreshToken("Bearer $jwtToken").execute()

                when {
                    response?.code() == 200 -> {
                        response.body()?.let { body ->
                            CentralLog.d(TAG, "IssueInitialRefreshTokenUseCase Success")
                            Success(body)
                        } ?: run {
                            CentralLog.d(TAG, "GetCaseStatistics Invalid response")
                            Failure(GetInitialRefreshtokenException.GetInitialRefreshtokenServiceException(response.code()))
                        }
                    }
                    else -> {
                        CentralLog.d(TAG, "GetCaseStatistics AWSAuthServiceError")
                        Failure(GetInitialRefreshtokenException.GetInitialRefreshtokenServiceException(response?.code()))
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

sealed class GetInitialRefreshtokenException : Exception() {
    class GetInitialRefreshtokenServiceException(val code: Int? = null) : GetInitialRefreshtokenException()
}


