package net.bullmc.client.core.anticheat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ViolationPayload(
    val playerName: String,
    val violations: List<ViolationReport>,
    val launcherVersion: String = "1.0.0"
)

class ViolationReporter {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val reportEndpoints = listOf(
        "https://bullmc.net/api/anticheat/report",
        "https://api.bullmc.net/anticheat/violations",
    )

    suspend fun reportViolations(
        playerName: String,
        violations: List<ViolationReport>,
        endpoint: String? = null
    ): Boolean {
        if (violations.isEmpty()) return true

        val payload = ViolationPayload(
            playerName = playerName,
            violations = violations
        )

        val body = try {
            json.encodeToString(payload)
        } catch (_: Exception) {
            return false
        }

        val endpoints = if (endpoint != null) listOf(endpoint) else reportEndpoints

        for (url in endpoints) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                if (response.status == HttpStatusCode.OK) {
                    return true
                }
            } catch (_: Exception) {
            }
        }

        return false
    }

    suspend fun reportAndBan(
        playerName: String,
        violations: List<ViolationReport>
    ): BanResult {
        val reported = reportViolations(playerName, violations)

        return BanResult(
            reported = reported,
            shouldKick = violations.any {
                it.violationType in listOf(
                    "BLACKLISTED_MOD",
                    "CHEAT_PROCESS",
                    "MOD_TAMPERED",
                    "CHEAT_IN_CLASSPATH",
                    "CHEAT_IN_GAME_DIR",
                    "SUSPICIOUS_NATIVE_LIB",
                    "CHEAT_TWEAK_CLASS",
                    "MOD_METADATA_BLOCKED",
                    "AI_INJECTED_CHEAT_DLL",
                    "AI_CHEAT_DLL_IN_DIR",
                    "AI_HOOK_LIBRARY",
                    "AI_INJECTION_TOOL",
                    "AI_CODE_CAVE",
                    "AI_NETWORK_HOOK",
                    "AI_REFLECTIVE_INJECTION",
                    "AI_DEBUGGER_DETECTED"
                )
            },
            message = if (violations.isNotEmpty()) {
                "Обнаружены запрещённые модификации. Доступ к серверу запрещён."
            } else {
                "Проверка пройдена"
            }
        )
    }

    fun close() {
        client.close()
    }
}

data class BanResult(
    val reported: Boolean,
    val shouldKick: Boolean,
    val message: String
)
