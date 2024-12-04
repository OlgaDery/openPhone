package com.codder.openphonetest.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.request
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

interface IRemoteDataSource {
    suspend fun getListOfUsersAndOrganizations(
        partialName: String,
        pageCounter: Int
    ): StateOfGitResponse

    suspend fun getNumberOfRepositoriesForOrganization(url: String): Int
}


class RemoteDataSource(
    private val ktorBuilderForPlatform: KtorBuilderForPlatform
) : IRemoteDataSource {

    private val httpClient = ktorBuilderForPlatform.buildHttpClient()

    override suspend fun getListOfUsersAndOrganizations(
        partialName: String,
        pageCounter: Int
    ): StateOfGitResponse {

        val urlString = "https://api.github.com/search/users?q=$partialName&page=$pageCounter"

        httpClient.get(urlString).apply {
            return try {

                val link = this.headers["Link"]
                val pageHasNextLink = link?.contains("next") == true
                StateOfGitResponse(
                    listOfUsers = (this.body() as ListOfUsersInResponse).items,
                    hasNextLink = pageHasNextLink,
                    responseState = ResponseState.OK
                )

            } catch (e: Exception) {
                StateOfGitResponse(
                    listOfUsers = null,
                    hasNextLink = false,
                    responseState = ResponseState.Error
                )
            }
        }

    }

    override suspend fun getNumberOfRepositoriesForOrganization(url: String): Int {
        httpClient.get(url).apply {
            return try {
                (this.body() as JsonArray).size
            } catch (e: Exception) {
                -1
            }
        }
    }

    companion object {
        val token = ""
    }
}

data class StateOfGitResponse (
    val listOfUsers: List<UserDataInResponse>?,
    val hasNextLink: Boolean,
    val responseState: ResponseState

)

@Serializable
data class ListOfUsersInResponse(
    val items: List<UserDataInResponse>
)

@Serializable
data class UserDataInResponse (
    val id: Int,
    val login: String,
    val avatar_url: String,
    val repos_url: String
)

enum class ResponseState {
    OK, Error
}