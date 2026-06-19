package net.bullmc.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.api.NewsItem
import net.bullmc.client.api.ServerStatus

@Composable
fun BottomCards(
    news: List<NewsItem>,
    serverStatuses: Map<String, ServerStatus>,
    primaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NewsCard(modifier = Modifier.weight(1f), news = news, primaryColor = primaryColor)

        val mainStatus = serverStatuses["play.bullmc.net"]
        val ytStatus = serverStatuses["yt.bullmc.net"]

        ServerCard(
            modifier = Modifier.weight(1f),
            label = "Survival",
            status = mainStatus
        )

        ServerCard(
            modifier = Modifier.weight(1f),
            label = "Creative",
            status = ytStatus
        )
    }
}

@Composable
private fun NewsCard(modifier: Modifier = Modifier, news: List<NewsItem>, primaryColor: Color) {
    val cardShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(cardShape)
            .shadow(6.dp, cardShape)
            .background(if (isHovered) Color(0xFF232323) else Color(0xFF1E1E1E))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(16.dp)
    ) {
        Column {
            Box(modifier = Modifier.size(width = 32.dp, height = 3.dp).background(primaryColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Новости", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (news.isEmpty()) {
                Text("Загрузка...", fontSize = 12.sp, color = Color(0xFF666666))
            } else {
                news.take(3).forEach { item ->
                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Text(item.content, fontSize = 11.sp, color = Color(0xFF888888), maxLines = 2)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerCard(modifier: Modifier = Modifier, label: String, status: ServerStatus?) {
    val cardShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isOnline = status?.online == true

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(cardShape)
            .shadow(6.dp, cardShape)
            .background(if (isHovered) Color(0xFF232323) else Color(0xFF1E1E1E))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOnline) Color(0xFF43A047) else Color(0xFFE53935))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (status != null && isOnline) {
                StatRow("Статус", "Онлайн")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Игроки", "${status.playersOnline}/${status.playersMax}")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Версия", status.version)
                if (status.motd.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(status.motd, fontSize = 10.sp, color = Color(0xFF666666), maxLines = 2)
                }
            } else {
                Text("Офлайн", fontSize = 13.sp, color = Color(0xFFE53935))
                Spacer(modifier = Modifier.height(4.dp))
                Text(status?.ip ?: "", fontSize = 11.sp, color = Color(0xFF555555))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
