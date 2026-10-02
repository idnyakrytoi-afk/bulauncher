package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.awt.Desktop
import java.net.URI

@Composable
fun AuthScreen(
    microsoftAuth: MicrosoftAuth,
    primaryColor: Color,
    onAuthComplete: (String) -> Unit
) {
    val colors = LocalBullColors.current
    val scope = rememberCoroutineScope()
    val state by remember { mutableStateOf(microsoftAuth.state) }
    val deviceCodeInfo by remember { mutableStateOf(microsoftAuth.deviceCode) }
    val errorMessage by remember { mutableStateOf(microsoftAuth.errorMessage) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BullCard(
            modifier = Modifier.fillMaxWidth(0.62f),
            colors = colors,
            contentPadding = PaddingValues(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Аккаунт", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                if (microsoftAuth.isLoggedIn()) {
                    val profile = microsoftAuth.playerProfile
                    if (profile != null) {
                        Text("Вы вошли как", fontSize = 12.sp, color = colors.textMuted)
                        Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    BullSecondaryButton(
                        text = "Выйти",
                        onClick = { microsoftAuth.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = colors
                    )
                } else when (state) {
                    AuthState.IDLE, AuthState.FAILED -> {
                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, fontSize = 11.sp, color = colors.error, textAlign = TextAlign.Center)
                        }

                        Text(
                            "Войдите через Microsoft, чтобы играть на серверах с проверкой",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )

                        BullPrimaryButton(
                            text = "Войти через Microsoft",
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val code = microsoftAuth.startDeviceCodeLogin()
                                    if (code != null) {
                                        try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                        microsoftAuth.pollForToken(5)
                                        if (microsoftAuth.isLoggedIn()) {
                                            onAuthComplete(microsoftAuth.playerProfile?.name ?: "")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = colors
                        )

                        Text(
                            "Или войдите без аккаунта (offline)",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            modifier = Modifier.clickable { onAuthComplete("") }
                        )
                    }

                    AuthState.DEVICE_CODE_PENDING -> {
                        val code = deviceCodeInfo
                        if (code != null) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                            Text("Перейдите по ссылке:", fontSize = 11.sp, color = colors.textMuted)
                            Text(
                                code.verificationUri,
                                fontSize = 13.sp,
                                color = colors.primary,
                                modifier = Modifier.clickable {
                                    try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Введите код:", fontSize = 11.sp, color = colors.textMuted)
                            Text(
                                code.userCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                                letterSpacing = 4.sp
                            )
                            BullSecondaryButton(
                                text = "Копировать код",
                                onClick = {
                                    try {
                                        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                        val selection = java.awt.datatransfer.StringSelection(code.userCode)
                                        clipboard.setContents(selection, selection)
                                    } catch (_: Exception) {}
                                },
                                colors = colors,
                                accent = true
                            )
                            Text("Ожидание подтверждения...", fontSize = 11.sp, color = colors.textMuted)
                        }
                    }

                    AuthState.AUTHENTICATING, AuthState.REFRESHING -> {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                        Text(
                            if (state == AuthState.AUTHENTICATING) "Получение профиля..." else "Обновление токена...",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    AuthState.SUCCESS -> {
                        val profile = microsoftAuth.playerProfile
                        if (profile != null) {
                            Text("Добро пожаловать,", fontSize = 12.sp, color = colors.textMuted)
                            Text(profile.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                    }
                    else -> {}
                }
            }
        }
    }
}
