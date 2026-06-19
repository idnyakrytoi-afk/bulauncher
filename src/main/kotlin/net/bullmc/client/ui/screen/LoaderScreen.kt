package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.bullmc.client.core.loader.LoaderChannel
import net.bullmc.client.core.loader.LoaderRegistry
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.loader.LoaderVersionEntry
import java.io.File

@Composable
fun LoaderScreen(
    selectedLoader: LoaderType,
    onLoaderChanged: (LoaderType) -> Unit,
    selectedLoaderVersion: String,
    onLoaderVersionChanged: (String) -> Unit,
    enabledMods: List<String>,
    onModsChanged: (List<String>) -> Unit,
    primaryColor: Color,
    selectedMcVersion: String,
    modsDir: File? = null
) {
    val cardShape = RoundedCornerShape(12.dp)

    var loaderChannel by remember { mutableStateOf(LoaderChannel.STABLE) }
    var customVersion by remember { mutableStateOf("") }
    var fetchedVersions by remember { mutableStateOf<List<LoaderVersionEntry>>(emptyList()) }
    var versionsLoading by remember { mutableStateOf(false) }
    var customExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedLoader, selectedMcVersion) {
        if (selectedLoader == LoaderType.VANILLA) return@LaunchedEffect
        versionsLoading = true
        fetchedVersions = LoaderRegistry.fetchAvailableVersions(selectedLoader, selectedMcVersion)
        versionsLoading = false
    }

    LaunchedEffect(loaderChannel, fetchedVersions, customVersion) {
        val resolved = LoaderRegistry.resolveVersion(loaderChannel, selectedLoader, selectedMcVersion, customVersion, fetchedVersions)
        if (resolved.isNotEmpty() && resolved != selectedLoaderVersion) {
            onLoaderVersionChanged(resolved)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Лоадер и моды", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
        Text("Настройте мод-лоадер и установите моды", fontSize = 14.sp, color = Color(0xFF6E7681), modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(20.dp))

        // Loader selection
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
        ) {
            Column {
                Text("Выбор лоадера", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(12.dp))

                val loaders = LoaderType.entries
                val chunkedLoaders = loaders.chunked(3)

                for (row in chunkedLoaders) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (loader in row) {
                            val isSelected = loader == selectedLoader
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF0D1117)
                                    )
                                    .clickable {
                                        onLoaderChanged(loader)
                                        customVersion = ""
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        loader.icon,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) primaryColor else Color(0xFF6E7681)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        loader.displayName,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color(0xFFE6EDF3) else Color(0xFF8B949E),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedLoader == LoaderType.VANILLA) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vanilla — чистый Minecraft без модов", fontSize = 13.sp, color = Color(0xFF6E7681))
                } else if (selectedLoader == LoaderType.FABRIC || selectedLoader == LoaderType.QUILT) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Fabric API будет скачан автоматически", fontSize = 13.sp, color = Color(0xFF34D399))
                }
            }
        }

        // Loader version selection
        if (selectedLoader != LoaderType.VANILLA) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
            ) {
                Column {
                    Text("Версия ${selectedLoader.displayName}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                    if (selectedMcVersion.isNotEmpty()) {
                        Text("Для MC $selectedMcVersion", fontSize = 13.sp, color = Color(0xFF6E7681))
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (channel in LoaderChannel.entries) {
                            val isSelected = channel == loaderChannel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) primaryColor else Color(0xFF0D1117))
                                    .clickable { loaderChannel = channel },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    channel.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF8B949E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (loaderChannel == LoaderChannel.CUSTOM) {
                        if (versionsLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryColor, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Загрузка версий...", fontSize = 14.sp, color = Color(0xFF8B949E))
                            }
                        } else if (fetchedVersions.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D1117))
                                    .clickable { customExpanded = true }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        customVersion.ifEmpty { "Выберите версию" },
                                        fontSize = 14.sp,
                                        color = if (customVersion.isEmpty()) Color(0xFF8B949E) else Color(0xFFE6EDF3)
                                    )
                                    Text(if (customExpanded) "\u25B2" else "\u25BC", fontSize = 11.sp, color = Color(0xFF8B949E))
                                }
                            }

                            DropdownMenu(
                                expanded = customExpanded,
                                onDismissRequest = { customExpanded = false },
                                modifier = Modifier.background(Color(0xFF161B22)).width(280.dp).heightIn(max = 300.dp)
                            ) {
                                fetchedVersions.forEach { entry ->
                                    DropdownMenuItem(onClick = {
                                        customVersion = entry.version
                                        customExpanded = false
                                    }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                entry.version,
                                                fontSize = 14.sp,
                                                color = if (entry.version == customVersion) primaryColor else Color(0xFFE6EDF3),
                                                fontWeight = if (entry.stable) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (entry.stable) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("stable", fontSize = 11.sp, color = Color(0xFF34D399))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Нет доступных версий", fontSize = 13.sp, color = Color(0xFF6E7681))
                        }
                    } else {
                        val displayVersion = when (loaderChannel) {
                            LoaderChannel.STABLE -> fetchedVersions.firstOrNull { it.stable }?.version
                            LoaderChannel.LATEST -> fetchedVersions.firstOrNull()?.version
                            else -> null
                        }

                        if (versionsLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryColor, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Определение версии...", fontSize = 14.sp, color = Color(0xFF8B949E))
                            }
                        } else if (displayVersion != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(displayVersion, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                    Text(
                                        if (loaderChannel == LoaderChannel.STABLE) "Stable" else "Latest",
                                        fontSize = 13.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }
                        } else {
                            Text("Не удалось определить версию. Используйте Custom.", fontSize = 13.sp, color = Color(0xFF8B949E))
                        }
                    }

                    if (selectedLoaderVersion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Активная: $selectedLoaderVersion",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7681)
                        )
                    }
                }
            }
        }

        // Mods section
        if (selectedLoader != LoaderType.VANILLA) {
            Spacer(modifier = Modifier.height(12.dp))

            val availableMods = LoaderRegistry.getModsForLoader(selectedLoader)

            if (availableMods.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(20.dp)
                ) {
                    Column {
                        Text("Моды", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                        Text("Совместимо с MC $selectedMcVersion", fontSize = 13.sp, color = Color(0xFF6E7681))
                        Spacer(modifier = Modifier.height(12.dp))

                        val categories = availableMods.groupBy { it.category }

                        for ((category, mods) in categories) {
                            Text(category.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryColor)
                            Spacer(modifier = Modifier.height(6.dp))

                            for (mod in mods) {
                                val isEnabled = mod.id in enabledMods

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isEnabled) primaryColor.copy(alpha = 0.1f) else Color(0xFF0D1117))
                                        .clickable {
                                            val newMods = if (isEnabled) enabledMods - mod.id else enabledMods + mod.id
                                            onModsChanged(newMods)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            mod.name,
                                            fontSize = 14.sp,
                                            color = if (isEnabled) Color(0xFFE6EDF3) else Color(0xFF8B949E),
                                            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(mod.description, fontSize = 12.sp, color = Color(0xFF6E7681))
                                    }

                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            val newMods = if (checked) enabledMods + mod.id else enabledMods - mod.id
                                            onModsChanged(newMods)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = primaryColor,
                                            uncheckedColor = Color(0xFF30363D)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val modsDirPath = modsDir ?: net.bullmc.client.core.util.LauncherPaths.mods
        Box(
            modifier = Modifier.fillMaxWidth().clip(cardShape).background(Color(0xFF161B22)).padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Папка модов", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE6EDF3))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(modsDirPath.absolutePath, fontSize = 13.sp, color = Color(0xFF6E7681))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1117))
                        .clickable {
                            try { java.awt.Desktop.getDesktop().open(modsDirPath) } catch (_: Exception) {}
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("\u2197 Открыть", fontSize = 13.sp, color = primaryColor)
                }
            }
        }
    }
}
