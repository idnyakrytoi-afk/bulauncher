package net.bullmc.client.ui

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
import net.bullmc.client.ThemeName
import net.bullmc.client.ThemeManager
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
    onOpenModsFolder: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))

        // RAM
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Column {
                Text("Оперативная память", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                val ramGB = ramMb / 1024f
                val sliderValue = remember { mutableFloatStateOf(ramGB) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2 GB", fontSize = 11.sp, color = Color(0xFF666666))
                    Text(
                        "${String.format("%.1f", sliderValue.floatValue)} GB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text("16 GB", fontSize = 11.sp, color = Color(0xFF666666))
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
                        inactiveTrackColor = Color(0xFF333333)
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
                            fontSize = 10.sp,
                            color = if (selected) primaryColor else Color(0xFF555555),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Java
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Column {
                Text("Java", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                val javaVer = detectJavaVersion(javaPath)
                Text("Путь: $javaPath", fontSize = 12.sp, color = Color(0xFF888888))
                if (javaVer.isNotEmpty()) {
                    Text("Версия: $javaVer", fontSize = 12.sp, color = Color(0xFF43A047))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    if (javaVer.contains("17") || javaVer.contains("21"))
                        "Подходит для Minecraft 1.17+"
                    else if (javaVer.contains("1.8"))
                        "Подходит для Minecraft 1.12 и ниже"
                    else "Версия Java не определена",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.2f))
                        .clickable {
                            val newPath = net.bullmc.client.FileUtils.chooseJavaExecutable()
                            if (newPath != null) {
                                onJavaPathChanged(newPath)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Выбрать другую Java...", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Папка игры
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Column {
                Text("Папка игры", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(gameDir, fontSize = 12.sp, color = Color(0xFF888888))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Тема оформления
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Column {
                Text("Тема оформления", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeName.values().forEach { theme ->
                        val isSelected = theme == currentTheme
                        val themePrimaryColor = ThemeManager.getPrimaryColor(theme)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF333333) else Color(0xFF262626))
                                .clickable { onThemeChanged(theme) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(themePrimaryColor)
                                )

                                Text(
                                    ThemeManager.getThemeName(theme),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (isSelected) {
                                Text("✓", fontSize = 14.sp, color = themePrimaryColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (theme != ThemeName.values().last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Моды
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Моды и шейдеры", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(modsDir, fontSize = 11.sp, color = Color(0xFF666666))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable { onOpenModsFolder() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Открыть", fontSize = 12.sp, color = primaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Join Server
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF1E1E1E)).padding(20.dp)
        ) {
            Column {
                Text("Quick Join", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Сервер для быстрого присоединения", fontSize = 11.sp, color = Color(0xFF666666))

                Spacer(modifier = Modifier.height(12.dp))

                var serverInput by remember { mutableStateOf(defaultServer) }

                androidx.compose.material.TextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    placeholder = { Text("play.bullmc.net", color = Color(0xFF666666), fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF2A2A2A),
                        cursorColor = primaryColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.2f))
                        .clickable {
                            onDefaultServerChanged(serverInput)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сохранить", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
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
