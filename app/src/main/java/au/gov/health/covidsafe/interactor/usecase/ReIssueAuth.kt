package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.request.ReIssueAuthRequest
import au.gov.health.covidsafe.networking.response.IssueInitialRefreshtokenResponse
import au.gov.health.covidsafe.networking.service.AwsClient

private const val TAG = "ReIssueAuth"

class ReIssueAuth(private val awsClient: AwsClient, lifecycle: Lifecycle, private val context: Context,
                    private val subject: String?, private val refreshToken: String?) : UseCase<IssueInitialRefreshtokenResponse, String>(lifecycle) {

    override suspend fun run(params: String): Either<Exception, IssueInitialRefreshtokenResponse> {
        return try {
                val response = awsClient.reIssueAuth(ReIssueAuthRequest(subject, refreshToken)).execute()

                when {
                    response?.code() == 200 -> {
                        response.body()?.let { body ->
                            CentralLog.d(TAG, "ReIssueAuth Success")
                            Success(body)
                        } ?: run {
                            CentralLog.d(TAG, "ReIssueAuth Invalid response")
                            Failure(GetReIssueAuthException.GetReIssueAuthExceptionServiceException(response.code()))
                        }
                    }
                    else -> {
                        CentralLog.d(TAG, "ReIssueAuth AWSAuthServiceError")
                        Failure(GetReIssueAuthException.GetReIssueAuthExceptionServiceException(response?.code()))
                    }
                }
            } catch (e: Exception) {
                Failure(e)
            }
    }
}

sealed class GetReIssueAuthException : Exception() {
    class GetReIssueAuthExceptionServiceException(val code: Int? = null) : GetReIssueAuthException()
}


