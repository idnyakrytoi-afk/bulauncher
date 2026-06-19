package net.bullmc.client.core.loader

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
enum class LoaderType(val displayName: String, val icon: String) {
    VANILLA("Vanilla", "V"),
    FABRIC("Fabric", "F"),
    FORGE("Forge", "FG"),
    NEOFORGE("NeoForge", "NF"),
    QUILT("Quilt", "Q")
}

@Serializable
enum class LoaderChannel(val displayName: String) {
    STABLE("Stable"),
    LATEST("Latest"),
    CUSTOM("Custom")
}

@Serializable
enum class ModCategory(val displayName: String) {
    OPTIMIZATION("Оптимизация"),
    RENDERING("Рендеринг"),
    UTILITIES("Утилиты")
}

@Serializable
data class ModInfo(
    val id: String,
    val name: String,
    val description: String,
    val loader: LoaderType,
    val category: ModCategory,
    val slug: String,
    val icon: String = "M"
)

@Serializable
data class LoaderConfig(
    val type: LoaderType = LoaderType.VANILLA,
    val enabledMods: List<String> = emptyList()
)

data class LoaderVersionEntry(
    val version: String,
    val stable: Boolean
)

object LoaderRegistry {

    val availableMods = listOf(
        ModInfo("sodium", "Sodium", "Оптимизация FPS, замена OptiFine", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "sodium"),
        ModInfo("lithium", "Lithium", "Оптимизация серверной логики (TPS)", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "lithium"),
        ModInfo("starlight", "Starlight", "Оптимизация светового движка", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "starlight"),
        ModInfo("ferritecore", "FerriteCore", "Оптимизация памяти", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "ferrite-core"),
        ModInfo("memoryleakfix", "MemoryLeakFix", "Исправление утечек памяти", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "memoryleakfix"),
        ModInfo("lazydfu", "LazyDFU", "Ускорение запуска", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "lazydfu"),
        ModInfo("entityculling", "EntityCulling", "Пропуск рендеринга невидимых сущностей", LoaderType.FABRIC, ModCategory.OPTIMIZATION, "entityculling"),
        ModInfo("iris", "Iris", "Поддержка шейдеров (замена OptiFine)", LoaderType.FABRIC, ModCategory.RENDERING, "iris"),
        ModInfo("modmenu", "Mod Menu", "Меню модов в игре", LoaderType.FABRIC, ModCategory.UTILITIES, "modmenu"),
        ModInfo("clothconfig", "Cloth Config API", "API конфигурации для модов", LoaderType.FABRIC, ModCategory.UTILITIES, "cloth-config"),

        ModInfo("embeddium", "Embeddium", "Оптимизация FPS (форк Sodium)", LoaderType.NEOFORGE, ModCategory.OPTIMIZATION, "embeddium"),
        ModInfo("oculus", "Oculus", "Поддержка шейдеров (форк Iris)", LoaderType.NEOFORGE, ModCategory.RENDERING, "oculus"),

        ModInfo("rubidium", "Rubidium", "Оптимизация FPS (форк Sodium)", LoaderType.FORGE, ModCategory.OPTIMIZATION, "rubidium"),
        ModInfo("oculus-forge", "Oculus", "Поддержка шейдеров (форк Iris)", LoaderType.FORGE, ModCategory.RENDERING, "oculus"),

        ModInfo("sodium-quilt", "Sodium", "Оптимизация FPS", LoaderType.QUILT, ModCategory.OPTIMIZATION, "sodium"),
        ModInfo("iris-quilt", "Iris", "Поддержка шейдеров", LoaderType.QUILT, ModCategory.RENDERING, "iris"),
    )

    fun getModsForLoader(loader: LoaderType): List<ModInfo> {
        return availableMods.filter { it.loader == loader || it.loader == LoaderType.FABRIC && loader == LoaderType.QUILT }
    }

    fun getLoaderVersions(loader: LoaderType): List<String> {
        return when (loader) {
            LoaderType.VANILLA -> emptyList()
            LoaderType.FABRIC -> listOf("0.19.3", "0.19.2", "0.19.1")
            LoaderType.FORGE -> listOf("47.3.0", "47.2.0", "47.1.0")
            LoaderType.NEOFORGE -> listOf("21.4.86", "21.3.100", "21.2.0")
            LoaderType.QUILT -> listOf("0.27.0", "0.26.4", "0.25.1")
        }
    }

    private val client = HttpClient(CIO) {
        followRedirects = true
    }
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun fetchFabricVersions(mcVersion: String): List<LoaderVersionEntry> {
        return try {
            val response = client.get("https://meta.fabricmc.net/v2/versions/loader/$mcVersion")
            val body = response.bodyAsText()
            val arr = jsonParser.parseToJsonElement(body).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val loader = obj["loader"]?.jsonObject ?: return@mapNotNull null
                val version = loader["version"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val stable = loader["stable"]?.jsonPrimitive?.booleanOrNull ?: false
                LoaderVersionEntry(version, stable)
            }
        } catch (e: Exception) {
            println("[LOADER] Failed to fetch Fabric versions: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchNeoForgeVersions(mcVersion: String): List<LoaderVersionEntry> {
        return try {
            val response = client.get("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml")
            val body = response.bodyAsText()
            val versions = Regex("<version>($mcVersion[^<]*)</version>").findAll(body).map { it.groupValues[1] }.toList()
            versions.take(20).map { LoaderVersionEntry(it, !it.contains("beta") && !it.contains("alpha")) }
        } catch (e: Exception) {
            println("[LOADER] Failed to fetch NeoForge versions: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAvailableVersions(loader: LoaderType, mcVersion: String): List<LoaderVersionEntry> {
        return when (loader) {
            LoaderType.FABRIC -> fetchFabricVersions(mcVersion)
            LoaderType.NEOFORGE -> fetchNeoForgeVersions(mcVersion)
            LoaderType.QUILT -> {
                try {
                    val response = client.get("https://meta.quiltmc.org/v3/versions/loader/$mcVersion")
                    val body = response.bodyAsText()
                    val arr = jsonParser.parseToJsonElement(body).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val version = obj["version"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val stable = obj["stable"]?.jsonPrimitive?.booleanOrNull ?: false
                        LoaderVersionEntry(version, stable)
                    }
                } catch (e: Exception) {
                    println("[LOADER] Failed to fetch Quilt versions: ${e.message}")
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    fun resolveVersion(channel: LoaderChannel, loader: LoaderType, mcVersion: String, customVersion: String, fetchedVersions: List<LoaderVersionEntry>): String {
        return when (channel) {
            LoaderChannel.STABLE -> {
                fetchedVersions.firstOrNull { it.stable }?.version
                    ?: getLoaderVersions(loader).firstOrNull()
                    ?: customVersion
            }
            LoaderChannel.LATEST -> {
                fetchedVersions.firstOrNull()?.version
                    ?: getLoaderVersions(loader).firstOrNull()
                    ?: customVersion
            }
            LoaderChannel.CUSTOM -> customVersion
        }
    }
}
