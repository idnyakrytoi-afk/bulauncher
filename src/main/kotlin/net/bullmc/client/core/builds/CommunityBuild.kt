package net.bullmc.client.core.builds

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

/**
 * Сборка, опубликованная игроком: версия игры + лоадер + набор модов.
 * Хранится как JSON, поэтому её легко расшарить файлом или положить в репозиторий.
 */
@Serializable
data class CommunityBuild(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val author: String,
    val description: String = "",
    val mcVersion: String = "1.20.4",
    val loader: LoaderType = LoaderType.FABRIC,
    val loaderVersion: String = "",
    val mods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val ramMb: Int = 4096,
    val downloads: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** true — сборка создана на этом компьютере и ещё не опубликована. */
    val local: Boolean = true
) {
    val modsCount: Int get() = mods.size

    fun formatSize(): String = "$modsCount " + when {
        modsCount % 10 == 1 && modsCount % 100 != 11 -> "мод"
        modsCount % 10 in 2..4 && modsCount % 100 !in 12..14 -> "мода"
        else -> "модов"
    }

    fun shortInfo(): String = buildString {
        append("MC ").append(mcVersion)
        if (loader != LoaderType.VANILLA) {
            append(" · ").append(loader.displayName)
            if (loaderVersion.isNotEmpty()) append(" ").append(loaderVersion)
        }
        append(" · ").append(formatSize())
    }
}

/** Результат установки сборки. */
sealed class BuildInstallResult {
    data class Success(val installed: Int, val failed: List<String>) : BuildInstallResult()
    data class Error(val message: String) : BuildInstallResult()
}

/**
 * Каталог сборок сообщества.
 *
 * Источники:
 *  1. Локальный файл `builds.json` в папке лаунчера (свои сборки + кэш).
 *  2. Удалённый индекс [REMOTE_INDEX_URL] (сборки, опубликованные игроками).
 *  3. Встроенный каталог [DEFAULT_BUILDS] — чтобы экран не был пустым офлайн.
 */
object BuildsRepository {
    /**
     * Адрес общего индекса сборок. Любой игрок может добавить свою сборку,
     * отправив PR с изменением builds.json в этом репозитории.
     */
    const val REMOTE_INDEX_URL =
        "https://raw.githubusercontent.com/idnyakrytoi-afk/bulauncher/main/builds.json"

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val catalogFile: File get() = File(LauncherPaths.root, "builds.json")

    @Volatile
    private var cache: MutableList<CommunityBuild> = mutableListOf()

    @Volatile
    private var loaded = false

    private val defaultBuilds = listOf(
        CommunityBuild(
            id = "default-fps",
            name = "FPS Boost",
            author = "BullMC Team",
            description = "Максимальный FPS без потери качества: Sodium, Lithium, EntityCulling и FerriteCore.",
            mcVersion = "1.20.4",
            loader = LoaderType.FABRIC,
            mods = listOf("sodium", "lithium", "entityculling", "ferritecore", "lazydfu"),
            tags = listOf("оптимизация", "pvp"),
            downloads = 1284,
            local = false
        ),
        CommunityBuild(
            id = "default-shaders",
            name = "Shaders & Beauty",
            author = "zakuril",
            description = "Красивая картинка и шейдеры. Нужна видеокарта от 4 ГБ.",
            mcVersion = "1.20.4",
            loader = LoaderType.FABRIC,
            mods = listOf("sodium", "iris", "entityculling", "modmenu"),
            tags = listOf("шейдеры", "графика"),
            downloads = 862,
            local = false
        ),
        CommunityBuild(
            id = "default-pvp",
            name = "PvP Pack",
            author = "DarkLord",
            description = "Сборка для PvP-арены: оптимизация + быстрый отклик клиента.",
            mcVersion = "1.20.4",
            loader = LoaderType.FABRIC,
            mods = listOf("sodium", "lithium", "memoryleakfix", "entityculling"),
            tags = listOf("pvp", "оптимизация"),
            downloads = 1533,
            local = false
        ),
        CommunityBuild(
            id = "default-neoforge",
            name = "NeoForge Starter",
            author = "player228",
            description = "Стартовый набор на NeoForge для модовых серверов.",
            mcVersion = "1.21.4",
            loader = LoaderType.NEOFORGE,
            mods = listOf("embeddium", "oculus"),
            tags = listOf("neoforge"),
            downloads = 411,
            local = false
        )
    )

    /** Все известные сборки: удалённые + локальные (локальные первыми). */
    fun getAll(): List<CommunityBuild> {
        ensureLoaded()
        return cache.toList().sortedWith(
            compareByDescending<CommunityBuild> { it.local }
                .thenByDescending { it.downloads }
                .thenByDescending { it.createdAt }
        )
    }

    fun getLocalOnly(): List<CommunityBuild> = getAll().filter { it.local }

    fun findById(id: String): CommunityBuild? {
        ensureLoaded()
        return cache.find { it.id == id }
    }

