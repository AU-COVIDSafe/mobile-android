package au.gov.health.covidsafe.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

fun Context.isInternetAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else{
        @Suppress("DEPRECATION")
        val allNetworkInfo = connectivityManager.allNetworkInfo

        for (networkInfo in allNetworkInfo) {
            @Suppress("DEPRECATION")
            if(networkInfo.isConnected){
                when(networkInfo.type){
                    ConnectivityManager.TYPE_MOBILE -> return true
                    ConnectivityManager.TYPE_WIFI -> return true
                    ConnectivityManager.TYPE_ETHERNET -> return true
                }
            }
        }
        return false
    }
}