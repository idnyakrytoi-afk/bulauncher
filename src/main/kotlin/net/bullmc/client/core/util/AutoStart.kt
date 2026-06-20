package net.bullmc.client.core.util

import java.io.File

object AutoStart {
    private const val REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val APP_NAME = "BullMCClient"

    fun isEnabled(): Boolean {
        return try {
            val process = ProcessBuilder("reg", "query", REG_KEY, "/v", APP_NAME)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains(APP_NAME) && !output.contains("ERROR")
        } catch (_: Exception) {
            false
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            enable()
        } else {
            disable()
        }
    }

    private fun enable() {
        try {
            val jarPath = getLauncherPath() ?: return
            val javaPath = findJavaPath() ?: return
            val cmd = "\"$javaPath\" -Xmx512M -jar \"$jarPath\" --minimized"

            val process = ProcessBuilder(
                "reg", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", cmd, "/f"
            )
            process.start().waitFor()
            println("[AUTOSTART] Enabled: $cmd")
        } catch (e: Exception) {
            println("[AUTOSTART] Failed to enable: ${e.message}")
        }
    }

    private fun disable() {
        try {
            val process = ProcessBuilder("reg", "delete", REG_KEY, "/v", APP_NAME, "/f")
            process.start().waitFor()
            println("[AUTOSTART] Disabled")
        } catch (e: Exception) {
            println("[AUTOSTART] Failed to disable: ${e.message}")
        }
    }

    private fun getLauncherPath(): String? {
        val codeSource = AutoStart::class.java.protectionDomain?.codeSource
        if (codeSource != null) {
            val jarFile = File(codeSource.location.toURI().path)
            if (jarFile.exists()) return jarFile.absolutePath
        }

        val jarFromProp = System.getProperty("java.class.path")
            ?.split(File.pathSeparator)
            ?.firstOrNull { it.endsWith(".jar") }
        if (jarFromProp != null && File(jarFromProp).exists()) return jarFromProp

        val batFile = File("BullMC.bat")
        if (batFile.exists()) return null

        return null
    }

    private fun findJavaPath(): String? {
        val bundled = File("runtime/jdk-21.0.3/bin/java.exe")
        if (bundled.exists()) return bundled.absolutePath

        val javaHome = System.getProperty("java.home")
        if (javaHome != null) {
            val javaExe = File(javaHome, "bin/java.exe")
            if (javaExe.exists()) return javaExe.absolutePath
        }

        return "java"
    }
}
