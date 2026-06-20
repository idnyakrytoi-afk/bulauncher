package net.bullmc.client.ui.component

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
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NewsCard(modifier = Modifier.weight(1.2f), news = news, primaryColor = primaryColor)

        val mainStatus = serverStatuses["play.bullmc.net"]
        val ytStatus = serverStatuses["yt.bullmc.net"]

        ServerCard(modifier = Modifier.weight(1f), label = "Survival", ip = "play.bullmc.net", status = mainStatus)
        ServerCard(modifier = Modifier.weight(1f), label = "Creative", ip = "yt.bullmc.net", status = ytStatus)
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
            .shadow(4.dp, cardShape)
            .background(if (isHovered) Color(0xFF1C2028) else Color(0xFF161B22))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(16.dp)
    ) {
        Column {
            Text("\u2605", fontSize = 16.sp, color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Новости", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
            Spacer(modifier = Modifier.height(8.dp))

            if (news.isEmpty()) {
                Text("Загрузка...", fontSize = 14.sp, color = Color(0xFF8B949E))
            } else {
                news.take(3).forEach { item ->
                    Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE6EDF3))
                    Text(item.content, fontSize = 13.sp, color = Color(0xFF8B949E), maxLines = 2)
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerCard(modifier: Modifier = Modifier, label: String, ip: String, status: ServerStatus?) {
    val cardShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isOnline = status?.online == true

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(cardShape)
            .shadow(4.dp, cardShape)
            .background(if (isHovered) Color(0xFF1C2028) else Color(0xFF161B22))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isOnline) Color(0xFF34D399) else Color(0xFFF87171))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (status != null && isOnline) {
                StatRow("Статус", "Онлайн")
                Spacer(modifier = Modifier.height(4.dp))
                StatRow("Игроки", "${status.playersOnline}/${status.playersMax}")
                Spacer(modifier = Modifier.height(4.dp))
                StatRow("Версия", status.version)
                if (status.motd.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(status.motd, fontSize = 12.sp, color = Color(0xFF8B949E), maxLines = 2)
                }
            } else {
                Text("Офлайн", fontSize = 14.sp, color = Color(0xFFF87171))
                Spacer(modifier = Modifier.height(4.dp))
                Text(ip, fontSize = 13.sp, color = Color(0xFF6E7681))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFF8B949E))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
    }
}
