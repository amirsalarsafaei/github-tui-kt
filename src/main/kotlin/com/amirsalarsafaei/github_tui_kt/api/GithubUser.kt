
package com.amirsalarsafaei.github_tui_kt.api
import com.google.gson.annotations.SerializedName

data class GithubUser(
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("name") val name: String?,
    @SerializedName("login") val username: String,
    @SerializedName("following") val following: Int,
    @SerializedName("followers") val followers: Int,
)