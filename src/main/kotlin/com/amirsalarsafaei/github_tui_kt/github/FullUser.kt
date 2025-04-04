package com.amirsalarsafaei.github_tui_kt.github

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubUser

import java.time.ZonedDateTime

data class FullUser(
    val user: GithubUser,
    val lastSynced: ZonedDateTime,
)