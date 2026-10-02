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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import net.bullmc.client.core.loader.LoaderType
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Serializable
data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchHit> = emptyList(),
    val offset: Int = 0,
    val limit: Int = 0,
    val total_hits: Int = 0
)

@Serializable
data class ModrinthSearchHit(
    val slug: String = "",
    val title: String = "",
    val project_id: String = "",
    val description: String = "",
    val icon_url: String? = null,
    val categories: List<String> = emptyList(),
    val versions: List<String> = emptyList(),
    val downloads: Long = 0,
    val author: String = "",
    val date_modified: String = ""
)

@Serializable
data class ModrinthProjectFull(
    val title: String = "",
    val slug: String = "",
    val description: String = "",
    val body: String = "",
    val icon_url: String? = null,
    val categories: List<String> = emptyList(),
    val versions: List<String> = emptyList(),
    val downloads: Long = 0,
    val author: String = "",
    val date_modified: String = "",
    val project_id: String = "",
    val server_side: String = "",
    val client_side: String = ""
)

@Serializable
data class ManagedModRecord(
    val fileName: String,
    val sha512: String,
    val gameVersion: String,
    val loader: LoaderType
)

class ModrinthApi(private val client: HttpClient = defaultClient()) {
    companion object {
    private fun defaultClient() = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }
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
            val sortParam = when (sort) {
                "downloads" -> "downloads"
                "newest" -> "newest"
                else -> "relevance"
            }
            val facets = buildFacets(mcVersion, loader, category)
            val url = "https://api.modrinth.com/v2/search?query=$query&limit=$limit&offset=$offset&index=$sortParam&facets=${java.net.URLEncoder.encode(facets, "UTF-8")}"

            val response = client.get(url)
            val body = response.bodyAsText()
            if (body.isBlank()) return emptyList()

            val searchResult = json.decodeFromString<ModrinthSearchResponse>(body)

