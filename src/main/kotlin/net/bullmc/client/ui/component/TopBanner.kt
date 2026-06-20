package net.bullmc.client.ui.component

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
import net.bullmc.client.core.loader.LoaderType

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
    onLaunch: (String) -> Unit,
    activeProfileName: String = "",
    selectedLoader: LoaderType = LoaderType.VANILLA,
    anticheatStatus: String = "",
    anticheatThreatLevel: String = ""
) {
    val nick = remember { mutableStateOf(savedNick) }
    val shape = RoundedCornerShape(16.dp)
    var versionMenuExpanded by remember { mutableStateOf(false) }

    val buttonText = when (launchState) {
        "READY" -> "\u25B6  PLAY"
        "DOWNLOADING" -> "\u23F3  ЗАГРУЗКА..."
        "LAUNCHING" -> "\u21BB  ЗАПУСК..."
        "RUNNING" -> "\u2714  ЗАПУЩЕНА"
        else -> "\u25B6  PLAY"
    }
    val buttonEnabled = launchState == "READY" && nick.value.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Color(0xFF0D1117))
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
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.06f),
                            Color(0xFF0D1117)
                        )
                    )
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.verticalGradient(colors = listOf(Color(0x00000000), Color(0xDD0D1117)))
            )
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("BULL MC", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)

                val mainServer = serverStatuses["play.bullmc.net"]
                if (mainServer != null && mainServer.online) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${mainServer.playersOnline}/${mainServer.playersMax} игроков",
                            fontSize = 14.sp,
                            color = Color(0xFF8B949E)
                        )
                    }
                }

                if (anticheatStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val shieldColor = when (anticheatThreatLevel) {
                            "CLEAN" -> Color(0xFF34D399)
                            "LOW" -> Color(0xFF34D399)
                            "MEDIUM" -> Color(0xFFFBBF24)
                            "HIGH", "CRITICAL" -> Color(0xFFF87171)
                            else -> Color(0xFF8B949E)
                        }
                        Text("\uD83D\uDEE1", fontSize = 12.sp, color = shieldColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(anticheatStatus, fontSize = 12.sp, color = Color(0xFF8B949E))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val btnInteraction = remember { MutableInteractionSource() }
                val btnHovered by btnInteraction.collectIsHoveredAsState()
                val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                    if (btnHovered) 1.03f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(150)
                )

                Button(
                    onClick = { if (buttonEnabled) onLaunch(nick.value) },
                    modifier = Modifier
                        .width(260.dp)
                        .height(54.dp)
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
                                    !buttonEnabled && launchState != "READY" -> listOf(Color(0xFF30363D), Color(0xFF21262D))
                                    btnHovered -> listOf(primaryColor.copy(alpha = 0.85f), primaryColor.copy(alpha = 0.65f))
                                    else -> listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
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
                                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                            }
                        } else {
                            Text(
                                text = buttonText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (launchState == "RUNNING") Color(0xFF34D399) else Color.White,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.width(260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(statusMessage, fontSize = 13.sp, color = Color(0xFF8B949E))
                        if (launchState == "DOWNLOADING") {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.width(260.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = primaryColor,
                                backgroundColor = Color(0xFF21262D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier.width(280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = nick.value,
                    onValueChange = { nick.value = it; onNickChanged(nick.value) },
                    placeholder = { Text("Введите ник", color = Color(0xFF8B949E), fontSize = 15.sp) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFFE6EDF3), fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = launchState == "READY",
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF1C2128),
                        cursorColor = primaryColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C2128))
                            .clickable { if (launchState == "READY") versionMenuExpanded = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("MC $selectedVersion", fontSize = 14.sp, color = Color(0xFFE6EDF3))
                            Text(if (versionMenuExpanded) "\u25B2" else "\u25BC", fontSize = 11.sp, color = Color(0xFF8B949E))
                        }
                    }

                    DropdownMenu(
                        expanded = versionMenuExpanded,
                        onDismissRequest = { versionMenuExpanded = false },
                        modifier = Modifier.width(280.dp).background(Color(0xFF1C2128), RoundedCornerShape(10.dp))
                    ) {
                        versions.forEach { version ->
                            DropdownMenuItem(onClick = {
                                onVersionSelected(version)
                                versionMenuExpanded = false
                            }) {
                                Text(version, fontSize = 14.sp, color = if (version == selectedVersion) primaryColor else Color(0xFFE6EDF3))
                            }
                        }
                    }
                }

                if (activeProfileName.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1117))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activeProfileName, fontSize = 13.sp, color = Color(0xFF8B949E))
                        Text(selectedLoader.displayName, fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
