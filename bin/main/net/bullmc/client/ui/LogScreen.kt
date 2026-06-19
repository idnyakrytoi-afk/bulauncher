package net.bullmc.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun LogScreen(logLines: List<String>, isGameRunning: Boolean, primaryColor: Color) {
    val cardShape = RoundedCornerShape(12.dp)
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(logLines.size, autoScroll) {
        if (autoScroll && logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Game Logs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${logLines.size} строк" + if (isGameRunning) " \u2022 Running" else "",
                    fontSize = 12.sp,
                    color = if (isGameRunning) Color(0xFF43A047) else Color(0xFF666666)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (autoScroll) primaryColor else Color(0xFF2A2A2A))
                    .clickable { autoScroll = !autoScroll }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Auto-scroll", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(cardShape).background(Color(0xFF0A0A0A)).padding(8.dp)
        ) {
            if (logLines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Логи пусты. Запустите игру.", fontSize = 13.sp, color = Color(0xFF555555))
                }
            } else {
                LazyColumn(state = listState) {
                    items(logLines) { line ->
                        val color = when {
                            line.contains("ERROR", ignoreCase = true) ||
                            line.contains("Exception", ignoreCase = true) ||
                            line.contains("FATAL", ignoreCase = true) -> Color(0xFFE53935)
                            line.contains("WARN", ignoreCase = true) -> Color(0xFFFFA726)
                            line.contains("INFO", ignoreCase = true) -> Color(0xFF81C784)
                            else -> Color(0xFFAAAAAA)
                        }
                        Text(
                            line,
                            fontSize = 11.sp,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
