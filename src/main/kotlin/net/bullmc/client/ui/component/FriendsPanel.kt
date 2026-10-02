package net.bullmc.client.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.bullmc.client.AnimatedEntry
import net.bullmc.client.PulsingDot
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun FriendsPanel(primaryColor: Color, currentTheme: ThemeName = ThemeName.DARK) {
    val colors = LocalBullColors.current

    BullCard(
        modifier = Modifier.width(248.dp).fillMaxHeight().padding(start = 4.dp),
        colors = colors,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Друзья", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            BullSecondaryButton(text = "+", onClick = {}, height = 30.dp, accent = true)
        }

        Row(
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingDot(color = colors.success, size = 7.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("3 в сети", fontSize = 12.sp, color = colors.success)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSunken)
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val friends = listOf(
                    Triple("zakuril", "В игре", true),
                    Triple("player228", "В лобби", true),
                    Triple("DarkLord", "Онлайн", true)
                )
                friends.forEachIndexed { index, (name, status, online) ->
                    AnimatedEntry(index = index, offsetY = 12f, delayPerItem = 70) {
                        FriendItem(name = name, status = status, online = online, colors = colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendItem(name: String, status: String, online: Boolean, colors: BullColors) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(name) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://visage.surgeplay.com/bust/100/$name")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "BullMC-Launcher/1.0")
                connection.connect()

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    if (bytes.isNotEmpty()) {
                        avatar = bytes.inputStream().use { loadImageBitmap(it) }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                println("[FriendsPanel] Failed to load avatar for $name: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHovered) colors.surfaceHover else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                avatar != null -> {
                    Image(
                        bitmap = avatar!!,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.primary.copy(alpha = 0.5f),
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Text(name.first().uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (online) {
                    PulsingDot(color = colors.success, size = 6.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            }
            Text(status, fontSize = 11.sp, color = colors.textSecondary)
        }
    }
}
