package au.gov.health.covidsafe.factory

import android.os.Build
import au.gov.health.covidsafe.BuildConfig
import au.gov.health.covidsafe.networking.service.AwsClient
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


interface NetworkFactory {
    companion object {
        private val logging = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)

        val awsClient: AwsClient by lazy {
            RetrofitServiceGenerator.createService(AwsClient::class.java)
        }

        private const val USER_AGENT_TAG = "User-Agent"

        val okHttpClient: OkHttpClient by lazy {
            val okHttpClientBuilder = OkHttpClient.Builder()

            if (!okHttpClientBuilder.interceptors().contains(logging) && BuildConfig.DEBUG) {
                okHttpClientBuilder.addInterceptor(logging)
            }

            okHttpClientBuilder.addInterceptor { chain ->
                val request = chain.request()
                val newRequest = request.newBuilder()
                        .removeHeader(USER_AGENT_TAG)
                        .addHeader(USER_AGENT_TAG, getCustomUserAgent())
                        .build()
                chain.proceed(newRequest)
            }

            // This certificate pinning mechanism is only needed on Android 23 and lower.
            // For Android 24 and above, the pinning is set up in AndroidManifest.xml
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                // '**' in '**.aws.covidsafe.gov.au' is to include all sub domains
                val hostPattern = "**.aws.covidsafe.gov.au"

                val amazonRootCa1Sha256 = "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI="
                val amazonRootCa2Sha256 = "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE="
                val amazonRootCa3Sha256 = "sha256/NqvDJlas/GRcYbcWE8S/IceH9cq77kg0jVhZeAPXq8k="
                val amazonRootCa4Sha256 = "sha256/9+ze1cZgR9KO1kZrVDxA4HQ6voHRCSVNz4RdTCx4U8U="
                val sfsRootCag2Sha256 = "sha256/KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I="

                val certificatePinner = CertificatePinner.Builder()
                        .add(hostPattern,
                                amazonRootCa1Sha256,
                                amazonRootCa2Sha256,
                                amazonRootCa3Sha256,
                                amazonRootCa4Sha256,
                                sfsRootCag2Sha256
                        )
                        .build()
                okHttpClientBuilder.certificatePinner(certificatePinner)
            }

            okHttpClientBuilder.build()
        }

        private fun getCustomUserAgent(): String = "COVIDSafe/${BuildConfig.VERSION_NAME}" +
                " (build:${BuildConfig.VERSION_CODE}; android-${Build.VERSION.SDK_INT}) " + okhttp3.internal.userAgent

    }
}

object RetrofitServiceGenerator {
    private val builder = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())

    private var retrofit = builder.build()

    fun <S> createService(
            serviceClass: Class<S>): S {
        builder.client(NetworkFactory.okHttpClient)
        retrofit = builder.build()

        return retrofit.create(serviceClass)
    }
}