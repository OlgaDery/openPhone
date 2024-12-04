package com.codder.openphonetest.localDatasource

import com.codder.openphonetest.model.GitUserData
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

interface IGitUserLocalDatasource {

    suspend fun saveOrUpdateUserData(
        id: String,
        imageUrl: String,
        name: String,
        repositoriesUrl: String,
        page: Int
    )

    suspend fun updateNumberOfRepositories(
        id: String,
        numberOfRepositories: Int
    )

    suspend fun deleteAllUsers()

    fun selectAllUsers(): Flow<List<GitUserData>>

    fun selectUsersByName(name: String):  Flow<List<GitUserData>>

    fun selectUsersByNameForPageNumbers(parameters: DatabaseQueryParameters): Flow<List<GitUserData>>
}

class GitUserLocalDatasource(
    private val sqlDelightDatabase: SqlDelightDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

) : IGitUserLocalDatasource {

    private val query = sqlDelightDatabase.appDatabase.gitUserQueries

    override fun selectUsersByName(name: String): Flow<List<GitUserData>> {
        return query.selectUsersByName(
            name = name,
            mapper = ::mapDatabaseRecordToGitUserData
        ).asFlow()
            .mapToList(dispatcher)
    }

    override suspend fun saveOrUpdateUserData(
        id: String,
        imageUrl: String,
        name: String,
        repositoriesUrl: String,
        page: Int
    ) {
        query.selectUserById(id).executeAsOneOrNull()?.let { gitUser ->
            if (gitUser.name != name || gitUser.imageUrl != imageUrl || gitUser.repositoriesUrl != repositoriesUrl) {
                query.updateGitUserPrimaryAttributes(
                    name = name,
                    imageUrl = imageUrl,
                    repositoriesUrl = repositoriesUrl,
                    id = id
                )
            }

        } ?: run {
            query.insertUser(
                id = id,
                imageUrl = imageUrl,
                name = name,
                numberOfRepositories = null,
                repositoriesUrl = repositoriesUrl,
                pageNumber = page.toLong()
            )
        }
    }

    override suspend fun updateNumberOfRepositories(
        id: String,
        numberOfRepositories: Int
    ) {
        query.updateUserSetNumberOfRepositories(
            numberOfRepositories = numberOfRepositories.toLong(),
            id = id
        )
    }

    override suspend fun deleteAllUsers() {
        query.deleteAllUsers()
    }

    override fun selectAllUsers(): Flow<List<GitUserData>> {
        return query.selectAllUsers(::mapDatabaseRecordToGitUserData)
            .asFlow()
            .mapToList(dispatcher)
    }

    override fun selectUsersByNameForPageNumbers(parameters: DatabaseQueryParameters): Flow<List<GitUserData>> {
        return query.selectUsersForPages(
            name = parameters.partialNameString ?: "",
            pageNumber = parameters.currentPageNumber.toLong(),
            pageNumber_ = (parameters.currentPageNumber).toLong(),
            mapper = ::mapDatabaseRecordToGitUserData

        ).asFlow()
            .mapToList(dispatcher)
    }

    private fun mapDatabaseRecordToGitUserData(id: String, name: String, imageUrl: String, repositoriesUrl: String, numberOfRepositories: Long?, pageNumber: Long): GitUserData {
        return GitUserData(
            id = id,
            name = name,
            imageUrl = imageUrl,
            numberOfRepositories = numberOfRepositories?.toInt(),
            urlForRepositories = repositoriesUrl
        )
    }

}

data class DatabaseQueryParameters(
    val currentPageNumber: Int,
    val limit: Int = 50,
    val partialNameString: String?,
    val appending: Boolean
)