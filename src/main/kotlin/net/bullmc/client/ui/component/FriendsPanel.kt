package net.bullmc.client.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.theme.LocalBullColors
import java.awt.Desktop
import java.net.URI

private val voteLinks = listOf(
    "Millida" to "https://millida.net/rating/servers/bullcraft",
    "Top Minecrafter" to "https://top-minecrafter.com/server/bullcraft/",
    "HotMC" to "https://hotmc.ru/minecraft-server-283171",
    "KLauncher" to "https://klauncher.gg/monitoring/server/12929",
    "McTop" to "https://mctop.su/servers/4415/servers/",
    "MC Monitor" to "https://mc-monitor.org/server/7747",
    "MisterLauncher" to "https://misterlauncher.org/server/bullmc/",
    "MinecraftRating" to "https://minecraftrating.ru/server/bullpe/"
)

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
        Spacer(Modifier.height(24.dp))
        Divider(color = colors.border)
        Spacer(Modifier.height(18.dp))
        Text("Проголосуйте за наш сервер", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Выберите мониторинг", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            voteLinks.forEach { (name, url) ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable { runCatching { Desktop.getDesktop().browse(URI(url)) } },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text("↗", fontSize = 17.sp, color = colors.primary)
                }
                Divider(color = colors.border)
            }
        }
    }
}
