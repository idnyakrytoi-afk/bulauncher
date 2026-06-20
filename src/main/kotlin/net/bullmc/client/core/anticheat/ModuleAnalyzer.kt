package net.bullmc.client.core.anticheat

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.math.ln
import kotlin.math.pow

class ModuleAnalyzer {

    fun isUnsignedOrSuspicious(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false

        if (!hasValidPeHeader(file)) return true

        if (!hasValidSignature(file)) return true

        if (hasSuspiciousSectionNames(file)) return true

        return false
    }

    fun isFromSuspiciousPath(path: String): Boolean {
        val lower = path.lowercase()
        val suspiciousPaths = listOf(
            "appdata\\local\\temp",
            "appdata\\local\\microsoft\\windows\\inetcache",
            "\\downloads\\",
            "\\recycle",
            "\\appdata\\roaming\\microsoft\\windows\\start menu",
            "\\desktop\\cheat",
            "\\desktop\\hack",
            "\\desktop\\inject",
            "\\tmp\\",
            "\\temp\\"
        )

        return suspiciousPaths.any { lower.contains(it) }
    }

    fun hasSuspiciousSectionNames(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false

        try {
            val raf = RandomAccessFile(file, "r")
            val bytes = ByteArray(minOf(4096, file.length().toInt()))
            raf.readFully(bytes)
            raf.close()

            val content = String(bytes, Charsets.ISO_8859_1)

            val suspiciousSections = listOf(
                ".vmp", ".vmp0", ".vmp1", ".vmp2",
                ".aspack", ".adata", ".aspr",
                ".upx0", ".upx1", ".upx2",
                ".enigma", ".themida",
                ".packed", ".crypt", ".protect",
                ".debug", ".rsrc"
            )

            for (section in suspiciousSections) {
                if (content.contains(section)) {
                    return true
                }
            }
        } catch (_: Exception) {}

        return false
    }

    fun hasHighEntropy(file: File, threshold: Double = 7.0): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 1024) return false

        try {
            val bytes = file.readBytes()
            val sampleSize = minOf(bytes.size, 65536)
            val sample = bytes.copyOf(sampleSize)

            val frequency = IntArray(256)
            for (b in sample) {
                frequency[b.toInt() and 0xFF]++
            }

            var entropy = 0.0
            for (freq in frequency) {
                if (freq > 0) {
                    val probability = freq.toDouble() / sampleSize
                    entropy -= probability * ln(probability) / ln(2.0)
                }
            }

            return entropy > threshold
        } catch (_: Exception) {}

        return false
    }

    fun computeFileHash(file: File): String? {
        if (!file.exists() || !file.isFile) return null

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getPeInfo(file: File): PeInfo? {
        if (!file.exists() || !file.isFile) return null

        return try {
            val raf = RandomAccessFile(file, "r")

            raf.seek(0x3C)
            val peOffset = raf.readInt()

            raf.seek(peOffset.toLong())
            val peSignature = ByteArray(4)
            raf.read(peSignature)
            if (String(peSignature) != "PE${0.toChar()}${0.toChar()}") {
                raf.close()
                return null
            }

            val machine = raf.readUnsignedShort()
            val numberOfSections = raf.readUnsignedShort()
            val timestamp = raf.readInt()

            val characteristics = raf.readUnsignedShort()

            val sections = mutableListOf<SectionInfo>()
            val optHeaderOffset = peOffset + 24
            raf.seek(optHeaderOffset.toLong())

            val magic = raf.readUnsignedShort()
            val is64 = magic == 0x20B

            val headerSize = if (is64) 112 else 96
            raf.seek(optHeaderOffset + headerSize.toLong())

            for (i in 0 until numberOfSections) {
                val nameBytes = ByteArray(8)
                raf.read(nameBytes)
                            val name = String(nameBytes).trim { it.code == 0 }

                val virtualSize = raf.readInt()
                val virtualAddress = raf.readInt()
                val sizeOfRawData = raf.readInt()
                val pointerToRawData = raf.readInt()
                val characteristics = raf.readInt()

                sections.add(SectionInfo(
                    name = name,
                    virtualSize = virtualSize,
                    virtualAddress = virtualAddress,
                    sizeOfRawData = sizeOfRawData,
                    pointerToRawData = pointerToRawData,
                    characteristics = characteristics
                ))
            }

            raf.close()

            PeInfo(
                machine = machine,
                timestamp = timestamp,
                characteristics = characteristics,
                sections = sections,
                is64Bit = is64
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isKnownCheatSignature(file: File): Boolean {
        val peInfo = getPeInfo(file) ?: return false

        for (section in peInfo.sections) {
            val sectionName = section.name.lowercase()
            val knownCheatSections = listOf(
                ".vmp", ".vmp0", ".vmp1", ".vmp2",
                ".themida", ".enigma",
                ".aspack", ".adata"
            )

            if (knownCheatSections.any { sectionName.startsWith(it) }) {
                return true
            }
        }

        if (peInfo.characteristics and 0x2000 != 0) {
            val hasUnusualSections = peInfo.sections.count {
                !it.name.startsWith(".") && it.name.length in 1..3
            }
            if (hasUnusualSections > 0) return true
        }

        return false
    }

    data class PeInfo(
        val machine: Int,
        val timestamp: Int,
        val characteristics: Int,
        val sections: List<SectionInfo>,
        val is64Bit: Boolean
    )

    data class SectionInfo(
        val name: String,
        val virtualSize: Int,
        val virtualAddress: Int,
        val sizeOfRawData: Int,
        val pointerToRawData: Int,
        val characteristics: Int
    )

    private fun hasValidPeHeader(file: File): Boolean {
        if (file.length() < 64) return false

        return try {
            val raf = RandomAccessFile(file, "r")
            val mz = ByteArray(2)
            raf.read(mz)
            val isMz = mz[0] == 'M'.code.toByte() && mz[1] == 'Z'.code.toByte()

            raf.seek(0x3C)
            val peOffset = raf.readInt()
            raf.seek(peOffset.toLong())
            val pe = ByteArray(4)
            raf.read(pe)
            val isPe = String(pe) == "PE${0.toChar()}${0.toChar()}"

            raf.close()
            isMz && isPe
        } catch (_: Exception) {
            false
        }
    }

    private fun hasValidSignature(file: File): Boolean {
        return try {
            val pb = ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "(Get-AuthenticodeSignature '${file.absolutePath}').Status -eq 'Valid'"
            )
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val completed = proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) {
                proc.destroyForcibly()
                return false
            }
            val output = proc.inputStream.bufferedReader().readText().trim()
            output.equals("True", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
