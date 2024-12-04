package com.codder.openphonetest.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual class KtorBuilderForPlatform {

    actual fun buildHttpClient(): HttpClient = HttpClient (CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    useAlternativeNames = false
                }
            )

        }
        defaultRequest {
            headers {
                headers { append(HttpHeaders.Authorization, "token ${RemoteDataSource.Companion.token}") }
            }
        }
    }
}