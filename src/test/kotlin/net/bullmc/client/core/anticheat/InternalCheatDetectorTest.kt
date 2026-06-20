package net.bullmc.client.core.anticheat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class InternalCheatDetectorTest {

    @Test
    fun `threat levels are ordered correctly`() {
        assertTrue(ThreatLevel.CLEAN.ordinal < ThreatLevel.LOW.ordinal)
        assertTrue(ThreatLevel.LOW.ordinal < ThreatLevel.MEDIUM.ordinal)
        assertTrue(ThreatLevel.MEDIUM.ordinal < ThreatLevel.HIGH.ordinal)
        assertTrue(ThreatLevel.HIGH.ordinal < ThreatLevel.CRITICAL.ordinal)
    }

    @Test
    fun `score thresholds are consistent`() {
        assertTrue(InternalCheatDetector.SCORE_LOW < InternalCheatDetector.SCORE_MEDIUM)
        assertTrue(InternalCheatDetector.SCORE_MEDIUM < InternalCheatDetector.SCORE_HIGH)
        assertTrue(InternalCheatDetector.SCORE_HIGH < InternalCheatDetector.SCORE_CRITICAL)
    }

    @Test
    fun `threat thresholds are consistent`() {
        assertTrue(InternalCheatDetector.THREAT_LOW < InternalCheatDetector.THREAT_MEDIUM)
        assertTrue(InternalCheatDetector.THREAT_MEDIUM < InternalCheatDetector.THREAT_HIGH)
        assertTrue(InternalCheatDetector.THREAT_HIGH < InternalCheatDetector.THREAT_CRITICAL)
    }

    @Test
    fun `known system hook libs contain Windows DLLs`() {
        assertTrue("kernel32.dll" in InternalCheatDetector.KNOWN_SYSTEM_HOOK_LIBS)
        assertTrue("ntdll.dll" in InternalCheatDetector.KNOWN_SYSTEM_HOOK_LIBS)
        assertTrue("dbghelp.dll" in InternalCheatDetector.KNOWN_SYSTEM_HOOK_LIBS)
        assertTrue("dbgcore.dll" in InternalCheatDetector.KNOWN_SYSTEM_HOOK_LIBS)
    }

    @Test
    fun `runFullDetection returns valid result for clean game dir`() {
        val gameDir = File(System.getProperty("java.io.tmpdir"), "bullmc-test-clean")
        gameDir.mkdirs()
        try {
            val detector = InternalCheatDetector(gameDir)
            val result = detector.runFullDetection()

            assertNotNull(result)
            assertNotNull(result.threatLevel)
            assertNotNull(result.findings)
            assertTrue(result.totalScore >= 0)

            if (result.findings.isEmpty()) {
                assertEquals(ThreatLevel.CLEAN, result.threatLevel)
                assertFalse(result.shouldBlock)
            }
        } finally {
            gameDir.deleteRecursively()
        }
    }

    @Test
    fun `finding types cover all detection categories`() {
        val types = FindingType.values()
        assertTrue(types.contains(FindingType.INJECTED_CHEAT_DLL))
        assertTrue(types.contains(FindingType.SUSPICIOUS_MODULE))
        assertTrue(types.contains(FindingType.CHEAT_DLL_IN_DIR))
        assertTrue(types.contains(FindingType.PACKED_SUSPICIOUS_DLL))
        assertTrue(types.contains(FindingType.HOOK_LIBRARY))
        assertTrue(types.contains(FindingType.INJECTION_TOOL))
        assertTrue(types.contains(FindingType.CODE_CAVE))
        assertTrue(types.contains(FindingType.PROCESS_MANIPULATION))
        assertTrue(types.contains(FindingType.NETWORK_HOOK))
        assertTrue(types.contains(FindingType.DEBUGGER_DETECTED))
        assertTrue(types.contains(FindingType.REFLECTIVE_INJECTION))
    }

    @Test
    fun `cheatFinding has required fields`() {
        val finding = CheatFinding(
            type = FindingType.INJECTED_CHEAT_DLL,
            description = "Test cheat DLL",
            score = 100,
            evidence = "cheat.dll"
        )

        assertEquals(FindingType.INJECTED_CHEAT_DLL, finding.type)
        assertEquals("Test cheat DLL", finding.description)
        assertEquals(100, finding.score)
        assertEquals("cheat.dll", finding.evidence)
    }

    @Test
    fun `cheatFinding default evidence is empty`() {
        val finding = CheatFinding(
            type = FindingType.HOOK_LIBRARY,
            description = "Hook detected",
            score = 50
        )
        assertEquals("", finding.evidence)
    }

    @Test
    fun `internalCheatResult reflects findings correctly`() {
        val cleanResult = InternalCheatResult(
            threatLevel = ThreatLevel.CLEAN,
            totalScore = 0,
            findings = emptyList(),
            shouldBlock = false
        )
        assertEquals(ThreatLevel.CLEAN, cleanResult.threatLevel)
        assertFalse(cleanResult.shouldBlock)
        assertTrue(cleanResult.findings.isEmpty())

        val criticalResult = InternalCheatResult(
            threatLevel = ThreatLevel.CRITICAL,
            totalScore = 500,
            findings = listOf(
                CheatFinding(FindingType.INJECTED_CHEAT_DLL, "Cheat DLL", 200)
            ),
            shouldBlock = true
        )
        assertEquals(ThreatLevel.CRITICAL, criticalResult.threatLevel)
        assertTrue(criticalResult.shouldBlock)
        assertEquals(1, criticalResult.findings.size)
    }
}
