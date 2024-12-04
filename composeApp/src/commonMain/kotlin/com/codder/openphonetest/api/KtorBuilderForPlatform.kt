package com.codder.openphonetest.api

import io.ktor.client.HttpClient

expect class KtorBuilderForPlatform() {
    fun buildHttpClient(): HttpClient
}