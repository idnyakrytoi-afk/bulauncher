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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SidebarItem(
    val label: String,
    val icon: String,
    val screen: String
)

private val items = listOf(
    SidebarItem("Home", "\u2302", "HOME"),
    SidebarItem("Mods", "\u2261", "MODS"),
    SidebarItem("Servers", "\u25CE", "SERVERS"),
    SidebarItem("Account", "\u263A", "ACCOUNT"),
    SidebarItem("Settings", "\u2699", "SETTINGS"),
    SidebarItem("Logs", "\u25A3", "LOGS")
)

@Composable
fun Sidebar(currentScreen: String, onNavigate: (String) -> Unit, primaryColor: Color) {
    Box(
        modifier = Modifier
            .width(68.dp)
            .fillMaxHeight()
            .background(Color(0xFF0B0D11))
            .padding(vertical = 16.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Text("B", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { item ->
                SidebarIcon(
                    icon = item.icon,
                    label = item.label,
                    isActive = currentScreen == item.screen,
                    primaryColor = primaryColor,
                    onClick = { onNavigate(item.screen) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SidebarIcon(icon: String, label: String, isActive: Boolean, primaryColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        isActive -> primaryColor.copy(alpha = 0.2f)
        isHovered -> Color(0xFF1A1D24)
        else -> Color.Transparent
    }
    val iconColor = when {
        isActive -> primaryColor
        isHovered -> Color(0xFF9CA3AF)
        else -> Color(0xFF4B5563)
    }
    val indicatorColor = if (isActive) primaryColor else Color.Transparent

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 18.sp, color = iconColor)
            Spacer(modifier = Modifier.height(3.dp))
            Text(label, fontSize = 8.sp, color = iconColor, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
        }
    }
}
