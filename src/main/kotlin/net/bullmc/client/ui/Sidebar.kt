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
    SidebarItem("Home", "H", "HOME"),
    SidebarItem("Friends", "F", "FRIENDS"),
    SidebarItem("Mods", "M", "MODS"),
    SidebarItem("Settings", "S", "SETTINGS"),
    SidebarItem("Logs", "L", "LOGS")
)

@Composable
fun Sidebar(currentScreen: String, onNavigate: (String) -> Unit, primaryColor: Color) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F0F))
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                SidebarIcon(
                    icon = item.icon,
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
private fun SidebarIcon(icon: String, isActive: Boolean, primaryColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        isActive -> primaryColor
        isHovered -> Color(0xFF2A2A2A)
        else -> Color(0xFF1E1E1E)
    }
    val textColor = when {
        isActive -> Color.White
        isHovered -> Color(0xFFCCCCCC)
        else -> Color(0xFF666666)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
