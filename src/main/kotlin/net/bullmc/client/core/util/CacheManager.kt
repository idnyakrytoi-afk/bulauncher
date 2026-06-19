package net.bullmc.client.core.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

object CacheManager {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }

    suspend fun getCachedOrFetch(key: String, ttlMs: Long = 3600000, fetch: suspend () -> String): String {
        val cacheFile = LauncherPaths.getCacheFile(key)

        if (cacheFile.exists()) {
            val age = System.currentTimeMillis() - cacheFile.lastModified()
            if (age < ttlMs) {
                return cacheFile.readText()
            }
        }

        val data = fetch()
        cacheFile.writeText(data)
        return data
    }

    suspend fun downloadWithCache(
        url: String,
        dest: File,
        expectedHash: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        if (dest.exists()) {
            if (expectedHash != null) {
                val actualHash = sha1(dest)
                if (actualHash == expectedHash) {
                    onProgress?.invoke(1f)
                    return true
                }
            } else {
                onProgress?.invoke(1f)
                return true
            }
        }

        return try {
            dest.parentFile?.mkdirs()
            val response = client.get(url)
            val bytes = response.readBytes()
            dest.writeBytes(bytes)
            onProgress?.invoke(1f)
            true
        } catch (e: Exception) {
            println("[CACHE] Ошибка скачивания $url: ${e.message}")
            false
        }
    }

    fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getCacheSize(): Long {
        return LauncherPaths.launcherCache.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun clearExpired(maxAgeMs: Long = 86400000) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        LauncherPaths.launcherCache.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun clearAll() {
        LauncherPaths.launcherCache.listFiles()?.forEach { it.delete() }
    }
}
