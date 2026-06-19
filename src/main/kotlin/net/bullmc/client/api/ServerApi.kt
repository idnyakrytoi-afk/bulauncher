package net.bullmc.client.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

@Serializable
data class NewsItem(val title: String, val content: String, val date: String)

@Serializable
data class McSrvStatResponse(
    val online: Boolean = false,
    val players: McSrvPlayers? = null,
    val version: String? = null,
    val motd: McSrvMotd? = null
)

@Serializable
data class McSrvPlayers(val online: Int = 0, val max: Int = 0)

@Serializable
data class McSrvMotd(val clean: List<String> = emptyList())

@Serializable
data class ServerStatus(
    val ip: String,
    val online: Boolean,
    val playersOnline: Int,
    val playersMax: Int,
    val version: String,
    val motd: String
)

object ServerApi {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val newsFile: File get() = File(LauncherPaths.root, "news.json")

    private val defaultNews = listOf(
        NewsItem("Добро пожаловать!", "Добро пожаловать на BullMC сервер!", "2024-01-01"),
        NewsItem("Новые мини-игры", "Добавлены новые мини-игры на сервере", "2024-01-15"),
        NewsItem("Обновление лаунчера", "Лаунчер обновлён с исправлениями", "2024-02-01")
    )

    fun loadNews(): List<NewsItem> {
        return try {
            if (newsFile.exists()) {
                val text = newsFile.readText()
                json.decodeFromString<List<NewsItem>>(text)
            } else {
                saveNews(defaultNews)
                defaultNews
            }
        } catch (e: Exception) {
            println("[API] Ошибка чтения новостей: ${e.message}")
            defaultNews
        }
    }

    fun saveNews(news: List<NewsItem>) {
        try {
            newsFile.parentFile?.mkdirs()
            newsFile.writeText(json.encodeToString(ListSerializer(NewsItem.serializer()), news))
            println("[API] Новости сохранены: ${newsFile.absolutePath}")
        } catch (e: Exception) {
            println("[API] Ошибка сохранения новостей: ${e.message}")
        }
    }

    fun addNews(title: String, content: String) {
        val current = loadNews().toMutableList()
        val date = java.time.LocalDate.now().toString()
        current.add(0, NewsItem(title, content, date))
        saveNews(current)
    }

    fun removeNews(index: Int) {
        val current = loadNews().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            saveNews(current)
        }
    }

    suspend fun getServerStatus(ip: String): ServerStatus {
        return try {
            val response = client.get("https://api.mcsrvstat.us/2/$ip").bodyAsText()
            val parsed = json.decodeFromString<McSrvStatResponse>(response)
            ServerStatus(
                ip = ip,
                online = parsed.online,
                playersOnline = parsed.players?.online ?: 0,
                playersMax = parsed.players?.max ?: 0,
                version = parsed.version ?: "?",
                motd = parsed.motd?.clean?.joinToString(" ") ?: ""
            )
        } catch (e: Exception) {
            println("[API] Ошибка статуса $ip: ${e.message}")
            ServerStatus(ip, false, 0, 0, "?", "")
        }
    }

    suspend fun getNews(): List<NewsItem> = loadNews()

    suspend fun checkLauncherUpdate(currentVersion: String): String? {
        return try {
            val text = client.get("https://raw.githubusercontent.com/bullmc/launcher/main/version.json").bodyAsText()
            val parsed = json.decodeFromString<LauncherVersion>(text)
            if (parsed.version != currentVersion) parsed.downloadUrl else null
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class LauncherVersion(
    val version: String = "",
    val downloadUrl: String = ""
)
