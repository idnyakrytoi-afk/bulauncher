package net.bullmc.client.core.launcher

import net.bullmc.client.core.auth.Auth
import net.bullmc.client.core.loader.LoaderManager
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

class Launcher {
    private val auth = Auth()

    fun getGameDir(profileGameDir: File) = profileGameDir
    fun getModsDir(profileGameDir: File) = File(profileGameDir, "mods").also { it.mkdirs() }

    suspend fun downloadAndLaunch(
        version: String,
        playerNick: String,
        javaPath: String = "java",
        ramMb: Int = 4096,
        serverIp: String? = null,
        loader: LoaderType = LoaderType.VANILLA,
        loaderVersion: String = "",
        enabledModIds: List<String> = emptyList(),
        gameDir: File,
        onStatus: (String, Float) -> Unit
    ): Process? {
        LauncherPaths.init()
        onStatus("Подготовка...", 0f)

        val mcLauncher = MinecraftLauncher(gameDir, auth)

        return try {
            val downloader = MinecraftDownloader(gameDir) { msg, progress ->
                onStatus(msg, progress)
            }
            downloader.ensureInstalled(version)

            val loaderManager = LoaderManager(gameDir) { msg, progress ->
                onStatus(msg, progress)
            }
            val actualVersionId = loaderManager.ensureLoaderInstalled(
                version, loader, loaderVersion, enabledModIds
            )

            onStatus("Запуск Minecraft...", 1.0f)
            mcLauncher.launch(actualVersionId, playerNick, javaPath, ramMb, serverIp)
        } catch (e: Exception) {
            println("[LAUNCH] Ошибка: ${e.message}")
            e.printStackTrace()
            LauncherPaths.writeErrorLog(e)
            onStatus("Ошибка: ${e.message}", 0f)
            null
        }
    }

    fun isInstalled(version: String, versionsDir: File): Boolean {
        val versionDir = versionsDir.resolve(version)
        return versionDir.resolve("$version.jar").exists() && versionDir.resolve("$version.json").exists()
    }

    suspend fun getAvailableVersions(): List<VersionEntry> {
        val downloader = MinecraftDownloader(LauncherPaths.game) { _, _ -> }
        return downloader.getAvailableVersions()
    }
}
