package net.bullmc.client.core.auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.bullmc.client.core.util.LauncherPaths

@Serializable
data class DeviceCodeResponse(
    val device_code: String = "",
    val user_code: String = "",
    val verification_uri: String = "",
    val expires_in: Int = 0,
    val interval: Int = 5,
    val message: String = ""
)

@Serializable
data class TokenResponse(
    val access_token: String = "",
    val refresh_token: String = "",
    val expires_in: Int = 0,
    val error: String = "",
    val error_description: String = ""
)

@Serializable
data class XboxAuthResponse(
    val IssueInstant: String = "",
    val NotAfter: String = "",
    val Token: String = "",
    val DisplayClaims: JsonObject = buildJsonObject {}
)

@Serializable
data class XSTSResponse(
    val IssueInstant: String = "",
    val NotAfter: String = "",
    val Token: String = "",
    val DisplayClaims: JsonObject = buildJsonObject {},
    val Error: String? = null,
    val Message: String? = null
)

@Serializable
data class MinecraftTokenResponse(
    val access_token: String = "",
    val expires_in: Int = 0
)

@Serializable
data class MinecraftProfile(
    val id: String = "",
    val name: String = "",
    val skins: List<SkinInfo> = emptyList()
)

@Serializable
data class SkinInfo(
    val id: String = "",
    val state: String = "",
    val url: String = "",
    val variant: String = "",
    val texture: String = ""
)

enum class AuthState {
    IDLE,
    DEVICE_CODE_PENDING,
    AUTHENTICATING,
    SUCCESS,
    FAILED,
    REFRESHING
}

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val message: String,
    val expiresInSeconds: Int
)

class MicrosoftAuth {
    companion object {
        private const val CLIENT_ID = "95dc0ced082b4577af4e0a238a0a7487"
        private const val TENANT = "consumers"
        private const val SCOPE = "XboxLive.signin offline_access"
        private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/$TENANT/oauth2/v2.0/devicecode"
        private const val TOKEN_URL = "https://login.microsoftonline.com/$TENANT/oauth2/v2.0/token"
        private const val XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
    }

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
        followRedirects = true
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    var state: AuthState = AuthState.IDLE
        private set

    @Volatile
    var deviceCode: DeviceCodeInfo? = null
        private set

    @Volatile
    var playerProfile: MinecraftProfile? = null
        private set

    @Volatile
    var accessToken: String = ""
        private set

    @Volatile
    var errorMessage: String = ""
        private set

    fun init() {
        loadTokens()
        if (accessToken.isNotEmpty()) {
            state = AuthState.SUCCESS
        }
    }

    suspend fun startDeviceCodeLogin(): DeviceCodeInfo? {
        state = AuthState.DEVICE_CODE_PENDING
        errorMessage = ""

        return try {
            val response = client.submitForm(
                url = DEVICE_CODE_URL,
                formParameters = parameters {
                    append("client_id", CLIENT_ID)
                    append("scope", SCOPE)
                }
            )

            val body = response.bodyAsText()
            val codeResponse = json.decodeFromString<DeviceCodeResponse>(body)

            if (codeResponse.user_code.isEmpty()) {
                state = AuthState.FAILED
                errorMessage = "Не удалось получить код устройства"
                return null
            }

            deviceCode = DeviceCodeInfo(
                deviceCode = codeResponse.device_code,
                userCode = codeResponse.user_code,
                verificationUri = codeResponse.verification_uri,
                message = codeResponse.message,
                expiresInSeconds = codeResponse.expires_in
            )

            deviceCode
        } catch (e: Exception) {
            state = AuthState.FAILED
            errorMessage = "Ошибка: ${e.message}"
            println("[AUTH] Device code error: ${e.message}")
            null
        }
    }

