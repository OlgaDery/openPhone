package com.codder.openphonetest

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform