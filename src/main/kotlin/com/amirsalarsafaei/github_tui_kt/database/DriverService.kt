package com.amirsalarsafaei.github_tui_kt.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}

object DriverService {
    private val driverDeferred = CompletableDeferred<SqlDriver>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driver = createDriverInternal()
                driverDeferred.complete(driver)
            } catch (e: Exception) {
                driverDeferred.completeExceptionally(e)
            }
        }
    }

    private fun createDriverInternal(): SqlDriver {
        val dbFileName = "${Data.appName}.db"
        val dbFile = File(Data.directory, dbFileName)
        val jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"

        logger.info { "database file: ${dbFile.absolutePath}" }

        return JdbcSqliteDriver(
            jdbcUrl,
            properties = Properties(),
            schema = Database.Schema
        )
    }

    suspend fun getDriver(): SqlDriver = driverDeferred.await()
}