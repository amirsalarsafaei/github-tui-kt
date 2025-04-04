package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

interface Msg

data object Pop : Msg
data object Exit : Msg
data object ClearScreen : Msg
data class Push(val model: Model) : Msg
data class Replace(val model: Model) : Msg
data class ErrorMsg(val error: Throwable) : Msg
data class KeyMsg(val event: KeyboardEvent) : Msg  // New message type for keyboard events

class BatchMsg(vararg val msgs: Msg) : Msg

typealias Cmd = suspend () -> Msg

class FlowCmd(val flow: Flow<Msg>): Cmd {
    override suspend fun invoke(): Msg {
        return BatchMsg(*flow.toList().toTypedArray())
    }
}
class BatchCmd(vararg val commands: Cmd) : Cmd {
    override suspend fun invoke(): Msg {
        val msgs = mutableListOf<Msg>()
        for (command in commands) {
            msgs.add(command())
        }

        return BatchMsg(*msgs.toTypedArray())
    }
}