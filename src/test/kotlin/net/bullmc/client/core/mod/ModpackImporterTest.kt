package net.bullmc.client.core.mod

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.*

class ModpackImporterTest {
    @Test fun `config paths stay relative to game directory`() = runBlocking {
        val root = Files.createTempDirectory("mrpack").toFile()
        try {
            val config = root.resolve("config/settings.txt")
            config.parentFile.mkdirs()
            config.writeText("settings")
            val manifest = MrpackManifest(files = listOf(MrpackFile(
                path = "config/settings.txt", fileSize = config.length(),
                downloads = listOf("https://example.invalid/settings")
            )))
            ModpackImporter.downloadModpackFiles(manifest, root)
            assertEquals("settings", config.readText())
            assertFalse(root.resolve("mods/config/settings.txt").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun `server only files are skipped`() = runBlocking {
        val root = Files.createTempDirectory("mrpack").toFile()
        try {
            ModpackImporter.downloadModpackFiles(MrpackManifest(files = listOf(
                MrpackFile(path = "mods/server.jar", env = mapOf("client" to "unsupported"))
            )), root)
            assertFalse(root.resolve("mods/server.jar").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun `manifest traversal is rejected before download`() = runBlocking {
        val root = Files.createTempDirectory("mrpack").toFile()
        try {
            assertFailsWith<IllegalArgumentException> {
                ModpackImporter.downloadModpackFiles(MrpackManifest(files = listOf(
                    MrpackFile(path = "../outside.jar", downloads = listOf("https://example.invalid/file"))
                )), root)
            }
        } finally { root.deleteRecursively() }
    }
}
