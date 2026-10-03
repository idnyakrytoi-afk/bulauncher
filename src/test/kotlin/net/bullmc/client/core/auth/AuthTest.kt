package net.bullmc.client.core.auth

import java.nio.file.Files
import net.bullmc.client.core.loader.LoaderType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthTest {
    @Test fun `loader version survives save and read`() {
        val dir = Files.createTempDirectory("auth-test").toFile()
        try {
            val auth = Auth(dir.resolve("credentials.txt"))
            auth.saveLoaderProfile("1.20.4", LoaderType.FABRIC, "0.15.11", listOf("sodium"))
            assertEquals("0.15.11" to listOf("sodium"), auth.getLoaderProfile("1.20.4", LoaderType.FABRIC))
        } finally { dir.deleteRecursively() }
    }
}
