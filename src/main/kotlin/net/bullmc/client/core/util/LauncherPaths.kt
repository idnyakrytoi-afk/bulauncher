package net.bullmc.client.core.util

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LauncherPaths {
    private val userHome: String = System.getProperty("user.home")
    private val appDir: File = File(userHome, ".bullmc-client")

    val root: File = appDir
    val db: File = File(appDir, "db")
    val game: File = File(appDir, "game")
    val gameCache: File = File(appDir, "game-cache")
    val launcherCache: File = File(appDir, "launcher-cache")
    val jre: File = File(appDir, "jre")
    val logs: File = File(appDir, "logs")
    val mods: File = File(game, "mods")
    val versions: File = File(game, "versions")
    val libraries: File = File(game, "libraries")
    val assets: File = File(game, "assets")
    val credentials: File = File(appDir, "credentials.txt")

    @Volatile
    private var initialized = false

    fun init() {
        if (initialized) return
        listOf(db, game, gameCache, launcherCache, jre, logs, mods, versions, libraries, assets).forEach {
            it.mkdirs()
        }
        initialized = true
        println("[PATHS] Root: ${appDir.absolutePath}")
        println("[PATHS] Game: ${game.absolutePath}")
        println("[PATHS] Libraries: ${libraries.absolutePath}")
        println("[PATHS] Assets: ${assets.absolutePath}")
    }

    fun getDbFile(): File = File(db, "bullmc.db")

    fun getLogFile(): File {
        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return File(logs, "launcher-$date.log")
    }

    fun getCacheFile(name: String): File = File(launcherCache, "$name.json")

    fun getGameCacheDir(version: String): File = File(gameCache, version).also { it.mkdirs() }

    fun getTempDir(): File = File(appDir, "temp").also { it.mkdirs() }

    fun cleanGameCache(maxAgeDays: Int = 7) {
        val cutoff = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        gameCache.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val lastModified = file.listFiles()?.maxOfOrNull { it.lastModified() } ?: 0L
                if (lastModified < cutoff) file.deleteRecursively()
            }
        }
    }

    fun cleanLauncherCache(maxAgeDays: Int = 3) {
        val cutoff = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        launcherCache.listFiles()?.filter { it.isFile }?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }

    fun cleanTemp() {
        getTempDir().deleteRecursively()
    }

    fun getDiskUsage(): DiskUsage {
        return DiskUsage(
            gameSize = folderSize(game),
            gameCacheSize = folderSize(gameCache),
            launcherCacheSize = folderSize(launcherCache),
            logsSize = folderSize(logs)
        )
    }

    fun writeLog(message: String) {
        try {
            val logFile = getLogFile()
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            logFile.appendText("[$time] $message\n")
        } catch (_: Exception) {}
    }

    fun writeErrorLog(e: Exception) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        writeLog("ERROR: ${e.message}\n$sw")
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}

data class DiskUsage(
    val gameSize: Long,
    val gameCacheSize: Long,
    val launcherCacheSize: Long,
    val logsSize: Long
) {
    val totalSize: Long get() = gameSize + gameCacheSize + launcherCacheSize + logsSize

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${String.format("%.1f", kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "${String.format("%.1f", mb)} MB"
        val gb = mb / 1024.0
        return "${String.format("%.2f", gb)} GB"
    }
}
