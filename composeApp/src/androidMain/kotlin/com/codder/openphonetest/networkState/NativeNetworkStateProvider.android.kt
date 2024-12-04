package com.codder.openphonetest.networkState

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo

class NativeNetworkStateProvider(private val context: Context): INetworkStateProvider {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var callbackProvider: NetworkStateCallbackProvider? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override suspend fun subscribe() {

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                callbackProvider?.onResultReceived(NetworkStates.Connected)
            }

            override fun onLost(network: Network) {
                callbackProvider?.onResultReceived(NetworkStates.Lost)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        callbackProvider?.onResultReceived(if (isNetworkAvailable(connectivityManager)) NetworkStates.Connected else NetworkStates.Lost)
    }

    override fun unsubscribe() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }


    override fun setCallbackProviderForPlatform(provider: NetworkStateCallbackProvider) {
        this.callbackProvider = provider
    }

    fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

}