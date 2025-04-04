package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import kotlin.math.max
import kotlin.math.min

class MainMenu : Model {

    private val menuItems = listOf(
        "Retrieve User",
        "Search Local Users",
        "Search Local Repositories"
    )

    private val menuModels: List<() -> Model> = listOf({ RetrieveUser() }, { SearchUsers() }, { SearchRepositories() })

    private var menuItemIdx = 0

    override fun view(terminal: Terminal) {
        val menuView = buildString {
            for ((index, item) in menuItems.withIndex()) {
                if (index == menuItemIdx) {
                    append((bold + green)("-> $item\n"))
                } else {
                    append((gray)(item))
                    append("\n")
                }
            }
        }

        terminal.println(
            Panel(
                Text(menuView),
                Text((bold + brightRed)("Main Menu")),
                expand = true,
                padding = Padding(5)
            )
        )

    }

    override fun update(msg: Msg): UpdateResult {
        when (msg) {
            is KeyMsg -> {
                if (msg.event.key == "ArrowUp" || msg.event.key == "k") {
                    this.menuItemIdx = max(0, menuItemIdx - 1)
                }
                if (msg.event.key == "ArrowDown" || msg.event.key == "j") {
                    this.menuItemIdx = min(menuItems.size - 1, menuItemIdx + 1)
                }
                if (msg.event.key == "Enter") {
                    return UpdateResult(this, { Push(this.menuModels[menuItemIdx]()) })
                }
            }
        }
        return UpdateResult(this, null)
    }

    override fun init(): Cmd? {
        return null
    }

}
