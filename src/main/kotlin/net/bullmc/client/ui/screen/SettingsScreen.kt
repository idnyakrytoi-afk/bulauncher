package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.core.profile.GameProfile
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeManager
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullSectionTitle
import net.bullmc.client.ui.component.BullTextField
import net.bullmc.client.ui.component.BullToggle

@Composable
fun SettingsScreen(
    ramMb: Int,
    onRamChanged: (Int) -> Unit,
    javaPath: String,
    onJavaPathChanged: (String) -> Unit,
    gameDir: String,
    modsDir: String,
    currentTheme: ThemeName,
    onThemeChanged: (ThemeName) -> Unit,
    primaryColor: Color,
    defaultServer: String,
    onDefaultServerChanged: (String) -> Unit,
    onOpenModsFolder: () -> Unit,
    profiles: List<GameProfile>? = null,
    activeProfileId: String = "",
    onProfileSelected: (String) -> Unit = {},
    onProfileCreate: (String) -> Unit = {},
    onProfileDelete: (String) -> Unit = {},
    onProfileRename: (String, String) -> Unit = { _, _ -> },
    autoStart: Boolean = false,
    onAutoStartChanged: (Boolean) -> Unit = {}
) {
    val colors = LocalBullColors.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text("Профили, память, Java и оформление", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(20.dp))

        if (profiles != null) {
            BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BullSectionTitle("Профили", "Отдельные настройки для каждой сборки", colors = colors)
                    BullSecondaryButton(text = "+ Новый", onClick = { onProfileCreate("Новый профиль") }, colors = colors, accent = true)
                }
                Spacer(modifier = Modifier.height(12.dp))

                for (profile in profiles) {
                    val isActive = profile.id == activeProfileId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) colors.primary.copy(alpha = 0.12f) else colors.surfaceSunken)
                            .clickable { onProfileSelected(profile.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.name,
                                fontSize = 14.sp,
                                color = if (isActive) colors.textPrimary else colors.textSecondary,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                            Text("${profile.mcVersion} • ${profile.loaderType.displayName}", fontSize = 12.sp, color = colors.textMuted)
                        }
                        if (isActive) {
                            Text("✓", fontSize = 16.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // RAM
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Оперативная память", colors = colors)
            Spacer(modifier = Modifier.height(12.dp))

            val ramGB = ramMb / 1024f
            val sliderValue = remember { mutableFloatStateOf(ramGB) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("2 GB", fontSize = 12.sp, color = colors.textMuted)
                Text(
                    "${String.format("%.1f", sliderValue.floatValue)} GB",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text("16 GB", fontSize = 12.sp, color = colors.textMuted)
            }

            Slider(
                value = sliderValue.floatValue,
                onValueChange = { sliderValue.floatValue = it },
                onValueChangeFinished = {
                    val mb = (sliderValue.floatValue * 1024).toInt().coerceIn(1024, 16384)
                    onRamChanged(mb)
                },
                valueRange = 2f..16f,
                steps = 13,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.border
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 4, 6, 8, 12, 16).forEach { gb ->
                    val selected = ramMb / 1024 == gb
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.primary.copy(alpha = 0.15f) else colors.surfaceSunken)
                            .clickable {
                                sliderValue.floatValue = gb.toFloat()
                                onRamChanged(gb * 1024)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${gb}G",
                            fontSize = 12.sp,
                            color = if (selected) colors.primary else colors.textMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Java
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Java", colors = colors)
            Spacer(modifier = Modifier.height(8.dp))

            val javaVer = detectJavaVersion(javaPath)
            Text("Путь: $javaPath", fontSize = 13.sp, color = colors.textSecondary)
            if (javaVer.isNotEmpty()) {
                Text("Версия: $javaVer", fontSize = 13.sp, color = colors.success)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when {
                    javaVer.contains("17") || javaVer.contains("21") -> "Подходит для Minecraft 1.17+"
                    javaVer.contains("1.8") -> "Подходит для Minecraft 1.12 и ниже"
                    else -> "Версия Java не определена"
                },
                fontSize = 12.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            BullSecondaryButton(
                text = "Выбрать другую Java...",
                onClick = {
                    val newPath = net.bullmc.client.FileUtils.chooseJavaExecutable()
                    if (newPath != null) onJavaPathChanged(newPath)
                },
                colors = colors,
                accent = true,
                height = 38.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Папка игры
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Папка игры", colors = colors)
            Spacer(modifier = Modifier.height(6.dp))
            Text(gameDir, fontSize = 13.sp, color = colors.textSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Тема
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Тема оформления", colors = colors)
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                ThemeName.values().forEach { theme ->
                    val isSelected = theme == currentTheme
                    val themePrimaryColor = ThemeManager.getPrimaryColor(theme)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceSunken)
                            .clickable { onThemeChanged(theme) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(themePrimaryColor)
                            )
                            Text(
                                ThemeManager.getThemeName(theme),
                                fontSize = 14.sp,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) Text("✓", fontSize = 16.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                    if (theme != ThemeName.values().last()) Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Моды
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Моды и шейдеры", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(modsDir, fontSize = 12.sp, color = colors.textMuted)
                }
                BullSecondaryButton(text = "↗ Открыть", onClick = onOpenModsFolder, colors = colors, accent = true)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Join
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Quick Join", "Сервер для быстрого присоединения", colors = colors)
            Spacer(modifier = Modifier.height(10.dp))

            var serverInput by remember { mutableStateOf(defaultServer) }
            BullTextField(
                value = serverInput,
                onValueChange = { serverInput = it },
                placeholder = "hot.bullmc.net",
                modifier = Modifier.fillMaxWidth(),
                colors = colors
            )
            Spacer(modifier = Modifier.height(10.dp))
            BullSecondaryButton(
                text = "Сохранить",
                onClick = { onDefaultServerChanged(serverInput) },
                colors = colors,
                accent = true,
                height = 40.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Автозапуск
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автозапуск", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Запускать лаунчер при старте Windows", fontSize = 12.sp, color = colors.textMuted)
                }
                BullToggle(checked = autoStart, onCheckedChange = onAutoStartChanged, colors = colors)
            }
        }
    }
}

private fun detectJavaVersion(javaPath: String): String {
    return try {
        val process = ProcessBuilder(javaPath, "-version")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val regex = Regex("\"([^\"]+)\"")
        regex.find(output)?.groupValues?.get(1) ?: ""
    } catch (_: Exception) {
        ""
    }
}
