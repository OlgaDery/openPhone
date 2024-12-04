package com.codder.openphonetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codder.openphonetest.repositories.GitUserDataRepository
import com.codder.openphonetest.repositories.IGitUserDataRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: IGitUserDataRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(repo = repository)
        }
    }
}
