package com.amirsalarsafaei.github_tui_kt.github

import com.amirsalarsafaei.github_tui_kt.api.APIService
import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubRepositoryOwner
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import com.amirsalarsafaei.github_tui_kt.database.Data
import com.amirsalarsafaei.github_tui_kt.database.Database
import com.amirsalarsafaei.github_tui_kt.database.DatabaseService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.toPath
import kotlin.math.log


val STALENESS_DURATION: Duration = Duration.ofDays(1)
val COMPLETE_REFRESH_DURATION: Duration = Duration.ofDays(7)

private val logger = KotlinLogging.logger {}

class Repository(
    private val database: Database, private val databaseRetriever: FullUserRetriever,
    private val apiRetriever: APIUserRetriever
) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            )
        )
        .build();


    companion object {
        private val repository: Repository by lazy {
            Repository(
                DatabaseService.getDatabase(),
                databaseRetriever = DatabaseFullUserRetriever(DatabaseService.getDatabase()),
                apiRetriever = APIUserRetriever(apiService = APIService)
            )
        }

        fun getInstance(): Repository = repository
    }

    suspend fun retrieveUserProfile(username: String): Pair<FullUser?, Flow<GithubRepository>?> {
        try {
            val dbUser = databaseRetriever.retrieveFullUser(username)
            if (dbUser != null && !needsRefresh(dbUser.lastSynced)) {
                val repositoryFlow = databaseRetriever.retrieveRepositories(username)
                if (isStale(dbUser.lastSynced) || needsRefresh(dbUser.lastSynced)) {
                    val (fullUser, repoFlow) = fetchUserUpdates(username, dbUser.lastSynced)

                    val flow = repoFlow.onEach { repo ->
                        syncRepository(fullUser.user.username, repo)
                    }.onCompletion {
                        if (it == null) {
                            syncUserInDatabase(fullUser.user, fullUser.lastSynced)
                        }
                    }.onStart {
                        emitAll(repositoryFlow)
                    }.flowOn(Dispatchers.IO)

                    return Pair(fullUser, flow)
                }

                return Pair(dbUser, repositoryFlow)
            }

            val apiUser = apiRetriever.retrieveFullUser(username) ?: return Pair(null, null)

            fetchAvatar(apiUser.user.avatarUrl)

            val repositoryFlow = apiRetriever.retrieveRepositories(apiUser.user.username)


            val flow = repositoryFlow.onEach { repo ->
                syncRepository(apiUser.user.username, repo)
            }.onCompletion {
                if (it == null) {
                    createUserInDatabase(apiUser.user)
                }
            }.flowOn(Dispatchers.IO)

            return Pair(apiUser, flow)

        } catch (e: Exception) {
            throw RetrieveFailed("Unexpected error retrieving user: ${e.message}", e)
        }
    }

    fun getAvatarFile(url: String): File {
        return fetchAvatar(url)
    }

    fun searchUsers(username: String): List<GithubUser> {
        return database.githubQueries.searchUsers(username).executeAsList().map { dbRow ->
            GithubUser(
                avatarUrl = dbRow.avatar_url,
                name = dbRow.name,
                username = dbRow.username,
                following = dbRow.following,
                followers = dbRow.followers,
            )
        }
    }

    fun searchRepositories(query: String): List<GithubRepository> {
        return database.githubQueries.searchRepository(query).executeAsList().map { dbRow ->
            GithubRepository(
                name = dbRow.name,
                owner = GithubRepositoryOwner(dbRow.owner),
                stars = dbRow.stars,
                forks = dbRow.forks,
                createdAt = null
            )
        }
    }

    private fun fetchAvatar(url: String): File {
        logger.info { "Fetching avatar $url" }
        val fileName = "${url.hashCode().toString().replace("-", "")}.png"

        val file = File(Data.directory, fileName)
        logger.info { "avatar file path: ${file.absolutePath}" }
        if (file.exists()) {
            return file
        }

        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RetrieveFailed("could not retrieve user avatar: ${response.body?.string()}")
            }
            response.body!!.byteStream().use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        return file
    }


    private suspend fun fetchUserUpdates(
        username: String,
        lastSynced: ZonedDateTime?
    ): Pair<FullUser, Flow<GithubRepository>> {
        val apiUser = apiRetriever.retrieveFullUser(username)
            ?: throw RetrieveFailed("user not found in fetching update: $username")

        return Pair(apiUser, apiRetriever.retrieveRepositories(username, lastSynced))
    }

    private fun createUserInDatabase(user: GithubUser) {
        val timeNow = ZonedDateTime.now()
        try {
            database.githubQueries.createUser(
                username = user.username,
                name = user.name ?: "",
                followers = user.followers,
                following = user.following,
                created_at = timeNow,
                last_synced = timeNow,
                avatar_url = user.avatarUrl,
            )

        } catch (e: Exception) {
            throw SaveUserException("Unexpected error creating user: ${e.message}", e)
        }
    }

    private fun syncUserInDatabase(user: GithubUser, lastSynced: ZonedDateTime) {
        try {
            database.githubQueries.syncUser(
                followers = user.followers,
                following = user.following,
                last_synced = lastSynced,
                username = user.username,
            )
        } catch (e: Exception) {
            throw SaveUserException("Unexpected error creating user", e)
        }
    }

    private fun syncRepository(username: String, repository: GithubRepository) {
        try {
            database.githubQueries.upsertRepository(
                owner = username,
                name = repository.name,
                stars = repository.stars,
                forks = repository.forks,
            )
        } catch (e: Exception) {
            throw SaveRepositoryException("Unexpected error syncing repository", e)
        }
    }

    private fun isStale(lastSynced: ZonedDateTime): Boolean {
        return lastSynced + STALENESS_DURATION < ZonedDateTime.now()
    }


    private fun needsRefresh(lastSynced: ZonedDateTime): Boolean {
        return lastSynced + COMPLETE_REFRESH_DURATION < ZonedDateTime.now()
    }
}