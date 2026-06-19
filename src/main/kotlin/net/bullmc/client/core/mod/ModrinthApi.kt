package net.bullmc.client.core.mod

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.bullmc.client.core.loader.LoaderType
import java.io.File

@Serializable
data class ModrinthSearchResult(
    val hits: List<ModrinthHit> = emptyList()
)

@Serializable
data class ModrinthHit(
    val slug: String,
    val title: String,
    val project_id: String,
    val versions: List<String> = emptyList()
)

@Serializable
data class ModrinthProject(
    val title: String,
    val slug: String,
    val loaders: List<String>,
    val versions: List<String>
)

class ModrinthApi {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getLoaderSlug(loader: LoaderType): String {
        return when (loader) {
            LoaderType.FABRIC -> "fabric"
            LoaderType.FORGE -> "forge"
            LoaderType.NEOFORGE -> "neoforge"
            LoaderType.QUILT -> "quilt"
            LoaderType.VANILLA -> "forge"
        }
    }

    private fun getLoaderSlugForMod(slug: String, loader: LoaderType): String {
        return when (loader) {
            LoaderType.QUILT -> "quilt"
            else -> getLoaderSlug(loader)
        }
    }

    suspend fun downloadMod(slug: String, mcVersion: String, loader: LoaderType, modsDir: File) {
        val loaderSlug = getLoaderSlugForMod(slug, loader)

        val versionUrl = io.ktor.http.URLBuilder("https://api.modrinth.com/v2/project/$slug/version").apply {
            parameters.append("game_versions", "[\"$mcVersion\"]")
            parameters.append("loaders", "[\"$loaderSlug\"]")
        }.buildString()

        try {
            val response = client.get(versionUrl)
            val body = response.bodyAsText()

            if (body.isBlank()) {
                println("[MODRINTH] Empty response for $slug")
                return
            }

            val versions = json.parseToJsonElement(body).jsonArray

            if (versions.isEmpty()) {
                println("[MODRINTH] No version found for $slug (MC $mcVersion, $loaderSlug)")
                return
            }

            val latestVersion = versions[0].jsonObject
            val files = latestVersion["files"]?.jsonArray
            if (files == null || files.isEmpty()) {
                println("[MODRINTH] No files for $slug")
                return
            }

            val primaryFile = files[0].jsonObject
            val downloadUrl = primaryFile["url"]?.jsonPrimitive?.content
            val fileName = primaryFile["filename"]?.jsonPrimitive?.content

            if (downloadUrl == null || fileName == null) {
                println("[MODRINTH] Invalid file info for $slug")
                return
            }

            val destFile = File(modsDir, fileName)
            if (destFile.exists() && destFile.length() > 0) {
                println("[MODRINTH] $fileName already exists, skipping")
                return
            }

            println("[MODRINTH] Downloading $fileName...")
            val fileResponse = client.get(downloadUrl)
            val bytes = fileResponse.readBytes()
            destFile.writeBytes(bytes)
            println("[MODRINTH] Saved: ${destFile.absolutePath} (${destFile.length()} bytes)")

        } catch (e: Exception) {
            println("[MODRINTH] Error downloading $slug: ${e.message}")
            throw e
        }
    }
}
