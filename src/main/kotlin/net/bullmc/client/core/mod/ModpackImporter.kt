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
import net.bullmc.client.core.util.safeDestination
import kotlinx.coroutines.CancellationException
import java.util.zip.ZipFile

@Serializable
data class MrpackManifest(
    val formatVersion: Int = 0,
    val game: String = "",
    val versionId: String = "",
    val name: String = "",
    val summary: String = "",
    val files: List<MrpackFile> = emptyList(),
    val dependencies: Map<String, String> = emptyMap()
)

@Serializable
data class MrpackFile(
    val path: String = "",
    val hashes: MrpackHashes = MrpackHashes(),
    val downloads: List<String> = emptyList(),
    val fileSize: Long = 0
)

@Serializable
data class MrpackHashes(
    val sha1: String = "",
    val sha512: String = ""
)

object ModpackImporter {
    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun readMrpack(zipFile: File): MrpackManifest? {
        return try {
            ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.getEntry("modrinth.index.json") ?: return null
                val manifestText = zip.getInputStream(manifestEntry).bufferedReader().readText()
                json.decodeFromString<MrpackManifest>(manifestText)
            }
        } catch (e: Exception) {
            println("[MODPACK] Failed to read mrpack: ${e.message}")
            null
        }
    }

    fun extractOverrides(zipFile: File, gameDir: File) {
        try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.name.startsWith("overrides/") && !entry.isDirectory) {
                        val relativePath = entry.name.removePrefix("overrides/")
                        val outFile = safeDestination(gameDir, relativePath)
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to extract overrides", e)
        }
    }

    suspend fun downloadModpackFiles(
        manifest: MrpackManifest,
        modsDir: File,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ) {
        modsDir.mkdirs()
        val total = manifest.files.size
        if (total == 0) return

        for ((index, file) in manifest.files.withIndex()) {
            val downloadUrl = file.downloads.firstOrNull()
                ?: throw IllegalArgumentException("No download URL for ${file.path}")
            val outFile = safeDestination(modsDir, file.path.removePrefix("mods/"))
            outFile.parentFile?.mkdirs()

            if (outFile.exists() && outFile.length() == file.fileSize) continue

            onProgress("Скачивание ${file.path.substringAfterLast('/')}", (index.toFloat() / total) * 0.9f)

            try {
                val response = client.get(downloadUrl)
                val bytes = response.readBytes()
                require(bytes.size.toLong() == file.fileSize) { "Incorrect size for ${file.path}" }
                outFile.writeBytes(bytes)
                println("[MODPACK] Downloaded: ${file.path}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw IllegalStateException("Failed to download ${file.path}", e)
            }
        }
        onProgress("Готово", 1.0f)
    }

    fun getLoaderFromDependencies(dependencies: Map<String, String>): Pair<LoaderType, String> {
        val fabricVersion = dependencies["fabric-loader"]
        if (fabricVersion != null) return LoaderType.FABRIC to fabricVersion

        val forgeVersion = dependencies["forge"]
        if (forgeVersion != null) return LoaderType.FORGE to forgeVersion

        val neoforgeVersion = dependencies["neoforge"]
        if (neoforgeVersion != null) return LoaderType.NEOFORGE to neoforgeVersion

        val quiltVersion = dependencies["quilt-loader"]
        if (quiltVersion != null) return LoaderType.QUILT to quiltVersion

        return LoaderType.VANILLA to ""
    }

    suspend fun importModpack(
        zipFile: File,
        gameDir: File,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): MrpackManifest? {
        onProgress("Чтение манифеста...", 0f)
        val manifest = readMrpack(zipFile) ?: return null

        onProgress("Распаковка overrides...", 0.1f)
        extractOverrides(zipFile, gameDir)

        val modsDir = File(gameDir, "mods")
        onProgress("Скачивание модов...", 0.2f)
        downloadModpackFiles(manifest, modsDir, onProgress)

        println("[MODPACK] Imported: ${manifest.name} (${manifest.versionId})")
        return manifest
    }
}
