package net.bullmc.client.core.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

@Serializable
data class ServerInfo(
    val ip: String,
    val port: Int = 25565,
    var ping: Long = -1,
    var online: Boolean = false,
    var motd: String = "",
    var version: String = "",
    var playersOnline: Int = 0,
    var playersMax: Int = 0,
    var favicon: String = "",
    var latency: Long = 0,
    val isFavorite: Boolean = false
) {
    fun getDisplayAddress(): String = if (port == 25565) ip else "$ip:$port"
}

object ServerPing {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun pingServer(
        host: String,
        port: Int = 25565,
        timeoutMs: Long = 5000
    ): ServerInfo {
        val info = ServerInfo(ip = host, port = port)
        val startTime = System.currentTimeMillis()

        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs.toInt())
            socket.soTimeout = timeoutMs.toInt()

            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Handshake
            val hostBytes = host.toByteArray()
            val handshake = buildPacket {
                writeVarInt(0) // packet id
                writeVarInt(47) // protocol version
                writeVarInt(hostBytes.size)
                writeBytes(hostBytes)
                writeShort(port)
                writeVarInt(1) // next state: status
            }
            out.write(handshake)
            out.flush()

            // Status request
            val statusReq = buildPacket {
                writeVarInt(0)
            }
            out.write(statusReq)
            out.flush()

            // Status response
            val length = input.readVarInt()
            val packetId = input.readVarInt()
            val jsonLength = input.readVarInt()
            val jsonBytes = ByteArray(jsonLength)
            input.readFully(jsonBytes)
            val responseText = String(jsonBytes)

            val response = json.parseToJsonElement(responseText).jsonObject

            val description = response["description"]
            info.motd = when (description) {
                is JsonObject -> description["text"]?.jsonPrimitive?.content ?: ""
                is JsonPrimitive -> description.content
                else -> ""
            }

            val versionObj = response["version"]?.jsonObject
            info.version = versionObj?.get("name")?.jsonPrimitive?.content ?: ""

            val playersObj = response["players"]?.jsonObject
            info.playersOnline = playersObj?.get("online")?.jsonPrimitive?.int ?: 0
            info.playersMax = playersObj?.get("max")?.jsonPrimitive?.int ?: 0

            info.favicon = response["favicon"]?.jsonPrimitive?.content ?: ""

            val endTime = System.currentTimeMillis()
            info.latency = endTime - startTime
            info.ping = info.latency
            info.online = true

            socket.close()

        } catch (e: Exception) {
            info.online = false
            info.ping = -1
        }

        return info
    }

    private fun buildPacket(block: PacketBuilder.() -> Unit): ByteArray {
        val builder = PacketBuilder()
        builder.block()
        val data = builder.toByteArray()
        val lengthBytes = encodeVarInt(data.size)
        return lengthBytes + data
    }

    private class PacketBuilder {
        private val buffer = java.io.ByteArrayOutputStream()

        fun writeVarInt(value: Int) {
            buffer.write(encodeVarInt(value))
        }

        fun writeBytes(bytes: ByteArray) {
            buffer.write(bytes)
        }

        fun writeShort(value: Int) {
            buffer.write((value shr 8) and 0xFF)
            buffer.write(value and 0xFF)
        }

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }

    private fun encodeVarInt(value: Int): ByteArray {
        val result = mutableListOf<Byte>()
        var temp = value
        do {
            var byte = temp and 0x7F
            temp = temp ushr 7
            if (temp != 0) byte = byte or 0x80
            result.add(byte.toByte())
        } while (temp != 0)
        return result.toByteArray()
    }

    private fun DataInputStream.readVarInt(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = readUnsignedByte()
            result = result or ((byte and 0x7F) shl shift)
            if ((byte and 0x80) == 0) break
            shift += 7
        }
        return result
    }
}
