package com.amirsalarsafaei.github_tui_kt.api

import com.google.gson.annotations.SerializedName

data class APIError(
    @SerializedName("message") val message: String,
    @SerializedName("documentation_url") val url: String,
)
