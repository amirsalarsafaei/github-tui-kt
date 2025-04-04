package com.amirsalarsafaei.github_tui_kt.github

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubRepositoryOwner
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import com.amirsalarsafaei.github_tui_kt.database.Database
import com.amirsalarsafaei.github_tui_kt.tui.RetrieveUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.ZonedDateTime

class DatabaseFullUserRetriever(private val database: Database) : FullUserRetriever {
    override suspend fun retrieveFullUser(username: String): FullUser? {
        try {
            val user = database.githubQueries.getUser(username).executeAsOneOrNull() ?: return null;
            return FullUser(
                GithubUser(
                    avatarUrl = user.avatar_url,
                    name = user.name,
                    username = user.username,
                    following = user.following,
                    followers = user.followers
                ),
                user.last_synced
            )
        }
        catch (e: Exception) {
            throw RetrieveFailed("could not retrieve user", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun retrieveRepositories(
        username: String,
        createdAtAfter: ZonedDateTime?
    ): Flow<GithubRepository> =
        database.githubQueries.getUserRepositories(
            username,
        ).asFlow().mapToList(Dispatchers.IO).flatMapConcat { it.asFlow() }.map { dbRow ->
            GithubRepository(
                name = dbRow.name,
                owner = GithubRepositoryOwner(dbRow.owner),
                stars = dbRow.stars,
                forks = dbRow.forks,
                createdAt = null,
            )
        }
}