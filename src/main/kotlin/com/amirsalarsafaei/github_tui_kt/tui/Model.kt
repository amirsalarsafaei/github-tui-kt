package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.terminal.Terminal



data class UpdateResult(val model: Model, val cmd: Cmd?)

interface Model {
    fun view(terminal: Terminal)
    fun update(msg: Msg): UpdateResult
    fun init(): Cmd?
}
