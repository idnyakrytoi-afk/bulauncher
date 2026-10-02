package net.bullmc.client.core.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

@Serializable
data class GameProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "Профиль",
    var mcVersion: String = "1.20.4",
    var loaderType: LoaderType = LoaderType.VANILLA,
    var loaderVersion: String = "",
    var enabledMods: List<String> = emptyList(),
    var serverIp: String = "hot.bullmc.net",
    var ramMb: Int = 4096,
    var gameDir: String = "",
    var createdAt: Long = System.currentTimeMillis()
) {
    fun getVersionId(): String {
        return when (loaderType) {
            LoaderType.VANILLA -> mcVersion
            else -> "${mcVersion}-${loaderType.name.lowercase()}-${loaderVersion.ifEmpty { "auto" }}"
        }
    }

    fun getGameDir(): File {
        val dir = if (gameDir.isEmpty()) File(LauncherPaths.game, "profiles/$id") else File(gameDir)
        dir.mkdirs()
        return dir
    }

    fun getModsDir(): File = File(getGameDir(), "mods").also { it.mkdirs() }
    fun getVersionsDir(): File = File(getGameDir(), "versions").also { it.mkdirs() }
    fun getLibrariesDir(): File = File(getGameDir(), "libraries").also { it.mkdirs() }
    fun getAssetsDir(): File = File(getGameDir(), "assets").also { it.mkdirs() }
    fun getLogsDir(): File = File(getGameDir(), "logs").also { it.mkdirs() }
}

object ProfileManager {
    private val profilesFile: File get() = File(LauncherPaths.root, "profiles.json")
    private val activeProfileFile: File get() = File(LauncherPaths.root, "active-profile.txt")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Volatile
    private var profiles: MutableList<GameProfile> = mutableListOf()

    @Volatile
    var activeProfileId: String = ""
        private set

    fun init() {
        LauncherPaths.init()
        if (profilesFile.exists()) {
            try {
                val saved = json.decodeFromString<List<GameProfile>>(profilesFile.readText())
                profiles = saved.toMutableList()
                if (profiles.isNotEmpty()) {
                    val lastActiveId = runCatching { activeProfileFile.takeIf { it.exists() }?.readText()?.trim() }.getOrNull()
                    activeProfileId = profiles.firstOrNull { it.id == lastActiveId }?.id ?: profiles.first().id
                }
            } catch (e: Exception) {
                println("[PROFILE] Error loading profiles: ${e.message}")
                profiles = mutableListOf()
                createDefault()
            }
        } else {
            createDefault()
        }
        println("[PROFILE] Loaded ${profiles.size} profiles, active=$activeProfileId")
    }

    private fun createDefault() {
        val default = GameProfile(
            name = "Fabric 1.20.4",
            mcVersion = "1.20.4",
            loaderType = LoaderType.FABRIC,
            loaderVersion = "",
            enabledMods = listOf("sodium", "iris", "entityculling"),
            ramMb = 4096
        )
        profiles.add(default)
        activeProfileId = default.id
        save()
    }

    fun getProfiles(): List<GameProfile> = profiles.toList()

    fun getActiveProfile(): GameProfile {
        return profiles.find { it.id == activeProfileId } ?: profiles.first().also {
            activeProfileId = it.id
        }
    }

    fun setActiveProfile(id: String) {
        require(profiles.any { it.id == id }) { "Профиль $id не найден" }
        activeProfileId = id
        runCatching { activeProfileFile.writeText(id) }
    }

    fun createProfile(profile: GameProfile): GameProfile {
        profiles.add(profile)
        save()
        return profile
    }

    fun updateProfile(id: String, block: GameProfile.() -> Unit) {
        val idx = profiles.indexOfFirst { it.id == id }
        if (idx >= 0) {
            profiles[idx] = profiles[idx].copy().apply(block)
            save()
        }
    }

    fun deleteProfile(id: String) {
        if (profiles.size <= 1) return
        profiles.removeAll { it.id == id }
        if (activeProfileId == id) {
            activeProfileId = profiles.first().id
            runCatching { activeProfileFile.writeText(activeProfileId) }
        }
        save()
    }

    fun duplicateProfile(id: String): GameProfile? {
        val source = profiles.find { it.id == id } ?: return null
        val copy = source.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${source.name} (копия)",
            createdAt = System.currentTimeMillis()
        )
        profiles.add(copy)
        save()
        return copy
    }

    fun save() {
        try {
            profilesFile.writeText(json.encodeToString(profiles))
        } catch (e: Exception) {
            println("[PROFILE] Error saving: ${e.message}")
        }
    }
}
