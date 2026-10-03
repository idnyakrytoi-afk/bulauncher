package net.bullmc.client.core.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

@Serializable
data class GameInstance(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "Основная",
    var gameDir: String = "",
    var isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getGameDir(): File {
        if (gameDir.isEmpty()) {
            gameDir = File(LauncherPaths.game, "instances/$id").absolutePath
        }
        return File(gameDir)
    }

    fun getModsDir(): File = File(getGameDir(), "mods").also { it.mkdirs() }
    fun getVersionsDir(): File = File(getGameDir(), "versions").also { it.mkdirs() }
    fun getLibrariesDir(): File = File(getGameDir(), "libraries").also { it.mkdirs() }
    fun getAssetsDir(): File = File(getGameDir(), "assets").also { it.mkdirs() }
    fun getLogsDir(): File = File(getGameDir(), "logs").also { it.mkdirs() }
    fun getLatestLog(): File = File(getLogsDir(), "latest.log")

    fun getDiskUsage(): Long {
        return getGameDir().walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun deleteInstance() {
        getGameDir().deleteRecursively()
    }
}

object InstanceManager {
    private val instancesFile: File get() = File(LauncherPaths.root, "instances.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Volatile
    private var instances: MutableList<GameInstance> = mutableListOf()

    @Volatile
    var activeInstanceId: String = ""
        private set

    fun init() {
        LauncherPaths.init()
        if (instancesFile.exists()) {
            try {
                instances = json.decodeFromString<List<GameInstance>>(instancesFile.readText()).toMutableList()
                if (instances.isNotEmpty()) {
                    activeInstanceId = (instances.firstOrNull { it.isActive } ?: instances.first()).id
                    instances.forEach { it.isActive = it.id == activeInstanceId }
                }
            } catch (e: Exception) {
                println("[INSTANCE] Error loading: ${e.message}")
                instances = mutableListOf()
                createDefault()
            }
        } else {
            createDefault()
        }

        if (instances.isEmpty()) createDefault()

        instances.forEach {
            it.getGameDir().mkdirs()
            it.getModsDir().mkdirs()
        }
        println("[INSTANCE] Loaded ${instances.size} instances")
    }

    private fun createDefault() {
        val default = GameInstance(name = "Основная", isActive = true)
        instances.add(default)
        activeInstanceId = default.id
        save()
    }

    fun getInstances(): List<GameInstance> = instances.toList()

    fun getActiveInstance(): GameInstance {
        return instances.find { it.id == activeInstanceId } ?: instances.first().also {
            activeInstanceId = it.id
        }
    }

    fun setActiveInstance(id: String) {
        if (instances.none { it.id == id }) return
        instances.forEach { it.isActive = (it.id == id) }
        activeInstanceId = id
        save()
    }

    fun createInstance(name: String): GameInstance {
        val instance = GameInstance(name = name, isActive = false)
        instances.add(instance)
        instance.getGameDir().mkdirs()
        instance.getModsDir().mkdirs()
        instance.getVersionsDir().mkdirs()
        save()
        return instance
    }

    fun deleteInstance(id: String) {
        if (instances.size <= 1) return
        val instance = instances.find { it.id == id } ?: return
        instance.deleteInstance()
        instances.removeAll { it.id == id }
        if (activeInstanceId == id) {
            activeInstanceId = instances.first().id
            instances.first().isActive = true
        }
        save()
    }

    fun renameInstance(id: String, newName: String) {
        instances.find { it.id == id }?.name = newName
        save()
    }

    fun duplicateInstance(id: String, newName: String): GameInstance? {
        val source = instances.find { it.id == id } ?: return null
        val copy = createInstance(newName)

        try {
            val sourceDir = source.getGameDir()
            val copyDir = copy.getGameDir()

            val modsDir = File(sourceDir, "mods")
            if (modsDir.exists()) {
                val targetMods = copy.getModsDir()
                check(modsDir.copyRecursively(targetMods, overwrite = true)) { "Failed to copy mods" }
            }

            val versionsDir = File(sourceDir, "versions")
            if (versionsDir.exists()) {
                val targetVersions = copy.getVersionsDir()
                check(versionsDir.copyRecursively(targetVersions, overwrite = true)) { "Failed to copy versions" }
            }
        } catch (e: Exception) {
            copy.getGameDir().deleteRecursively()
            instances.remove(copy)
            save()
            throw IllegalStateException("Instance copy failed", e)
        }

        return copy
    }

    fun save() {
        try {
            instancesFile.writeText(json.encodeToString(instances))
        } catch (e: Exception) {
            println("[INSTANCE] Error saving: ${e.message}")
        }
    }
}
