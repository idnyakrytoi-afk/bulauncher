package net.bullmc.client.core.mod

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.bullmc.client.core.loader.LoaderType
import java.io.File

@Serializable
data class CurseForgeSearchResponse(
    val data: List<CurseForgeMod> = emptyList(),
    val pagination: CurseForgePagination? = null
)

@Serializable
data class CurseForgeMod(
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
    val summary: String = "",
    val downloadCount: Double = 0.0,
    val logo: CurseForgeLogo? = null,
    val authors: List<CurseForgeAuthor> = emptyList(),
    val latestFiles: List<CurseForgeFile> = emptyList()
)

@Serializable
data class CurseForgeLogo(
    val thumbnailUrl: String = "",
    val url: String = ""
)

@Serializable
data class CurseForgeAuthor(
    val name: String = ""
)

@Serializable
data class CurseForgeFile(
    val id: Int = 0,
    val displayName: String = "",
    val fileName: String = "",
    val downloadUrl: String? = null,
    val gameVersions: List<String> = emptyList(),
    val modLoader: Int = 0
)

@Serializable
data class CurseForgePagination(
    val totalCount: Int = 0
)

class CurseForgeApi {
    companion object {
        const val API_KEY = "\$2a\$10\$bL4bIL5pUWqfcO7KQtnMReakwtfHbNKh6v1uTpKlzhwoueEJQnPnm"
        const val BASE_URL = "https://api.curseforge.com"
        const val GAME_ID_MINECRAFT = 432
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getLoaderClassId(loader: LoaderType): Int {
        return when (loader) {
            LoaderType.FABRIC -> 4
            LoaderType.FORGE -> 1
            LoaderType.NEOFORGE -> 6
            LoaderType.QUILT -> 5
            LoaderType.VANILLA -> 0
        }
    }

    private fun getCategoryId(category: String?): Int {
        return when (category) {
            "optimization" -> 4
            "rendering" -> 5
            "utility" -> 2
            "technology" -> 3
            "adventure" -> 6
            "magic" -> 8
            "storage" -> 9
            "farming" -> 10
            "decoration" -> 11
            "mobs" -> 12
            "food" -> 13
            "library" -> 21
            "worldgen" -> 14
            else -> 0
        }
    }

    suspend fun searchMods(
        query: String,
        mcVersion: String? = null,
        loader: LoaderType? = null,
        limit: Int = 25,
        offset: Int = 0,
        category: String? = null,
        sort: String = "relevance"
    ): List<BrowserMod> {
        try {
            val sortField = when (sort) {
                "downloads" -> "2"
                "newest" -> "1"
                else -> "2"
            }
            val url = URLBuilder("$BASE_URL/v1/mods/search").apply {
                parameters.append("gameId", GAME_ID_MINECRAFT.toString())
                parameters.append("searchFilter", query)
                parameters.append("pageSize", limit.toString())
                parameters.append("index", offset.toString())
                parameters.append("sortField", sortField)
                if (mcVersion != null) {
                    parameters.append("gameVersion", mcVersion)
                }
                if (loader != null && loader != LoaderType.VANILLA) {
                    parameters.append("classId", getLoaderClassId(loader).toString())
                }
                val catId = getCategoryId(category)
                if (catId > 0) {
                    parameters.append("categoryId", catId.toString())
                }
            }.buildString()

            val response = client.get(url) {
                header("x-api-key", API_KEY)
                header("Accept", "application/json")
            }
            val body = response.bodyAsText()
            if (body.isBlank()) return emptyList()

            val searchResult = json.decodeFromString<CurseForgeSearchResponse>(body)

            return searchResult.data.map { mod ->
                BrowserMod(
                    id = mod.id.toString(),
                    slug = mod.slug,
                    name = mod.name,
                    description = mod.summary,
                    iconUrl = mod.logo?.thumbnailUrl,
                    source = ModSource.CURSEFORGE,
                    downloads = mod.downloadCount.toLong(),
                    author = mod.authors.firstOrNull()?.name ?: "",
                    installed = false
                )
            }
        } catch (e: Exception) {
            println("[CURSEFORGE] Search error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun downloadMod(projectId: String, mcVersion: String, loader: LoaderType, modsDir: File): Boolean {
        try {
            val response = client.get("$BASE_URL/v1/mods/$projectId") {
                header("x-api-key", API_KEY)
                header("Accept", "application/json")
            }
            val body = response.bodyAsText()
            if (body.isBlank()) return false

            val modData = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject ?: return false
            val latestFiles = modData["latestFiles"]?.jsonArray ?: return false

            val loaderTypeId = getLoaderClassId(loader)
            val compatibleFile = latestFiles.mapNotNull { el ->
                val file = el.jsonObject
                val gameVersions = file["gameVersions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val fileLoader = file["modLoader"]?.jsonPrimitive?.intOrNull ?: 0
                if (gameVersions.contains(mcVersion) && (fileLoader == 0 || fileLoader == loaderTypeId)) {
                    file
                } else null
            }.firstOrNull()

            if (compatibleFile == null) {
                println("[CURSEFORGE] No compatible file for project $projectId")
                return false
            }

            val downloadUrl = compatibleFile["downloadUrl"]?.jsonPrimitive?.content
            val fileName = compatibleFile["fileName"]?.jsonPrimitive?.content

            if (downloadUrl == null || fileName == null) {
                println("[CURSEFORGE] No download URL for project $projectId")
                return false
            }

            val destFile = File(modsDir, fileName)
            if (destFile.exists() && destFile.length() > 0) return true

            println("[CURSEFORGE] Downloading $fileName...")
            val fileResponse = client.get(downloadUrl)
            val bytes = fileResponse.readBytes()
            destFile.writeBytes(bytes)
            println("[CURSEFORGE] Saved: ${destFile.absolutePath} (${destFile.length()} bytes)")
            return true
        } catch (e: Exception) {
            println("[CURSEFORGE] Download error: ${e.message}")
            return false
        }
    }
}
