package net.bullmc.client.core.util

import kotlinx.serialization.json.*
import java.io.*
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

data class DiscordPresence(
    val details: String = "",
    val state: String = "",
    val startTimestamp: Long = 0,
    val largeImageKey: String = "",
    val largeImageText: String = "",
    val smallImageKey: String = "",
    val smallImageText: String = ""
)

class DiscordRPC {
    companion object {
        private const val DISCORD_APP_ID = "1277366554925342752"
    }

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    @Volatile
    var isConnected = false
        private set

    fun connect(): Boolean {
        if (isConnected) return true
        try {
            val pipePath = findDiscordPipe() ?: return false
            val localSocket = Socket("localhost", 0)
            val channel = java.nio.channels.SocketChannel.open()
            channel.connect(java.net.UnixDomainSocketAddress.of(Paths.get(pipePath)))
            socket = localSocket
            outputStream = DataOutputStream(BufferedOutputStream(channel.socket().getOutputStream()))
            inputStream = DataInputStream(BufferedInputStream(channel.socket().getInputStream()))
            isConnected = true
            println("[DISCORD] Connected via $pipePath")
            return true
        } catch (e: Exception) {
            println("[DISCORD] Could not connect: ${e.message}")
            return false
        }
    }

    private fun findDiscordPipe(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val isMac = System.getProperty("os.name").lowercase().contains("mac")

        val candidates = mutableListOf<String>()

        if (isWindows) {
            for (i in 0..9) {
                candidates.add("\\\\.\\pipe\\discord-ipc-$i")
            }
        } else if (isMac) {
            val home = System.getProperty("user.home")
            candidates.add("$home/Library/Application Support/discord-ipc-0")
        } else {
            val xdg = System.getenv("XDG_RUNTIME_DIR") ?: "/run/user/${ProcessHandle.current().pid()}"
            candidates.add("$xdg/discord-ipc-0")
            candidates.add("/tmp/discord-ipc-0")
        }

        for (path in candidates) {
            if (java.io.File(path).exists() || path.contains("\\\\.\\pipe\\")) {
                return path
            }
        }
        return null
    }

    fun handshake() {
        if (!isConnected) return
        try {
            val handshake = buildJsonObject {
                put("v", 1)
                put("client_id", DISCORD_APP_ID)
            }
            sendFrame(0, handshake.toString().toByteArray())
            readFrame()
            println("[DISCORD] Handshake OK")
        } catch (e: Exception) {
            println("[DISCORD] Handshake failed: ${e.message}")
            isConnected = false
        }
    }

    fun setActivity(presence: DiscordPresence) {
        if (!isConnected) return
        try {
            val activity = buildJsonObject {
                put("details", presence.details)
                put("state", presence.state)
                if (presence.startTimestamp > 0) {
                    put("timestamps", buildJsonObject {
                        put("start", presence.startTimestamp)
                    })
                }
                put("assets", buildJsonObject {
                    if (presence.largeImageKey.isNotEmpty()) {
                        put("large_image", presence.largeImageKey)
                        put("large_text", presence.largeImageText)
                    }
                    if (presence.smallImageKey.isNotEmpty()) {
                        put("small_image", presence.smallImageKey)
                        put("small_text", presence.smallImageText)
                    }
                })
            }

            val payload = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                put("args", buildJsonObject {
                    put("pid", ProcessHandle.current().pid())
                    put("activity", activity)
                })
                put("nonce", System.currentTimeMillis().toString())
            }

            sendFrame(1, payload.toString().toByteArray())
            readFrame()
        } catch (e: Exception) {
            println("[DISCORD] Set activity failed: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            val payload = buildJsonObject {
                put("cmd", "CLOSE")
                put("args", buildJsonObject {})
                put("nonce", System.currentTimeMillis().toString())
            }
            sendFrame(2, payload.toString().toByteArray())
        } catch (_: Exception) {}
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        isConnected = false
    }

    private fun sendFrame(opcode: Int, data: ByteArray) {
        val out = outputStream ?: return
        out.writeInt(data.size)
        out.writeInt(opcode)
        out.write(data)
        out.flush()
    }

    private fun readFrame(): String? {
        val input = inputStream ?: return null
        return try {
            input.readInt()
            input.readInt()
            val len = input.readInt()
            val data = ByteArray(len)
            input.readFully(data)
            String(data)
        } catch (e: Exception) { null }
    }
}

object DiscordManager {
    private val rpc = DiscordRPC()
    private var startTime = System.currentTimeMillis() / 1000

    fun init() {
        try {
            if (rpc.connect()) {
                rpc.handshake()
            }
        } catch (e: Exception) {
            println("[DISCORD] Init failed: ${e.message}")
        }
    }

    fun setPlaying(version: String, serverIp: String? = null) {
        rpc.setActivity(DiscordPresence(
            details = if (serverIp != null) "\u041d\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0435" else "\u0412 \u043c\u0435\u043d\u044e",
            state = if (serverIp != null) serverIp else "BullMC Client",
            startTimestamp = startTime,
            largeImageKey = "bullmc_logo",
            largeImageText = "BullMC Client",
            smallImageKey = "mc_$version",
            smallImageText = "Minecraft $version"
        ))
    }

    fun setIdle() {
        rpc.setActivity(DiscordPresence(
            details = "BullMC Client",
            state = "\u0412 \u043b\u0430\u0443\u043d\u0447\u0435\u0440\u0435",
            startTimestamp = startTime,
            largeImageKey = "bullmc_logo",
            largeImageText = "BullMC Client"
        ))
    }

    fun setDownloading(message: String) {
        rpc.setActivity(DiscordPresence(
            details = "\u041f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u043a\u0430 \u043a \u0437\u0430\u043f\u0443\u0441\u043a\u0443",
            state = message,
            startTimestamp = startTime,
            largeImageKey = "bullmc_logo",
            largeImageText = "BullMC Client"
        ))
    }

    fun shutdown() {
        rpc.disconnect()
    }
}
