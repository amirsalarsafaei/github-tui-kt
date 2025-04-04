package com.amirsalarsafaei.github_tui_kt.github

import com.amirsalarsafaei.github_tui_kt.api.APIService
import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.*
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}
class APIUserRetriever(private val apiService: APIService) : FullUserRetriever {
    override suspend fun retrieveFullUser(username: String): FullUser? {
        val user = getUser(username) ?: return null

        return FullUser(
            user = user,
            lastSynced = ZonedDateTime.now(),
        )
    }


    override fun retrieveRepositories(username: String, createdAtAfter: ZonedDateTime?): Flow<GithubRepository> = flow {
        var pageNum = 1
        do {
            val pageResp = apiService.github.getUserRepositories(username, pageNum)
            val pageRepos: List<GithubRepository> = if (pageResp.isSuccessful) {
                pageResp.body() ?: throw RetrieveFailed("repository list is null")
            } else {
                throw RetrieveFailed("Couldn't fetch repositories: ${pageResp.code()} - ${pageResp.errorBody()}")
            }
            logger.info { "retrieved ${pageRepos.size} repositories" }

            emitAll(pageRepos.asFlow())

            pageNum++
        } while (pageResp.headers().get("link") != null)
    }

    private suspend fun getUser(username: String): GithubUser? {
        val getUserInfoResponse = this.apiService.github.getUserInfo(username)

        val user: GithubUser = if (getUserInfoResponse.code() in 200..299) {
            getUserInfoResponse.body() ?: throw RetrieveFailed("user is null")
        } else if (getUserInfoResponse.code() == 404) {
            logger.info { "user not found" }
            return null
        } else {
            throw RetrieveFailed("could not get user info: ${getUserInfoResponse.code()} - ${getUserInfoResponse.errorBody()}")
        }

        return user
    }

}