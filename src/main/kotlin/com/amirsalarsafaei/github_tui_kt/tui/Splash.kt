package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.delay
import java.io.File

class Splash : Model {

    companion object {
        data object Change : Msg
    }

    override fun view(terminal: Terminal) {
        terminal.print(TerminalImageRenderer.splashImageString(50), align = TextAlign.CENTER)
    }

    override fun update(msg: Msg): UpdateResult {
        if (msg is Change) {
            return UpdateResult(this) { Push(MainMenu()) }
        }
        return UpdateResult(this, null)
    }

    override fun init(): Cmd {
        return {
            delay(1000)
            Change
        }
    }
}

