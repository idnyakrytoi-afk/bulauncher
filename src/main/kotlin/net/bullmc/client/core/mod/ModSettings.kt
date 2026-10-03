package net.bullmc.client.core.mod

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.bullmc.client.core.util.LauncherPaths
import java.io.File

object ModSettings {
    private var gameDir: File = LauncherPaths.game

    private val configFile: File get() = File(gameDir, "bulltweaks-config.json")
    private val modsDir: File get() = File(gameDir, "mods")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Volatile
    private var current: BullTweaksConfig = BullTweaksConfig()

    fun init(gameDirOverride: File? = null) {
        LauncherPaths.init()
        gameDir = gameDirOverride ?: LauncherPaths.game
        current = BullTweaksConfig()
        if (configFile.exists()) {
            try {
                current = json.decodeFromString<BullTweaksConfig>(configFile.readText())
            } catch (e: Exception) {
                println("[MOD] Ошибка чтения конфига: ${e.message}")
                current = BullTweaksConfig()
            }
        } else {
            save()
        }
        println("[MOD] Конфиг: ${configFile.absolutePath}")
    }

    fun get(): BullTweaksConfig = current

    fun update(block: BullTweaksConfig.() -> Unit) {
        current = current.copy().apply(block)
        save()
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(current))
        } catch (e: Exception) {
            println("[MOD] Ошибка сохранения конфига: ${e.message}")
        }
    }

    fun getModJarFile(): File = File(modsDir, "bulltweaks-1.0.0.jar")
    fun isModInstalled(): Boolean = getModJarFile().exists()
}

@Serializable
data class BullTweaksConfig(
    val visuals: VisualsConfig = VisualsConfig(),
    val hud: HUDConfig = HUDConfig(),
    val utilities: UtilitiesConfig = UtilitiesConfig()
)

@Serializable
data class VisualsConfig(
    val chinaHit: Boolean = true,
    val chinaHitScale: Float = 1.0f,
    val customHand: Boolean = true,
    val customHandScale: Float = 1.0f,
    val customHandColor: String = "#FFFFFF",
    val hitColor: Boolean = true,
    val defaultHitColor: String = "#FF0000",
    val criticalHitColor: String = "#FFFF00",
    val hitboxEnabled: Boolean = false,
    val hitboxColor: String = "#FF00FF",
    val hitboxAlpha: Float = 0.3f,
    val hitboxCornersOnly: Boolean = true,
    val hitboxCornerLength: Float = 0.25f,
    val jumpCircles: Boolean = true,
    val jumpCircleColor: String = "#0000FF",
    val jumpCircleCount: Int = 12,
    val landingEffect: Boolean = true,
    val landingScale: Float = 1.0f,
    val fullBright: Boolean = false,
    val brightnessLevel: Float = 1.0f,
    val particles: Boolean = true,
    val particleCount: Int = 1,
    val renderTweaks: Boolean = true,
    val smoothCamera: Boolean = false,
    val fovMultiplier: Float = 1.0f,
    val aspectRatioLock: Boolean = false,
    val aspectRatio: Float = 1.7778f,
    val customCrosshair: Boolean = false,
    val crosshairColor: String = "#FF00FF",
    val crosshairSize: Int = 2
)

@Serializable
data class HUDConfig(
    val damageIndicator: Boolean = true,
    val healthBar: Boolean = true,
    val scoreDisplay: Boolean = true,
    val miniMap: Boolean = false,
    val compassDisplay: Boolean = true,
    val fps: Boolean = true
)

@Serializable
data class UtilitiesConfig(
    val autoFish: Boolean = false,
    val brightness: Boolean = false,
    val hud: Boolean = true,
    val music: Boolean = true,
    val skillHud: Boolean = false
)