    suspend fun pollForToken(intervalSeconds: Int = 5): Boolean {
        val code = deviceCode ?: return false

        while (state == AuthState.DEVICE_CODE_PENDING) {
            try {
                val response = client.submitForm(
                    url = TOKEN_URL,
                    formParameters = parameters {
                        append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        append("client_id", CLIENT_ID)
                        append("device_code", code.deviceCode)
                    }
                )

                val body = response.bodyAsText()
                val tokenResponse = json.decodeFromString<TokenResponse>(body)

                when {
                    tokenResponse.access_token.isNotEmpty() -> {
                        state = AuthState.AUTHENTICATING
                        val success = authenticateWithXbox(tokenResponse.access_token, tokenResponse.refresh_token)
                        if (success) {
                            saveTokens(tokenResponse.refresh_token)
                            return true
                        }
                        return false
                    }
                    tokenResponse.error == "authorization_pending" -> {
                        kotlinx.coroutines.delay(intervalSeconds * 1000L)
                    }
                    tokenResponse.error == "authorization_declined" -> {
                        state = AuthState.FAILED
                        errorMessage = "Авторизация отклонена"
                        return false
                    }
                    tokenResponse.error == "expired_token" -> {
                        state = AuthState.FAILED
                        errorMessage = "Код истёк. Попробуйте снова."
                        return false
                    }
                    else -> {
                        println("[AUTH] Token poll: ${tokenResponse.error} - ${tokenResponse.error_description}")
                        kotlinx.coroutines.delay(intervalSeconds * 1000L)
                    }
                }
            } catch (e: Exception) {
                println("[AUTH] Poll error: ${e.message}")
                kotlinx.coroutines.delay(intervalSeconds * 1000L)
            }
        }
        return false
    }

