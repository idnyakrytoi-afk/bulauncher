package net.bullmc.client.core.mod

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

@Serializable
data class ModUpdateInfo(
    val slug: String,
    val name: String,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String,
    val fileName: String
)

object ModUpdateChecker {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun checkForUpdates(
        modsDir: File,
        mcVersion: String,
        loader: LoaderType
    ): List<ModUpdateInfo> {
        val mods = modsDir.listFiles()?.filter { it.extension == "jar" } ?: return emptyList()
        val updates = mutableListOf<ModUpdateInfo>()

        for (modFile in mods) {
            val slug = guessSlugFromFile(modFile.name) ?: continue
            try {
                val update = checkModUpdate(slug, modFile, mcVersion, loader)
                if (update != null) {
                    updates.add(update)
                }
            } catch (e: Exception) {
                println("[UPDATE] Error checking $slug: ${e.message}")
            }
        }

        return updates
    }

    private fun guessSlugFromFile(fileName: String): String? {
        val name = fileName.removeSuffix(".jar")
            .replace(Regex("-\\d+\\.\\d+.*"), "")
            .replace(Regex("-fabric-.*"), "")
            .replace(Regex("-forge-.*"), "")
            .replace(Regex("-neoforge-.*"), "")
            .replace(Regex("-quilt-.*"), "")
            .lowercase()

        if (name.length < 2) return null
        return name
    }

    private suspend fun checkModUpdate(
        slug: String,
        localFile: File,
        mcVersion: String,
        loader: LoaderType
    ): ModUpdateInfo? {
        val loaderSlug = when (loader) {
            LoaderType.FABRIC -> "fabric"
            LoaderType.FORGE -> "forge"
            LoaderType.NEOFORGE -> "neoforge"
            LoaderType.QUILT -> "quilt"
            LoaderType.VANILLA -> return null
        }

        val url = "https://api.modrinth.com/v2/project/$slug/version?game_versions=%5B%22$mcVersion%22%5D&loaders=%5B%22$loaderSlug%22%5D"

        val response = client.get(url)
        val body = response.bodyAsText()

        if (body.isBlank() || body == "[]") return null

        val versions = json.parseToJsonElement(body).jsonArray
        if (versions.isEmpty()) return null

        val latest = versions[0].jsonObject
        val latestVersion = latest["version_number"]?.jsonPrimitive?.content ?: return null
        val files = latest["files"]?.jsonArray ?: return null
        if (files.isEmpty()) return null

        val primaryFile = files[0].jsonObject
        val downloadUrl = primaryFile["url"]?.jsonPrimitive?.content ?: return null
        val fileName = primaryFile["filename"]?.jsonPrimitive?.content ?: return null

        val currentVersion = extractVersionFromFilename(localFile.name)
        if (currentVersion == latestVersion) return null

        return ModUpdateInfo(
            slug = slug,
            name = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            downloadUrl = downloadUrl,
            fileName = fileName
        )
    }

    private fun extractVersionFromFilename(fileName: String): String {
        val regex = Regex("""[\d]+\.[\d]+(?:\.[\d]+)?""")
        val matches = regex.findAll(fileName).toList()
        return matches.lastOrNull()?.value ?: fileName
    }

    suspend fun applyUpdate(update: ModUpdateInfo, modsDir: File): Boolean {
        return try {
            val response = client.get(update.downloadUrl)
            val bytes = response.readBytes()

            val oldFile = modsDir.listFiles()?.find {
                it.name.contains(update.slug, ignoreCase = true) && it.extension == "jar"
            }
            oldFile?.delete()

            val newFile = File(modsDir, update.fileName)
            newFile.writeBytes(bytes)
            println("[UPDATE] Updated ${update.slug}: ${update.currentVersion} -> ${update.latestVersion}")
            true
        } catch (e: Exception) {
            println("[UPDATE] Failed to update ${update.slug}: ${e.message}")
            false
        }
    }
}
