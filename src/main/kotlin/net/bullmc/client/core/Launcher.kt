package net.bullmc.client.core

class Launcher {
    private val auth = Auth()
    private val gameDir = LauncherPaths.game
    private val mcLauncher = MinecraftLauncher(gameDir, auth)

    fun getGameDir() = LauncherPaths.game
    fun getModsDir() = LauncherPaths.mods.also { it.mkdirs() }

    suspend fun downloadAndLaunch(
        version: String,
        playerNick: String,
        javaPath: String = "java",
        ramMb: Int = 4096,
        serverIp: String? = null,
        loader: LoaderType = LoaderType.VANILLA,
        loaderVersion: String = "",
        enabledModIds: List<String> = emptyList(),
        onStatus: (String, Float) -> Unit
    ): Process? {
        LauncherPaths.init()
        onStatus("Подготовка...", 0f)

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

    fun isInstalled(version: String = "1.20.4"): Boolean {
        val versionDir = LauncherPaths.versions.resolve(version)
        return versionDir.resolve("$version.jar").exists() && versionDir.resolve("$version.json").exists()
    }

    suspend fun getAvailableVersions(): List<VersionEntry> {
        val downloader = MinecraftDownloader(gameDir) { _, _ -> }
        return downloader.getAvailableVersions()
    }
}
