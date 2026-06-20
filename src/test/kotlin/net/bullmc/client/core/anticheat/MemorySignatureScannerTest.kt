package net.bullmc.client.core.anticheat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MemorySignatureScannerTest {

    private val scanner = MemorySignatureScanner()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `isKnownCheatDll detects known cheat DLLs`() {
        assertTrue(scanner.isKnownCheatDll("cheatengine-x86_64.dll"))
        assertTrue(scanner.isKnownCheatDll("frida-agent-16.dll"))
        assertTrue(scanner.isKnownCheatDll("injector.exe"))
        assertTrue(scanner.isKnownCheatDll("x64dbg.exe"))
        assertTrue(scanner.isKnownCheatDll("minhook.dll"))
        assertTrue(scanner.isKnownCheatDll("easyhook.dll"))
        assertTrue(scanner.isKnownCheatDll("xenos.exe"))
    }

    @Test
    fun `isKnownCheatDll allows safe DLLs`() {
        assertFalse(scanner.isKnownCheatDll("opengl32.dll"))
        assertFalse(scanner.isKnownCheatDll("lwjgl.dll"))
        assertFalse(scanner.isKnownCheatDll("dbghelp.dll"))
        assertFalse(scanner.isKnownCheatDll("kernel32.dll"))
        assertFalse(scanner.isKnownCheatDll("sodium.dll"))
        assertFalse(scanner.isKnownCheatDll("iris.dll"))
    }

    @Test
    fun `scanForHookSignatures detects detour trampoline x64`() {
        val file = File(tempDir, "test.dll")
        val bytes = byteArrayOf(
            0x48, 0x89.toByte(), 0x5C, 0x24, 0x08,
            0x48, 0x89.toByte(), 0x6C, 0x24, 0x10,
            0x48, 0x89.toByte(), 0x74, 0x24, 0x18,
            0x57,
            0xE9.toByte(), 0x00, 0x00, 0x00, 0x00
        )
        file.writeBytes(bytes)

        val findings = scanner.scanForHookSignatures(file)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.any { it.name.contains("Trampoline") })
    }

    @Test
    fun `scanForHookSignatures detects detour trampoline x86`() {
        val file = File(tempDir, "test.dll")
        val bytes = byteArrayOf(
            0x55,
            0x8B.toByte(), 0xEC.toByte(),
            0xE9.toByte(), 0x00, 0x00, 0x00, 0x00
        )
        file.writeBytes(bytes)

        val findings = scanner.scanForHookSignatures(file)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.any { it.name.contains("Trampoline") })
    }

    @Test
    fun `scanForHookSignatures detects IAT hook`() {
        val file = File(tempDir, "test.dll")
        val bytes = byteArrayOf(
            0xFF.toByte(), 0x25, 0x00, 0x00, 0x00, 0x00
        )
        file.writeBytes(bytes)

        val findings = scanner.scanForHookSignatures(file)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.any { it.name.contains("IAT") })
    }

    @Test
    fun `scanForHookSignatures returns empty for non-existent file`() {
        val file = File(tempDir, "nonexistent.dll")
        val findings = scanner.scanForHookSignatures(file)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `scanForHookSignatures returns empty for tiny file`() {
        val file = File(tempDir, "tiny.dll")
        file.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        val findings = scanner.scanForHookSignatures(file)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `scanForInjectionSignatures detects VirtualAllocEx`() {
        val file = File(tempDir, "inject.dll")
        val content = "\u0000VirtualAllocEx\u0000WriteProcessMemory\u0000CreateRemoteThread\u0000"
        file.writeBytes(content.toByteArray(Charsets.ISO_8859_1))

        val findings = scanner.scanForInjectionSignatures(file)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.any { it.apiName == "VirtualAllocEx" })
    }

    @Test
    fun `scanForInjectionSignatures detects LoadLibrary pattern`() {
        val file = File(tempDir, "loader.dll")
        val content = "\u0000LoadLibraryA\u0000GetProcAddress\u0000"
        file.writeBytes(content.toByteArray(Charsets.ISO_8859_1))

        val findings = scanner.scanForInjectionSignatures(file)
        assertTrue(findings.isNotEmpty())
        assertTrue(findings.any { it.apiName == "LoadLibraryA" })
    }

    @Test
    fun `scanForInjectionSignatures returns empty for clean file`() {
        val file = File(tempDir, "clean.dll")
        val content = "This is a clean file with no injection APIs."
        file.writeBytes(content.toByteArray(Charsets.ISO_8859_1))

        val findings = scanner.scanForInjectionSignatures(file)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `scanForInjectionSignatures returns empty for non-existent file`() {
        val file = File(tempDir, "nonexistent.dll")
        val findings = scanner.scanForInjectionSignatures(file)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `hook severity levels are ordered`() {
        assertTrue(HookSeverity.LOW.ordinal < HookSeverity.MEDIUM.ordinal)
        assertTrue(HookSeverity.MEDIUM.ordinal < HookSeverity.HIGH.ordinal)
        assertTrue(HookSeverity.HIGH.ordinal < HookSeverity.CRITICAL.ordinal)
    }

    @Test
    fun `hook signature data is non-empty`() {
        assertTrue(MemorySignatureScanner.HOOK_SIGNATURES.isNotEmpty())
        assertTrue(MemorySignatureScanner.INJECTION_APIS.isNotEmpty())
    }
}
