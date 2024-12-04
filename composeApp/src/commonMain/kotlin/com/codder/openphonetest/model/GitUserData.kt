package com.codder.openphonetest.model

data class GitUserData(
    val id: String,
    val name: String,
    val imageUrl: String,
    val numberOfRepositories: Int?,
    val urlForRepositories: String
)
