package net.bullmc.client.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.bullmc.client.core.launcher.VersionEntry
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.ui.state.VersionFilter
import net.bullmc.client.ui.state.VersionState

/**
 * Модальный выбор версии Minecraft — полный список из манифеста Mojang
 * (release / snapshot / beta / alpha) с поиском и фильтрами.
 */
@Composable
fun VersionPickerDialog(
    versionState: VersionState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    colors: BullColors = LocalBullColors.current
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(VersionFilter.RELEASE) }
    val listState = rememberLazyListState()

    val versions = remember(versionState.allVersions, filter, query) {
        versionState.filtered(filter, query)
    }

    // Сброс прокрутки при смене фильтра/поиска
    LaunchedEffect(filter, query) { listState.scrollToItem(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
      Box(modifier = Modifier.fillMaxSize().background(Color(0xB0000000)), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Выбор версии Minecraft", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(
                            "Доступны все версии: релизы, снапшоты и старые сборки",
                            fontSize = 14.sp, color = colors.textSecondary
                        )
                    }
                    BullSecondaryButton(text = "✕", onClick = onDismiss, height = 34.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                BullTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Поиск версии, например 1.20 или 1.8.9...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    VersionFilter.entries.forEach { f ->
                        BullChip(
                            text = f.displayName,
                            selected = filter == f,
                            onClick = { filter = f }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (versions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            if (versionState.allVersions.isEmpty()) "Загрузка списка версий..." else "Ничего не найдено",
                            fontSize = 15.sp, color = colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(versions, key = { it.id }) { entry ->
                            VersionRow(
                                entry = entry,
                                selected = entry.id == versionState.selectedVersion,
                                colors = colors,
                                onClick = {
                                    onSelect(entry.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun VersionRow(
    entry: VersionEntry,
    selected: Boolean,
    colors: BullColors,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val typeColor = when (entry.type) {
        "release" -> colors.success
        "snapshot" -> colors.warning
        else -> colors.textMuted
    }
    val typeLabel = when (entry.type) {
        "release" -> "Release"
        "snapshot" -> "Snapshot"
        "old_beta" -> "Beta"
        "old_alpha" -> "Alpha"
        else -> entry.type
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.surfaceSunken)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                entry.id,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.primary else colors.textPrimary
            )
            val date = entry.releaseTime.take(10)
            if (date.isNotEmpty()) {
                Text(date, fontSize = 13.sp, color = colors.textSecondary)
            }
        }
        BullBadge(text = typeLabel, color = typeColor)
    }
}
