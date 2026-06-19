package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun ServerBrowserScreen(
    primaryColor: Color,
    savedServers: List<String>,
    onAddServer: (String) -> Unit,
    onRemoveServer: (String) -> Unit,
    onJoinServer: (String) -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val scope = rememberCoroutineScope()
    var serverResults by remember { mutableStateOf<Map<String, ServerInfo>>(emptyMap()) }
    var pinging by remember { mutableStateOf(false) }
    var newServerIp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("Серверы", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9D1D9))
        Text("Добавьте серверы и проверьте статус", fontSize = 12.sp, color = Color(0xFF484F58), modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material.TextField(
                value = newServerIp,
                onValueChange = { newServerIp = it },
                placeholder = { Text("IP:Порт (например 1.2.3.4:25565)", color = Color(0xFF484F58), fontSize = 12.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFC9D1D9), fontSize = 12.sp),
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFF0D1117),
                    cursorColor = primaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor)
                    .clickable {
                        if (newServerIp.isNotBlank()) {
                            onAddServer(newServerIp.trim())
                            newServerIp = ""
                        }
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Добавить", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(primaryColor.copy(alpha = 0.15f))
                .clickable {
                    pinging = true
                    scope.launch(Dispatchers.IO) {
                        val results = mutableMapOf<String, ServerInfo>()
                        savedServers.forEach { server ->
                            val parts = server.split(":")
                            val host = parts[0]
                            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 25565 else 25565
                            val info = ServerPing.pingServer(host, port)
                            results[server] = info
                            withContext(Dispatchers.Main) {
                                serverResults = results.toMap()
                            }
                        }
                        pinging = false
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pinging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primaryColor, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Пинг...", fontSize = 12.sp, color = primaryColor)
                }
            } else {
                Text("\u21BB Проверить все серверы", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedServers) { server ->
                val info = serverResults[server]
                val parts = server.split(":")
                val host = parts[0]
                val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 25565 else 25565

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(Color(0xFF161B22))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when {
                                        info?.online == true -> Color(0xFF34D399)
                                        info != null && !info.online -> Color(0xFFF87171)
                                        else -> Color(0xFF30363D)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                info?.motd?.take(50) ?: host,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC9D1D9)
                            )
                            Text(
                                buildString {
                                    append(host)
                                    if (port != 25565) append(":$port")
                                    if (info?.online == true) {
                                        append(" \u2022 ${info.playersOnline}/${info.playersMax}")
                                        if (info.latency > 0) append(" \u2022 ${info.latency}ms")
                                    }
                                    if (info?.version?.isNotEmpty() == true) append(" \u2022 ${info.version}")
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF6E7681)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .clickable { onJoinServer(server) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("\u25B6 Играть", fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF87171).copy(alpha = 0.1f))
                                .clickable { onRemoveServer(server) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("\u2715", fontSize = 10.sp, color = Color(0xFFF87171))
                        }
                    }
                }
            }
        }
    }
}