    private suspend fun authenticateWithXbox(msToken: String, refreshToken: String): Boolean {
        try {
            // Step 1: Xbox Live auth
            val xboxResponse = client.post(XBOX_AUTH_URL) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("Properties", buildJsonObject {
                        put("AuthMethod", "RPS")
                        put("SiteName", "user.auth.xboxlive.com")
                        put("RpsTicket", msToken)
                    })
                    put("RelyingParty", "http://auth.xboxlive.com")
                    put("TokenType", "JWT")
                })
            }

            val xboxBody = json.parseToJsonElement(xboxResponse.bodyAsText()).jsonObject
            val xboxToken = xboxBody["Token"]?.jsonPrimitive?.content ?: run {
                state = AuthState.FAILED
                errorMessage = "Xbox auth failed"
                return false
            }

            val userClaims = xboxBody["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray?.getOrNull(0)?.jsonObject
            val userHash = userClaims?.get("uhs")?.jsonPrimitive?.content ?: ""

            // Step 2: XSTS auth
            val xstsResponse = client.post(XSTS_AUTH_URL) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("Properties", buildJsonObject {
                        put("SandboxId", "RETAIL")
                        put("UserTokens", buildJsonArray { add(JsonPrimitive(xboxToken)) })
                    })
                    put("RelyingParty", "rp://api.minecraftservices.com/")
                    put("TokenType", "JWT")
                })
            }

            val xstsBody = json.parseToJsonElement(xstsResponse.bodyAsText()).jsonObject
            val xstsError = xstsBody["Error"]?.jsonPrimitive?.contentOrNull
            if (xstsError != null) {
                state = AuthState.FAILED
                errorMessage = "XSTS error: ${xstsBody["Message"]?.jsonPrimitive?.contentOrNull ?: xstsError}"
                return false
            }

            val xstsToken = xstsBody["Token"]?.jsonPrimitive?.content ?: run {
                state = AuthState.FAILED
                errorMessage = "XSTS token empty"
                return false
            }

            val xstsClaims = xstsBody["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray?.getOrNull(0)?.jsonObject
            val xstsUserHash = xstsClaims?.get("uhs")?.jsonPrimitive?.content ?: userHash

            // Step 3: Minecraft login
            val mcResponse = client.post(MC_LOGIN_URL) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("identityToken", "XBL3.0 x=$xstsUserHash;$xstsToken")
                })
            }

            val mcBody = json.parseToJsonElement(mcResponse.bodyAsText()).jsonObject
            accessToken = mcBody["access_token"]?.jsonPrimitive?.content ?: run {
                state = AuthState.FAILED
                errorMessage = "MC token empty"
                return false
            }

            // Step 4: Get profile
            val profileResponse = client.get(MC_PROFILE_URL) {
                header("Authorization", "Bearer $accessToken")
            }

            if (profileResponse.status == HttpStatusCode.OK) {
                val profileBody = json.parseToJsonElement(profileResponse.bodyAsText()).jsonObject
                playerProfile = MinecraftProfile(
                    id = profileBody["id"]?.jsonPrimitive?.content ?: "",
                    name = profileBody["name"]?.jsonPrimitive?.content ?: ""
                )
                println("[AUTH] Logged in as: ${playerProfile?.name}")
            } else if (profileResponse.status.value == 404) {
                state = AuthState.FAILED
                errorMessage = "Аккаунт не купил Minecraft"
                return false
            }

            state = AuthState.SUCCESS
            return true

        } catch (e: Exception) {
            state = AuthState.FAILED
            errorMessage = "Auth error: ${e.message}"
            println("[AUTH] Xbox auth error: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = loadRefreshToken()
        if (refreshToken.isEmpty()) return false

        state = AuthState.REFRESHING
        return try {
            val response = client.submitForm(
                url = TOKEN_URL,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", CLIENT_ID)
                    append("refresh_token", refreshToken)
                }
            )

            val body = response.bodyAsText()
            val tokenResponse = json.decodeFromString<TokenResponse>(body)

            if (tokenResponse.access_token.isNotEmpty()) {
                val success = authenticateWithXbox(tokenResponse.access_token, tokenResponse.refresh_token)
                if (success) {
                    saveTokens(tokenResponse.refresh_token)
                }
                success
            } else {
                state = AuthState.FAILED
                errorMessage = "Refresh failed"
                false
            }
        } catch (e: Exception) {
            state = AuthState.FAILED
            errorMessage = "Refresh error: ${e.message}"
            false
        }
    }

    fun logout() {
        accessToken = ""
        playerProfile = null
        deviceCode = null
        state = AuthState.IDLE
        errorMessage = ""
        clearTokens()
    }

    fun cancelLogin() {
        deviceCode = null
        state = AuthState.IDLE
        errorMessage = ""
    }

    fun isLoggedIn(): Boolean = state == AuthState.SUCCESS && accessToken.isNotEmpty()

    fun getSkinUrl(): String? {
        return playerProfile?.skins?.firstOrNull { it.state == "ACTIVE" }?.url
    }

    private fun getTokensFile() = java.io.File(LauncherPaths.root, "ms_auth.json")

    private fun saveTokens(refreshToken: String) {
        try {
            val tokens = buildJsonObject {
                put("refresh_token", refreshToken)
                put("access_token", accessToken)
                put("player_name", playerProfile?.name ?: "")
                put("player_id", playerProfile?.id ?: "")
            }
            getTokensFile().writeText(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), tokens))
            println("[AUTH] Tokens saved")
        } catch (e: Exception) {
            println("[AUTH] Failed to save tokens: ${e.message}")
        }
    }

    private fun loadTokens() {
        try {
            val file = getTokensFile()
            if (!file.exists()) return
            val tokens = json.parseToJsonElement(file.readText()).jsonObject
            val rt = tokens["refresh_token"]?.jsonPrimitive?.content ?: return
            val at = tokens["access_token"]?.jsonPrimitive?.content ?: ""
            val name = tokens["player_name"]?.jsonPrimitive?.content ?: ""
            val id = tokens["player_id"]?.jsonPrimitive?.content ?: ""

            if (at.isNotEmpty() && name.isNotEmpty()) {
                accessToken = at
                playerProfile = MinecraftProfile(id = id, name = name)
            }
        } catch (e: Exception) {
            println("[AUTH] Failed to load tokens: ${e.message}")
        }
    }

    private fun loadRefreshToken(): String {
        return try {
            val file = getTokensFile()
            if (!file.exists()) return ""
            json.parseToJsonElement(file.readText()).jsonObject["refresh_token"]?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun clearTokens() {
        try {
            val file = getTokensFile()
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
