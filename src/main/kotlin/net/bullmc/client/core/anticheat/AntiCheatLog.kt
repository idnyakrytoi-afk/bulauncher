package net.bullmc.client.core.anticheat

object AntiCheatLog {
    var enabled: Boolean = false

    fun d(message: String) {
        if (enabled) println("[AC] $message")
    }

    fun w(message: String) {
        if (enabled) println("[AC-WARN] $message")
    }

    fun e(message: String) {
        if (enabled) println("[AC-ERROR] $message")
    }

    fun block(message: String) {
        if (enabled) println("[AC-BLOCK] $message")
    }
}
