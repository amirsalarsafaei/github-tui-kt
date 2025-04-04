package com.amirsalarsafaei.github_tui_kt.github

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

interface FullUserRetriever {
    suspend fun retrieveFullUser(username: String): FullUser?
    fun retrieveRepositories(username: String, createdAtAfter: ZonedDateTime? = null): Flow<GithubRepository>
}