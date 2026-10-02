package net.bullmc.client.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.AnimDurations
import net.bullmc.client.AnimatedEntry
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName

data class SidebarItem(
    val label: String,
    val icon: ImageVector,
    val screen: String
)

/** Порядок экранов — используется для направления анимации перехода. */
val SidebarScreens: List<String> = listOf("HOME", "CLIENT", "BUILDS", "SHOP", "SERVERS", "ACCOUNT", "SETTINGS", "LOGS")

private val items = listOf(
    SidebarItem("Главная", Icons.Default.Home, "HOME"),
    SidebarItem("Клиент", Icons.Default.Build, "CLIENT"),
    SidebarItem("Сборки", Icons.Default.FolderShared, "BUILDS"),
    SidebarItem("Магазин", Icons.Default.ShoppingCart, "SHOP"),
    SidebarItem("Серверы", Icons.Default.List, "SERVERS"),
    SidebarItem("Аккаунт", Icons.Default.Person, "ACCOUNT"),
    SidebarItem("Настройки", Icons.Default.Settings, "SETTINGS"),
    SidebarItem("Логи", Icons.Default.DateRange, "LOGS")
)

@Composable
fun Sidebar(currentScreen: String, onNavigate: (String) -> Unit, primaryColor: Color, currentTheme: ThemeName = ThemeName.DARK) {
    val colors = LocalBullColors.current
    val hasLogo = remember { Thread.currentThread().contextClassLoader?.getResource("bull.png") != null }

    Column(
        modifier = Modifier
            .width(102.dp)
            .fillMaxHeight()
            .background(colors.background)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(colors.primaryGradient)),
            contentAlignment = Alignment.Center
        ) {
            if (hasLogo) {
                Image(
                    painter = androidx.compose.ui.res.painterResource("bull.png"),
                    contentDescription = "BullMC",
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("B", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                AnimatedEntry(index = index, offsetY = 10f) {
                    SidebarIcon(
                        icon = item.icon,
                        label = item.label,
                        isActive = currentScreen == item.screen,
                        colors = colors,
                        onClick = { onNavigate(item.screen) }
                    )
                }
            }
        }

        Text("v1.0", fontSize = 10.sp, color = colors.textMuted, letterSpacing = 1.sp)
    }
}

@Composable
private fun SidebarIcon(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    colors: net.bullmc.client.theme.BullColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetBg = when {
        isActive -> colors.primary.copy(alpha = 0.16f)
        isHovered -> colors.surfaceHover
        else -> Color.Transparent
    }
    val targetIcon = when {
        isActive -> colors.primary
        isHovered -> colors.textSecondary
        else -> colors.textMuted
    }

    val bgColor by animateColorAsState(targetBg, tween(AnimDurations.NORMAL), label = "sidebarBg")
    val iconColor by animateColorAsState(targetIcon, tween(AnimDurations.NORMAL), label = "sidebarIcon")
    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            isHovered || isActive -> 1.1f
            else -> 1f
        },
        animationSpec = tween(AnimDurations.FAST, easing = FastOutSlowInEasing),
        label = "sidebarIconScale"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(AnimDurations.NORMAL, easing = FastOutSlowInEasing),
        label = "sidebarIndicator"
    )

    Box(
        modifier = Modifier
            .width(78.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                label,
                fontSize = 12.sp,
                color = iconColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(22.dp)
                .graphicsLayer { scaleY = indicatorScale; alpha = indicatorScale }
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(Brush.verticalGradient(colors.primaryGradient))
        )
    }
}
