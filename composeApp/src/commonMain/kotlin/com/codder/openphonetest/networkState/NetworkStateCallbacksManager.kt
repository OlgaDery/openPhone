package com.codder.openphonetest.networkState

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface INetworkStateCallbacksManager {
    fun networkStateFlow(): Flow<NetworkStates>
}


class NetworkStateCallbacksManager(
    private val networkStateProvider: INetworkStateProvider
): INetworkStateCallbacksManager {

    private val _networkUpdates: Flow<NetworkStates> = callbackFlow {
        val callbackProviderImpl = object : NetworkStateCallbackProvider {

            override fun onResultReceived(state: NetworkStates) {
                trySend(state)
            }
        }

        networkStateProvider.setCallbackProviderForPlatform(callbackProviderImpl)
        networkStateProvider.subscribe()

        awaitClose {
            networkStateProvider.unsubscribe()
        }
    }


    override fun networkStateFlow(): Flow<NetworkStates> {
        return _networkUpdates
    }

}

interface NetworkStateCallbackProvider {
    fun onResultReceived(state: NetworkStates)
}

interface INetworkStateProvider {
    suspend fun subscribe()
    fun unsubscribe()
    fun setCallbackProviderForPlatform(provider: NetworkStateCallbackProvider)
}

enum class NetworkStates {
    Connected, Lost
}