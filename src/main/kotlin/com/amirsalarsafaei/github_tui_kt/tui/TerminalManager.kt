package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.coroutines.receiveKeyEventsFlow
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.takeWhile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayDeque

private val logger = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class)
class TerminalManager(private val terminal: Terminal) {
    private val uiDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val uiScope = CoroutineScope(uiDispatcher + SupervisorJob())
    private val messageChannel = Channel<Msg>(Channel.BUFFERED)
    private val drawersStack = ArrayDeque<Model>()
    private val isRunning = AtomicBoolean(true)

    fun run(firstDrawer: Model) = runBlocking {
        // Initialize on UI thread
        withContext(uiDispatcher) {
            terminal.cursor.move { this.clearScreen() }
            terminal.cursor.hide()
            drawersStack.add(firstDrawer)
            render()
            firstDrawer.init()?.let { executeCommand(it) }
        }

        // Start UI message processor
        val messageProcessor = uiScope.launch {
            while (isRunning.get() && !messageChannel.isClosedForReceive) {
                try {
                    processMessage(messageChannel.receive())
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        println("Error processing message: ${e.message}")
                    }
                }
            }
        }

        launch {
            terminal.receiveKeyEventsFlow()
                .takeWhile { event -> !event.isCtrlC && isRunning.get() }
                .collect { event ->
                    convertKeyEventToMsg(event)?.let { msg ->
                        if (isRunning.get()) messageChannel.send(msg)
                    }

                    if (event.isCtrlC) {
                        isRunning.set(false)
                        messageChannel.send(Exit)
                    }
                }
        }

        messageProcessor.join()
    }

    private fun convertKeyEventToMsg(event: KeyboardEvent): Msg? {
        return when {
            event.isCtrlC -> Exit
            event.key == "Escape" -> Pop
            else -> KeyMsg(event)
        }
    }

    private fun render() {
        terminal.cursor.move { this.setPosition(0, 1) }
        if (drawersStack.isNotEmpty()) {
            drawersStack.last().view(terminal)
        }

        terminal.cursor.move {
            this.clearLineAfterCursor()
            this.clearScreenAfterCursor()
        }
    }

    private fun executeCommand(cmd: Cmd) {
        scope.launch {
            try {
                if (cmd is FlowCmd) {
                    cmd.flow.collect {
                        if (isRunning.get() && !messageChannel.isClosedForSend) {
                            if (it is BatchMsg) {
                                for (msg in it.msgs) {
                                    messageChannel.send(msg)
                                }
                            } else {
                                messageChannel.send(it)
                            }
                        }
                    }
                    return@launch
                }

                if (cmd is BatchCmd) {
                    for (c in cmd.commands) {
                        executeCommand(c)
                    }
                    return@launch
                }

                val resultMsg = cmd()
                if (isRunning.get() && !messageChannel.isClosedForSend) {
                    if (resultMsg is BatchMsg) {
                        for (msg in resultMsg.msgs) {
                            messageChannel.send(msg)
                        }
                    } else {
                        messageChannel.send(resultMsg)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get() && !messageChannel.isClosedForSend) {
                    messageChannel.send(ErrorMsg(e))
                }
            }
        }
    }

    private fun processMessage(msg: Msg) {
        when(msg) {
            is Push -> {
                drawersStack.add(msg.model)
                terminal.cursor.move { this.clearScreen() }
                msg.model.init()?.let { executeCommand(it) }
                render()
                return
            }
            is Replace -> {
                if (drawersStack.isNotEmpty()) drawersStack.removeLast()
                drawersStack.add(msg.model)
                msg.model.init()?.let { executeCommand(it) }
                render()
                return
            }
            is Pop -> {
                if (drawersStack.size > 1) {
                    drawersStack.removeLast()
                    render()
                } else if (drawersStack.size == 1) {
                    isRunning.set(false)
                }
                return
            }
            is ErrorMsg -> {
                logger.error { msg.error; "error message: ${msg.error.message}" }
                render()
                return
            }
            is Exit -> {
                isRunning.set(false)
                return
            }
            is ClearScreen -> {
                this.terminal.cursor.move { this.clearScreen() }
            }
        }

        if (drawersStack.isNotEmpty()) {
            val currentModel = drawersStack.last()
            val updateResult = currentModel.update(msg)

            drawersStack.removeLast()
            drawersStack.addLast(updateResult.model)
            render()
            updateResult.cmd?.let { executeCommand(it) }
        }
    }

    fun shutdown() {
        isRunning.set(false)
        scope.cancel()
        uiScope.cancel()
        messageChannel.close()
        uiDispatcher.close()
    }
}
