package net.bullmc.client.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.theme.LocalBullColors
import java.awt.Desktop
import java.net.URI

private const val RATING_URL = "https://millida.net/rating/servers/bullcraft"

@Composable
fun FriendsPanel(serverStatus: ServerStatus?, onAddServer: () -> Unit) {
    val colors = LocalBullColors.current
    BullCard(modifier = Modifier.width(252.dp).fillMaxHeight().padding(start = 4.dp), contentPadding = PaddingValues(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BullCraft", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            BullSecondaryButton(text = "+", onClick = onAddServer, height = 34.dp, accent = true)
        }
        Spacer(Modifier.height(5.dp))
        Text("Анархия и приключения", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        Text("СЕРВЕР", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Text("hot.bullmc.net", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        val online = serverStatus?.online == true
        Text(
            when {
                serverStatus == null -> "Проверяем доступность..."
                !serverStatus.checked -> "Не удалось проверить"
                online -> "Онлайн · ${serverStatus.playersOnline}/${serverStatus.playersMax} игроков"
                else -> "Сейчас недоступен"
            },
            fontSize = 14.sp,
            color = when {
                serverStatus == null -> colors.textSecondary
                !serverStatus.checked -> colors.textSecondary
                online -> colors.success
                else -> colors.error
            }
        )
        if (online && serverStatus != null) {
            Spacer(Modifier.height(12.dp))
            Text("Minecraft ${serverStatus.version}", fontSize = 14.sp, color = colors.textSecondary)
        }
        Spacer(Modifier.height(28.dp))
        Text("РЕЙТИНГ MILLIDA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Text("BullCraft на Millida", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(7.dp))
        Text("Новости сервера и голоса игроков", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(8.dp))
                .background(colors.primary)
                .clickable { runCatching { Desktop.getDesktop().browse(URI(RATING_URL)) } },
            contentAlignment = Alignment.Center
        ) {
            Text("Открыть страницу ↗", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
        }
        Spacer(Modifier.weight(1f))
        Text("BullMC Client", fontSize = 13.sp, color = colors.textSecondary)
    }
}
