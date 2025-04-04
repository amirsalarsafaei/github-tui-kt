package com.amirsalarsafaei.github_tui_kt.tui

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import com.amirsalarsafaei.github_tui_kt.github.Repository
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.table.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.Text
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class RetrieveUser : Model {

    companion object {
        data class FetchUser(val user: GithubUser, val repositories: Flow<GithubRepository>) : Msg
        data class UserNotFound(val username: String) : Msg
        data object SpinnerTick : Msg

        fun fetchUser(username: String): Cmd {
            return {
                val (fullUser, repoFlow) = Repository.getInstance().retrieveUserProfile(username)
                if (fullUser == null || repoFlow == null) {
                    logger.info { "No user found for $username" }
                    UserNotFound(username)
                } else {
                    FetchUser(user = fullUser.user, repoFlow)
                }
            }
        }
    }

    private var username: String = ""
    private var error: String? = null
    private var spinner: Spinner? = null

    override fun view(terminal: Terminal) {
        terminal.println(
            Panel(
                grid {
                    column(0) { this.width = ColumnWidth(2, width = 10) }
                    column(1) { this.width = ColumnWidth(10) }
                    row {
                        cell((bold + brightBlue)("Username:")) { align = TextAlign.LEFT }
                        cell(username) { align = TextAlign.LEFT; cellBorders = Borders.ALL }
                    }
                    if (error != null) {
                        row { cell(Text((red)("error:\t"))); cell(Text((red)(error!!))) }
                    }

                    cellBorders = Borders.ALL
                },
                Text(brightRed("Retrieve Github User")),
                bottomTitle = if (spinner != null) {
                    horizontalLayout {
                        cell(Text("Fetching"))
                        cell(spinner)
                        spacing = 4
                    }
                } else {
                    null
                },
                expand = true,
                padding = Padding(8, 2, 8, 2)
            )
        )
    }

    override fun update(msg: Msg): UpdateResult {
        when (msg) {
            is SpinnerTick -> {
                this.spinner?.advanceTick()
                if (this.spinner != null) {
                    return UpdateResult(model = this, cmd = { delay(100); SpinnerTick })
                }
            }

            is UserNotFound -> {
                this.spinner = null
                if (msg.username == this.username) {
                    this.error = "User '${msg.username}' not found"
                    return UpdateResult(this, null)
                }
            }

            is FetchUser -> {
                this.spinner = null
                return UpdateResult(this) { Push(UserProfile(msg.user, msg.repositories)) }
            }

            is KeyMsg -> {
                if (this.spinner != null) {
                    return UpdateResult(this, null)
                }
                var modified = false
                if (!msg.event.alt && !msg.event.ctrl && msg.event.key.length == 1) {
                    modified = true
                    this.username += if (msg.event.shift) {
                        msg.event.key.uppercase()
                    } else {
                        msg.event.key
                    }
                }
                if (msg.event.key == "Backspace" && this.username.isNotEmpty()) {
                    modified = true
                    this.username = this.username.substring(0, username.length - 1)
                }
                if (msg.event.key == "u" && msg.event.ctrl && this.username.isNotEmpty()) {
                    modified = true
                    this.username = ""
                }

                if (msg.event.key == "Enter") {
                    this.spinner = Spinner.Dots()
                    return UpdateResult(
                        this,
                        BatchCmd(fetchUser(this.username), { SpinnerTick })
                    )
                }

                if (modified) {
                    this.error = null
                }

                return UpdateResult(this, null)
            }

            is ErrorMsg -> {
                error = msg.error.message
            }
        }

        return UpdateResult(this, null)
    }

    override fun init(): Cmd? {
        return null
    }

}
