package com.amirsalarsafaei.github_tui_kt.api

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class GithubRepository (
    @SerializedName("name") val name: String,
    @SerializedName("owner") val owner: GithubRepositoryOwner,
    @SerializedName("stargazers_count") val stars: Int,
    @SerializedName("forks_count") val forks: Int,

    @SerializedName("created_at") val createdAt: ZonedDateTime?,
)