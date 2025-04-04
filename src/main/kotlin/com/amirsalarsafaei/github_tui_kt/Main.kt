package com.amirsalarsafaei.github_tui_kt

import com.amirsalarsafaei.github_tui_kt.tui.Splash
import com.amirsalarsafaei.github_tui_kt.tui.TerminalManager
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    logger.info { "Starting GitHub Repository..." }
    val terminal = Terminal()
    TerminalManager(terminal).run(Splash())
}