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
import java.awt.Desktop
import java.net.URI

@Composable
fun AuthScreen(
    microsoftAuth: MicrosoftAuth,
    primaryColor: Color,
    onAuthComplete: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state by remember { mutableStateOf(microsoftAuth.state) }
    val deviceCodeInfo by remember { mutableStateOf(microsoftAuth.deviceCode) }
    val errorMessage by remember { mutableStateOf(microsoftAuth.errorMessage) }

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.6f).clip(cardShape).background(Color(0xFF161B22)).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Вход в Microsoft", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9D1D9))

                if (microsoftAuth.isLoggedIn()) {
                    val profile = microsoftAuth.playerProfile
                    if (profile != null) {
                        Text(
                            "Вы вошли как",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7681)
                        )
                        Text(
                            profile.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF87171).copy(alpha = 0.15f))
                            .clickable {
                                microsoftAuth.logout()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Выйти", fontSize = 12.sp, color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                    }
                } else when (state) {
                    AuthState.IDLE, AuthState.FAILED -> {
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                errorMessage,
                                fontSize = 11.sp,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            "Войдите через Microsoft, чтобы играть на серверах с проверкой",
                            fontSize = 11.sp,
                            color = Color(0xFF6E7681),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor)
                                .clickable {
                                    scope.launch(Dispatchers.IO) {
                                        val code = microsoftAuth.startDeviceCodeLogin()
                                        if (code != null) {
                                            try {
                                                Desktop.getDesktop().browse(URI(code.verificationUri))
                                            } catch (_: Exception) {}
                                            microsoftAuth.pollForToken(5)
                                            if (microsoftAuth.isLoggedIn()) {
                                                onAuthComplete(microsoftAuth.playerProfile?.name ?: "")
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Войти через Microsoft", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Или войдите без аккаунта (offline)",
                            fontSize = 11.sp,
                            color = Color(0xFF484F58),
                            modifier = Modifier.clickable { onAuthComplete("") }
                        )
                    }

                    AuthState.DEVICE_CODE_PENDING -> {
                        val code = deviceCodeInfo
                        if (code != null) {
                            CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Перейдите по ссылке:", fontSize = 11.sp, color = Color(0xFF6E7681))
                            Text(
                                code.verificationUri,
                                fontSize = 13.sp,
                                color = primaryColor,
                                modifier = Modifier.clickable {
                                    try {
                                        Desktop.getDesktop().browse(URI(code.verificationUri))
                                    } catch (_: Exception) {}
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Введите код:", fontSize = 11.sp, color = Color(0xFF6E7681))
                            Text(
                                code.userCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC9D1D9),
                                letterSpacing = 4.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF21262D))
                                    .clickable {
                                        try {
                                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                            val selection = java.awt.datatransfer.StringSelection(code.userCode)
                                            clipboard.setContents(selection, selection)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Копировать код", fontSize = 11.sp, color = primaryColor)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ожидание подтверждения...", fontSize = 11.sp, color = Color(0xFF484F58))
                        }
                    }

                    AuthState.AUTHENTICATING, AuthState.REFRESHING -> {
                        CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (state == AuthState.AUTHENTICATING) "Получение профиля..." else "Обновление токена...",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7681)
                        )
                    }

                    AuthState.SUCCESS -> {
                        val profile = microsoftAuth.playerProfile
                        if (profile != null) {
                            Text("Добро пожаловать,", fontSize = 12.sp, color = Color(0xFF6E7681))
                            Text(profile.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp)
                    }
                    else -> {}
                }
            }
        }
    }
}
