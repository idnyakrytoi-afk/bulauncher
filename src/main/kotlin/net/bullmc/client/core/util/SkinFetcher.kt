package net.bullmc.client.core.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.RenderingHints

object SkinFetcher {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        followRedirects = true
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val cacheDir = File(LauncherPaths.gameCache, "skins")

    suspend fun getSkinUrl(nickname: String): String? {
        return try {
            val uuidResponse = client.get("https://api.mojang.com/users/profiles/minecraft/$nickname")
            if (uuidResponse.status.value != 200) return null

            val uuidBody = jsonParser.parseToJsonElement(uuidResponse.bodyAsText()).jsonObject
            val uuid = uuidBody["id"]?.jsonPrimitive?.content ?: return null

            val profileResponse = client.get("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
            if (profileResponse.status.value != 200) return null

            val profileBody = jsonParser.parseToJsonElement(profileResponse.bodyAsText()).jsonObject
            val textures = profileBody["textures"]?.jsonObject ?: return null
            val skinEntry = textures["SKIN"]?.jsonObject ?: return null

            skinEntry["url"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("[SKIN] Failed to get skin for $nickname: ${e.message}")
            null
        }
    }

    suspend fun downloadSkin(nickname: String): File? {
        cacheDir.mkdirs()
        val cacheFile = File(cacheDir, "$nickname.png")
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

        val url = getSkinUrl(nickname) ?: return null

        return try {
            val response = client.get(url)
            val bytes = response.readBytes()
            cacheFile.writeBytes(bytes)
            cacheFile
        } catch (e: Exception) {
            println("[SKIN] Failed to download skin: ${e.message}")
            null
        }
    }

    suspend fun getOrPlaceholder(nickname: String): File {
        val skin = downloadSkin(nickname)
        if (skin != null && skin.exists()) return skin

        val placeholder = File(cacheDir, "_placeholder.png")
        if (!placeholder.exists()) {
            val img = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = java.awt.Color(0x30, 0x36, 0x3D)
            g.fillRoundRect(0, 0, 64, 64, 12, 12)
            g.dispose()
            ImageIO.write(img, "png", placeholder)
        }
        return placeholder
    }

    fun getCachedSkin(nickname: String): File? {
        val file = File(cacheDir, "$nickname.png")
        return if (file.exists() && file.length() > 0) file else null
    }
}
