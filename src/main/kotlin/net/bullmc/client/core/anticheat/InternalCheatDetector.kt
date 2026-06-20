package net.bullmc.client.core.anticheat

import java.io.File
import java.security.MessageDigest

class InternalCheatDetector(
    private val gameDir: File
) {
    private val moduleAnalyzer = ModuleAnalyzer()
    private val signatureScanner = MemorySignatureScanner()

    fun runFullDetection(): InternalCheatResult {
        val findings = mutableListOf<CheatFinding>()

        findings.addAll(detectInjectedModules())
        findings.addAll(detectSuspiciousDlls())
        findings.addAll(detectHookLibraries())
        findings.addAll(detectInjectionTools())
        findings.addAll(detectCodeCaves())
        findings.addAll(detectProcessManipulation())
        findings.addAll(detectNetworkHooks())
        findings.addAll(detectAntiDebugBypass())
        findings.addAll(analyzeDllSignatures())
        findings.addAll(detectReflectiveInjection())

        val totalScore = findings.sumOf { it.score }
        val threatLevel = when {
            totalScore >= THREAT_CRITICAL -> ThreatLevel.CRITICAL
            totalScore >= THREAT_HIGH -> ThreatLevel.HIGH
            totalScore >= THREAT_MEDIUM -> ThreatLevel.MEDIUM
            totalScore >= THREAT_LOW -> ThreatLevel.LOW
            else -> ThreatLevel.CLEAN
        }

        return InternalCheatResult(
            threatLevel = threatLevel,
            totalScore = totalScore,
            findings = findings,
            shouldBlock = threatLevel >= ThreatLevel.HIGH
        )
    }

    private fun detectInjectedModules(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            try {
                val pb = ProcessBuilder("powershell", "-NoProfile", "-Command",
                    "Get-Process java* -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val completed = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                if (!completed) { proc.destroyForcibly(); return findings }
                val output = proc.inputStream.bufferedReader().readText()

                val minecraftPids = output.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.all { c -> c.isDigit() } }

                for (pid in minecraftPids) {
                    findings.addAll(analyzeProcessModules(pid))
                }
            } catch (e: Exception) {
                println("[AI-DETECT] Ошибка анализа процессов: ${e.message}")
            }
        }

        return findings
    }

    private fun analyzeProcessModules(pid: String): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        try {
            val pb = ProcessBuilder("tasklist", "/M", "/FI", "PID eq $pid", "/FO", "CSV")
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val completed = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) { proc.destroyForcibly(); return findings }
            val output = proc.inputStream.bufferedReader().readText()

            val modules = output.lines().flatMap { line ->
                val parts = line.split(",")
                if (parts.size > 6) {
                    parts.drop(6).map { it.trim().removeSurrounding("\"").lowercase() }
                } else emptyList()
            }

            for (module in modules) {
                val moduleName = File(module).name

                if (signatureScanner.isKnownCheatDll(moduleName)) {
                    findings.add(CheatFinding(
                        type = FindingType.INJECTED_CHEAT_DLL,
                        description = "Обнаружена инжектированная чит-DLL: $moduleName",
                        score = SCORE_CRITICAL,
                        evidence = module
                    ))
                }

                if (moduleAnalyzer.hasSuspiciousSectionNames(File(module))) {
                    findings.add(CheatFinding(
                        type = FindingType.SUSPICIOUS_MODULE,
                        description = "Подозрительный модуль (подозрительные секции): $moduleName",
                        score = SCORE_HIGH,
                        evidence = module
                    ))
                }

                if (moduleAnalyzer.isFromSuspiciousPath(module)) {
                    findings.add(CheatFinding(
                        type = FindingType.SUSPICIOUS_MODULE_PATH,
                        description = "Модуль из подозрительного пути: $module",
                        score = SCORE_MEDIUM,
                        evidence = module
                    ))
                }
            }
        } catch (e: Exception) {
            println("[AI-DETECT] Ошибка анализа модулей PID $pid: ${e.message}")
        }

        return findings
    }

    private fun detectSuspiciousDlls(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val suspiciousDirs = listOf(
            gameDir,
            File(gameDir, "bin"),
            File(gameDir, "natives"),
            File(gameDir, "natives-extracted")
        )

        for (dir in suspiciousDirs) {
            if (!dir.exists() || !dir.isDirectory) continue

            val dlls = dir.listFiles()?.filter {
                it.isFile && it.extension.lowercase() in setOf("dll", "so", "dylib")
            } ?: emptyList()

            for (dll in dlls) {
                if (isKnownSafeDll(dll.name)) continue

                if (signatureScanner.isKnownCheatDll(dll.name)) {
                    findings.add(CheatFinding(
                        type = FindingType.CHEAT_DLL_IN_DIR,
                        description = "Чит-DLL в директории: ${dll.absolutePath}",
                        score = SCORE_CRITICAL,
                        evidence = dll.absolutePath
                    ))
                }

                if (moduleAnalyzer.hasSuspiciousSectionNames(dll)) {
                    findings.add(CheatFinding(
                        type = FindingType.PACKED_SUSPICIOUS_DLL,
                        description = "Подозрительная секция в DLL: ${dll.name}",
                        score = SCORE_HIGH,
                        evidence = dll.absolutePath
                    ))
                }

                if (moduleAnalyzer.hasHighEntropy(dll)) {
                    findings.add(CheatFinding(
                        type = FindingType.PACKED_DLL,
                        description = "Высокая энтропия (паковка): ${dll.name}",
                        score = SCORE_MEDIUM,
                        evidence = dll.absolutePath
                    ))
                }
            }
        }

        return findings
    }

    private fun detectHookLibraries(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val hookLibPatterns = listOf(
            "minhook", "easyhook", "mhook", "detours", "trampoline",
            "devirtualize", "vtable", "vmthook", "vmthooker",
            "iat_hook", "eat_hook", "inline_hook", "detour",
            "funchook", "subhook", "plthook", "dobby"
        )

        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            try {
                val pb = ProcessBuilder("powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='javaw.exe'\" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty CommandLine")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val completed = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                if (!completed) { proc.destroyForcibly(); return findings }
                val output = proc.inputStream.bufferedReader().readText()

                val lowerOutput = output.lowercase()
                for (pattern in hookLibPatterns) {
                    if (lowerOutput.contains(pattern)) {
                        findings.add(CheatFinding(
                            type = FindingType.HOOK_LIBRARY,
                            description = "Обнаружена hook-библиотека вCommandLine: $pattern",
                            score = SCORE_HIGH,
                            evidence = pattern
                        ))
                    }
                }
            } catch (_: Exception) {}
        }

        val systemDirs = listOf(
            File("C:/Windows/System32"),
            File("C:/Windows/SysWOW64")
        )

        for (dir in systemDirs) {
            if (!dir.exists()) continue
            val hookDlls = dir.listFiles()?.filter { dll ->
                hookLibPatterns.any { pattern -> dll.name.lowercase().contains(pattern) }
            } ?: emptyList()

            for (dll in hookDlls) {
                if (!KNOWN_SYSTEM_HOOK_LIBS.contains(dll.name.lowercase())) {
                    findings.add(CheatFinding(
                        type = FindingType.HOOK_LIBRARY,
                        description = "Подозрительная hook-DLL в System32: ${dll.name}",
                        score = SCORE_MEDIUM,
                        evidence = dll.absolutePath
                    ))
                }
            }
        }

        return findings
    }

    private fun detectInjectionTools(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val injectionIndicators = listOf(
            "LoadLibrary", "VirtualAllocEx", "WriteProcessMemory",
            "CreateRemoteThread", "NtCreateThreadEx", "RtlCreateUserThread",
            "QueueUserAPC", "SetWindowsHookEx", "Inject",
            "open_process", "write_memory", "create_thread"
        )

        val tempDir = File(System.getProperty("java.io.tmpdir") ?: "")
        if (tempDir.exists()) {
            val suspiciousFiles = tempDir.listFiles()?.filter { file ->
                file.isFile && file.extension.lowercase() in setOf("dll", "exe", "bat", "cmd", "ps1")
                    && !isKnownSafeDll(file.name)
            } ?: emptyList()

            for (file in suspiciousFiles) {
                try {
                    val content = file.readBytes()
                    val contentStr = String(content, Charsets.ISO_8859_1)

                    var matchCount = 0
                    for (indicator in injectionIndicators) {
                        if (contentStr.contains(indicator, ignoreCase = true)) {
                            matchCount++
                        }
                    }

                    if (matchCount >= 3) {
                        findings.add(CheatFinding(
                            type = FindingType.INJECTION_TOOL,
                            description = "Инструмент инъекции: ${file.name} (совпадений: $matchCount)",
                            score = SCORE_HIGH,
                            evidence = file.absolutePath
                        ))
                    }
                } catch (_: Exception) {}
            }
        }

        return findings
    }

    private fun detectCodeCaves(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val suspiciousPatterns = listOf(
            byteArrayOf(0x90.toByte(), 0x90.toByte(), 0x90.toByte(), 0x90.toByte(), 0x90.toByte()),
            byteArrayOf(0xCC.toByte(), 0xCC.toByte(), 0xCC.toByte(), 0xCC.toByte(), 0xCC.toByte()),
            ByteArray(32) { 0x00 }
        )

        val minecraftJar = File(gameDir, "versions").listFiles()?.flatMap { versionDir ->
            versionDir.listFiles()?.filter { it.name.endsWith(".jar") }?.toList() ?: emptyList()
        }?.firstOrNull()

        if (minecraftJar != null && minecraftJar.exists()) {
            try {
                val bytes = minecraftJar.readBytes()
                for (pattern in suspiciousPatterns) {
                    var idx = 0
                    while (idx < bytes.size - pattern.size * 10) {
                        var consecutive = 0
                        var checkIdx = idx
                        while (checkIdx < bytes.size && consecutive < 10) {
                            if (bytes.sliceArray(checkIdx until checkIdx + pattern.size).contentEquals(pattern)) {
                                consecutive++
                                checkIdx += pattern.size
                            } else break
                        }

                        if (consecutive >= 10) {
                            findings.add(CheatFinding(
                                type = FindingType.CODE_CAVE,
                                description = "Обнаружена code cave (${consecutive}x NOP/INT3) в client JAR",
                                score = SCORE_CRITICAL,
                                evidence = "offset=$idx, pattern_length=${pattern.size}"
                            ))
                            break
                        }
                        idx++
                    }
                }
            } catch (_: Exception) {}
        }

        return findings
    }

    private fun detectProcessManipulation(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val manipulationPatterns = listOf(
            "ReadProcessMemory", "WriteProcessMemory",
            "VirtualProtectEx", "VirtualAllocEx",
            "NtWriteVirtualMemory", "NtReadVirtualMemory",
            "ZwWriteVirtualMemory", "ZwReadVirtualMemory",
            "MiniDumpWriteDump", "MiniDump",
            "DebugActiveProcess", "ptrace"
        )

        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            try {
                val pb = ProcessBuilder("tasklist", "/V", "/FO", "CSV")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val completed = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                if (!completed) { proc.destroyForcibly(); return findings }
                val output = proc.inputStream.bufferedReader().readText()

                val suspiciousProcesses = output.lines().filter { line ->
                    val lower = line.lowercase()
                    manipulationPatterns.any { pattern -> lower.contains(pattern.lowercase()) }
                }

                for (suspProc in suspiciousProcesses) {
                    val procName = suspProc.split(",").firstOrNull()?.removeSurrounding("\"") ?: "unknown"
                    findings.add(CheatFinding(
                        type = FindingType.PROCESS_MANIPULATION,
                        description = "Процесс с манипуляцией памяти: $procName",
                        score = SCORE_HIGH,
                        evidence = procName
                    ))
                }
            } catch (_: Exception) {}
        }

        return findings
    }

    private fun detectNetworkHooks(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val networkHookPatterns = listOf(
            "ws2_32.dll", "wsock32.dll", "winhttp.dll",
            "ws2_32.dll", "wship6.dll", "afunix.sys",
            "Winsock", "winsock2", "LSP", "layered service provider",
            "Windows Filtering Platform", "WFP"
        )

        val tempDir = File(System.getProperty("java.io.tmpdir") ?: "")
        if (tempDir.exists()) {
            val dlls = tempDir.listFiles()?.filter {
                it.isFile && it.extension.lowercase() == "dll" && !isKnownSafeDll(it.name)
            } ?: emptyList()

            for (dll in dlls) {
                try {
                    val bytes = dll.readBytes()
                    val contentStr = String(bytes, Charsets.ISO_8859_1)

                    var matchCount = 0
                    for (pattern in networkHookPatterns) {
                        if (contentStr.contains(pattern, ignoreCase = true)) {
                            matchCount++
                        }
                    }

                    if (matchCount >= 2) {
                        findings.add(CheatFinding(
                            type = FindingType.NETWORK_HOOK,
                            description = "Сетевой хук: ${dll.name} (совпадений: $matchCount)",
                            score = SCORE_HIGH,
                            evidence = dll.absolutePath
                        ))
                    }
                } catch (_: Exception) {}
            }
        }

        return findings
    }

    private fun detectAntiDebugBypass(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val antiDebugPatterns = listOf(
            "IsDebuggerPresent", "CheckRemoteDebuggerPresent",
            "NtQueryInformationProcess", "ZwQueryInformationProcess",
            "GetTickCount", "QueryPerformanceCounter",
            "rdtsc", "NtSetInformationThread",
            "OutputDebugString", "NtClose",
            "FindWindow", "EnumWindows"
        )

        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            try {
                val pb = ProcessBuilder("tasklist", "/V", "/FO", "CSV")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val completed = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                if (!completed) { proc.destroyForcibly(); return findings }
                val output = proc.inputStream.bufferedReader().readText()

                val debuggerProcesses = listOf(
                    "x64dbg", "x32dbg", "ollydbg", "ida", "idag",
                    "idaq", "windbg", "cdb", "ntsd", "immunity",
                    "processhacker", "procmon", "procmon64"
                )

                for (dbgProc in debuggerProcesses) {
                    if (output.lowercase().contains(dbgProc)) {
                        findings.add(CheatFinding(
                            type = FindingType.DEBUGGER_DETECTED,
                            description = "Обнаружен отладчик: $dbgProc",
                            score = SCORE_CRITICAL,
                            evidence = dbgProc
                        ))
                    }
                }
            } catch (_: Exception) {}
        }

        return findings
    }

    private fun analyzeDllSignatures(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val gameLibs = File(gameDir, "libraries")
        if (gameLibs.exists()) {
            val nativeDlls = gameLibs.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in setOf("dll", "so", "dylib") }
                .toList()

            for (dll in nativeDlls) {
                if (moduleAnalyzer.isUnsignedOrSuspicious(dll)) {
                    findings.add(CheatFinding(
                        type = FindingType.UNSIGNED_LIBRARY,
                        description = "Библиотека без валидной подписи: ${dll.name}",
                        score = SCORE_LOW,
                        evidence = dll.absolutePath
                    ))
                }
            }
        }

        return findings
    }

    private fun detectReflectiveInjection(): List<CheatFinding> {
        val findings = mutableListOf<CheatFinding>()

        val reflectivePatterns = listOf(
            "Reflectively", "LoadLibrary", "GetProcAddress",
            "LdrLoadDll", "LdrGetProcedureAddress",
            "NtOpenSection", "NtMapViewOfSection",
            "RtlInitUnicodeString", "NtCreateSection",
            "PEB", "TEB", "Ldr", "InMemoryOrderModuleList"
        )

        val tempDir = File(System.getProperty("java.io.tmpdir") ?: "")
        if (tempDir.exists()) {
            val files = tempDir.listFiles()?.filter {
                it.isFile && it.extension.lowercase() in setOf("dll", "exe", "bin") && !isKnownSafeDll(it.name)
            } ?: emptyList()

            for (file in files) {
                try {
                    val bytes = file.readBytes()
                    val contentStr = String(bytes, Charsets.ISO_8859_1)

                    var matchCount = 0
                    for (pattern in reflectivePatterns) {
                        if (contentStr.contains(pattern, ignoreCase = true)) {
                            matchCount++
                        }
                    }

                    if (matchCount >= 4) {
                        findings.add(CheatFinding(
                            type = FindingType.REFLECTIVE_INJECTION,
                            description = "Reflective injection: ${file.name} (совпадений: $matchCount)",
                            score = SCORE_CRITICAL,
                            evidence = file.absolutePath
                        ))
                    }
                } catch (_: Exception) {}
            }
        }

        return findings
    }

    private fun isKnownSafeDll(name: String): Boolean {
        val lower = name.lowercase()
        return SAFE_DLL_PREFIXES.any { lower.startsWith(it) } || SAFE_DLL_EXACT.contains(lower)
    }

    companion object {
        const val SCORE_CRITICAL = 100
        const val SCORE_HIGH = 75
        const val SCORE_MEDIUM = 50
        const val SCORE_LOW = 25

        const val THREAT_CRITICAL = 200
        const val THREAT_HIGH = 150
        const val THREAT_MEDIUM = 75
        const val THREAT_LOW = 25

        val KNOWN_SYSTEM_HOOK_LIBS = setOf(
            "kernel32.dll", "user32.dll", "ntdll.dll", "ws2_32.dll",
            "advapi32.dll", "ole32.dll", "oleaut32.dll",
            "dbghelp.dll", "dbgcore.dll", "dbgeng.dll"
        )

        private val SAFE_DLL_PREFIXES = setOf(
            "jansi", "catboost", "rocksdbjni", "librocksdbjni",
            "vcruntime", "msvcp", "msvcrt", "api-ms-win",
            "winmm", "opengl32", "glu32", "d3d", "dwmapi",
            "shlwapi", "shell32", "comctl32", "comdlg32",
            "gdi32", "version", "setupapi", "iphlpapi",
            "wsock", "ws2_", "wship6", "crypt32", "wintrust",
            "bcrypt", "ncrypt", "secur32", "schannel",
            "rasapi32", "rasman", "netapi32", "mpr",
            "winsta", "wtsapi32", "userenv", "profapi",
            "clbcatq", "oleacc", "msutb", "msctf",
            "imm32", "input", "textinput", "coremessaging",
            "twinapi", "windows.", "directx", "dxgi",
            "d3d11", "d3d12", "d3dcompiler", "ddraw",
            "dinput", "dsound", "xinput", "mfplat",
            "mfreadwrite", "mfuuid", "evr", "dshow",
            "quartz", "qedit", "msdmo", "wmvcore",
            "wmasf", "mf", "mshtml", "urlmon",
            "winhttp", "wininet", "cryptsp", "cryptdll",
            "imagehlp", "psapi", "powrprof", "cfgmgr32",
            "devobj", "newdev", "hidsdi", "hid",
            "usb", "usp10", "mlang",
            "normaliz", "kernelbase", "ucrtbase",
            "msvcr", "msvcp", "vcruntime", "concrt",
            "amp", "vcamp", "onecore"
        )

        private val SAFE_DLL_EXACT = setOf(
            "dbghelp.dll", "dbgcore.dll", "dbgeng.dll",
            "kernel32.dll", "user32.dll", "ntdll.dll",
            "advapi32.dll", "gdi32.dll", "shell32.dll",
            "ole32.dll", "oleaut32.dll", "ws2_32.dll",
            "winmm.dll", "opengl32.dll", "msvcrt.dll"
        )
    }
}

enum class ThreatLevel {
    CLEAN, LOW, MEDIUM, HIGH, CRITICAL
}

enum class FindingType {
    INJECTED_CHEAT_DLL,
    SUSPICIOUS_MODULE,
    SUSPICIOUS_MODULE_PATH,
    CHEAT_DLL_IN_DIR,
    PACKED_SUSPICIOUS_DLL,
    PACKED_DLL,
    HOOK_LIBRARY,
    INJECTION_TOOL,
    CODE_CAVE,
    PROCESS_MANIPULATION,
    NETWORK_HOOK,
    DEBUGGER_DETECTED,
    UNSIGNED_LIBRARY,
    REFLECTIVE_INJECTION
}

data class CheatFinding(
    val type: FindingType,
    val description: String,
    val score: Int,
    val evidence: String = ""
)

data class InternalCheatResult(
    val threatLevel: ThreatLevel,
    val totalScore: Int,
    val findings: List<CheatFinding>,
    val shouldBlock: Boolean
)
