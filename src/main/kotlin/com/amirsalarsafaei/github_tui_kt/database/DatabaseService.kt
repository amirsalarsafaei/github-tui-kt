package com.amirsalarsafaei.github_tui_kt.database

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.amirsalarsafaei.githubtuikt.database.Github_repositories
import com.amirsalarsafaei.githubtuikt.database.Github_users
import kotlinx.coroutines.runBlocking


object DatabaseService {
    private val _database: Database by lazy {
        val database = Database(
            runBlocking { DriverService.getDriver() },
            github_repositoriesAdapter = Github_repositories.Adapter(
                starsAdapter = IntColumnAdapter,
                forksAdapter = IntColumnAdapter
            ),
            github_usersAdapter = Github_users.Adapter(
                followingAdapter = IntColumnAdapter,
                followersAdapter = IntColumnAdapter,
                created_atAdapter = ZoneDateTimeAdapter,
                last_syncedAdapter = ZoneDateTimeAdapter,
            ),
        )

        database
    }

    fun getDatabase(): Database {
        return _database
    }
}