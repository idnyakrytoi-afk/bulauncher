package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bullmc.client.core.util.ServerInfo
import net.bullmc.client.core.util.ServerPing
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.ui.component.BullBadge
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullPrimaryButton
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullTextField

@Composable
fun ServerBrowserScreen(
    primaryColor: Color,
    savedServers: List<String>,
    onAddServer: (String) -> Unit,
    onRemoveServer: (String) -> Unit,
    onJoinServer: (String) -> Unit
) {
    val colors = LocalBullColors.current
    val scope = rememberCoroutineScope()
    var serverResults by remember { mutableStateOf<Map<String, ServerInfo>>(emptyMap()) }
    var pinging by remember { mutableStateOf(false) }
    var newServerIp by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Серверы", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text("Добавьте серверы и проверьте статус", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BullTextField(
                value = newServerIp,
                onValueChange = { newServerIp = it },
                placeholder = "IP:Порт (например 1.2.3.4:25565)",
                modifier = Modifier.weight(1f),
                colors = colors
            )
            BullPrimaryButton(
                text = "+ Добавить",
                onClick = {
                    if (newServerIp.isNotBlank()) {
                        onAddServer(newServerIp.trim())
                        newServerIp = ""
                    }
                },
                colors = colors,
                height = 48.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BullSecondaryButton(
            text = if (pinging) "Пинг..." else "⟳ Проверить все серверы",
            onClick = {
                pinging = true
                scope.launch(Dispatchers.IO) {
                    val results = mutableMapOf<String, ServerInfo>()
                    savedServers.forEach { server ->
                        val parts = server.split(":")
                        val host = parts[0]
                        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 25565 else 25565
                        val info = ServerPing.pingServer(host, port)
                        results[server] = info
                        withContext(Dispatchers.Main) { serverResults = results.toMap() }
                    }
                    pinging = false
                }
            },
            colors = colors,
            accent = true,
            enabled = !pinging
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(savedServers) { server ->
                val info = serverResults[server]
                val parts = server.split(":")
                val host = parts[0]
                val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 25565 else 25565

                ServerRow(
                    host = host,
                    port = port,
                    info = info,
                    colors = colors,
                    onJoin = { onJoinServer(server) },
                    onRemove = { onRemoveServer(server) }
                )
            }
        }
    }
}

@Composable
private fun ServerRow(
    host: String,
    port: Int,
    info: ServerInfo?,
    colors: BullColors,
    onJoin: () -> Unit,
    onRemove: () -> Unit
) {
    BullCard(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        hoverable = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                info?.online == true -> colors.success
                                info != null && !info.online -> colors.error
                                else -> colors.borderStrong
                            }
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        info?.motd?.take(50) ?: host,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        buildString {
                            append(host)
                            if (port != 25565) append(":$port")
                            if (info?.online == true) {
                                append(" • ${info.playersOnline}/${info.playersMax}")
                                if (info.latency > 0) append(" • ${info.latency}ms")
                            }
                            if (info?.version?.isNotEmpty() == true) append(" • ${info.version}")
                        },
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (info?.online == true) {
                    BullBadge(text = "Онлайн", color = colors.success)
                    Spacer(modifier = Modifier.width(2.dp))
                }
                BullSecondaryButton(text = "▶ Играть", onClick = onJoin, colors = colors, accent = true)
                BullSecondaryButton(text = "✕", onClick = onRemove, colors = colors)
            }
        }
    }
}
