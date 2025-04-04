package com.amirsalarsafaei.github_tui_kt.tui

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import com.amirsalarsafaei.github_tui_kt.github.Repository
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Spinner
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.min
import kotlin.math.max
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

class SearchUsers : Model {

    companion object {
        data class SearchResultReceived(val query: String, val users: List<GithubUser>) : Msg
        data class UserProfileReceived(val user: GithubUser, val repoFlow: Flow<GithubRepository>) : Msg

        data object SearchDebounce : Msg
        data object SpinnerTick : Msg

        const val DEBOUNCE_DURATION_MILLIS: Long = 2000
        fun searchUser(query: String): Cmd {
            return {
                SearchResultReceived(query, Repository.getInstance().searchUsers(username = query))
            }
        }

        fun searchUserDebounce(): Cmd {
            return {
                delay(DEBOUNCE_DURATION_MILLIS)
                SearchDebounce
            }
        }
    }


    private var query = ""
    private var users = emptyList<GithubUser>()
    private var lastKeyTime: ZonedDateTime? = null
    private var searching = false
    private var pendingResult = false
    private var spinner: Spinner = Spinner.Dots()
    private var error: String? = null
    private var cursorIdx: Int = 0
    private var loadingProfile = false

    override fun view(terminal: Terminal) {
        if (loadingProfile) {
            terminal.println(Panel(Text(""), Text("Loading User Profile"), spinner, expand = true, padding = Padding(2)))
            return
        }

        if (error != null) {
            terminal.println((red)("error: $error"))
            return
        }
        terminal.println(
            Panel(
                Text("${(brightBlue + bold)("Search:")}\t ${query}"),
                Text((brightRed + bold)("Search Users")),
                expand = true,
                padding = Padding(2, 2, 2, 2)
            )
        )


        val usersResultStr = buildString {
            if (pendingResult) {
                return@buildString
            }

            if (query == "") {
                append("Please start typing to search")
                return@buildString
            }

            if (users.isEmpty()) {
                append("No users found")
                return@buildString
            }

            for ((idx, user) in users.withIndex()) {
                if (idx < min(cursorIdx, users.size - 4) - 3 || idx > max(cursorIdx, 3) + 3) {
                    continue
                }
                val style = if (idx == cursorIdx) {
                    (bold + green)
                } else {
                    (blue)
                }
                if (cursorIdx == idx) {

                    append((style)("->"))
                }
                append((style)("@${user.username}"))
                if (user.name != "" && user.name != null) {
                    append(" ")
                    append((gray)("(${user.name})"))
                }
                append(" ")
                append("following:\t${user.following}")
                append(" ")
                append("followers:\t${user.followers}")
                append("\n")
                for (i in 1..terminal.size.width - 6) {
                    append("─")
                }
                append("\n")
            }
        }

        terminal.println(
            Panel(
                Text(usersResultStr),
                Text("$query results"),
                bottomTitle = if (pendingResult) {
                    spinner
                } else {
                    null
                },
                expand = true,
                padding = Padding(2, 2, 2, 2)
            )
        )
    }

    override fun update(msg: Msg): UpdateResult {
        when (msg) {
            is SpinnerTick -> {
                spinner.advanceTick()
                return UpdateResult(this, cmd = { delay(100); SpinnerTick })
            }

            is SearchResultReceived -> {
                if (msg.query == this.query) {
                    searching = false
                    pendingResult = false
                    users = msg.users
                }
            }

            is SearchDebounce -> {
                if (!searching && lastKeyTime != null &&
                    ZonedDateTime.now().isAfter(lastKeyTime!!.plusNanos(DEBOUNCE_DURATION_MILLIS * 1000))
                ) {
                    this.searching = true
                    pendingResult = true
                    return UpdateResult(
                        this,
                        searchUser(this.query)
                    )
                }
            }

            is UserProfileReceived -> {
                loadingProfile = false
                return UpdateResult(this, { Push(UserProfile(msg.user, msg.repoFlow))})
            }

            is ErrorMsg -> {
                loadingProfile = false
                error = msg.error.message
            }

            is KeyMsg -> {
                var modified = false
                if (!msg.event.alt && !msg.event.ctrl && msg.event.key.length == 1) {
                    modified = true
                    this.query += if (msg.event.shift) {
                        msg.event.key.uppercase()
                    } else {
                        msg.event.key
                    }
                }
                if (msg.event.key == "Backspace" && this.query.isNotEmpty()) {
                    modified = true
                    this.query = this.query.substring(0, query.length - 1)
                }
                if (msg.event.key == "u" && msg.event.ctrl && this.query.isNotEmpty()) {
                    modified = true
                    this.query = ""
                }

                if (users.isNotEmpty()) {
                    if (msg.event.key == "ArrowUp") {
                        cursorIdx = max(0, cursorIdx - 1)

                        return UpdateResult(this, null)
                    }

                    if (msg.event.key == "ArrowDown") {
                        cursorIdx = min(users.size - 1, cursorIdx + 1)

                        return UpdateResult(this, null)
                    }

                    if (msg.event.key == "Enter") {
                        loadingProfile = true
                        return UpdateResult(this, {
                            val (profile, repoFlow) = Repository.getInstance()
                                .retrieveUserProfile(users[cursorIdx].username)
                            if (profile == null || repoFlow == null) {
                                ErrorMsg(Exception("could not retrieve user profile"))
                            }

                            UserProfileReceived(profile!!.user, repoFlow!!)
                        })
                    }

                }

//                if (modified) {
//                    this.error = ""
//                }
                if (modified) {
                    lastKeyTime = ZonedDateTime.now()
                    users = emptyList()
                    cursorIdx = 0
                    pendingResult = true

                    return UpdateResult(this, searchUserDebounce())
                }
            }
        }

        return UpdateResult(this, null)
    }

    override fun init(): Cmd {
        return { SpinnerTick }
    }

}