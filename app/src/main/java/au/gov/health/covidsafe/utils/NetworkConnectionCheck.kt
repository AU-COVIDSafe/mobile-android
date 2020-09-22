package au.gov.health.covidsafe.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import au.gov.health.covidsafe.ui.utils.Utils
import kotlin.collections.ArrayList


object NetworkConnectionCheck {

    private var networkConnectionListener: ArrayList<NetworkConnectionListener>? = null

    private var isInternetConnected:Boolean? = null

    fun addNetworkChangedListener(context: Context, listener: NetworkConnectionListener) {
        if (networkConnectionListener == null) {
            networkConnectionListener = ArrayList()
            registerNetworkChange(context)
        }

        isInternetConnected?.let {
            listener.onNetworkStatusChanged(it)
        }

        networkConnectionListener?.add(listener)
    }

    fun removeNetworkChangedListener(listener: NetworkConnectionListener) {
        networkConnectionListener?.let {
            val i: Int = it.indexOf(listener)
            if (i >= 0) {
                it.removeAt(i)
            }
        }
    }

    private fun registerNetworkChange(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                it.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        networkStatusUpdate(true)
                    }

                    override fun onLost(network: Network?) {
                        networkStatusUpdate(false)
                    }
                })
            } else {
                it.registerNetworkCallback(NetworkRequest.Builder().build(),
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network?) {
                                networkStatusUpdate(true)
                            }

                            override fun onLost(network: Network?) {
                                networkStatusUpdate(false)
                            }
                        })
            }
        }
    }

    private fun networkStatusUpdate(isAvailable: Boolean) {
        if (!isAvailable) {
            isInternetConnected = isAvailable
            sendNetworkStatusToListeners(isAvailable)
        } else {
            checkInternetConnection()
        }
    }

    private fun checkInternetConnection() {
        Utils.checkInternetConnectionToGoogle {
            isInternetConnected = it
            sendNetworkStatusToListeners(it)
        }
    }

    private fun sendNetworkStatusToListeners(isAvailable: Boolean) {
        networkConnectionListener?.forEach {
            it.onNetworkStatusChanged(isAvailable)
        }
    }

    interface NetworkConnectionListener {
        fun onNetworkStatusChanged(isAvailable: Boolean)
    }
}