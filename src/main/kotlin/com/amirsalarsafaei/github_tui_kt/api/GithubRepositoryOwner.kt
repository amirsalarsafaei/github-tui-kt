package com.amirsalarsafaei.github_tui_kt.api

import com.google.gson.annotations.SerializedName

data class GithubRepositoryOwner(
    @SerializedName("login") val username: String,
)
