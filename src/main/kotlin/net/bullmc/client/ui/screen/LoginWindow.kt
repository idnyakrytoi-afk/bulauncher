package net.bullmc.client.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.bullmc.client.core.auth.AuthState
import net.bullmc.client.core.auth.MicrosoftAuth
import net.bullmc.client.theme.LocalBullColors
import net.bullmc.client.ui.component.BullCard
import net.bullmc.client.ui.component.BullPrimaryButton
import net.bullmc.client.ui.component.BullSecondaryButton
import net.bullmc.client.ui.component.BullTextField
import java.awt.Desktop
import java.net.URI

@Composable
fun LoginWindow(
    microsoftAuth: MicrosoftAuth,
    savedNick: String,
    primaryColor: Color,
    onLoginComplete: (nick: String, isOffline: Boolean) -> Unit
) {
    val colors = LocalBullColors.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(microsoftAuth.state) }
    var deviceCodeInfo by remember { mutableStateOf(microsoftAuth.deviceCode) }
    var errorMessage by remember { mutableStateOf(microsoftAuth.errorMessage) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            state = microsoftAuth.state
            deviceCodeInfo = microsoftAuth.deviceCode
            errorMessage = microsoftAuth.errorMessage
        }
    }

    var nickInput by remember { mutableStateOf(savedNick) }
    var isOfflineMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Подсветка фона
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(300f, 120f),
                    radius = 800f
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val hasLogo = remember { Thread.currentThread().contextClassLoader?.getResource("bull.png") != null }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(colors.primaryGradient)),
                contentAlignment = Alignment.Center
            ) {
                if (hasLogo) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource("bull.png"),
                        contentDescription = "BullMC",
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("B", fontSize = 40.sp, fontWeight = FontWeight.Black, color = colors.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("BULL MC", fontSize = 30.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, letterSpacing = 6.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Войдите, чтобы начать играть", fontSize = 13.sp, color = colors.textSecondary)

            Spacer(modifier = Modifier.height(28.dp))

            BullCard(
                modifier = Modifier.width(420.dp),
                colors = colors,
                contentPadding = PaddingValues(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (microsoftAuth.isLoggedIn()) {
                        val profile = microsoftAuth.playerProfile
                        if (profile != null) {
                            Text("Вы вошли как", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(profile.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            Spacer(modifier = Modifier.height(22.dp))

                            BullPrimaryButton(
                                text = "ИГРАТЬ",
                                onClick = { onLoginComplete(profile.name, false) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = colors,
                                height = 48.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            BullSecondaryButton(
                                text = "Выйти из аккаунта",
                                onClick = { microsoftAuth.logout() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = colors,
                                height = 40.dp
                            )
                        }
                    } else if (isOfflineMode) {
                        Text("Оффлайн-вход", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Введите ник для игры", fontSize = 12.sp, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && nickInput.isNotBlank()) {
                                        onLoginComplete(nickInput.trim(), true)
                                        true
                                    } else false
                                }
                        ) {
                            BullTextField(
                                value = nickInput,
                                onValueChange = { nickInput = it },
                                placeholder = "Ваш ник",
                                modifier = Modifier.fillMaxWidth(),
                                colors = colors,
                                fontSize = 16
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        BullPrimaryButton(
                            text = "ИГРАТЬ",
                            onClick = { onLoginComplete(nickInput.trim(), true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = colors,
                            enabled = nickInput.isNotBlank()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "← Назад",
                            fontSize = 12.sp,
                            color = colors.primary,
                            modifier = Modifier.clickable { isOfflineMode = false }
                        )
                    } else when (state) {
                        AuthState.DEVICE_CODE_PENDING -> {
                            val code = deviceCodeInfo
                            if (code != null) {
                                CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Перейдите по ссылке и введите код", fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceSunken)
                                        .clickable {
                                            try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(code.verificationUri, fontSize = 13.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Ваш код:", fontSize = 12.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceSunken)
                                        .clickable {
                                            try {
                                                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                                val selection = java.awt.datatransfer.StringSelection(code.userCode)
                                                clipboard.setContents(selection, selection)
                                            } catch (_: Exception) {}
                                        }
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        code.userCode,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.textPrimary,
                                        letterSpacing = 6.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Нажмите на код чтобы скопировать", fontSize = 11.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(color = colors.primary, strokeWidth = 1.5.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Ожидание подтверждения...", fontSize = 11.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Отмена",
                                    fontSize = 12.sp,
                                    color = colors.error,
                                    modifier = Modifier.clickable { microsoftAuth.cancelLogin() }
                                )
                            }
                        }

                        AuthState.AUTHENTICATING, AuthState.REFRESHING -> {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (state == AuthState.AUTHENTICATING) "Получение профиля..." else "Обновление токена...",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }

                        AuthState.SUCCESS -> {
                            val profile = microsoftAuth.playerProfile
                            if (profile != null) {
                                Text("Добро пожаловать!", fontSize = 13.sp, color = colors.textMuted)
                                Text(profile.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                        }

                        else -> {
                            if (errorMessage.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.error.copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Text(errorMessage, fontSize = 11.sp, color = colors.error, textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            BullPrimaryButton(
                                text = "Войти через Microsoft",
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val code = microsoftAuth.startDeviceCodeLogin()
                                        if (code != null) {
                                            try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                            microsoftAuth.pollForToken(5)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = colors
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
                                Text("  или  ", fontSize = 12.sp, color = colors.textMuted)
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            BullSecondaryButton(
                                text = "Играть без аккаунта",
                                onClick = { isOfflineMode = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = colors,
                                height = 46.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("BullMC Client v1.0.0", fontSize = 11.sp, color = colors.textMuted)
        }
    }
}
