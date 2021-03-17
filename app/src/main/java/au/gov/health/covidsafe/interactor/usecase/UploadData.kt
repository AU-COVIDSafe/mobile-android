package au.gov.health.covidsafe.interactor.usecase

import android.content.Context
import androidx.lifecycle.Lifecycle
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.app.TracerApp
import au.gov.health.covidsafe.interactor.Either
import au.gov.health.covidsafe.interactor.Failure
import au.gov.health.covidsafe.interactor.Success
import au.gov.health.covidsafe.interactor.UseCase
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.networking.service.AwsClient
import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecordStorage
import au.gov.health.covidsafe.ui.upload.model.ExportData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class UploadData(private val awsClient: AwsClient,
                 private val okHttpClient: OkHttpClient,
                 private val context: Context?,
                 lifecycle: Lifecycle)
    : UseCase<UseCase.None, String>(lifecycle) {

    private val TAG = this.javaClass.simpleName

    override suspend fun run(params: String): Either<Exception, None> {
        val token = Preference.getEncrypterJWTToken(context)
        context?.let {
            val authenticate = Preference.getAuthenticate(context)
            // Token is not authenticated
            if (!authenticate) {  return Failure(Exception("005")) }
        }
        return token?.let { jwtToken ->
            try {
                val initialUploadResponse = retryRetrofitCall {
                    awsClient.initiateUpload("Bearer $jwtToken", params).execute()
                }
                if (initialUploadResponse == null) {
                    // No data in response
                    Failure(Exception("100"))
                } else if (initialUploadResponse.isSuccessful) {
                    val uploadLink = initialUploadResponse.body()?.uploadLink
                    if (uploadLink.isNullOrEmpty()) {
                        // Upload link is null or empty
                        Failure(Exception("104"))
                    } else {
                        zipAndUploadData(uploadLink)
                    }
                } else if (initialUploadResponse.code() == 400) {
                    Failure(UploadDataException.UploadDataIncorrectPinException)
                } else if (initialUploadResponse.code() == 403) {
                    Failure(UploadDataException.UploadDataJwtExpiredException)
                } else {
                    // any other status code
                    Failure(Exception(initialUploadResponse.code().toString()))
                }
            } catch (e: Exception) {
                // unable to parse success response
                Failure(Exception("101"))
            }
        } ?: run {
            // Error getting db context
            return Failure(Exception("001"))
        }
    }

    private suspend fun zipAndUploadData(uploadLink: String): Either<Exception, None> {
        val exportedData = ExportData(StreetPassRecordStorage(TracerApp.AppContext).getAllRecords())
        CentralLog.d(TAG, "records: ${exportedData.records}")

        val jsonData = Gson().toJson(exportedData)

        val request = Request.Builder()
                .url(uploadLink)
                .put(jsonData.toRequestBody(null))
                .build()
        return try {
            val response = retryOkhttpCall { okHttpClient.newCall(request).execute() }
            return if (response == null) {
                // Error creating url obj
                Failure(Exception("102"))
            } else {
                Success(None)
            }
        } catch (e: Exception) {
            // Error encoding data to json
            Failure(Exception("003"))
        }
    }
}

sealed class UploadDataException : Exception() {
    object UploadDataIncorrectPinException : UploadDataException()
    object UploadDataJwtExpiredException : UploadDataException()
}
