package net.bullmc.client.core.anticheat

import kotlinx.coroutines.runBlocking
import java.io.File

class AntiCheatManager(
    private val modsDir: File,
    private val gameDir: File
) {
    private val scanner = AntiCheatScanner(modsDir, gameDir)
    private val reporter = ViolationReporter()

    @Volatile
    var lastScanResult: FullScanResult? = null
        private set

    @Volatile
    var enabled: Boolean = true

    fun checkBeforeLaunch(playerName: String): PreLaunchCheckResult {
        if (!enabled) {
            return PreLaunchCheckResult(
                allowed = true,
                message = "Античит отключён",
                violations = emptyList()
            )
        }

        println("[ANTICHEAT] Запуск предстартовой проверки для $playerName")

        val fullScan = scanner.fullScan(playerName)
        lastScanResult = fullScan

        if (!fullScan.clean) {
            println("[ANTICHEAT] ОБНАРУЖЕНО ${fullScan.totalViolations} НАРУШЕНИЙ:")
            for (v in fullScan.violations) {
                println("  - [${v.violationType}] ${v.details}")
            }

            runBlocking {
                reporter.reportAndBan(playerName, fullScan.violations)
            }

            val blocked = fullScan.violations.any {
                it.violationType in listOf(
                    "BLACKLISTED_MOD",
                    "CHEAT_PROCESS",
                    "MOD_TAMPERED",
                    "CHEAT_IN_CLASSPATH"
                )
            }

            return PreLaunchCheckResult(
                allowed = !blocked,
                message = if (blocked) {
                    "Обнаружены запрещённые модификации! Запуск заблокирован."
                } else {
                    "Обнаружены неизвестные моды. Запуск разрешён с предупреждением."
                },
                violations = fullScan.violations
            )
        }

        println("[ANTICHEAT] Проверка пройдена — всё чисто")
        return PreLaunchCheckResult(
            allowed = true,
            message = "Проверка пройдена",
            violations = emptyList()
        )
    }

    fun getAgentJarPath(): File {
        val agentDir = File(gameDir, "anticheat")
        agentDir.mkdirs()
        return File(agentDir, "bullmc-anticheat-agent.jar")
    }

    fun buildAgentJvmArg(): String? {
        val agentJar = getAgentJarPath()
        if (!agentJar.exists()) return null
        return "-javaagent:${agentJar.absolutePath}"
    }

    fun scanClasspathDuringRuntime(classpath: String): ClasspathScanResult {
        return scanner.scanClasspath(classpath)
    }

    fun scanJvmArgs(args: List<String>): JvmArgScanResult {
        return scanner.scanJvmArgs(args)
    }

    fun close() {
        reporter.close()
    }
}

data class PreLaunchCheckResult(
    val allowed: Boolean,
    val message: String,
    val violations: List<ViolationReport>
)