    /** Загружает локальный каталог; при первом запуске добавляет встроенные сборки. */
    fun ensureLoaded() {
        if (loaded) return
        LauncherPaths.init()
        cache = readLocal().toMutableList()
        if (cache.isEmpty()) {
            cache.addAll(defaultBuilds)
            persist()
        } else {
            // добираем встроенные сборки, если их ещё нет в файле
            defaultBuilds.forEach { def ->
                if (cache.none { it.id == def.id }) cache.add(def)
            }
        }
        loaded = true
        println("[BUILDS] Загружено сборок: ${cache.size}")
    }

    /** Подтягивает опубликованные игроками сборки из удалённого индекса. */
    suspend fun refreshRemote(url: String = REMOTE_INDEX_URL): List<CommunityBuild> {
        ensureLoaded()
        return try {
            val text = fetchText(url)
            if (text.isBlank()) return emptyList()
            val remote = json.decodeFromString(ListSerializer(CommunityBuild.serializer()), text)
            val remoteBuilds = remote.map { it.copy(local = false) }

            // заменяем удалённые сборки свежими, локальные не трогаем
            val localOnly = cache.filter { it.local }
            cache = (localOnly + remoteBuilds).distinctBy { it.id }.toMutableList()
            persist()
            println("[BUILDS] Обновлено из индекса: ${remoteBuilds.size}")
            remoteBuilds
        } catch (e: Exception) {
            println("[BUILDS] Индекс недоступен: ${e.message}")
            emptyList()
        }
    }

    /** Сохраняет новую сборку (публикация своей сборки). */
    fun publish(build: CommunityBuild): CommunityBuild {
        ensureLoaded()
        val prepared = build.copy(
            id = build.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            createdAt = System.currentTimeMillis(),
            local = true
        )
        cache.removeAll { it.id == prepared.id }
        cache.add(0, prepared)
        persist()
        println("[BUILDS] Опубликована локально: ${prepared.name}")
        return prepared
    }

    fun delete(id: String): Boolean {
        ensureLoaded()
        val removed = cache.removeAll { it.id == id && it.local }
        if (removed) persist()
        return removed
    }

    /** +1 к счётчику загрузок (для своих сборок). */
    fun incrementDownloads(id: String) {
        ensureLoaded()
        val index = cache.indexOfFirst { it.id == id }
        if (index >= 0) {
            cache[index] = cache[index].copy(downloads = cache[index].downloads + 1)
            persist()
        }
    }

    /**
     * Экспорт сборки в файл `.bullbuild` — так её можно передать другому игроку.
     * Для публикации в общем индексе содержимое файла добавляется в builds.json репозитория.
     */
    fun exportToFile(build: CommunityBuild, target: File): File {
        target.parentFile?.mkdirs()
        target.writeText(json.encodeToString(ListSerializer(CommunityBuild.serializer()), listOf(build.copy(local = false))))
        return target
    }

    /** Импорт сборок из файла `.bullbuild` / `builds.json`. */
    fun importFromFile(source: File): List<CommunityBuild> {
        ensureLoaded()
        return try {
            val imported = json.decodeFromString(ListSerializer(CommunityBuild.serializer()), source.readText())
            imported.forEach { build ->
                val prepared = build.copy(id = java.util.UUID.randomUUID().toString(), local = true)
                cache.removeAll { it.name == prepared.name && it.local }
                cache.add(0, prepared)
            }
            persist()
            println("[BUILDS] Импортировано: ${imported.size} из ${source.name}")
            imported
        } catch (e: Exception) {
            println("[BUILDS] Ошибка импорта ${source.name}: ${e.message}")
            emptyList()
        }
    }

    /** JSON-представление сборки для вставки в общий индекс. */
    fun toShareableJson(build: CommunityBuild): String =
        json.encodeToString(ListSerializer(CommunityBuild.serializer()), listOf(build.copy(local = false)))

    fun search(query: String, tag: String?, loader: LoaderType?): List<CommunityBuild> {
        val q = query.trim().lowercase()
        return getAll().filter { build ->
            val matchesQuery = q.isEmpty() ||
                    build.name.lowercase().contains(q) ||
                    build.author.lowercase().contains(q) ||
                    build.description.lowercase().contains(q) ||
                    build.mods.any { it.lowercase().contains(q) }
            val matchesTag = tag == null || build.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesLoader = loader == null || build.loader == loader
            matchesQuery && matchesTag && matchesLoader
        }
    }

    fun allTags(): List<String> = getAll().flatMap { it.tags }.distinct().sorted()

    private fun readLocal(): List<CommunityBuild> {
        return try {
            if (catalogFile.exists()) {
                json.decodeFromString(ListSerializer(CommunityBuild.serializer()), catalogFile.readText())
            } else emptyList()
        } catch (e: Exception) {
            println("[BUILDS] Ошибка чтения каталога: ${e.message}")
            emptyList()
        }
    }

    private fun persist() {
        try {
            catalogFile.parentFile?.mkdirs()
            catalogFile.writeText(json.encodeToString(ListSerializer(CommunityBuild.serializer()), cache))
        } catch (e: Exception) {
            println("[BUILDS] Ошибка сохранения каталога: ${e.message}")
        }
    }

    private suspend fun fetchText(url: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val conn = java.net.URL(url).openConnection() as? java.net.HttpURLConnection ?: return@withContext ""
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "BullMC-Launcher/1.0")
            if (conn.responseCode !in 200..299) return@withContext ""
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
