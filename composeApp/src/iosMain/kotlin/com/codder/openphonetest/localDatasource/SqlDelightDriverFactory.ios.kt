package com.codder.openphonetest.localDatasource

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.codder.openphonetest.composeApp.cache.Database

actual class SqlDelightDriverFactory() {

    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, "app.db")
    }
}