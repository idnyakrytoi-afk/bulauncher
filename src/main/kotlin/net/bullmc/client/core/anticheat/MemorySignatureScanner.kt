package net.bullmc.client.core.anticheat

import java.io.File
import java.io.RandomAccessFile

class MemorySignatureScanner {

    fun isKnownCheatDll(dllName: String): Boolean {
        val lower = dllName.lowercase().removeSuffix(".dll").removeSuffix(".so").removeSuffix(".dylib")

        return CHEAT_DLL_SIGNATURES.any { signature ->
            lower.contains(signature) || lower == signature
        }
    }

    fun scanForHookSignatures(file: File): List<HookSignature> {
        if (!file.exists() || !file.isFile) return emptyList()

        val findings = mutableListOf<HookSignature>()

        try {
            val raf = RandomAccessFile(file, "r")
            val fileSize = minOf(file.length(), 10_000_000)
            val buffer = ByteArray(fileSize.toInt())
            raf.readFully(buffer)
            raf.close()

            for ((pattern, name, severity) in HOOK_SIGNATURES) {
                val offset = findPattern(buffer, pattern)
                if (offset >= 0) {
                    findings.add(HookSignature(
                        name = name,
                        offset = offset,
                        severity = severity,
                        pattern = pattern
                    ))
                }
            }

            val nopSleds = findNopSleds(buffer)
            if (nopSleds.isNotEmpty()) {
                for (sled in nopSleds) {
                    findings.add(HookSignature(
                        name = "NOP SLED",
                        offset = sled.first,
                        severity = HookSeverity.HIGH,
                        pattern = ByteArray(sled.second) { 0x90.toByte() }
                    ))
                }
            }

            val int3Sleds = findInt3Sleds(buffer)
            if (int3Sleds.isNotEmpty()) {
                for (sled in int3Sleds) {
                    findings.add(HookSignature(
                        name = "INT3 SLED (Code Cave)",
                        offset = sled.first,
                        severity = HookSeverity.CRITICAL,
                        pattern = ByteArray(sled.second) { 0xCC.toByte() }
                    ))
                }
            }

        } catch (_: Exception) {}

        return findings
    }

    fun scanForInjectionSignatures(file: File): List<InjectionSignature> {
        if (!file.exists() || !file.isFile) return emptyList()

        val findings = mutableListOf<InjectionSignature>()

        try {
            val bytes = file.readBytes()
            val content = String(bytes, Charsets.ISO_8859_1)

            for ((apiName, severity) in INJECTION_APIS) {
                var idx = 0
                while (true) {
                    val found = content.indexOf(apiName, idx, ignoreCase = true)
                    if (found == -1) break

                    findings.add(InjectionSignature(
                        apiName = apiName,
                        offset = found,
                        severity = severity
                    ))

                    idx = found + apiName.length
                    if (findings.size > 100) break
                }
                if (findings.size > 100) break
            }

            val peSignature = scanPeForSuspiciousImports(content)
            findings.addAll(peSignature)

        } catch (_: Exception) {}

        return findings
    }

    fun scanPeForSuspiciousImports(content: String): List<InjectionSignature> {
        val findings = mutableListOf<InjectionSignature>()

        val suspiciousImports = listOf(
            "LoadLibraryA", "LoadLibraryW", "LoadLibraryExA", "LoadLibraryExW",
            "GetProcAddress",
            "VirtualAlloc", "VirtualAllocEx", "VirtualProtect", "VirtualProtectEx",
            "WriteProcessMemory", "ReadProcessMemory",
            "CreateRemoteThread", "NtCreateThreadEx",
            "QueueUserAPC", "NtQueueApcThread",
            "SetWindowsHookExA", "SetWindowsHookExW",
            "OpenProcess", "OpenThread",
            "NtOpenProcess", "NtOpenThread",
            "NtWriteVirtualMemory", "NtReadVirtualMemory",
            "ZwWriteVirtualMemory", "ZwReadVirtualMemory",
            "LdrLoadDll", "LdrGetProcedureAddress",
            "NtMapViewOfSection", "NtCreateSection",
            "RtlCreateUserThread",
            "NtSetInformationThread",
            "IsDebuggerPresent", "CheckRemoteDebuggerPresent",
            "NtQueryInformationProcess",
            "FindWindowA", "FindWindowW",
            "EnumWindows", "GetWindowTextA",
            "GetAsyncKeyState", "GetKeyState",
            "GetDC", "GetWindowDC", "ReleaseDC",
            "BitBlt", "GetDIBits", "GetPixel"
        )

        for (api in suspiciousImports) {
            var idx = 0
            while (true) {
                val found = content.indexOf(api, idx, ignoreCase = true)
                if (found == -1) break

                val isLikelyImport = found > 0 && (
                    content[found - 1] == '\u0000' ||
                    content[found - 1] == ' ' ||
                    content[found - 1] == '(' ||
                    content[found - 1] == ','
                )

                if (isLikelyImport) {
                    findings.add(InjectionSignature(
                        apiName = api,
                        offset = found,
                        severity = HookSeverity.HIGH
                    ))
                }

                idx = found + api.length
                if (findings.size > 200) break
            }
            if (findings.size > 200) break
        }

        return findings
    }

