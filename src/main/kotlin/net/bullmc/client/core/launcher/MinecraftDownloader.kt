package net.bullmc.client.core.launcher

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionEntry>
)

@Serializable
data class LatestVersions(
    val release: String,
    val snapshot: String
)

@Serializable
data class VersionEntry(
    val id: String,
    val url: String,
    val type: String,
    val releaseTime: String = ""
)

@Serializable
data class VersionJson(
    val id: String,
    val downloads: Downloads,
    val libraries: List<Library>,
    val assetIndex: AssetIndex,
    val assets: String,
    val mainClass: String,
    val arguments: Arguments? = null
)

@Serializable
data class Downloads(
    val client: DownloadEntry
)

@Serializable
data class DownloadEntry(
    val url: String,
    val sha1: String,
    val size: Long
)

@Serializable
data class Library(
    val name: String,
    val downloads: LibraryDownloads? = null,
    val rules: List<Rule>? = null
)

@Serializable
data class LibraryDownloads(
    val artifact: Artifact? = null,
    val classifiers: Map<String, Artifact>? = null
)

@Serializable
data class Artifact(
    val path: String,
    val url: String,
    val size: Long
)

@Serializable
data class Rule(
    val action: String,
    val os: OsRule? = null
)

@Serializable
data class OsRule(
    val name: String? = null
)

@Serializable
data class AssetIndex(
    val id: String,
    val url: String,
    val totalSize: Long
)

@Serializable
data class Arguments(
    val game: List<kotlinx.serialization.json.JsonElement>? = null,
    val jvm: List<kotlinx.serialization.json.JsonElement>? = null
)

@Serializable
data class AssetsIndex(
    val objects: Map<String, AssetObject>
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)

