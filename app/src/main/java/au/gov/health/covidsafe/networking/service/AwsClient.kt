package au.gov.health.covidsafe.networking.service

import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.networking.response.CaseStatisticResponse
import au.gov.health.covidsafe.networking.request.AuthChallengeRequest
import au.gov.health.covidsafe.networking.request.ChangePostcodeRequest
import au.gov.health.covidsafe.networking.request.OTPChallengeRequest
import au.gov.health.covidsafe.networking.response.*
import retrofit2.Call
import retrofit2.http.*

interface AwsClient {

    @POST(BuildConfig.END_POINT_PREFIX + "/initiateAuth")
    fun initiateAuth(@Body body: OTPChallengeRequest): Call<OTPChallengeResponse>

    @POST(BuildConfig.END_POINT_PREFIX + "/respondToAuthChallenge")
    fun respondToAuthChallenge(@Body body: AuthChallengeRequest): Call<AuthChallengeResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/getTempId")
    fun getTempId(
            @Header("Authorization") jwtToken: String?,
            @Query("version") apiVersion: Int
    ): Call<BroadcastMessageResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/initiateDataUpload")
    fun initiateUpload(
            @Header("Authorization") jwtToken: String?,
            @Header("pin") pin: String
    ): Call<InitiateUploadResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/initiateDataUpload")
    fun initiateReUpload(@Header("Authorization") jwtToken: String?): Call<InitiateUploadResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/requestUploadOtp")
    fun requestUploadOtp(@Header("Authorization") jwtToken: String?): Call<UploadOTPResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/messages")
    fun getMessages(
            @Header("Authorization") jwtToken: String?,
            @Query("os") os: String,
            @Query("appversion") appversion: String,
            @Query("token") token: String,
            @Query("healthcheck") healthcheck: String,
            @Query("encountershealth") encountershealth: String,
            @Query("preferredlanguages") preferredLanguages: String
    ): Call<MessagesResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/statistics")
    fun getCaseStatistics(@Header("Authorization") jwtToken: String?): Call<CaseStatisticResponse>

    @GET(BuildConfig.END_POINT_PREFIX + "/v2/statistics")
    fun getCaseStatisticsVersion2(@Header("Authorization") jwtToken: String?): Call<CaseStatisticResponse>

    @POST(BuildConfig.END_POINT_PREFIX + "/device")
    fun changePostcode(@Header("Authorization") jwtToken: String?,
                       @Body body: ChangePostcodeRequest): Call<UploadPostcodeResponse>
}