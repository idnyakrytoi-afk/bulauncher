package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.core.loader.LoaderChannel
import net.bullmc.client.core.loader.LoaderRegistry
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.loader.LoaderVersionEntry
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeManager
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullSectionTitle
import net.bullmc.client.ui.component.BullToggle
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
    val colors = LocalBullColors.current

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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text("Лоадер и моды", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text("Настройте мод-лоадер и установите моды", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(20.dp))

        // Выбор лоадера
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            BullSectionTitle("Выбор лоадера", colors = colors)
            Spacer(modifier = Modifier.height(12.dp))

            val loaders = LoaderType.entries
            for (row in loaders.chunked(3)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (loader in row) {
                        LoaderTile(
                            loader = loader,
                            isSelected = loader == selectedLoader,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        ) {
                            onLoaderChanged(loader)
                            customVersion = ""
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedLoader == LoaderType.VANILLA) {
                Text("Vanilla — чистый Minecraft без модов", fontSize = 13.sp, color = colors.textMuted)
            } else if (selectedLoader == LoaderType.FABRIC || selectedLoader == LoaderType.QUILT) {
                Text("Fabric API будет скачан автоматически", fontSize = 13.sp, color = colors.success)
            }
        }

        // Версия лоадера
        if (selectedLoader != LoaderType.VANILLA) {
            Spacer(modifier = Modifier.height(12.dp))
            BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
                BullSectionTitle("Версия ${selectedLoader.displayName}", if (selectedMcVersion.isNotEmpty()) "Для MC $selectedMcVersion" else null, colors = colors)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (channel in LoaderChannel.entries) {
                        val isSelected = channel == loaderChannel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) colors.primary else colors.surfaceSunken)
                                .clickable { loaderChannel = channel },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                channel.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.onPrimary else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (loaderChannel == LoaderChannel.CUSTOM) {
                    if (versionsLoading) {
                        LoadingRow("Загрузка версий...", colors)
                    } else if (fetchedVersions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.surfaceSunken)
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
                                    color = if (customVersion.isEmpty()) colors.textMuted else colors.textPrimary
                                )
                                Text("▾", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = customExpanded,
                            onDismissRequest = { customExpanded = false },
                            modifier = Modifier.background(colors.surface).width(280.dp).heightIn(max = 300.dp)
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
                                            color = if (entry.version == customVersion) colors.primary else colors.textPrimary,
                                            fontWeight = if (entry.stable) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (entry.stable) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("stable", fontSize = 11.sp, color = colors.success)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Нет доступных версий", fontSize = 13.sp, color = colors.textMuted)
                    }
                } else {
                    val displayVersion = when (loaderChannel) {
                        LoaderChannel.STABLE -> fetchedVersions.firstOrNull { it.stable }?.version
                        LoaderChannel.LATEST -> fetchedVersions.firstOrNull()?.version
                        else -> null
                    }

                    if (versionsLoading) {
                        LoadingRow("Определение версии...", colors)
                    } else if (displayVersion != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(displayVersion, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                                Text(if (loaderChannel == LoaderChannel.STABLE) "Stable" else "Latest", fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }
                    } else {
                        Text("Не удалось определить версию. Используйте Custom.", fontSize = 13.sp, color = colors.textSecondary)
                    }
                }

                if (selectedLoaderVersion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Активная: $selectedLoaderVersion", fontSize = 12.sp, color = colors.textMuted)
                }
            }
        }

        // Моды
        if (selectedLoader != LoaderType.VANILLA) {
            Spacer(modifier = Modifier.height(12.dp))
            val availableMods = LoaderRegistry.getModsForLoader(selectedLoader)

            if (availableMods.isNotEmpty()) {
                BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
                    BullSectionTitle("Моды", "Совместимо с MC $selectedMcVersion", colors = colors)
                    Spacer(modifier = Modifier.height(12.dp))

                    for ((category, mods) in availableMods.groupBy { it.category }) {
                        Text(category.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        for (mod in mods) {
                            val isEnabled = mod.id in enabledMods
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isEnabled) colors.primary.copy(alpha = 0.1f) else colors.surfaceSunken)
                                    .clickable {
                                        onModsChanged(if (isEnabled) enabledMods - mod.id else enabledMods + mod.id)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        mod.name,
                                        fontSize = 14.sp,
                                        color = if (isEnabled) colors.textPrimary else colors.textSecondary,
                                        fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(mod.description, fontSize = 12.sp, color = colors.textMuted)
                                }
                                BullToggle(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        onModsChanged(if (checked) enabledMods + mod.id else enabledMods - mod.id)
                                    },
                                    colors = colors
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val modsDirPath = modsDir ?: net.bullmc.client.core.util.LauncherPaths.mods
        BullCard(modifier = Modifier.fillMaxWidth(), colors = colors) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Папка модов", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(modsDirPath.absolutePath, fontSize = 12.sp, color = colors.textMuted)
                }
                BullSecondaryButton(
                    text = "↗ Открыть",
                    onClick = { try { java.awt.Desktop.getDesktop().open(modsDirPath) } catch (_: Exception) {} },
                    colors = colors,
                    accent = true
                )
            }
        }
    }
}

@Composable
private fun LoaderTile(loader: LoaderType, isSelected: Boolean, colors: BullColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceSunken)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                loader.icon,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.primary else colors.textMuted
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                loader.displayName,
                fontSize = 13.sp,
                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun LoadingRow(text: String, colors: BullColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = colors.textSecondary)
    }
}
