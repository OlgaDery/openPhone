package com.codder.openphonetest.localDatasource

import app.cash.sqldelight.db.SqlDriver

expect class SqlDelightDriverFactory{
    fun createDriver(): SqlDriver
}