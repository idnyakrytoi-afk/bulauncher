package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
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
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.theme.ThemeManager
import net.bullmc.client.core.profile.GameProfile
import java.awt.Desktop
import java.io.File

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
    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
        Spacer(modifier = Modifier.height(20.dp))

        // Profiles
        if (profiles != null) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Профили", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .clickable { onProfileCreate("Новый профиль") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("+ Новый", fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    for (profile in profiles) {
                        val isActive = profile.id == activeProfileId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) primaryColor.copy(alpha = 0.1f) else Color(0xFF0D1117))
                                .clickable { onProfileSelected(profile.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    fontSize = 14.sp,
                                    color = if (isActive) Color(0xFFE6EDF3) else Color(0xFF8B949E),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "${profile.mcVersion} \u2022 ${profile.loaderType.displayName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6E7681)
                                )
                            }
                            if (isActive) {
                                Text("\u2713", fontSize = 16.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // RAM
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Оперативная память", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(12.dp))

                val ramGB = ramMb / 1024f
                val sliderValue = remember { mutableFloatStateOf(ramGB) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2 GB", fontSize = 12.sp, color = Color(0xFF6E7681))
                    Text(
                        "${String.format("%.1f", sliderValue.floatValue)} GB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text("16 GB", fontSize = 12.sp, color = Color(0xFF6E7681))
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
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color(0xFF21262D)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(2, 4, 6, 8, 12, 16).forEach { gb ->
                        val selected = ramMb / 1024 == gb
                        Text(
                            "${gb}G",
                            fontSize = 12.sp,
                            color = if (selected) primaryColor else Color(0xFF6E7681),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                sliderValue.floatValue = gb.toFloat()
                                onRamChanged(gb * 1024)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Java
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Java", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(8.dp))

                val javaVer = detectJavaVersion(javaPath)
                Text("Путь: $javaPath", fontSize = 13.sp, color = Color(0xFF8B949E))
                if (javaVer.isNotEmpty()) {
                    Text("Версия: $javaVer", fontSize = 13.sp, color = Color(0xFF34D399))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    if (javaVer.contains("17") || javaVer.contains("21"))
                        "Подходит для Minecraft 1.17+"
                    else if (javaVer.contains("1.8"))
                        "Подходит для Minecraft 1.12 и ниже"
                    else "Версия Java не определена",
                    fontSize = 12.sp,
                    color = Color(0xFF6E7681)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.15f))
                        .clickable {
                            val newPath = net.bullmc.client.FileUtils.chooseJavaExecutable()
                            if (newPath != null) {
                                onJavaPathChanged(newPath)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Выбрать другую Java...", fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Папка игры
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Папка игры", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(6.dp))
                Text(gameDir, fontSize = 13.sp, color = Color(0xFF8B949E))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Тема оформления
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Тема оформления", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeName.values().forEach { theme ->
                        val isSelected = theme == currentTheme
                        val themePrimaryColor = ThemeManager.getPrimaryColor(theme)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryColor.copy(alpha = 0.1f) else Color(0xFF0D1117))
                                .clickable { onThemeChanged(theme) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themePrimaryColor)
                                )

                                Text(
                                    ThemeManager.getThemeName(theme),
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFFE6EDF3) else Color(0xFF8B949E),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (isSelected) {
                                Text("\u2713", fontSize = 16.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (theme != ThemeName.values().last()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Моды
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Моды и шейдеры", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE6EDF3))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(modsDir, fontSize = 12.sp, color = Color(0xFF6E7681))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1117))
                        .clickable { onOpenModsFolder() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("\u2197 Открыть", fontSize = 13.sp, color = primaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Join Server
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Quick Join", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Сервер для быстрого присоединения", fontSize = 13.sp, color = Color(0xFF6E7681))

                Spacer(modifier = Modifier.height(10.dp))

                var serverInput by remember { mutableStateOf(defaultServer) }

                androidx.compose.material.TextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    placeholder = { Text("play.bullmc.net", color = Color(0xFF8B949E), fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE6EDF3), fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF1C2128),
                        cursorColor = primaryColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.25f))
                        .clickable {
                            onDefaultServerChanged(serverInput)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сохранить", fontSize = 14.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Автозапуск
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автозапуск", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE6EDF3))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Запускать лаунчер при старте Windows", fontSize = 12.sp, color = Color(0xFF6E7681))
                }

                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (autoStart) primaryColor else Color(0xFF30363D))
                        .clickable { onAutoStartChanged(!autoStart) },
                    contentAlignment = if (autoStart) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White)
                            .padding(3.dp)
                    )
                }
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
