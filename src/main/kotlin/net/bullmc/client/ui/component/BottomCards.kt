package net.bullmc.client.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.AnimatedEntry
import net.bullmc.client.PulsingDot
import net.bullmc.client.api.NewsItem
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName

@Composable
fun BottomCards(
    news: List<NewsItem>,
    serverStatuses: Map<String, ServerStatus>,
    primaryColor: Color,
    currentTheme: ThemeName = ThemeName.DARK
) {
    val colors = LocalBullColors.current

    Row(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedEntry(index = 0, modifier = Modifier.weight(1f)) {
            NewsCard(modifier = Modifier.fillMaxSize(), news = news, colors = colors)
        }

        AnimatedEntry(index = 1, modifier = Modifier.weight(1f)) {
            ServerCard(
                modifier = Modifier.fillMaxSize(),
                label = "BullCraft",
                ip = "hot.bullmc.net",
                status = serverStatuses["hot.bullmc.net"],
                colors = colors
            )
        }
    }
}

@Composable
private fun NewsCard(modifier: Modifier = Modifier, news: List<NewsItem>, colors: BullColors) {
    BullCard(modifier = modifier, colors = colors, hoverable = true, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("★", fontSize = 15.sp, color = colors.primary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Новости", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (news.isEmpty()) {
            Text("Новостей пока нет", fontSize = 14.sp, color = colors.textSecondary)
        } else {
            news.take(3).forEach { item ->
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
                Text(item.content, fontSize = 14.sp, color = colors.textSecondary, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ServerCard(
    modifier: Modifier = Modifier,
    label: String,
    ip: String,
    status: ServerStatus?,
    colors: BullColors
) {
    val isOnline = status?.online == true

    BullCard(modifier = modifier, colors = colors, hoverable = true, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOnline) {
                PulsingDot(color = colors.success, size = 9.dp)
            } else {
                Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(colors.error))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (status != null && isOnline) {
            StatRow("Статус", "Онлайн", colors, valueColor = colors.success)
            Spacer(modifier = Modifier.height(5.dp))
            StatRow("Игроки", "${status.playersOnline}/${status.playersMax}", colors)
            Spacer(modifier = Modifier.height(5.dp))
            StatRow("Версия", status.version, colors)
            if (status.motd.isNotEmpty()) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(status.motd, fontSize = 14.sp, color = colors.textSecondary, maxLines = 2)
            }
        } else {
            Text(
                when {
                    status == null -> "Проверяем..."
                    !status.checked -> "Не удалось проверить"
                    else -> "Недоступен"
                },
                fontSize = 15.sp,
                color = if (status == null || !status.checked) colors.textSecondary else colors.error,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(ip, fontSize = 14.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, colors: BullColors, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = colors.textSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor ?: colors.textPrimary)
    }
}
