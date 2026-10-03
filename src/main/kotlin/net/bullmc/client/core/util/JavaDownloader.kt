package net.bullmc.client.core.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

@Serializable
data class AdoptiumRelease(
    val binary: AdoptiumBinary? = null,
    val release_name: String = ""
)

@Serializable
data class AdoptiumBinary(
    @SerialName("package") val pkg: AdoptiumPackage? = null,
    val installer: AdoptiumPackage? = null
)

@Serializable
data class AdoptiumPackage(
    val link: String = "",
    val name: String = "",
    val size: Long = 0
)

object JavaDownloader {
    private const val ADOPTIUM_API = "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse"

    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 300_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun downloadJava(
        targetDir: File,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): String? {
        val javaDir = File(targetDir, "jdk-21")
        val javaExe = File(javaDir, "bin/java.exe")

        if (javaExe.exists()) {
            println("[JRE] Java already exists: ${javaExe.absolutePath}")
            return javaExe.absolutePath
        }

        onProgress("Поиск Java 21...", 0f)

        return try {
            val response = client.get(ADOPTIUM_API)
            val body = response.bodyAsText()
            val releases = json.parseToJsonElement(body).jsonArray

            if (releases.isEmpty()) {
                println("[JRE] No Adoptium releases found")
                return null
            }

            val release = json.decodeFromString<AdoptiumRelease>(releases[0].toString())
            val downloadUrl = release.binary?.pkg?.link
            if (downloadUrl.isNullOrEmpty()) {
                println("[JRE] No download link found")
                return null
            }

            println("[JRE] Downloading: $downloadUrl")
            onProgress("Скачивание Java 21...", 0.1f)

            val tempFile = File(targetDir, "jdk-21-temp.zip")
            tempFile.parentFile?.mkdirs()

            val fileResponse = client.get(downloadUrl)
            val bytes = fileResponse.readBytes()
            tempFile.writeBytes(bytes)

            println("[JRE] Downloaded ${tempFile.length()} bytes, extracting...")
            onProgress("Распаковка Java 21...", 0.8f)

            targetDir.mkdirs()
            extractZip(tempFile, targetDir)

            tempFile.delete()

            val installedJava = findJavaInDir(targetDir)
            if (installedJava != null) {
                println("[JRE] Java installed: ${installedJava}")
                onProgress("Java 21 установлена!", 1.0f)
                installedJava
            } else {
                println("[JRE] Java exe not found after extraction")
                onProgress("Ошибка: Java не найдена после распаковки", 0f)
                null
            }
        } catch (e: Exception) {
            println("[JRE] Download failed: ${e.message}")
            onProgress("Ошибка скачивания Java: ${e.message}", 0f)
            null
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        zipFile.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = safeDestination(targetDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out ->
                            zis.copyTo(out)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    fun findJavaInDir(jreDir: File): String? {
        if (!jreDir.isDirectory) return null
        return jreDir.walkTopDown().maxDepth(4).firstOrNull {
            it.isFile && it.parentFile?.name == "bin" && it.name in setOf("java.exe", "java")
        }?.absolutePath
    }
}
