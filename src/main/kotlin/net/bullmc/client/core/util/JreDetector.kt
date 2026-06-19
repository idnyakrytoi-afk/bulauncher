package net.bullmc.client.core.util

import java.io.File

object JreDetector {
    private val commonJavaPaths = listOf(
        // Windows
        "C:/Program Files/Java",
        "C:/Program Files (x86)/Java",
        "C:/Program Files/Eclipse Adoptium",
        "C:/Program Files/Microsoft",
        "C:/Program Files/Zulu",
        // macOS
        "/Library/Java/JavaVirtualMachines",
        "/opt/homebrew/opt/openjdk",
        // Linux
        "/usr/lib/jvm",
        "/usr/java",
        System.getProperty("user.home") + "/.sdkman/candidates/java"
    )

    data class JavaInstall(
        val path: String,
        val version: String,
        val majorVersion: Int,
        val isBundled: Boolean = false
    )

    fun detectAll(): List<JavaInstall> {
        val installs = mutableListOf<JavaInstall>()

        // Проверяем bundled JRE
        val bundled = detectBundled()
        if (bundled != null) installs.add(bundled)

        // Проверяем системные установки
        commonJavaPaths.forEach { basePath ->
            val base = File(basePath)
            if (base.exists()) {
                base.listFiles()?.forEach { dir ->
                    detectJavaInDir(dir)?.let { installs.add(it) }
                }
            }
        }

        // Проверяем PATH
        detectFromPath()?.let { installs.add(it) }

        // Проверяем JAVA_HOME
        detectFromJavaHome()?.let { installs.add(it) }

        return installs.distinctBy { it.version }.sortedByDescending { it.majorVersion }
    }

    fun detectBestForVersion(mcVersion: String): JavaInstall? {
        val installs = detectAll()
        val mcMajor = parseMcMajorVersion(mcVersion)

        return when {
            mcMajor >= 17 -> installs.find { it.majorVersion >= 17 }
                ?: installs.find { it.majorVersion >= 11 }
                ?: installs.firstOrNull()
            mcMajor >= 11 -> installs.find { it.majorVersion in 8..17 }
                ?: installs.firstOrNull()
            else -> installs.find { it.majorVersion <= 8 }
                ?: installs.firstOrNull()
        }
    }

    fun detectBundled(): JavaInstall? {
        val bundledDir = LauncherPaths.jre
        if (!bundledDir.exists()) return null

        val javaExe = findJavaExecutable(bundledDir) ?: return null
        val version = getJavaVersion(javaExe) ?: return null

        return JavaInstall(
            path = javaExe,
            version = version,
            majorVersion = parseMajorVersion(version),
            isBundled = true
        )
    }

    fun detectFromPath(): JavaInstall? {
        return try {
            val process = ProcessBuilder("java", "-version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val version = extractVersion(output) ?: return null
            val javaHome = System.getProperty("java.home") ?: return null
            val javaExe = findJavaExecutable(File(javaHome).parentFile) ?: return null

            JavaInstall(
                path = javaExe,
                version = version,
                majorVersion = parseMajorVersion(version)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun detectFromJavaHome(): JavaInstall? {
        val javaHome = System.getenv("JAVA_HOME") ?: return null
        val dir = File(javaHome)
        if (!dir.exists()) return null

        val javaExe = findJavaExecutable(dir) ?: return null
        val version = getJavaVersion(javaExe) ?: return null

        return JavaInstall(
            path = javaExe,
            version = version,
            majorVersion = parseMajorVersion(version)
        )
    }

    private fun detectJavaInDir(dir: File): JavaInstall? {
        val javaExe = findJavaExecutable(dir) ?: return null
        val version = getJavaVersion(javaExe) ?: return null

        return JavaInstall(
            path = javaExe,
            version = version,
            majorVersion = parseMajorVersion(version)
        )
    }

    private fun findJavaExecutable(dir: File): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val exeName = if (isWindows) "java.exe" else "java"

        // Прямой путь
        val direct = File(dir, exeName)
        if (direct.exists()) return direct.absolutePath

        // bin/java
        val binJava = File(dir, "bin/$exeName")
        if (binJava.exists()) return binJava.absolutePath

        // Рекурсивный поиск (ограниченный)
        dir.listFiles()?.take(5)?.forEach { sub ->
            val found = findJavaExecutable(sub)
            if (found != null) return found
        }

        return null
    }

    private fun getJavaVersion(javaExe: String): String? {
        return try {
            val process = ProcessBuilder(javaExe, "-version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            extractVersion(output)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVersion(output: String): String? {
        val regex = Regex("\"([^\"]+)\"")
        return regex.find(output)?.groupValues?.get(1)
    }

    fun parseMajorVersion(version: String): Int {
        return try {
            val parts = version.split(".")
            val first = parts[0].toIntOrNull() ?: return 0
            if (first > 10) first
            else parts.getOrNull(1)?.toIntOrNull() ?: first
        } catch (_: Exception) {
            0
        }
    }

    private fun parseMcMajorVersion(mcVersion: String): Int {
        val parts = mcVersion.split(".")
        return parts.getOrNull(1)?.toIntOrNull() ?: 0
    }

    fun getRequirementText(mcVersion: String): String {
        val major = parseMcMajorVersion(mcVersion)
        return when {
            major >= 17 -> "Требуется Java 17+"
            major >= 11 -> "Требуется Java 11-17"
            else -> "Требуется Java 8"
        }
    }
}
