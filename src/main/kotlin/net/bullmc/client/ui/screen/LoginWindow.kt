package net.bullmc.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bullmc.client.core.auth.AuthState
import net.bullmc.client.core.auth.MicrosoftAuth
import java.awt.Desktop
import java.net.URI

@Composable
fun LoginWindow(
    microsoftAuth: MicrosoftAuth,
    savedNick: String,
    primaryColor: Color,
    onLoginComplete: (nick: String, isOffline: Boolean) -> Unit
) {
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

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.06f),
                    Color(0xFF0D1117)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(Color(0x00000000), Color(0xDD0D1117)))
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text("BULL MC", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 6.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Войдите, чтобы начать игру", fontSize = 14.sp, color = Color(0xFF8B949E))

            Spacer(modifier = Modifier.height(32.dp))

            // Login card
            Box(
                modifier = Modifier
                    .width(400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161B22))
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (microsoftAuth.isLoggedIn()) {
                        val profile = microsoftAuth.playerProfile
                        if (profile != null) {
                            Text("Вы вошли как", fontSize = 13.sp, color = Color(0xFF6E7681))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryColor)
                                    .clickable {
                                        onLoginComplete(profile.name, false)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\u25B6  ИГРАТЬ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF87171).copy(alpha = 0.1f))
                                    .clickable { microsoftAuth.logout() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Выйти из аккаунта", fontSize = 12.sp, color = Color(0xFFF87171))
                            }
                        }
                    } else if (isOfflineMode) {
                        // Offline login
                        Text("Оффлайн вход", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE6EDF3))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Введите ник для игры", fontSize = 12.sp, color = Color(0xFF6E7681))

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = nickInput,
                            onValueChange = { nickInput = it },
                            placeholder = { Text("Ваш ник", color = Color(0xFF6E7681), fontSize = 15.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE6EDF3), fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && nickInput.isNotBlank()) {
                                        onLoginComplete(nickInput.trim(), true)
                                        true
                                    } else false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color(0xFF0D1117),
                                cursorColor = primaryColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (nickInput.isNotBlank()) primaryColor else Color(0xFF30363D))
                                .clickable(enabled = nickInput.isNotBlank()) {
                                    onLoginComplete(nickInput.trim(), true)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\u25B6  ИГРАТЬ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "← Назад",
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.clickable { isOfflineMode = false }
                        )

                    } else when (state) {
                        AuthState.DEVICE_CODE_PENDING -> {
                            val code = deviceCodeInfo
                            if (code != null) {
                                CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Перейдите по ссылке и введите код", fontSize = 13.sp, color = Color(0xFF8B949E), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(10.dp))

                                // URL box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0D1117))
                                        .clickable {
                                            try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(code.verificationUri, fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Code display
                                Text("Ваш код:", fontSize = 12.sp, color = Color(0xFF6E7681))
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0D1117))
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
                                        color = Color(0xFFE6EDF3),
                                        letterSpacing = 6.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Нажмите на код чтобы скопировать", fontSize = 11.sp, color = Color(0xFF484F58))

                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(color = primaryColor, strokeWidth = 1.5.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Ожидание подтверждения...", fontSize = 11.sp, color = Color(0xFF6E7681))

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Отмена",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.clickable {
                                        microsoftAuth.cancelLogin()
                                    }
                                )
                            }
                        }

                        AuthState.AUTHENTICATING, AuthState.REFRESHING -> {
                            CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (state == AuthState.AUTHENTICATING) "Получение профиля..." else "Обновление токена...",
                                fontSize = 13.sp,
                                color = Color(0xFF8B949E)
                            )
                        }

                        AuthState.SUCCESS -> {
                            val profile = microsoftAuth.playerProfile
                            if (profile != null) {
                                Text("Добро пожаловать!", fontSize = 13.sp, color = Color(0xFF6E7681))
                                Text(profile.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp)
                        }

                        else -> {
                            // IDLE / FAILED
                            if (errorMessage.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF87171).copy(alpha = 0.1f))
                                        .padding(10.dp)
                                ) {
                                    Text(errorMessage, fontSize = 11.sp, color = Color(0xFFF87171), textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Microsoft login button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0078D4))
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            val code = microsoftAuth.startDeviceCodeLogin()
                                            if (code != null) {
                                                try { Desktop.getDesktop().browse(URI(code.verificationUri)) } catch (_: Exception) {}
                                                microsoftAuth.pollForToken(5)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Войти через Microsoft", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF21262D)))
                                Text("  или  ", fontSize = 12.sp, color = Color(0xFF484F58))
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF21262D)))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Offline button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF21262D))
                                    .clickable { isOfflineMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Играть без аккаунта", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8B949E))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("BullMC Client v0.1.0", fontSize = 11.sp, color = Color(0xFF30363D))
        }
    }
}
