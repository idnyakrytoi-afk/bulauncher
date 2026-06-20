package net.bullmc.client.core.anticheat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AntiCheatScannerTest {

    @TempDir
    lateinit var tempDir: File

    private fun createModJar(name: String, classes: List<String> = emptyList()): File {
        val modsDir = File(tempDir, "mods")
        modsDir.mkdirs()
        val jar = File(modsDir, name)
        ZipOutputStream(FileOutputStream(jar)).use { zos ->
            for (cls in classes) {
                val entry = ZipEntry(cls)
                zos.putNextEntry(entry)
                zos.write(ByteArray(100))
                zos.closeEntry()
            }
            if (classes.isEmpty()) {
                val entry = ZipEntry("META-INF/MANIFEST.MF")
                zos.putNextEntry(entry)
                zos.write("Manifest-Version: 1.0\n".toByteArray())
                zos.closeEntry()
            }
        }
        return jar
    }

    @Test
    fun `scanMods detects blacklisted mod`() {
        createModJar("wurst-3.22.1.jar")
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.scanMods()

        assertFalse(result.clean)
        assertTrue(result.violations.any { it.violationType == "BLACKLISTED_MOD" })
    }

    @Test
    fun `scanMods allows legitimate mods`() {
        createModJar("sodium-fabric-0.6.13+mc1.21.4.jar")
        createModJar("fabric-api-0.119.4+1.21.4.jar")
        createModJar("iris-fabric-1.8.8+mc1.21.4.jar")
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.scanMods()

        assertTrue(result.clean)
    }

    @Test
    fun `scanMods detects cheat classes inside jar`() {
        createModJar("my-cheat.jar", listOf("net/wurst/client/KillAura.class"))
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.scanMods()

        assertFalse(result.clean)
        assertTrue(result.violations.any { it.violationType == "MOD_METADATA_BLOCKED" })
    }

    @Test
    fun `scanMods handles missing mods directory`() {
        val missingDir = File(tempDir, "no-mods-here")
        val scanner = AntiCheatScanner(missingDir, tempDir)
        val result = scanner.scanMods()

        assertTrue(result.clean)
    }

    @Test
    fun `scanMods handles empty mods directory`() {
        val modsDir = File(tempDir, "mods")
        modsDir.mkdirs()
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.scanMods()

        assertTrue(result.clean)
    }

    @Test
    fun `scanProcesses returns clean when no cheat processes`() {
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanProcesses()

        assertNotNull(result)
        assertNotNull(result.foundProcesses)
    }

    @Test
    fun `scanJvmArgs blocks suspicious args`() {
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanJvmArgs(listOf(
            "-javaagent:/some/cheat.jar",
            "-Xdebug",
            "-noverify",
            "-Xmx4G"
        ))

        assertFalse(result.clean)
        assertTrue(result.suspiciousArgs.contains("-javaagent:/some/cheat.jar"))
        assertTrue(result.suspiciousArgs.contains("-Xdebug"))
        assertTrue(result.suspiciousArgs.contains("-noverify"))
    }

    @Test
    fun `scanJvmArgs allows standard args`() {
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanJvmArgs(listOf(
            "-Xmx4G",
            "-Xms2G",
            "-Dfile.encoding=UTF-8",
            "-Djava.library.path=/natives"
        ))

        assertTrue(result.clean)
    }

    @Test
    fun `scanJvmArgs allows bullmc agent`() {
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanJvmArgs(listOf(
            "-javaagent:/game/anticheat/bullmc-anticheat-agent.jar"
        ))

        assertTrue(result.clean)
    }

    @Test
    fun `scanClasspath detects cheat jars in classpath`() {
        val cheatJar = createModJar("wurst-3.22.1.jar")
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanClasspath(cheatJar.absolutePath)

        assertFalse(result.clean)
        assertTrue(result.suspiciousEntries.contains(cheatJar.absolutePath))
    }

    @Test
    fun `scanClasspath allows clean classpath`() {
        val cleanJar = createModJar("sodium-fabric.jar")
        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanClasspath(cleanJar.absolutePath)

        assertTrue(result.clean)
    }

    @Test
    fun `scanGameDir detects cheat mods in game dir`() {
        val modsDir = File(tempDir, "mods")
        modsDir.mkdirs()
        File(modsDir, "wurst-3.22.1.jar").writeBytes(ByteArray(10))

        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.scanGameDir()

        assertFalse(result.clean)
        assertTrue(result.suspiciousFiles.any { it.contains("wurst") })
    }

    @Test
    fun `scanNativeLibraries detects cheat native libs`() {
        val nativeDir = File(tempDir, "natives")
        nativeDir.mkdirs()
        File(nativeDir, "cheatengine-x86_64.dll").writeBytes(ByteArray(10))

        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanNativeLibraries()

        assertFalse(result.clean)
        assertTrue(result.suspiciousLibs.any { it.contains("cheatengine") })
    }

    @Test
    fun `scanNativeLibraries allows clean native libs`() {
        val nativeDir = File(tempDir, "natives")
        nativeDir.mkdirs()
        File(nativeDir, "lwjgl.dll").writeBytes(ByteArray(10))
        File(nativeDir, "opengl32.dll").writeBytes(ByteArray(10))

        val scanner = AntiCheatScanner(File(tempDir, "mods"), tempDir)
        val result = scanner.scanNativeLibraries()

        assertTrue(result.clean)
    }

    @Test
    fun `fullScan aggregates all scan results`() {
        createModJar("sodium-fabric-0.6.13.jar")
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.fullScan("TestPlayer")

        assertNotNull(result)
        assertNotNull(result.modScan)
        assertNotNull(result.processScan)
        assertNotNull(result.gameDirScan)
        assertTrue(result.totalViolations >= 0)
    }

    @Test
    fun `fullScan marks player name on violations`() {
        createModJar("wurst-3.22.1.jar")
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        val result = scanner.fullScan("Cheater123")

        assertTrue(result.violations.all { it.playerName == "Cheater123" })
    }

    @Test
    fun `getViolationReportJson returns valid JSON`() {
        createModJar("wurst-3.22.1.jar")
        val modsDir = File(tempDir, "mods")
        val scanner = AntiCheatScanner(modsDir, tempDir)
        scanner.scanMods()

        val json = scanner.getViolationReportJson()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }
}
