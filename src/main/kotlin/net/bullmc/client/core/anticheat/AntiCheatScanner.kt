package net.bullmc.client.core.anticheat

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class ViolationReport(
    val playerName: String,
    val violationType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AntiCheatScanner(
    private val modsDir: File,
    private val gameDir: File
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    var violations: MutableList<ViolationReport> = mutableListOf()
        private set

    @Volatile
    var lastScanClean: Boolean = false
        private set

    fun scanMods(): ScanResult {
        violations.clear()

        if (!modsDir.exists()) {
            lastScanClean = true
            return ScanResult(clean = true, message = "Папка mods не найдена — чисто")
        }

        val mods = modsDir.listFiles()?.filter { it.extension == "jar" } ?: emptyList()
        if (mods.isEmpty()) {
            lastScanClean = true
            return ScanResult(clean = true, message = "Модов не найдено — чисто")
        }

        for (mod in mods) {
            val fileName = mod.name

            if (CheatDatabase.isModBlacklisted(fileName)) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "BLACKLISTED_MOD",
                        details = "Обнаружен запрещённый мод: $fileName"
                    )
                )
            }

            val innerViolations = CheatDatabase.scanModJarInternals(mod)
            for (detail in innerViolations) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "MOD_METADATA_BLOCKED",
                        details = "$fileName: $detail"
                    )
                )
            }
        }

        val knownBadHashes = mods.filter { mod ->
            val expectedHash = CheatDatabase.allowedModFileHashes[mod.name]
            if (expectedHash != null && expectedHash.isNotEmpty()) {
                val actualHash = computeSha256(mod)
                actualHash != expectedHash
            } else false
        }

        for (mod in knownBadHashes) {
            violations.add(
                ViolationReport(
                    playerName = "",
                    violationType = "MOD_TAMPERED",
                    details = "Мод повреждён/изменён: ${mod.name}"
                )
            )
        }

        lastScanClean = violations.isEmpty()
        val msg = if (violations.isEmpty()) {
            "Все моды прошли проверку (${mods.size} шт.)"
        } else {
            "Обнаружено ${violations.size} нарушений"
        }

        return ScanResult(
            clean = violations.isEmpty(),
            message = msg,
            violations = violations.toList()
        )
    }

    fun scanProcesses(): ProcessScanResult {
        val found = mutableListOf<String>()

        try {
            val os = System.getProperty("os.name").lowercase()
            val processBuilder = if (os.contains("win")) {
                ProcessBuilder("tasklist", "/FO", "CSV")
            } else {
                ProcessBuilder("ps", "aux")
            }
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val lines = output.lines()
            for (line in lines) {
                val lower = line.lowercase()
                for (cheatProcess in CheatDatabase.blacklistedProcesses) {
                    if (lower.contains(cheatProcess.lowercase())) {
                        found.add(cheatProcess)
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (found.isNotEmpty()) {
            for (proc in found) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "CHEAT_PROCESS",
                        details = "Обнаружена чит-программа: $proc"
                    )
                )
            }
        }

        return ProcessScanResult(
            clean = found.isEmpty(),
            foundProcesses = found
        )
    }

    fun scanJvmArgs(args: List<String>): JvmArgScanResult {
        val suspicious = mutableListOf<String>()

        for (arg in args) {
            if (CheatDatabase.suspiciousJvmArgs.any { it.matches(arg) }) {
                suspicious.add(arg)
            }
        }

        if (suspicious.isNotEmpty()) {
            for (arg in suspicious) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "SUSPICIOUS_JVM_ARG",
                        details = "Подозрительный JVM аргумент: $arg"
                    )
                )
            }
        }

        return JvmArgScanResult(
            clean = suspicious.isEmpty(),
            suspiciousArgs = suspicious
        )
    }

    fun scanClasspath(classpath: String): ClasspathScanResult {
        val suspicious = mutableListOf<String>()
        val entries = classpath.split(File.pathSeparator)

        for (entry in entries) {
            val file = File(entry)
            if (file.exists() && file.extension == "jar") {
                if (CheatDatabase.isModBlacklisted(file.name)) {
                    suspicious.add(entry)
                }
            }
        }

        if (suspicious.isNotEmpty()) {
            for (path in suspicious) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "CHEAT_IN_CLASSPATH",
                        details = "Чит в classpath: $path"
                    )
                )
            }
        }

        return ClasspathScanResult(
            clean = suspicious.isEmpty(),
            suspiciousEntries = suspicious
        )
    }

    fun scanGameDir(): GameDirScanResult {
        val suspicious = mutableListOf<String>()

        val dirsToScan = listOf(
            File(gameDir, "mods"),
            File(gameDir, ".fabric"),
        )

        for (dir in dirsToScan) {
            if (dir.exists() && dir.isDirectory) {
                val jars = dir.listFiles()?.filter { it.extension == "jar" } ?: emptyList()
                for (jar in jars) {
                    if (CheatDatabase.isModBlacklisted(jar.name)) {
                        val relPath = dir.name + "/" + jar.name
                        if (!suspicious.contains(relPath)) {
                            suspicious.add(relPath)
                        }
                    }
                }
            }
        }

        if (suspicious.isNotEmpty()) {
            for (path in suspicious) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "CHEAT_IN_GAME_DIR",
                        details = "Чит в game dir: $path"
                    )
                )
            }
        }

        return GameDirScanResult(
            clean = suspicious.isEmpty(),
            suspiciousFiles = suspicious
        )
    }

    fun scanNativeLibraries(): NativeLibScanResult {
        val suspicious = mutableListOf<String>()

        val dirsToScan = mutableListOf(gameDir)
        listOf("bin", "natives", "natives-extracted", "anticheat").forEach { name ->
            val dir = File(gameDir, name)
            if (dir.exists()) dirsToScan.add(dir)
        }
        val tmpDir = System.getProperty("java.io.tmpdir")
        if (tmpDir != null) {
            val tmpBull = File(tmpDir, "bullmc")
            if (tmpBull.exists()) dirsToScan.add(tmpBull)
        }

        val extensions = setOf("dll", "so", "dylib", "jnilib")

        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.isDirectory) continue

            scanDirForNativeLibs(dir, extensions, suspicious)
        }

        if (suspicious.isNotEmpty()) {
            for (path in suspicious) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "SUSPICIOUS_NATIVE_LIB",
                        details = "Подозрительная нативная библиотека: $path"
                    )
                )
            }
        }

        return NativeLibScanResult(
            clean = suspicious.isEmpty(),
            suspiciousLibs = suspicious
        )
    }

    private fun scanDirForNativeLibs(dir: File, extensions: Set<String>, suspicious: MutableList<String>) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in extensions) {
                    if (CheatDatabase.isNativeLibBlacklisted(file.name)) {
                        suspicious.add(file.absolutePath)
                    }
                }
                if (file.isDirectory && file.name != "assets" && file.name != "versions") {
                    scanDirForNativeLibs(file, extensions, suspicious)
                }
            }
        } catch (_: Exception) {
            // Нет доступа к директории — пропускаем
        }
    }

    fun scanTweakClasses(classpath: String): TweakClassScanResult {
        val suspicious = mutableListOf<String>()
        val entries = classpath.split(File.pathSeparator)

        for (entry in entries) {
            val file = File(entry)
            if (!file.exists() || file.extension != "jar") continue

            try {
                java.util.zip.ZipFile(file).use { zip ->
                    zip.entries().asSequence().forEach { jarEntry ->
                        if (jarEntry.name.endsWith(".class")) {
                            val className = jarEntry.name
                                .replace("/", ".")
                                .removeSuffix(".class")
                                .lowercase()

                            if (CheatDatabase.isTweakClassBlacklisted(className)) {
                                suspicious.add("${file.name} -> ${jarEntry.name}")
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Невалидный JAR — пропускаем
            }
        }

        if (suspicious.isNotEmpty()) {
            for (path in suspicious) {
                violations.add(
                    ViolationReport(
                        playerName = "",
                        violationType = "CHEAT_TWEAK_CLASS",
                        details = "Запрещённый tweak-класс: $path"
                    )
                )
            }
        }

        return TweakClassScanResult(
            clean = suspicious.isEmpty(),
            suspiciousClasses = suspicious
        )
    }

    fun fullScan(playerName: String): FullScanResult {
        violations.clear()

        val modScan = scanMods()
        val processScan = scanProcesses()
        val gameDirScan = scanGameDir()
        val nativeLibScan = scanNativeLibraries()

        val allViolations = violations.map {
            it.copy(playerName = playerName)
        }
        violations.clear()
        violations.addAll(allViolations)

        val clean = modScan.clean && processScan.clean && gameDirScan.clean && nativeLibScan.clean

        return FullScanResult(
            clean = clean,
            modScan = modScan,
            processScan = processScan,
            gameDirScan = gameDirScan,
            nativeLibScan = nativeLibScan,
            totalViolations = allViolations.size,
            violations = allViolations
        )
    }

    fun getViolationReportJson(): String {
        return try {
            json.encodeToString(violations)
        } catch (e: Exception) {
            "[]"
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}

data class ScanResult(
    val clean: Boolean,
    val message: String,
    val violations: List<ViolationReport> = emptyList()
)

data class ProcessScanResult(
    val clean: Boolean,
    val foundProcesses: List<String>
)

data class JvmArgScanResult(
    val clean: Boolean,
    val suspiciousArgs: List<String>
)

data class ClasspathScanResult(
    val clean: Boolean,
    val suspiciousEntries: List<String>
)

data class GameDirScanResult(
    val clean: Boolean,
    val suspiciousFiles: List<String>
)

data class NativeLibScanResult(
    val clean: Boolean,
    val suspiciousLibs: List<String>
)

data class TweakClassScanResult(
    val clean: Boolean,
    val suspiciousClasses: List<String>
)

data class FullScanResult(
    val clean: Boolean,
    val modScan: ScanResult,
    val processScan: ProcessScanResult,
    val gameDirScan: GameDirScanResult,
    val nativeLibScan: NativeLibScanResult? = null,
    val totalViolations: Int,
    val violations: List<ViolationReport>
)
