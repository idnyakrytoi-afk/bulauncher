package net.bullmc.client.core.util

import java.nio.file.Files
import kotlin.test.*
import org.junit.jupiter.api.Test

class SafePathsTest {
    @Test fun `reject archive paths outside root`() {
        val root = Files.createTempDirectory("safe-paths").toFile()
        try {
            for (path in listOf("../escape", "..\\escape", "/absolute", "C:\\absolute", "")) {
                assertFailsWith<IllegalArgumentException> { safeDestination(root, path) }
            }
            assertEquals(root.resolve("config/settings.json").canonicalFile,
                safeDestination(root, "config/settings.json"))
        } finally { root.deleteRecursively() }
    }
}
