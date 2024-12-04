package com.codder.openphonetest

import android.app.Application
import com.codder.openphonetest.api.IRemoteDataSource
import com.codder.openphonetest.api.KtorBuilderForPlatform
import com.codder.openphonetest.api.RemoteDataSource
import com.codder.openphonetest.localDatasource.GitUserLocalDatasource
import com.codder.openphonetest.localDatasource.IGitUserLocalDatasource
import com.codder.openphonetest.localDatasource.SqlDelightDatabase
import com.codder.openphonetest.localDatasource.SqlDelightDriverFactory
import com.codder.openphonetest.networkState.INetworkStateCallbacksManager
import com.codder.openphonetest.networkState.NativeNetworkStateProvider
import com.codder.openphonetest.networkState.NetworkStateCallbacksManager
import com.codder.openphonetest.repositories.GitUserDataRepository
import com.codder.openphonetest.repositories.IGitUserDataRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class Application(): Application() {

    override fun onCreate() {
        super.onCreate()

        val koinModule = module {
            single { SqlDelightDriverFactory(get()) }

            single <IRemoteDataSource>{ RemoteDataSource(KtorBuilderForPlatform()) }

            single <IGitUserLocalDatasource>{ GitUserLocalDatasource(SqlDelightDatabase(get())) }

            single <INetworkStateCallbacksManager> { NetworkStateCallbacksManager(NativeNetworkStateProvider(get())) }

            single <IGitUserDataRepository>{
                GitUserDataRepository(
                    remoteDataSource = get(),
                    gitUserLocalDatasource = get(),
                    networkStateCallbackManager = get()
                )
            }
        }

        startKoin {
            androidContext(applicationContext)
            modules(koinModule)
        }
    }
}