    private fun findPattern(buffer: ByteArray, pattern: ByteArray): Int {
        if (pattern.isEmpty() || buffer.size < pattern.size) return -1

        for (i in 0..buffer.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (buffer[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun findNopSleds(buffer: ByteArray): List<Pair<Int, Int>> {
        val sleds = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var count = 0

        for (i in buffer.indices) {
            if (buffer[i] == 0x90.toByte()) {
                if (start == -1) start = i
                count++
            } else {
                if (count >= 20) {
                    sleds.add(Pair(start, count))
                }
                start = -1
                count = 0
            }
        }

        if (count >= 20) {
            sleds.add(Pair(start, count))
        }

        return sleds
    }

    private fun findInt3Sleds(buffer: ByteArray): List<Pair<Int, Int>> {
        val sleds = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var count = 0

        for (i in buffer.indices) {
            if (buffer[i] == 0xCC.toByte()) {
                if (start == -1) start = i
                count++
            } else {
                if (count >= 10) {
                    sleds.add(Pair(start, count))
                }
                start = -1
                count = 0
            }
        }

        if (count >= 10) {
            sleds.add(Pair(start, count))
        }

        return sleds
    }

    companion object {
        val CHEAT_DLL_SIGNATURES = setOf(
            "cheat", "hack", "inject", "hook", "exploit",
            "injector", "loader", "bootstrap",
            "vmp", "themida", "enigma",
            "frida", "xposed", "substrate",
            "minhook", "easyhook", "detours",
            "x96dbg", "x64dbg", "x32dbg",
            "olly", "ida", "idaq",
            "dnspy", "dnSpy",
            "processhack", "procmon",
            "wireshark", "fiddler",
            "dumpcap", "tcpdump",
            "mempatch", "memhack",
            "extreme", "xenos", "minject"
        )

        val HOOK_SIGNATURES = listOf(
            Triple(byteArrayOf(
                0x48, 0x89.toByte(), 0x5C, 0x24, 0x08,
                0x48, 0x89.toByte(), 0x6C, 0x24, 0x10,
                0x48, 0x89.toByte(), 0x74, 0x24, 0x18,
                0x57,
                0xE9.toByte()
            ), "Detour Trampoline (x64)", HookSeverity.CRITICAL),
            Triple(byteArrayOf(
                0x55,
                0x8B.toByte(), 0xEC.toByte(),
                0xE9.toByte()
            ), "Detour Trampoline (x86)", HookSeverity.CRITICAL),
            Triple(byteArrayOf(
                0xFF.toByte(), 0x25, 0x00, 0x00, 0x00, 0x00
            ), "JMP [addr] (IAT Hook)", HookSeverity.HIGH),
            Triple(byteArrayOf(
                0xE9.toByte()
            ), "JMP (Inline Hook)", HookSeverity.MEDIUM),
            Triple(byteArrayOf(
                0xEB.toByte()
            ), "Short JMP (Hook)", HookSeverity.LOW),
            Triple(byteArrayOf(
                0xFF.toByte(), 0x15
            ), "CALL [addr] (IAT)", HookSeverity.MEDIUM),
            Triple(byteArrayOf(
                0x68, 0x00, 0x00, 0x00, 0x00,
                0xC3.toByte()
            ), "PUSH addr; RET (Trampoline)", HookSeverity.HIGH)
        )

        val INJECTION_APIS = listOf(
            "LoadLibraryA" to HookSeverity.HIGH,
            "LoadLibraryW" to HookSeverity.HIGH,
            "LoadLibraryExA" to HookSeverity.HIGH,
            "LoadLibraryExW" to HookSeverity.HIGH,
            "GetProcAddress" to HookSeverity.MEDIUM,
            "VirtualAllocEx" to HookSeverity.CRITICAL,
            "VirtualProtectEx" to HookSeverity.HIGH,
            "WriteProcessMemory" to HookSeverity.CRITICAL,
            "ReadProcessMemory" to HookSeverity.HIGH,
            "CreateRemoteThread" to HookSeverity.CRITICAL,
            "NtCreateThreadEx" to HookSeverity.CRITICAL,
            "QueueUserAPC" to HookSeverity.HIGH,
            "SetWindowsHookExA" to HookSeverity.HIGH,
            "SetWindowsHookExW" to HookSeverity.HIGH,
            "OpenProcess" to HookSeverity.MEDIUM,
            "NtOpenProcess" to HookSeverity.HIGH,
            "NtWriteVirtualMemory" to HookSeverity.CRITICAL,
            "NtReadVirtualMemory" to HookSeverity.CRITICAL,
            "ZwWriteVirtualMemory" to HookSeverity.CRITICAL,
            "ZwReadVirtualMemory" to HookSeverity.CRITICAL,
            "LdrLoadDll" to HookSeverity.HIGH,
            "LdrGetProcedureAddress" to HookSeverity.MEDIUM,
            "NtMapViewOfSection" to HookSeverity.HIGH,
            "NtCreateSection" to HookSeverity.HIGH,
            "RtlCreateUserThread" to HookSeverity.CRITICAL,
            "NtSetInformationThread" to HookSeverity.MEDIUM,
            "IsDebuggerPresent" to HookSeverity.LOW,
            "CheckRemoteDebuggerPresent" to HookSeverity.MEDIUM,
            "NtQueryInformationProcess" to HookSeverity.MEDIUM
        )
    }
}

enum class HookSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class HookSignature(
    val name: String,
    val offset: Int,
    val severity: HookSeverity,
    val pattern: ByteArray
)

data class InjectionSignature(
    val apiName: String,
    val offset: Int,
    val severity: HookSeverity
)