            return searchResult.hits.map { hit ->
                BrowserMod(
                    id = hit.project_id,
                    slug = hit.slug,
                    name = hit.title,
                    description = hit.description,
                    iconUrl = hit.icon_url,
                    source = ModSource.MODRINTH,
                    downloads = hit.downloads,
                    author = hit.author,
                    categories = hit.categories,
                    mcVersions = hit.versions,
                    dateModified = hit.date_modified
                )
            }
        } catch (e: Exception) {
            println("[MODRINTH] Search error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getModDetails(projectId: String): BrowserMod? {
        try {
            val response = client.get("https://api.modrinth.com/v2/project/$projectId")
            val body = response.bodyAsText()
            if (body.isBlank()) return null

            val project = json.decodeFromString<ModrinthProjectFull>(body)

            return BrowserMod(
                id = project.project_id,
                slug = project.slug,
                name = project.title,
                description = project.description,
                body = project.body,
                iconUrl = project.icon_url,
                source = ModSource.MODRINTH,
                downloads = project.downloads,
                author = project.author,
                categories = project.categories,
                mcVersions = project.versions,
                dateModified = project.date_modified,
                clientSide = project.client_side,
                serverSide = project.server_side
            )
        } catch (e: Exception) {
            println("[MODRINTH] Details error: ${e.message}")
            return null
        }
    }

    suspend fun hasCompatibleVersion(slug: String, mcVersion: String, loader: LoaderType): Boolean =
        compatibleVersions(slug, mcVersion, loader).isNotEmpty()

    private suspend fun compatibleVersions(slug: String, mcVersion: String, loader: LoaderType): JsonArray {
        val loaderSlug = getLoaderSlugForMod(slug, loader)
        val versionUrl = URLBuilder("https://api.modrinth.com/v2/project/$slug/version").apply {
            parameters.append("game_versions", "[\"$mcVersion\"]")
            parameters.append("loaders", "[\"$loaderSlug\"]")
        }.buildString()
        val response = client.get(versionUrl)
        return json.parseToJsonElement(response.bodyAsText()).jsonArray
    }

    suspend fun downloadMod(slug: String, mcVersion: String, loader: LoaderType, modsDir: File): File {
        modsDir.mkdirs()
        val cached = readManagedMods(modsDir)[slug]
        if (cached != null && cached.gameVersion == mcVersion && cached.loader == loader &&
            cached.fileName == File(cached.fileName).name && cached.fileName.endsWith(".jar", ignoreCase = true)) {
            val cachedFile = File(modsDir, cached.fileName)
            if (cachedFile.isFile && sha512(cachedFile.readBytes()).equals(cached.sha512, ignoreCase = true)) {
                return cachedFile
            }
        }
        val version = compatibleVersions(slug, mcVersion, loader).firstOrNull()?.jsonObject
            ?: throw IllegalStateException("Для $slug нет версии под Minecraft $mcVersion (${loader.displayName})")
        val files = version["files"]?.jsonArray.orEmpty()
        val selected = files.firstOrNull { it.jsonObject["primary"]?.jsonPrimitive?.booleanOrNull == true }
            ?: files.firstOrNull()
            ?: throw IllegalStateException("Для $slug нет файла загрузки")
        val file = selected.jsonObject
        val fileName = file["filename"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("У $slug не указано имя файла")
        require(fileName == File(fileName).name && fileName.endsWith(".jar", ignoreCase = true)) {
            "Недопустимое имя файла мода: $fileName"
        }
        val downloadUrl = file["url"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("У $slug не указана ссылка на файл")
        val expectedHash = file["hashes"]?.jsonObject?.get("sha512")?.jsonPrimitive?.content
            ?: throw IllegalStateException("У $slug нет SHA-512 для проверки файла")
        val dest = File(modsDir, fileName)
        if (dest.exists() && sha512(dest.readBytes()).equals(expectedHash, ignoreCase = true)) {
            recordManagedMod(modsDir, slug, ManagedModRecord(fileName, expectedHash, mcVersion, loader))
            return dest
        }

        val bytes = client.get(downloadUrl).readBytes()
        check(bytes.isNotEmpty() && sha512(bytes).equals(expectedHash, ignoreCase = true)) {
            "Проверка целостности $slug не прошла"
        }
        val temporary = File(modsDir, ".$fileName.part")
        try {
            temporary.writeBytes(bytes)
            moveReplace(temporary, dest)
        } finally {
            temporary.delete()
        }
        println("[MODRINTH] Saved: ${dest.absolutePath} (${dest.length()} bytes)")
        recordManagedMod(modsDir, slug, ManagedModRecord(fileName, expectedHash, mcVersion, loader))
        return dest
    }

    private fun readManagedMods(modsDir: File): Map<String, ManagedModRecord> {
        val index = File(modsDir, ".bullmc-managed-mods.json")
        return if (index.exists()) {
            try { json.decodeFromString<Map<String, ManagedModRecord>>(index.readText()) } catch (_: Exception) { emptyMap() }
        } else emptyMap()
    }

    private fun recordManagedMod(modsDir: File, slug: String, record: ManagedModRecord) {
        val index = File(modsDir, ".bullmc-managed-mods.json")
        val previous = readManagedMods(modsDir)
        if (previous[slug] == record) return
        val oldName = previous[slug]?.fileName
        if (oldName != null && oldName == File(oldName).name && oldName.endsWith(".jar", ignoreCase = true)) {
            val old = File(modsDir, oldName)
            check(!old.exists() || old.delete()) { "Не удалось удалить старую версию $slug: $oldName" }
        }
        val temporary = File(modsDir, ".bullmc-managed-mods.json.part")
        try {
            temporary.writeText(json.encodeToString(previous + (slug to record)))
            moveReplace(temporary, index)
        } finally {
            temporary.delete()
        }
    }

    private fun moveReplace(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun sha512(bytes: ByteArray): String = MessageDigest.getInstance("SHA-512")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun downloadModById(projectId: String, mcVersion: String, loader: LoaderType, modsDir: File): Boolean {
        val loaderSlug = getLoaderSlug(loader)

        val versionUrl = URLBuilder("https://api.modrinth.com/v2/project/$projectId/version").apply {
            parameters.append("game_versions", "[\"$mcVersion\"]")
            parameters.append("loaders", "[\"$loaderSlug\"]")
        }.buildString()

        try {
            val response = client.get(versionUrl)
            val body = response.bodyAsText()
            if (body.isBlank()) return false

            val versions = json.parseToJsonElement(body).jsonArray
            if (versions.isEmpty()) {
                println("[MODRINTH] No compatible version for $projectId")
                return false
            }

            val latestVersion = versions[0].jsonObject
            val files = latestVersion["files"]?.jsonArray
            if (files == null || files.isEmpty()) return false

            val primaryFile = files[0].jsonObject
            val downloadUrl = primaryFile["url"]?.jsonPrimitive?.content
            val fileName = primaryFile["filename"]?.jsonPrimitive?.content

            if (downloadUrl == null || fileName == null) return false

            val destFile = File(modsDir, fileName)
            if (destFile.exists() && destFile.length() > 0) return true

            println("[MODRINTH] Downloading $fileName...")
            val fileResponse = client.get(downloadUrl)
            val bytes = fileResponse.readBytes()
            destFile.writeBytes(bytes)
            return true
        } catch (e: Exception) {
            println("[MODRINTH] Download error: ${e.message}")
            return false
        }
    }

    private fun getLoaderSlugForMod(slug: String, loader: LoaderType): String {
        return when (loader) {
            LoaderType.QUILT -> "quilt"
            else -> getLoaderSlug(loader)
        }
    }

    private fun buildFacets(mcVersion: String?, loader: LoaderType?, category: String? = null): String {
        val facets = mutableListOf<List<String>>()

        if (mcVersion != null) {
            facets.add(listOf("versions:$mcVersion"))
        }
        if (loader != null && loader != LoaderType.VANILLA) {
            facets.add(listOf("loaders:${getLoaderSlug(loader)}"))
        }
        if (category != null) {
            facets.add(listOf("categories:$category"))
        }
        facets.add(listOf("project_type:mod"))

        return json.encodeToString(JsonArraySerializer, JsonArray(facets.map { facet ->
            JsonArray(facet.map { JsonPrimitive(it) })
        }))
    }

    private val JsonArraySerializer = JsonArray.serializer()
}
