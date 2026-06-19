package net.bullmc.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.core.BullTweaksConfig
import net.bullmc.client.core.ModDownloader
import net.bullmc.client.core.ModSettings
import net.bullmc.client.core.VisualsConfig
import net.bullmc.client.core.HUDConfig
import net.bullmc.client.core.UtilitiesConfig

class ModConfigState(initial: BullTweaksConfig) {
    private val _config = mutableStateOf(initial)
    val config: BullTweaksConfig get() = _config.value

    fun updateVisuals(block: VisualsConfig.() -> VisualsConfig) {
        val newVisuals = _config.value.visuals.block()
        _config.value = _config.value.copy(visuals = newVisuals)
        ModSettings.update { copy(visuals = newVisuals) }
    }

    fun updateHUD(block: HUDConfig.() -> HUDConfig) {
        val newHud = _config.value.hud.block()
        _config.value = _config.value.copy(hud = newHud)
        ModSettings.update { copy(hud = newHud) }
    }

    fun updateUtilities(block: UtilitiesConfig.() -> UtilitiesConfig) {
        val newUtils = _config.value.utilities.block()
        _config.value = _config.value.copy(utilities = newUtils)
        ModSettings.update { copy(utilities = newUtils) }
    }
}

@Composable
fun ModSettingsScreen() {
    val cardShape = RoundedCornerShape(12.dp)
    val state = remember { ModConfigState(ModSettings.get()) }
    var modInstalled by remember { mutableStateOf(ModDownloader.isModInstalled()) }

    LaunchedEffect(Unit) {
        ModSettings.init()
        state.updateVisuals { ModSettings.get().visuals }
        modInstalled = ModDownloader.isModInstalled()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bull Tweaks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (modInstalled) "Установлен" else "Не установлен",
                    fontSize = 12.sp,
                    color = if (modInstalled) Color(0xFF43A047) else Color(0xFFE53935)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable {
                        if (modInstalled) {
                            ModDownloader.uninstallMod()
                            modInstalled = false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (modInstalled) "Удалить" else "Установить",
                    fontSize = 12.sp,
                    color = if (modInstalled) Color(0xFFE53935) else Color(0xFFFF7A00)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsSection("Визуал") {
            ToggleSetting("China Hit", state.config.visuals.chinaHit) {
                state.updateVisuals { copy(chinaHit = it) }
            }
            ToggleSetting("Custom Hand", state.config.visuals.customHand) {
                state.updateVisuals { copy(customHand = it) }
            }
            ToggleSetting("Hit Color", state.config.visuals.hitColor) {
                state.updateVisuals { copy(hitColor = it) }
            }
            ToggleSetting("Jump Circles", state.config.visuals.jumpCircles) {
                state.updateVisuals { copy(jumpCircles = it) }
            }
            ToggleSetting("Full Bright", state.config.visuals.fullBright) {
                state.updateVisuals { copy(fullBright = it) }
            }
            ToggleSetting("Particles", state.config.visuals.particles) {
                state.updateVisuals { copy(particles = it) }
            }
            ToggleSetting("Render Tweaks", state.config.visuals.renderTweaks) {
                state.updateVisuals { copy(renderTweaks = it) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection("Хитбокс") {
            ToggleSetting("Показывать хитбокс", state.config.visuals.hitboxEnabled) {
                state.updateVisuals { copy(hitboxEnabled = it) }
            }
            ToggleSetting("Только уголки", state.config.visuals.hitboxCornersOnly) {
                state.updateVisuals { copy(hitboxCornersOnly = it) }
            }
            SliderSetting("Прозрачность", state.config.visuals.hitboxAlpha, 0f..1f) {
                state.updateVisuals { copy(hitboxAlpha = it) }
            }
            SliderSetting("Длина уголков", state.config.visuals.hitboxCornerLength, 0.1f..1f) {
                state.updateVisuals { copy(hitboxCornerLength = it) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection("HUD") {
            ToggleSetting("Damage Indicator", state.config.hud.damageIndicator) {
                state.updateHUD { copy(damageIndicator = it) }
            }
            ToggleSetting("Health Bar", state.config.hud.healthBar) {
                state.updateHUD { copy(healthBar = it) }
            }
            ToggleSetting("Score Display", state.config.hud.scoreDisplay) {
                state.updateHUD { copy(scoreDisplay = it) }
            }
            ToggleSetting("FPS", state.config.hud.fps) {
                state.updateHUD { copy(fps = it) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection("Утилиты") {
            ToggleSetting("Auto Fish", state.config.utilities.autoFish) {
                state.updateUtilities { copy(autoFish = it) }
            }
            ToggleSetting("Skill HUD", state.config.utilities.skillHud) {
                state.updateUtilities { copy(skillHud = it) }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(16.dp)
    ) {
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF7A00))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleSetting(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color.White)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF7A00),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun SliderSetting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = Color.White)
            Text(String.format("%.2f", value), fontSize = 12.sp, color = Color(0xFF888888))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF7A00),
                activeTrackColor = Color(0xFFFF7A00),
                inactiveTrackColor = Color(0xFF333333)
            )
        )
    }
}
