package com.codder.openphonetest.localDatasource

import com.codder.openphonetest.composeApp.cache.Database

class SqlDelightDatabase(
    private val sqlDelightDriverFactory: SqlDelightDriverFactory
) {

    val appDatabase: Database by lazy {

        val driver = sqlDelightDriverFactory.createDriver()

        Database.Schema.migrate(
            driver = driver,
            oldVersion = DATABASE_CURRENT_VERSION,
            newVersion = Database.Schema.version
        )

        val database = Database(driver)
        database
    }


    companion object {
        const val DATABASE_CURRENT_VERSION = 1L
    }
}