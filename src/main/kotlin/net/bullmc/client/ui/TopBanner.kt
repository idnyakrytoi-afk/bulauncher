package net.bullmc.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bullmc.client.api.ServerStatus
import net.bullmc.client.ThemeManager
import net.bullmc.client.ThemeName

@Composable
fun TopBanner(
    savedNick: String,
    launchState: String,
    statusMessage: String,
    progress: Float,
    selectedVersion: String,
    versions: List<String>,
    serverStatuses: Map<String, ServerStatus>,
    primaryColor: Color,
    profiles: List<String>,
    onNickChanged: (String) -> Unit,
    onVersionSelected: (String) -> Unit,
    onLaunch: (String) -> Unit
) {
    val nick = remember { mutableStateOf(savedNick) }
    val shape = RoundedCornerShape(16.dp)
    var versionMenuExpanded by remember { mutableStateOf(false) }

    val buttonText = when (launchState) {
        "READY" -> "PLAY"
        "DOWNLOADING" -> "ЗАГРУЗКА..."
        "LAUNCHING" -> "ЗАПУСК..."
        "RUNNING" -> "ЗАПУЩЕНА"
        else -> "PLAY"
    }
    val buttonEnabled = launchState == "READY" && nick.value.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Color(0xFF1A1A1A))
    ) {
        val hasBanner = remember {
            javaClass.classLoader.getResource("images/banner.png") != null
        }

        if (hasBanner) {
            Image(
                painter = androidx.compose.ui.res.painterResource("images/banner.png"),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    brush = Brush.verticalGradient(colors = listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A)))
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.verticalGradient(colors = listOf(Color(0x00000000), Color(0xDD000000)))
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BULL MC", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)
            Spacer(modifier = Modifier.height(4.dp))

            val mainServer = serverStatuses["play.bullmc.net"]
            if (mainServer != null && mainServer.online) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF43A047))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${mainServer.playersOnline}/${mainServer.playersMax} игроков онлайн",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PLAY button with animation
            val btnInteraction = remember { MutableInteractionSource() }
            val btnHovered by btnInteraction.collectIsHoveredAsState()
            val animatedScale by animateFloatAsState(if (btnHovered) 1.05f else 1f)

            Button(
                onClick = { if (buttonEnabled) onLaunch(nick.value) },
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp)
                    .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
                    .shadow(12.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = ButtonDefaults.elevation(0.dp),
                enabled = buttonEnabled,
                interactionSource = btnInteraction
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.horizontalGradient(
                            colors = when {
                                !buttonEnabled && launchState != "READY" -> listOf(Color(0xFF555555), Color(0xFF444444))
                                btnHovered -> listOf(primaryColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.7f))
                                else -> listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (launchState == "DOWNLOADING" || launchState == "LAUNCHING") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                        }
                    } else {
                        Text(
                            text = buttonText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (launchState == "RUNNING") Color(0xFFAAFFAA) else Color.White,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(statusMessage, fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    if (launchState == "DOWNLOADING") {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.width(280.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = primaryColor,
                            backgroundColor = Color(0xFF333333)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box {
                Row(
                    modifier = Modifier.width(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = nick.value,
                        onValueChange = { nick.value = it; onNickChanged(nick.value) },
                        placeholder = { Text("Введите ник", color = Color(0xFF666666), fontSize = 14.sp) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = launchState == "READY",
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color(0xFF2A2A2A),
                            cursorColor = primaryColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    var profilesExpanded by remember { mutableStateOf(false) }

                    if (profiles.isNotEmpty()) {
                        Box(modifier = Modifier.height(52.dp)) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { profilesExpanded = !profilesExpanded },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 18.sp)
                            }

                            DropdownMenu(
                                expanded = profilesExpanded,
                                onDismissRequest = { profilesExpanded = false },
                                modifier = Modifier.background(Color(0xFF2A2A2A))
                            ) {
                                profiles.forEach { profile ->
                                    DropdownMenuItem(
                                        onClick = {
                                            nick.value = profile
                                            onNickChanged(profile)
                                            profilesExpanded = false
                                        }
                                    ) {
                                        Text(profile, color = if (nick.value == profile) primaryColor else Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box {
                Box(
                    modifier = Modifier
                        .width(280.dp).height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable { if (launchState == "READY") versionMenuExpanded = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Minecraft $selectedVersion", fontSize = 13.sp, color = Color.White)
                        Text(if (versionMenuExpanded) "\u25B2" else "\u25BC", fontSize = 10.sp, color = Color(0xFF888888))
                    }
                }

                DropdownMenu(
                    expanded = versionMenuExpanded,
                    onDismissRequest = { versionMenuExpanded = false },
                    modifier = Modifier.width(280.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                ) {
                    versions.forEach { version ->
                        DropdownMenuItem(onClick = {
                            onVersionSelected(version)
                            versionMenuExpanded = false
                        }) {
                            Text(version, fontSize = 13.sp, color = if (version == selectedVersion) primaryColor else Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun animateFloatAsState(targetValue: Float): androidx.compose.runtime.State<Float> {
    return androidx.compose.animation.core.animateFloatAsState(
        targetValue,
        animationSpec = androidx.compose.animation.core.tween(200)
    )
}
