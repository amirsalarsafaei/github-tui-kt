package com.amirsalarsafaei.github_tui_kt.tui

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.github.Repository

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.Text
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

class SearchRepositories : Model {

    companion object {
        data class SearchResultReceived(val query: String, val repositories: List<GithubRepository>) : Msg

        data object SearchDebounce : Msg
        data object SpinnerTick : Msg

        const val DEBOUNCE_DURATION_MILLIS: Long = 2000
        fun searchRepository(query: String): Cmd {
            return {
                SearchResultReceived(query, Repository.getInstance().searchRepositories(query))
            }
        }

        fun searchRepositoryDebounce(): Cmd {
            return {
                delay(DEBOUNCE_DURATION_MILLIS)
                SearchDebounce
            }
        }
    }


    private var query = ""
    private var repositories = emptyList<GithubRepository>()
    private var lastKeyTime: ZonedDateTime? = null
    private var searching = false
    private var pendingResult = false
    private var spinner: Spinner = Spinner.Dots()
    private var error: String? = null
    private var cursorIdx: Int = 0
    override fun view(terminal: Terminal) {

        terminal.println(
            Panel(
                Text("${(brightBlue + bold)("Search:")}\t ${query}"),
                Text((brightRed + bold)("Search Repositories")),
                expand = true,
                padding = Padding(2, 2, 2, 2)
            )
        )


        val repoResultView = buildString {
            if (pendingResult) {
                return@buildString
            }

            if (query == "") {
                append("Please start typing to search")
                return@buildString
            }

            if (repositories.isEmpty()) {
                append("No repos found")
                return@buildString
            }

            for ((idx, repo) in repositories.withIndex()) {
                if (idx < min(cursorIdx, repositories.size - 4) - 3 || idx > max(cursorIdx, 3) + 3) {
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
                append("${repo.owner.username}/${repo.name}")
                append(" ")
                append("\t\t⭐\t${repo.stars}")
                append("\n")
                for (i in 1..terminal.size.width - 6) {
                    append("─")
                }
                append("\n")
            }
        }

        terminal.println(
            Panel(
                Text(repoResultView),
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
                    repositories = msg.repositories
                }
            }

            is SearchDebounce -> {
                if (!searching && pendingResult && lastKeyTime != null &&
                    ZonedDateTime.now().isAfter(lastKeyTime!!.plusNanos(DEBOUNCE_DURATION_MILLIS * 1000))
                ) {
                    this.searching = true
                    return UpdateResult(
                        this,
                        searchRepository(this.query)
                    )
                }
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

                if (repositories.isNotEmpty()) {
                    if (msg.event.key == "ArrowUp") {
                        cursorIdx = max(0, cursorIdx - 1)

                        return UpdateResult(this, null)
                    }

                    if (msg.event.key == "ArrowDown") {
                        cursorIdx = min(repositories.size - 1, cursorIdx + 1)

                        return UpdateResult(this, null)
                    }


                }

//                if (modified) {
//                    this.error = ""
//                }
                if (modified) {
                    lastKeyTime = ZonedDateTime.now()
                    repositories = emptyList()
                    cursorIdx = 0
                    pendingResult = true

                    return UpdateResult(this, searchRepositoryDebounce())
                }
            }
        }

        return UpdateResult(this, null)
    }

    override fun init(): Cmd {
        return { SpinnerTick }
    }
}