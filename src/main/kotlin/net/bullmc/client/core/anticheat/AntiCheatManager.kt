package net.bullmc.client.core.anticheat

import kotlinx.coroutines.runBlocking
import java.io.File

class AntiCheatManager(
    private val modsDir: File,
    private val gameDir: File
) {
    private val scanner = AntiCheatScanner(modsDir, gameDir)
    private val reporter = ViolationReporter()
    private val internalDetector = InternalCheatDetector(gameDir)

    @Volatile
    var lastScanResult: FullScanResult? = null
        private set

    @Volatile
    var lastInternalResult: InternalCheatResult? = null
        private set

    @Volatile
    var enabled: Boolean = true

    @Volatile
    private var runtimeMonitoringActive = false

    fun checkBeforeLaunch(playerName: String): PreLaunchCheckResult {
        if (!enabled) {
            return PreLaunchCheckResult(
                allowed = true,
                message = "Античит отключён",
                violations = emptyList(),
                internalCheatResult = null
            )
        }

        val fullScan = scanner.fullScan(playerName)
        lastScanResult = fullScan

        val internalResult = internalDetector.runFullDetection()
        lastInternalResult = internalResult

        val allViolations = mutableListOf<ViolationReport>()
        allViolations.addAll(fullScan.violations)

        for (finding in internalResult.findings) {
            allViolations.add(
                ViolationReport(
                    playerName = playerName,
                    violationType = "AI_${finding.type.name}",
                    details = finding.description
                )
            )
        }

        if (allViolations.isNotEmpty()) {
            runBlocking {
                reporter.reportAndBan(playerName, allViolations)
            }

            val blocked = allViolations.any {
                it.violationType in listOf(
                    "BLACKLISTED_MOD",
                    "CHEAT_PROCESS",
                    "MOD_TAMPERED",
                    "CHEAT_IN_CLASSPATH",
                    "CHEAT_IN_GAME_DIR",
                    "SUSPICIOUS_NATIVE_LIB",
                    "CHEAT_TWEAK_CLASS",
                    "MOD_METADATA_BLOCKED"
                )
            } || internalResult.shouldBlock

            val message = when {
                blocked && internalResult.shouldBlock -> {
                    "AI-детект: обнаружены интернал читы (уровень: ${internalResult.threatLevel})! Запуск заблокирован."
                }
                blocked -> {
                    "Обнаружены запрещённые модификации! Запуск заблокирован."
                }
                internalResult.shouldBlock -> {
                    "AI-детект: высокий уровень угрозы. Запуск заблокирован."
                }
                else -> {
                    "Обнаружены неизвестные моды. Запуск разрешён с предупреждением."
                }
            }

            return PreLaunchCheckResult(
                allowed = !blocked,
                message = message,
                violations = allViolations,
                internalCheatResult = internalResult
            )
        }

        ensureAgentExtracted()

        return PreLaunchCheckResult(
            allowed = true,
            message = "Проверка пройдена",
            violations = emptyList(),
            internalCheatResult = internalResult
        )
    }

    fun getAgentJarPath(): File {
        val agentDir = File(gameDir, "anticheat")
        agentDir.mkdirs()
        return File(agentDir, "bullmc-anticheat-agent.jar")
    }

    fun buildAgentJvmArg(): String? {
        val agentJar = getAgentJarPath()
        if (!agentJar.exists()) {
            return null
        }

        if (agentJar.length() == 0L) {
            ensureAgentExtracted()
            if (!agentJar.exists() || agentJar.length() == 0L) {
                return null
            }
        }

        if (!verifyAgentIntegrity(agentJar)) {
            ensureAgentExtracted()
            if (!verifyAgentIntegrity(agentJar)) {
                return null
            }
        }

        return "-javaagent:${agentJar.absolutePath}"
    }

    private fun verifyAgentIntegrity(agentJar: File): Boolean {
        if (!agentJar.exists() || agentJar.length() == 0L) return false

        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            agentJar.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }

            hash == AGENT_JAR_HASH || AGENT_JAR_HASH.isEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun ensureAgentExtracted(): Boolean {
        val agentJar = getAgentJarPath()
        if (agentJar.exists() && agentJar.length() > 0) return true

        val resource = this::class.java.getResourceAsStream("/anticheat-agent/bullmc-anticheat-agent.jar")
            ?: return false

        return try {
            agentJar.parentFile?.mkdirs()
            agentJar.outputStream().use { out ->
                resource.copyTo(out)
            }
            resource.close()
            agentJar.exists() && agentJar.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    fun scanClasspathDuringRuntime(classpath: String): ClasspathScanResult {
        return scanner.scanClasspath(classpath)
    }

    fun scanJvmArgs(args: List<String>): JvmArgScanResult {
        return scanner.scanJvmArgs(args)
    }

    fun startRuntimeMonitoring(intervalMs: Long = 30_000): Thread {
        runtimeMonitoringActive = true
        val monitorThread = Thread({
            while (runtimeMonitoringActive) {
                try {
                    Thread.sleep(intervalMs)
                    if (!runtimeMonitoringActive) break
                    scanner.scanProcesses()
                    scanner.scanNativeLibraries()
                    internalDetector.runFullDetection()
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                }
            }
        }, "AntiCheat-RuntimeMonitor")
        monitorThread.isDaemon = true
        monitorThread.priority = Thread.MIN_PRIORITY
        monitorThread.start()
        return monitorThread
    }

    fun stopRuntimeMonitoring() {
        runtimeMonitoringActive = false
    }

    fun close() {
        stopRuntimeMonitoring()
        reporter.close()
    }

    companion object {
        private const val AGENT_JAR_HASH = ""
    }
}

data class PreLaunchCheckResult(
    val allowed: Boolean,
    val message: String,
    val violations: List<ViolationReport>,
    val internalCheatResult: InternalCheatResult? = null
)
