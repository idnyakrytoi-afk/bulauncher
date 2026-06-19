package net.bullmc.client.core

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class FabricLoaderVersion(
    val loader: FabricLoaderInfo
)

@Serializable
data class FabricLoaderInfo(
    val version: String,
    val stable: Boolean
)

@Serializable
data class FabricVersionJson(
    val id: String,
    val inheritsFrom: String,
    val mainClass: String? = null,
    val arguments: JsonElement? = null,
    val libraries: List<JsonElement>
)

class LoaderManager(
    private val gameDir: File,
    private val onProgress: (String, Float) -> Unit = { _, _ -> }
) {
    private val versionsDir = File(gameDir, "versions")
    private val librariesDir = File(gameDir, "libraries")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getLoaderVersionId(version: String, loader: LoaderType, loaderVersion: String): String {
        return when (loader) {
            LoaderType.VANILLA -> version
            LoaderType.FABRIC -> "$version-loader-$loaderVersion"
            LoaderType.FORGE -> "$version-forge-$loaderVersion"
            LoaderType.NEOFORGE -> "$version-neoforge-$loaderVersion"
            LoaderType.QUILT -> "$version-quilt-$loaderVersion"
        }
    }

    suspend fun ensureLoaderInstalled(
        version: String,
        loader: LoaderType,
        loaderVersion: String,
        enabledModIds: List<String> = emptyList()
    ): String {
        if (loader == LoaderType.VANILLA) {
            onProgress("Vanilla — лоадер не нужен", 1.0f)
            return version
        }

        val loaderVersionId = getLoaderVersionId(version, loader, loaderVersion)

        val loaderDir = File(versionsDir, loaderVersionId)
        val loaderJsonFile = File(loaderDir, "$loaderVersionId.json")

        if (loaderJsonFile.exists() && loaderJsonFile.length() > 0) {
            onProgress("Лоадер $loaderVersionId уже установлен", 0.8f)
            installMods(version, loader, enabledModIds)
            return loaderVersionId
        }

        loaderDir.mkdirs()

        when (loader) {
            LoaderType.FABRIC -> installFabric(version, loaderVersion, loaderVersionId, loaderJsonFile)
            LoaderType.FORGE -> installForge(version, loaderVersion, loaderVersionId, loaderJsonFile)
            LoaderType.NEOFORGE -> installNeoForge(version, loaderVersion, loaderVersionId, loaderJsonFile)
            LoaderType.QUILT -> installQuilt(version, loaderVersion, loaderVersionId, loaderJsonFile)
            LoaderType.VANILLA -> {}
        }

        installMods(version, loader, enabledModIds)

        onProgress("Лоадер $loaderVersionId установлен", 1.0f)
        return loaderVersionId
    }

    private suspend fun installFabric(
        mcVersion: String,
        fabricVersion: String,
        loaderVersionId: String,
        destJson: File
    ) {
        onProgress("Загрузка Fabric Loader...", 0.2f)
        println("[LOADER] Installing Fabric $fabricVersion for MC $mcVersion")

        val infoUrl = "https://meta.fabricmc.net/v2/versions/loader/$mcVersion/$fabricVersion"
        val response = client.get(infoUrl)
        val body = response.bodyAsText()
        println("[LOADER] Fabric info: ${body.length} chars")

        val infoJson = json.parseToJsonElement(body).jsonObject
        val launcherMeta = infoJson["launcherMeta"]?.jsonObject
            ?: throw IllegalStateException("Fabric: launcherMeta не найден")

        val mainClassObj = launcherMeta["mainClass"]?.jsonObject
        val mainClass = mainClassObj?.get("client")?.jsonPrimitive?.content
            ?: "net.fabricmc.loader.impl.launch.knot.KnotClient"

        val librariesObj = launcherMeta["libraries"]?.jsonObject
        val commonLibs = librariesObj?.get("common")?.jsonArray ?: JsonArray(emptyList())
        val clientLibs = librariesObj?.get("client")?.jsonArray ?: JsonArray(emptyList())
        val allLibs = buildJsonArray { addAll(commonLibs); addAll(clientLibs) }

        if (allLibs.isEmpty()) {
            destJson.delete()
            throw IllegalStateException("Fabric не предоставил библиотеки для MC $mcVersion")
        }

        val fabricMaven = infoJson["loader"]?.jsonObject?.get("maven")?.jsonPrimitive?.content ?: ""
        val intermediaryMaven = infoJson["intermediary"]?.jsonObject?.get("maven")?.jsonPrimitive?.content ?: ""

        val extraLibs = buildJsonArray {
            if (fabricMaven.isNotEmpty()) {
                addJsonObject {
                    put("name", fabricMaven)
                    put("url", "https://maven.fabricmc.net/")
                }
            }
            if (intermediaryMaven.isNotEmpty()) {
                addJsonObject {
                    put("name", intermediaryMaven)
                    put("url", "https://maven.fabricmc.net/")
                }
            }
        }

        val versionJson = buildJsonObject {
            put("id", loaderVersionId)
            put("inheritsFrom", mcVersion)
            put("mainClass", mainClass)
            put("type", "release")
            put("libraries", buildJsonArray {
                for (lib in allLibs) add(lib)
                for (lib in extraLibs) add(lib)
            })
        }

        destJson.writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), versionJson))
        println("[LOADER] Fabric JSON: mainClass=$mainClass, libs=${allLibs.size + extraLibs.size}")
        println("[LOADER] Fabric JSON written: ${destJson.absolutePath} (${destJson.length()} bytes)")

        val allLibsToDownload = buildJsonArray {
            for (lib in allLibs) add(lib)
            for (lib in extraLibs) add(lib)
        }
        downloadFabricLibraries(allLibsToDownload)
    }

    private suspend fun downloadFabricLibraries(libraries: JsonArray) {
        onProgress("Скачивание библиотек Fabric...", 0.5f)
        var count = 0
        for (lib in libraries) {
            val libObj = lib.jsonObject
            val name = libObj["name"]?.jsonPrimitive?.content ?: continue
            val url = libObj["url"]?.jsonPrimitive?.content ?: continue

            val parts = name.split(":")
            if (parts.size < 3) continue
            val group = parts[0].replace('.', '/')
            val artifact = parts[1]
            val version = parts[2]
            val classifier = parts.getOrNull(3)

            val fileName = if (classifier != null) "$artifact-$version-$classifier.jar" else "$artifact-$version.jar"
            val path = "$group/$artifact/$version/$fileName"

            val destFile = File(librariesDir, path)
            if (destFile.exists() && destFile.length() > 0) continue

            val downloadUrl = "$url$path"
            try {
                println("[LOADER] Downloading: $fileName")
                val resp = client.get(downloadUrl)
                val bytes = resp.readBytes()
                destFile.parentFile?.mkdirs()
                destFile.writeBytes(bytes)
                count++
            } catch (e: Exception) {
                println("[LOADER] Failed to download $fileName: ${e.message}")
            }
        }
        println("[LOADER] Downloaded $count Fabric libraries")
    }

    private suspend fun installForge(
        mcVersion: String,
        forgeVersion: String,
        loaderVersionId: String,
        destJson: File
    ) {
        onProgress("Загрузка Forge...", 0.2f)
        println("[LOADER] Installing Forge $forgeVersion for MC $mcVersion")

        val installProfileUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/$mcVersion-$forgeVersion/forge-$mcVersion-$forgeVersion-installer.json"

        try {
            val response = client.get(installProfileUrl)
            val body = response.bodyAsText()
            val profile = json.parseToJsonElement(body).jsonObject

            val versionJson = profile["versionInfo"]?.jsonObject
            if (versionJson != null) {
                val id = versionJson["id"]?.jsonPrimitive?.content ?: loaderVersionId
                val finalJson = versionJson.toMutableMap()
                finalJson["id"] = JsonPrimitive(loaderVersionId)

                destJson.writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(),
                    JsonObject(finalJson)))
                println("[LOADER] Forge version JSON written")

                val libraries = versionJson["libraries"]?.jsonArray ?: JsonArray(emptyList())
                downloadForgeLibraries(libraries)
            }
        } catch (e: Exception) {
            println("[LOADER] Forge install failed: ${e.message}, falling back to simple profile")
            createSimpleForgeProfile(mcVersion, forgeVersion, loaderVersionId, destJson)
        }
    }

    private fun createSimpleForgeProfile(mcVersion: String, forgeVersion: String, loaderVersionId: String, destJson: File) {
        val forgeLibPath = "net/minecraftforge/forge/$mcVersion-$forgeVersion/forge-$mcVersion-$forgeVersion.jar"
        val forgeLibName = "net.minecraftforge:forge:$mcVersion-$forgeVersion"

        val json = buildJsonObject {
            put("id", loaderVersionId)
            put("inheritsFrom", mcVersion)
            put("mainClass", "net.minecraft.launchwrapper.Launch")
            put("type", "release")

            put("libraries", buildJsonArray {
                addJsonObject {
                    put("name", forgeLibName)
                    put("url", "https://maven.minecraftforge.net/")
                }
                addJsonObject {
                    put("name", "net.minecraft:launchwrapper:2.5")
                    put("url", "https://libraries.minecraft.net/")
                }
                addJsonObject {
                    put("name", "org.ow2.asm:asm-all:5.2")
                    put("url", "https://libraries.minecraft.net/")
                }
            })

            put("arguments", buildJsonObject {
                put("game", buildJsonArray {
                    add("--tweakClass")
                    add("net.minecraftforge.fml.common.launcher.FMLTweaker")
                })
            })
        }

        destJson.writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), json))
    }

    private suspend fun installNeoForge(
        mcVersion: String,
        neoForgeVersion: String,
        loaderVersionId: String,
        destJson: File
    ) {
        onProgress("Загрузка NeoForge...", 0.2f)
        println("[LOADER] Installing NeoForge $neoForgeVersion for MC $mcVersion")

        val versionJsonUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/neoforge-$neoForgeVersion-installer.jar"

        try {
            val metaUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/neoforge-$neoForgeVersion.json"
            val response = client.get(metaUrl)
            val body = response.bodyAsText()
            val neoforgeJson = json.parseToJsonElement(body).jsonObject

            val mergedJson = buildJsonObject {
                put("id", loaderVersionId)
                put("inheritsFrom", mcVersion)
                put("mainClass", neoforgeJson["mainClass"]?.jsonPrimitive?.content ?: "cpw.mods.fml.relauncher.ServerLaunchWrapper")
                put("type", "release")

                val baseLibs = neoforgeJson["libraries"]?.jsonArray ?: JsonArray(emptyList())
                put("libraries", baseLibs)

                val baseArgs = neoforgeJson["arguments"]
                if (baseArgs != null) {
                    put("arguments", baseArgs)
                }
            }

            destJson.writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), mergedJson))
            println("[LOADER] NeoForge version JSON written")

            val libraries = neoforgeJson["libraries"]?.jsonArray ?: JsonArray(emptyList())
            downloadForgeLibraries(libraries)
        } catch (e: Exception) {
            println("[LOADER] NeoForge install failed: ${e.message}")
            throw e
        }
    }

    private suspend fun installQuilt(
        mcVersion: String,
        quiltVersion: String,
        loaderVersionId: String,
        destJson: File
    ) {
        onProgress("Загрузка Quilt...", 0.2f)
        println("[LOADER] Installing Quilt $quiltVersion for MC $mcVersion")

        val loaderUrl = "https://meta.quiltmc.org/v3/versions/loader/$mcVersion/$quiltVersion/json"

        try {
            val response = client.get(loaderUrl)
            val body = response.bodyAsText()
            val quiltJson = json.parseToJsonElement(body).jsonObject

            val libraries = quiltJson["libraries"]?.jsonArray ?: JsonArray(emptyList())
            val mainClass = quiltJson["mainClass"]?.jsonPrimitive?.content
                ?: "org.quiltmc.loader.impl.launch.knot.KnotClient"

            val versionJson = buildJsonObject {
                put("id", loaderVersionId)
                put("inheritsFrom", mcVersion)
                put("mainClass", mainClass)
                put("type", "release")

                val argsJson = quiltJson["arguments"]
                if (argsJson != null) {
                    put("arguments", argsJson)
                }

                put("libraries", libraries)
            }

            destJson.writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), versionJson))
            println("[LOADER] Quilt version JSON written")

            downloadFabricLibraries(libraries)
        } catch (e: Exception) {
            println("[LOADER] Quilt install failed: ${e.message}")
            throw e
        }
    }

    private suspend fun downloadForgeLibraries(libraries: JsonArray) {
        onProgress("Скачивание библиотек лоадера...", 0.5f)
        var count = 0
        for (lib in libraries) {
            val libObj = lib.jsonObject
            val name = libObj["name"]?.jsonPrimitive?.content ?: continue
            val url = libObj["url"]?.jsonPrimitive?.content ?: continue

            val parts = name.split(":")
            if (parts.size < 3) continue
            val group = parts[0].replace('.', '/')
            val artifact = parts[1]
            val version = parts[2]
            val classifier = parts.getOrNull(3)

            val fileName = if (classifier != null) "$artifact-$version-$classifier.jar" else "$artifact-$version.jar"
            val path = "$group/$artifact/$version/$fileName"

            val destFile = File(librariesDir, path)
            if (destFile.exists() && destFile.length() > 0) continue

            val downloadUrl = "$url$path"
            try {
                println("[LOADER] Downloading: $fileName")
                val resp = client.get(downloadUrl)
                val bytes = resp.readBytes()
                destFile.parentFile?.mkdirs()
                destFile.writeBytes(bytes)
                count++
            } catch (e: Exception) {
                println("[LOADER] Failed: $fileName: ${e.message}")
            }
        }
        println("[LOADER] Downloaded $count loader libraries")
    }

    private suspend fun installMods(mcVersion: String, loader: LoaderType, enabledModIds: List<String>) {
        if (enabledModIds.isEmpty()) return

        val modsDir = File(gameDir, "mods")
        modsDir.mkdirs()

        onProgress("Скачивание модов...", 0.9f)

        val modrinthApi = ModrinthApi()
        for (modId in enabledModIds) {
            val modInfo = LoaderRegistry.availableMods.find { it.id == modId } ?: continue
            try {
                modrinthApi.downloadMod(modInfo.slug, mcVersion, loader, modsDir)
                println("[MODS] Installed: ${modInfo.name}")
            } catch (e: Exception) {
                println("[MODS] Failed to install ${modInfo.name}: ${e.message}")
            }
        }
    }
}
