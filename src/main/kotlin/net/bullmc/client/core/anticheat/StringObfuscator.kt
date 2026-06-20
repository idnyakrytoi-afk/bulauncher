package net.bullmc.client.core.anticheat

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object StringObfuscator {
    private val KEY = byteArrayOf(
        0x42.toByte(), 0x75.toByte(), 0x6C.toByte(), 0x6C,
        0x4D.toByte(), 0x43, 0x41, 0x6E,
        0x74.toByte(), 0x69, 0x63, 0x68,
        0x65, 0x61, 0x74, 0x53
    )

    private val random = SecureRandom()

    fun obfuscate(value: String): ByteArray {
        val iv = ByteArray(16)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(KEY, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)

        val padded = value.toByteArray()
        val blockSize = 16
        val paddedSize = ((padded.size / blockSize) + 1) * blockSize
        val paddedData = ByteArray(paddedSize)
        padded.copyInto(paddedData)
        paddedData[padded.size] = 0x80.toByte()

        val encrypted = cipher.doFinal(paddedData)
        return iv + encrypted
    }

    fun deobfuscate(data: ByteArray): String {
        val iv = data.copyOfRange(0, 16)
        val encrypted = data.copyOfRange(16, data.size)

        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(KEY, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)

        val decrypted = cipher.doFinal(encrypted)
        val end = decrypted.indexOf(0x80.toByte())
        return if (end >= 0) String(decrypted.copyOfRange(0, end)) else String(decrypted)
    }

    fun obfuscateSet(values: Set<String>): Set<String> {
        return values.map { deobfuscate(obfuscate(it)) }.toSet()
    }
}
