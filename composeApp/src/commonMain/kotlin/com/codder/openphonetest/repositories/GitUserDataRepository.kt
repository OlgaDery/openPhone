package com.codder.openphonetest.repositories

import com.codder.openphonetest.api.IRemoteDataSource
import com.codder.openphonetest.api.ResponseState
import com.codder.openphonetest.localDatasource.DatabaseQueryParameters
import com.codder.openphonetest.localDatasource.IGitUserLocalDatasource
import com.codder.openphonetest.model.GitUserData
import com.codder.openphonetest.networkState.INetworkStateCallbacksManager
import com.codder.openphonetest.networkState.NetworkStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface IGitUserDataRepository {

    val networkState: Flow<NetworkStates>

    fun getUsersFromDatabase(queryParameters: DatabaseQueryParameters? = null): Flow<List<GitUserData>>

    suspend fun getUsersAndOrganizationsWithNameFromRemoteDataSource(
        partialNameString: String,
        pageNumber: Int,
        firstRequest: Boolean
    ): DataFlowStates

    suspend fun requestNumberOfRepositoriesForUser(gitUserData: GitUserData)
}

class GitUserDataRepository(
    private val remoteDataSource: IRemoteDataSource,
    private val gitUserLocalDatasource: IGitUserLocalDatasource,
    private val networkStateCallbackManager: INetworkStateCallbacksManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

) : IGitUserDataRepository {
    private val mutex = Mutex()

    override val networkState: Flow<NetworkStates>
        get() = networkStateCallbackManager.networkStateFlow()

    override fun getUsersFromDatabase(queryParameters: DatabaseQueryParameters?): Flow<List<GitUserData>> {
        queryParameters?.let {
            queryParameters.partialNameString?.let {
                return gitUserLocalDatasource.selectUsersByName(queryParameters.partialNameString)
            } ?: kotlin.run {
                return gitUserLocalDatasource.selectUsersByNameForPageNumbers(parameters = queryParameters)
            }
        } ?: run {
            return gitUserLocalDatasource.selectAllUsers()
        }
    }

    override suspend fun getUsersAndOrganizationsWithNameFromRemoteDataSource(
        partialNameString: String,
        pageNumber: Int,
        firstRequest: Boolean
    ): DataFlowStates {

        mutex.withLock {
            if (pageNumber < 1) {
                return DataFlowStates.EndOfData
            } else {
                val data = remoteDataSource.getListOfUsersAndOrganizations(
                    partialName = partialNameString,
                    pageCounter = pageNumber
                )

                when (data.responseState) {
                    ResponseState.OK -> {
                        if (data.listOfUsers?.isEmpty() == true) {
                            return DataFlowStates.NoDataFound
                        }
                        if (firstRequest) {
                            gitUserLocalDatasource.deleteAllUsers()
                        }
                        val requestsForNumberOfRepos = data.listOfUsers?.map { item ->
                            scope.async {
                                gitUserLocalDatasource.saveOrUpdateUserData(
                                    id = item.id.toString(),
                                    imageUrl = item.avatar_url,
                                    name = item.login,
                                    repositoriesUrl = item.repos_url,
                                    page = pageNumber
                                )
                            }
                        }
                        requestsForNumberOfRepos?.awaitAll()
                        if (!data.hasNextLink) {
                            return DataFlowStates.EndOfData
                        }
                        return DataFlowStates.Success
                    }
                    ResponseState.Error -> {
                        return DataFlowStates.ErrorWithRequest
                    }
                }
            }
        }
    }

    override suspend fun requestNumberOfRepositoriesForUser(gitUserData: GitUserData) {
        val numberOfRepos = remoteDataSource.getNumberOfRepositoriesForOrganization(gitUserData.urlForRepositories)
        gitUserLocalDatasource.updateNumberOfRepositories(gitUserData.id, numberOfRepos)
    }
}

enum class DataFlowStates {
   Success, EndOfData, NoDataFound, ErrorWithRequest
}

