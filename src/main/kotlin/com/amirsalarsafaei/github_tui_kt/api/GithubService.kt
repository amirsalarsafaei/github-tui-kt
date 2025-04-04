
package com.amirsalarsafaei.github_tui_kt.api
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubService {
    @GET("/users/{user}")
    suspend fun getUserInfo(@Path("user") user: String): Response<GithubUser>

    @GET("/users/{user}/repos")
    suspend fun getUserRepositories(
        @Path("user") user: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "created",
        @Query("direction") direction: String = "desc"
    ): Response<List<GithubRepository>>
}
