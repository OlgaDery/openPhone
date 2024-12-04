package com.codder.openphonetest

import androidx.compose.ui.window.ComposeUIViewController
import com.codder.openphonetest.repositories.GitUserDataRepository
import com.codder.openphonetest.repositories.IGitUserDataRepository

fun MainViewController() = ComposeUIViewController {

    val repository: IGitUserDataRepository = KoinHelper().getDependency() as IGitUserDataRepository
    App(repository)

}