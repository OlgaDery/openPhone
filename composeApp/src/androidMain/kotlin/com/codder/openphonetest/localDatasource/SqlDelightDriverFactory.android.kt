package com.codder.openphonetest.localDatasource

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.codder.openphonetest.composeApp.cache.Database

actual class SqlDelightDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "app.db")
    }
}