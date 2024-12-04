package com.codder.openphonetest

import com.codder.openphonetest.api.IRemoteDataSource
import com.codder.openphonetest.api.KtorBuilderForPlatform
import com.codder.openphonetest.api.RemoteDataSource
import com.codder.openphonetest.localDatasource.GitUserLocalDatasource
import com.codder.openphonetest.localDatasource.IGitUserLocalDatasource
import com.codder.openphonetest.localDatasource.SqlDelightDatabase
import com.codder.openphonetest.localDatasource.SqlDelightDriverFactory
import com.codder.openphonetest.networkState.INetworkStateCallbacksManager
import com.codder.openphonetest.networkState.INetworkStateProvider
import com.codder.openphonetest.networkState.NetworkStateCallbacksManager
import com.codder.openphonetest.repositories.GitUserDataRepository
import com.codder.openphonetest.repositories.IGitUserDataRepository
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIDevice
import org.koin.core.component.get

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

class KoinHelper : KoinComponent {
    inline fun <reified T> getDependency(): T = get()
}

fun initKoinIos(
    networkStateProvider: INetworkStateProvider// The external dependency provided by the client app
) = startKoin {

    modules(
        module {
            // Provide the external dependency
            single { networkStateProvider }

            single { SqlDelightDriverFactory() }

            single <IRemoteDataSource>{ RemoteDataSource(KtorBuilderForPlatform()) }
            single <IGitUserLocalDatasource>{ GitUserLocalDatasource(SqlDelightDatabase(get())) }
            single <INetworkStateCallbacksManager>{ NetworkStateCallbacksManager(get()) }

            // Other dependencies
            single<IGitUserDataRepository> {
                GitUserDataRepository(
                    remoteDataSource = get(),
                    gitUserLocalDatasource = get(),
                    networkStateCallbackManager = get()
                )
            }
        }
    )
}

actual fun getPlatform(): Platform = IOSPlatform()