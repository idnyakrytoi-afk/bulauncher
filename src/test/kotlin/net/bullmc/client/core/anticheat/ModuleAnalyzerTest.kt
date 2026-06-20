package net.bullmc.client.core.anticheat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile

class ModuleAnalyzerTest {

    private val analyzer = ModuleAnalyzer()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `hasValidPeHeader rejects tiny file`() {
        val file = File(tempDir, "tiny.dll")
        file.writeBytes(byteArrayOf(0x4D, 0x5A))
        assertTrue(analyzer.isUnsignedOrSuspicious(file))
    }

    @Test
    fun `hasValidPeHeader rejects non-PE file`() {
        val file = File(tempDir, "notpe.dll")
        file.writeBytes("This is not a PE file at all, just some text content here".toByteArray())
        assertTrue(analyzer.isUnsignedOrSuspicious(file))
    }

    @Test
    fun `hasValidPeHeader rejects zero-byte file`() {
        val file = File(tempDir, "empty.dll")
        file.writeBytes(ByteArray(0))
        assertTrue(analyzer.isUnsignedOrSuspicious(file))
    }

    @Test
    fun `hasSuspiciousSectionNames detects VMP sections`() {
        val file = File(tempDir, "packed.dll")
        val content = ByteArray(4096)
        ".vmp0".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertTrue(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasSuspiciousSectionNames detects UPX sections`() {
        val file = File(tempDir, "packed.dll")
        val content = ByteArray(4096)
        ".upx0".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertTrue(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasSuspiciousSectionNames detects themida sections`() {
        val file = File(tempDir, "packed.dll")
        val content = ByteArray(4096)
        ".themida".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertTrue(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasSuspiciousSectionNames detects aspack sections`() {
        val file = File(tempDir, "packed.dll")
        val content = ByteArray(4096)
        ".aspack".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertTrue(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasSuspiciousSectionNames allows clean sections`() {
        val file = File(tempDir, "clean.dll")
        val content = ByteArray(4096)
        ".text".toByteArray().copyInto(content, 0)
        ".rdata".toByteArray().copyInto(content, 8)
        ".data".toByteArray().copyInto(content, 16)
        file.writeBytes(content)
        assertFalse(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasSuspiciousSectionNames handles empty file`() {
        val file = File(tempDir, "empty.dll")
        file.writeBytes(ByteArray(0))
        assertFalse(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `hasHighEntropy detects packed file`() {
        val file = File(tempDir, "packed.bin")
        val bytes = ByteArray(65536) { (Math.random() * 256).toInt().toByte() }
        file.writeBytes(bytes)
        assertTrue(analyzer.hasHighEntropy(file))
    }

    @Test
    fun `hasHighEntropy allows zeroed file`() {
        val file = File(tempDir, "clean.bin")
        val bytes = ByteArray(65536) { 0x00 }
        file.writeBytes(bytes)
        assertFalse(analyzer.hasHighEntropy(file))
    }

    @Test
    fun `hasHighEntropy returns false for small file`() {
        val file = File(tempDir, "small.bin")
        file.writeBytes(ByteArray(100) { 0x42 })
        assertFalse(analyzer.hasHighEntropy(file))
    }

    @Test
    fun `hasHighEntropy returns false for non-existent file`() {
        val file = File(tempDir, "nonexistent.bin")
        assertFalse(analyzer.hasHighEntropy(file))
    }

    @Test
    fun `computeFileHash returns consistent hash`() {
        val file = File(tempDir, "test.bin")
        file.writeBytes("hello world".toByteArray())

        val hash1 = analyzer.computeFileHash(file)
        val hash2 = analyzer.computeFileHash(file)

        assertNotNull(hash1)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1!!.length)
    }

    @Test
    fun `computeFileHash returns null for non-existent file`() {
        val file = File(tempDir, "nonexistent.bin")
        assertNull(analyzer.computeFileHash(file))
    }

    @Test
    fun `computeFileHash produces different hashes for different content`() {
        val file1 = File(tempDir, "a.bin")
        val file2 = File(tempDir, "b.bin")
        file1.writeBytes("content A".toByteArray())
        file2.writeBytes("content B".toByteArray())

        val hash1 = analyzer.computeFileHash(file1)
        val hash2 = analyzer.computeFileHash(file2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `getPeInfo returns null for non-existent file`() {
        val file = File(tempDir, "nonexistent.dll")
        assertNull(analyzer.getPeInfo(file))
    }

    @Test
    fun `isFromSuspiciousPath detects windows temp paths`() {
        assertTrue(analyzer.isFromSuspiciousPath("C:\\Users\\user\\AppData\\Local\\Temp\\cheat.dll"))
    }

    @Test
    fun `isFromSuspiciousPath detects windows download paths`() {
        assertTrue(analyzer.isFromSuspiciousPath("C:\\Users\\user\\Downloads\\hack.exe"))
    }

    @Test
    fun `isFromSuspiciousPath detects linux temp paths`() {
        assertTrue(analyzer.isFromSuspiciousPath("/tmp/inject.so"))
    }

    @Test
    fun `isFromSuspiciousPath allows safe windows paths`() {
        assertFalse(analyzer.isFromSuspiciousPath("C:\\Windows\\System32\\kernel32.dll"))
    }

    @Test
    fun `isFromSuspiciousPath allows safe linux paths`() {
        assertFalse(analyzer.isFromSuspiciousPath("/usr/lib/jvm/lib/server/libjvm.so"))
    }

    @Test
    fun `isKnownCheatSignature detects VMP packed file via section scan`() {
        val file = File(tempDir, "cheat.dll")
        val content = ByteArray(4096)
        ".vmp0".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertTrue(analyzer.hasSuspiciousSectionNames(file))
    }

    @Test
    fun `isKnownCheatSignature returns false for non-PE file`() {
        val file = File(tempDir, "notpe.dll")
        file.writeBytes("Not PE content without any suspicious sections".toByteArray())
        assertFalse(analyzer.isKnownCheatSignature(file))
    }

    @Test
    fun `isKnownCheatSignature allows clean file`() {
        val file = File(tempDir, "clean.dll")
        val content = ByteArray(4096)
        ".text".toByteArray().copyInto(content, 0)
        file.writeBytes(content)
        assertFalse(analyzer.hasSuspiciousSectionNames(file))
    }
}
