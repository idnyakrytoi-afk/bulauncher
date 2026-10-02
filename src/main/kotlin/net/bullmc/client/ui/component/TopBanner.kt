package net.bullmc.client.ui.component

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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.AnimatedProgressBar
import net.bullmc.client.PulsingDot
import net.bullmc.client.rememberBreathingScale
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.theme.BullColors
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.theme.ThemeName
import net.bullmc.client.ui.state.VersionState

/** Плавное изменение числа игроков онлайн. */
@Composable
private fun rememberAnimatedPlayers(target: Int): Int {
    val value by androidx.compose.animation.core.animateIntAsState(
        targetValue = target,
        animationSpec = tween(600),
        label = "playersCount"
    )
    return value
}

@Composable
fun TopBanner(
    savedNick: String,
    launchState: String,
    statusMessage: String,
    progress: Float,
    versionState: VersionState,
    serverStatuses: Map<String, ServerStatus>,
    primaryColor: Color,
    profiles: List<String>,
    onNickChanged: (String) -> Unit,
    onVersionSelected: (String) -> Unit,
    onLaunch: (String) -> Unit,
    activeProfileName: String = "",
    selectedLoader: LoaderType = LoaderType.VANILLA,
    currentTheme: ThemeName = ThemeName.DARK
) {
    val colors = LocalBullColors.current
    val nick = remember { mutableStateOf(savedNick) }
    var showVersionPicker by remember { mutableStateOf(false) }

    val isBusy = launchState == "DOWNLOADING" || launchState == "LAUNCHING"
    val isRunning = launchState == "RUNNING"
    val buttonEnabled = launchState == "READY" && nick.value.isNotBlank()

    Row(
        modifier = Modifier.fillMaxWidth().height(364.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Hero-карточка ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .shadow(8.dp, RoundedCornerShape(8.dp), spotColor = colors.primary.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
        ) {
            // Фон: мягкий градиент + подсветка акцентом
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(colors.backgroundTop, colors.surface))
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(240f, 40f),
                        radius = 520f
                    )
                )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val hasLogo = remember { javaClass.classLoader.getResource("bull.png") != null }
                if (hasLogo) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource("bull.png"),
                        contentDescription = "BullMC Logo",
                        modifier = Modifier.height(96.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        "BULL MC",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        letterSpacing = 6.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "BULLCRAFT · MINECRAFT",
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )

                val mainServer = serverStatuses["hot.bullmc.net"]
                Spacer(modifier = Modifier.height(14.dp))
                if (mainServer != null && mainServer.online) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceSunken)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot(color = colors.success, size = 9.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        val animatedPlayers = rememberAnimatedPlayers(mainServer.playersOnline)
                        Text(
                            "$animatedPlayers/${mainServer.playersMax} игроков онлайн",
                            fontSize = 15.sp,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceSunken)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(colors.error))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                mainServer == null -> "Проверяем сервер..."
                                !mainServer.checked -> "Не удалось проверить сервер"
                                else -> "Сервер недоступен"
                            },
                            fontSize = 15.sp, color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                PlayButton(
                    launchState = launchState,
                    enabled = buttonEnabled,
                    colors = colors,
                    onClick = { if (buttonEnabled) onLaunch(nick.value) }
                )

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(statusMessage, fontSize = 14.sp, color = colors.textSecondary)
                        if (isBusy) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AnimatedProgressBar(
                                progress = progress,
                                color = colors.primary,
                                backgroundColor = colors.surfaceSunken,
                                modifier = Modifier.width(280.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Панель запуска ───────────────────────────────────────────────
        BullCard(
            modifier = Modifier.width(320.dp).fillMaxHeight(),
            contentPadding = PaddingValues(20.dp)
        ) {
            Text("Запуск игры", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("Настройте профиль и играйте", fontSize = 14.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel("Ник в игре", colors)
            Spacer(modifier = Modifier.height(6.dp))
            BullTextField(
                value = nick.value,
                onValueChange = { nick.value = it; onNickChanged(it) },
                placeholder = "Введите ник",
                enabled = launchState == "READY",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            FieldLabel("Версия Minecraft", colors)
            Spacer(modifier = Modifier.height(6.dp))
            VersionSelector(
                versionState = versionState,
                colors = colors,
                enabled = launchState == "READY",
                onClick = { showVersionPicker = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            FieldLabel("Мод-лоадер", colors)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceSunken)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(selectedLoader.displayName, fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (activeProfileName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSunken)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Профиль", fontSize = 14.sp, color = colors.textSecondary)
                    Text(activeProfileName, fontSize = 14.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (isRunning) colors.success else colors.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (launchState) {
                        "READY" -> "Готов к запуску"
                        "DOWNLOADING" -> "Загрузка файлов..."
                        "LAUNCHING" -> "Запуск игры..."
                        "RUNNING" -> "Игра запущена"
                        else -> "Готов к запуску"
                    },
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }
    }

    if (showVersionPicker) {
        VersionPickerDialog(
            versionState = versionState,
            onDismiss = { showVersionPicker = false },
            onSelect = { onVersionSelected(it) },
            colors = colors
        )
    }
}

@Composable
private fun FieldLabel(text: String, colors: BullColors) {
    Text(text, fontSize = 14.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
}

@Composable
private fun VersionSelector(
    versionState: VersionState,
    colors: BullColors,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val type = versionState.typeOf(versionState.selectedVersion)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSunken)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                versionState.selectedVersion,
                fontSize = 16.sp,
                color = if (hovered && enabled) colors.primary else colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            BullBadge(
                text = when (type) {
                    "snapshot" -> "Snapshot"
                    "old_beta" -> "Beta"
                    "old_alpha" -> "Alpha"
                    else -> "Release"
                },
                color = when (type) {
                    "snapshot" -> colors.warning
                    "release" -> colors.success
                    else -> colors.textMuted
                }
            )
        }
        Text("▾", fontSize = 20.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayButton(
    launchState: String,
    enabled: Boolean,
    colors: BullColors,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val breathing by rememberBreathingScale(enabled = enabled)
    val hoverScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else if (hovered) 1.03f else 1f,
        animationSpec = tween(150),
        label = "playHoverScale"
    )
    val finalScale = breathing * hoverScale

    val text = when (launchState) {
        "READY" -> "▶  ИГРАТЬ"
        "DOWNLOADING" -> "ЗАГРУЗКА..."
        "LAUNCHING" -> "ЗАПУСК..."
        "RUNNING" -> "✔  ЗАПУЩЕНА"
        else -> "▶  ИГРАТЬ"
    }
    val busy = launchState == "DOWNLOADING" || launchState == "LAUNCHING"
    val running = launchState == "RUNNING"

    val gradient = when {
        running -> listOf(colors.success, colors.success.copy(alpha = 0.75f))
        !enabled -> listOf(colors.borderStrong, colors.border)
        hovered -> listOf(colors.primary, colors.primaryVariant)
        else -> listOf(colors.primary.copy(alpha = 0.92f), colors.primaryVariant.copy(alpha = 0.92f))
    }
    val contentColor = if (running || enabled) Color.White else colors.textMuted

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(56.dp)
            .graphicsLayer { scaleX = finalScale; scaleY = finalScale }
            .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = colors.primary.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(gradient), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = contentColor, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor, letterSpacing = 2.sp)
            }
        } else {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black, color = contentColor, letterSpacing = 2.sp)
        }
    }
}
