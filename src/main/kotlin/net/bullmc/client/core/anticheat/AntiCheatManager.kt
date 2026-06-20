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

        println("[ANTICHEAT] Запуск предстартовой проверки для $playerName")

        val fullScan = scanner.fullScan(playerName)
        lastScanResult = fullScan

        println("[AI-DETECT] Запуск AI-анализа интернал читов...")
        val internalResult = internalDetector.runFullDetection()
        lastInternalResult = internalResult
        println("[AI-DETECT] Уровень угрозы: ${internalResult.threatLevel} (score: ${internalResult.totalScore})")
        if (internalResult.findings.isNotEmpty()) {
            for (finding in internalResult.findings) {
                println("  - [${finding.type}] ${finding.description} (score: ${finding.score})")
            }
        }

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
            println("[ANTICHEAT] ОБНАРУЖЕНО ${allViolations.size} НАРУШЕНИЙ:")
            for (v in allViolations) {
                println("  - [${v.violationType}] ${v.details}")
            }

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

        println("[ANTICHEAT] Проверка пройдена — всё чисто")
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
            println("[ANTICHEAT] Agent JAR не найден: ${agentJar.absolutePath}")
            return null
        }

        if (agentJar.length() == 0L) {
            println("[ANTICHEAT] Agent JAR пустой, попытка переизвлечения...")
            ensureAgentExtracted()
            if (!agentJar.exists() || agentJar.length() == 0L) {
                return null
            }
        }

        return "-javaagent:${agentJar.absolutePath}"
    }

    fun ensureAgentExtracted(): Boolean {
        val agentJar = getAgentJarPath()
        if (agentJar.exists() && agentJar.length() > 0) return true

        val resource = this::class.java.getResourceAsStream("/anticheat-agent/bullmc-anticheat-agent.jar")
        if (resource == null) {
            println("[ANTICHEAT] Agent JAR не найден в ресурсах")
            return false
        }

        return try {
            agentJar.parentFile?.mkdirs()
            agentJar.outputStream().use { out ->
                resource.copyTo(out)
            }
            resource.close()
            println("[ANTICHEAT] Agent JAR извлечён: ${agentJar.absolutePath} (${agentJar.length()} байт)")
            agentJar.exists() && agentJar.length() > 0
        } catch (e: Exception) {
            println("[ANTICHEAT] Ошибка извлечения agent JAR: ${e.message}")
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
            println("[ANTICHEAT] Рантайм-мониторинг запущен (интервал: ${intervalMs}ms)")
            while (runtimeMonitoringActive) {
                try {
                    Thread.sleep(intervalMs)
                    if (!runtimeMonitoringActive) break

                    val processResult = scanner.scanProcesses()
                    if (!processResult.clean) {
                        println("[ANTICHEAT] [RUNTIME] Обнаружены чит-процессы: ${processResult.foundProcesses}")
                    }

                    val nativeScan = scanner.scanNativeLibraries()
                    if (!nativeScan.clean) {
                        println("[ANTICHEAT] [RUNTIME] Обнаружены подозрительные нативные библиотеки: ${nativeScan.suspiciousLibs}")
                    }

                    val internalResult = internalDetector.runFullDetection()
                    if (internalResult.findings.isNotEmpty()) {
                        println("[AI-DETECT] [RUNTIME] Уровень угрозы: ${internalResult.threatLevel} (score: ${internalResult.totalScore})")
                        for (finding in internalResult.findings) {
                            println("  - [${finding.type}] ${finding.description}")
                        }
                        if (internalResult.shouldBlock) {
                            println("[AI-DETECT] [RUNTIME] КРИТИЧЕСКАЯ УГРОЗА — запуск будет заблокирован при следующем запуске")
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("[ANTICHEAT] [RUNTIME] Ошибка мониторинга: ${e.message}")
                }
            }
            println("[ANTICHEAT] Рантайм-мониторинг остановлен")
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
}

data class PreLaunchCheckResult(
    val allowed: Boolean,
    val message: String,
    val violations: List<ViolationReport>,
    val internalCheatResult: InternalCheatResult? = null
)
