package com.amirsalarsafaei.github_tui_kt.tui

import com.amirsalarsafaei.github_tui_kt.api.GithubRepository
import com.amirsalarsafaei.github_tui_kt.api.GithubUser
import com.amirsalarsafaei.github_tui_kt.github.Repository
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.VerticalAlign
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.UnorderedList
import com.github.ajalt.mordant.widgets.withPadding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.Key
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class UserProfile(private val user: GithubUser, private val repoFlow: Flow<GithubRepository>) : Model {
    companion object {
        data class RepositoryReceived(val repository: GithubRepository) : Msg
    }

    private var avatarImage: String = TerminalImageRenderer.imageToString(
        Repository.getInstance().getAvatarFile(user.avatarUrl),
        20
    )

    private val repositories = mutableListOf<GithubRepository>()
    private var repositoryIdx = 0

    override fun view(terminal: Terminal) {
        terminal.println(
            Panel(
                horizontalLayout {
                    cell(Text(avatarImage).withPadding {
                        horizontal = 3
                    })
                    cell(
                        UnorderedList(
                            (underline + bold + brightBlue)("@${user.username}"),
                            (gray)(user.name ?: ""),
                            (white)("following: ${user.following}\tfollowers: ${user.followers}"),
                            bulletText = "",
                        ).withPadding { horizontal = 2; vertical = 1 })
                    verticalAlign = VerticalAlign.MIDDLE
                },
                Text((brightRed + bold)("User Info")), expand = true
            )
        )

        val repositoriesView = buildString {
            for (repository in repositories.subList(max(0, min(repositoryIdx, repositories.size - 3)- 3), min(repositories.size, max(repositoryIdx, 3) + 3))) {
                append((white)(repository.name))
                append("\n")
                append((yellow)("⭐\t${repository.stars}\n"))
            }
        }

        terminal.print(Panel(Text(repositoriesView), Text((brightRed + bold)("Repositories")),
            Text((gray)("Number of repositories: ${repositories.size}")),expand = true))

    }


    override fun update(msg: Msg): UpdateResult {
        if (msg is RepositoryReceived) {
            this.repositories.add(msg.repository)
            return UpdateResult(this, null)
        }

        if (msg is KeyMsg && repositories.isNotEmpty()) {
            if (msg.event.key == "ArrowDown") {
                repositoryIdx = min(repositories.size - 1, repositoryIdx +1)
            }

            if (msg.event.key == "ArrowUp") {
                repositoryIdx = max(0, repositoryIdx -1)
            }
        }

        return UpdateResult(this, null)
    }

    override fun init(): Cmd {
        return BatchCmd(
            FlowCmd(repoFlow.map { repo -> RepositoryReceived(repo) }),
        )
    }
}
