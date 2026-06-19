package net.bullmc.client.core.mod

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

object ModDownloader {
    private const val MOD_URL = "https://github.com/bullmc/bull-tweaks/releases/download/v1.0.0/bulltweaks-1.0.0.jar"
    private const val CHECKSUM_URL = "https://github.com/bullmc/bull-tweaks/releases/download/v1.0.0/bulltweaks-1.0.0.jar.sha1"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
        }
        followRedirects = true
    }

    suspend fun ensureModInstalled(onProgress: (String, Float) -> Unit = { _, _ -> }): Boolean {
        val jarFile = ModSettings.getModJarFile()
        val modsDir = LauncherPaths.mods

        if (!modsDir.exists()) modsDir.mkdirs()

        if (jarFile.exists() && jarFile.length() > 0) {
            onProgress("Мод уже установлен", 1f)
            return true
        }

        onProgress("Скачивание Bull Tweaks...", 0f)

        return try {
            val response = client.get(MOD_URL)
            val bytes = response.readBytes()
            jarFile.writeBytes(bytes)

            if (jarFile.length() > 0) {
                println("[MOD] Скачан: ${jarFile.absolutePath} (${jarFile.length()} байт)")
                onProgress("Мод установлен!", 1f)
                true
            } else {
                println("[MOD] Ошибка: файл пустой")
                onProgress("Ошибка скачивания", 0f)
                false
            }
        } catch (e: Exception) {
            println("[MOD] Ошибка скачивания мода: ${e.message}")
            onProgress("Ошибка: ${e.message}", 0f)
            false
        }
    }

    fun isModInstalled(): Boolean {
        val jarFile = ModSettings.getModJarFile()
        return jarFile.exists() && jarFile.length() > 0
    }

    fun uninstallMod(): Boolean {
        val jarFile = ModSettings.getModJarFile()
        return if (jarFile.exists()) jarFile.delete() else true
    }
}