class MinecraftDownloader(
    private val gameDir: File,
    private val onProgress: (String, Float) -> Unit = { _, _ -> }
) {
    private val versionsDir = File(gameDir, "versions")
    private val librariesDir = File(gameDir, "libraries")
    private val assetsDir = File(gameDir, "assets")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun ensureInstalled(version: String = "1.20.4"): File {
        listOf(versionsDir, librariesDir, assetsDir).forEach {
            if (!it.exists()) it.mkdirs()
        }

        val versionDir = File(versionsDir, version)
        versionDir.mkdirs()
        val clientJar = File(versionDir, "$version.jar")
        val versionJsonFile = File(versionDir, "$version.json")

        if (!versionJsonFile.exists() || versionJsonFile.length() == 0L) {
            onProgress("Получение манифеста версий...", 0f)
            val manifest = fetchVersionManifest()
            val entry = manifest.versions.find { it.id == version }
                ?: throw IllegalArgumentException("Версия $version не найдена в манифесте Mojang")

            onProgress("Загрузка JSON версии...", 0.1f)
            println("[DL] Скачиваем JSON: ${entry.url}")
            val response = client.get(entry.url)
            println("[DL] HTTP ${response.status}")
            val body = response.bodyAsText()
            println("[DL] Тело ${body.length} символов")
            versionJsonFile.writeText(body)
            println("[DL] Записано: ${versionJsonFile.absolutePath} (${versionJsonFile.length()} байт)")
        }

        if (!versionJsonFile.exists() || versionJsonFile.length() == 0L) {
            throw IllegalStateException("JSON версии не был создан: ${versionJsonFile.absolutePath}")
        }

        val versionJson = json.decodeFromString<VersionJson>(versionJsonFile.readText())

        if (!clientJar.exists()) {
            onProgress("Скачивание клиента...", 0.2f)
            downloadFile(versionJson.downloads.client.url, clientJar, versionJson.downloads.client.size, 0.2f, 0.5f)
        }

        onProgress("Скачивание библиотек...", 0.5f)
        downloadLibraries(versionJson.libraries, 0.5f, 0.8f)

        onProgress("Скачивание ассетов...", 0.8f)
        downloadAssets(versionJson.assets, versionJson.assetIndex, 0.8f, 0.95f)

        onProgress("Готово!", 1.0f)
        return clientJar
    }

    private suspend fun fetchVersionManifest(): VersionManifest {
        val response = client.get("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
        println("Манифест статус: ${response.status}")
        val text = response.bodyAsText()
        println("Манифест размер: ${text.length}")
        return json.decodeFromString(text)
    }

    /**
     * Полный список версий из манифеста Mojang: release, snapshot, old_beta, old_alpha.
     * Именно он даёт выбор любой версии игры, как в Legacy Launcher / TLauncher.
     */
    suspend fun getAvailableVersions(): List<VersionEntry> {
        return try {
            fetchVersionManifest().versions
        } catch (e: Exception) {
            println("Ошибка загрузки списка версий: ${e.message}")
            listOf(VersionEntry("1.20.4", "", "release"))
        }
    }

    private suspend fun downloadFile(url: String, dest: File, totalSize: Long, progressStart: Float, progressEnd: Float) {
        dest.parentFile?.mkdirs()
        try {
            val response = client.get(url)
            println("Скачиваем $url -> статус: ${response.status}")
            val bytes = response.readBytes()
            println("Получено ${bytes.size} байт")
            dest.writeBytes(bytes)
            println("Сохранено: ${dest.absolutePath}, размер: ${dest.length()}")
            val progress = if (totalSize > 0) dest.length().toFloat() / totalSize else 1f
            onProgress("", progressStart + (progressEnd - progressStart) * progress)
        } catch (e: Exception) {
            println("Ошибка скачивания $url: ${e.message}")
            throw e
        }
    }

    // Проверка целостности файла по размеру и SHA1 хешу
    private fun isFileValid(file: File, expectedSize: Long, expectedSha1: String): Boolean {
        if (!file.exists()) {
            return false
        }

        // Проверяем размер
        if (file.length() != expectedSize) {
            println("[VERIFY] Файл ${file.name}: неверный размер (${file.length()} вместо $expectedSize)")
            return false
        }

        // Если хеш пустой, только проверяем размер
        if (expectedSha1.isEmpty()) {
            return true
        }

        // Проверяем SHA1 хеш
        val actualSha1 = file.inputStream().use { stream ->
            MessageDigest.getInstance("SHA-1")
                .digest(stream.readAllBytes())
                .joinToString("") { "%02x".format(it) }
        }

        val isValid = actualSha1 == expectedSha1
        if (!isValid) {
            println("[VERIFY] Файл ${file.name}: неверный хеш SHA1")
            println("[VERIFY] Ожидается: $expectedSha1")
            println("[VERIFY] Получено: $actualSha1")
        }
        return isValid
    }

    private suspend fun downloadLibraries(libraries: List<Library>, progressStart: Float, progressEnd: Float) {
        val toDownload = libraries.mapNotNull { lib ->
            lib.downloads?.artifact?.let { artifact ->
                val dest = File(librariesDir, artifact.path)
                if (!isFileValid(dest, artifact.size, "")) {
                    dest.delete()
                    Triple(artifact.url, dest, artifact.size)
                } else null
            }
        }
        if (toDownload.isEmpty()) return

        println("[DL] Скачивание ${toDownload.size} библиотек параллельно...")
        val completed = java.util.concurrent.atomic.AtomicInteger(0)
        val total = toDownload.size

        coroutineScope {
            toDownload.chunked(8).forEach { batch ->
                batch.map { (url, dest, size) ->
                    async {
                        try {
                            val response = client.get(url)
                            val bytes = response.readBytes()
                            dest.parentFile?.mkdirs()
                            dest.writeBytes(bytes)
                            val done = completed.incrementAndGet()
                            onProgress("", progressStart + (progressEnd - progressStart) * (done.toFloat() / total))
                        } catch (e: Exception) {
                            println("[DL] Ошибка библиотеки $url: ${e.message}")
                        }
                    }
                }.awaitAll()
            }
        }
        println("[DL] Библиотеки скачаны")
    }

    private suspend fun downloadAssets(assetsId: String, assetIndex: AssetIndex, progressStart: Float, progressEnd: Float) {
        val indexFile = File(assetsDir, "indexes/$assetsId.json")
        indexFile.parentFile?.mkdirs()

        if (!indexFile.exists() || indexFile.length() == 0L) {
            downloadFile(assetIndex.url, indexFile, assetIndex.totalSize, 0f, 0f)
        }

        val index = json.decodeFromString<AssetsIndex>(indexFile.readText())
        val objectsDir = File(assetsDir, "objects")

        val toDownload = index.objects.entries.mapNotNull { (name, obj) ->
            val hash = obj.hash
            val prefix = hash.substring(0, 2)
            val file = File(objectsDir, "$prefix/$hash")
            if (!isFileValid(file, obj.size, hash)) {
                file.delete()
                Triple("https://resources.download.minecraft.net/$prefix/$hash", file, obj.size)
            } else null
        }

        if (toDownload.isEmpty()) {
            println("[DL] Все ассеты уже скачаны")
            onProgress("", progressEnd)
            return
        }

        println("[DL] Скачивание ${toDownload.size} ассетов параллельно (из ${index.objects.size} всего)...")
        val completed = java.util.concurrent.atomic.AtomicInteger(0)
        val total = toDownload.size

        coroutineScope {
            toDownload.chunked(16).forEach { batch ->
                batch.map { (url, dest, _) ->
                    async {
                        try {
                            val response = client.get(url)
                            val bytes = response.readBytes()
                            dest.parentFile?.mkdirs()
                            dest.writeBytes(bytes)
                            val done = completed.incrementAndGet()
                            if (done % 50 == 0 || done == total) {
                                println("[DL] Ассеты: $done/$total")
                            }
                            onProgress("", progressStart + (progressEnd - progressStart) * (done.toFloat() / total))
                        } catch (e: Exception) {
                            println("[DL] Ошибка ассета $url: ${e.message}")
                        }
                    }
                }.awaitAll()
            }
        }
        println("[DL] Ассеты скачаны")
    }
}
