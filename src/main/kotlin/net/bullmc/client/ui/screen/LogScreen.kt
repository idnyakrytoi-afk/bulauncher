package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.ui.component.BullSecondaryButton

@Composable
fun LogScreen(logLines: List<String>, isGameRunning: Boolean, primaryColor: Color, currentTheme: ThemeName = ThemeName.DARK) {
    val colors = LocalBullColors.current
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(logLines.size, autoScroll) {
        if (autoScroll && logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Логи", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    "${logLines.size} строк" + if (isGameRunning) " • Running" else "",
                    fontSize = 12.sp,
                    color = if (isGameRunning) colors.success else colors.textMuted
                )
            }

            BullSecondaryButton(
                text = if (autoScroll) "Auto-scroll: вкл" else "Auto-scroll: выкл",
                onClick = { autoScroll = !autoScroll },
                colors = colors,
                accent = autoScroll
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceSunken)
                .padding(12.dp)
        ) {
            if (logLines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Логи пусты. Запустите игру.", fontSize = 12.sp, color = colors.textMuted)
                }
            } else {
                LazyColumn(state = listState) {
                    items(logLines) { line ->
                        val color = when {
                            line.contains("ERROR", ignoreCase = true) ||
                            line.contains("Exception", ignoreCase = true) ||
                            line.contains("FATAL", ignoreCase = true) -> colors.error
                            line.contains("WARN", ignoreCase = true) -> colors.warning
                            line.contains("INFO", ignoreCase = true) -> colors.success
                            else -> colors.textSecondary
                        }
                        Text(
                            line,
                            fontSize = 10.sp,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
