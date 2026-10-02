package net.bullmc.client.core.mod

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.bullmc.client.core.loader.LoaderType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger

class ModrinthApiTest {
    @TempDir lateinit var modsDir: File

    @Test
    fun `downloads primary jar and verifies its hash`() = runBlocking {
        val bytes = "valid mod jar".toByteArray()
        val hash = sha512(bytes)
        val versions = """[{"files":[
            {"filename":"secondary.jar","url":"https://files.example/secondary.jar","primary":false,"hashes":{"sha512":"$hash"}},
            {"filename":"primary.jar","url":"https://files.example/primary.jar","primary":true,"hashes":{"sha512":"$hash"}}
        ]}]"""
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "api.modrinth.com" -> respond(versions)
                "files.example" -> respond("valid mod jar")
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val installed = ModrinthApi(client).downloadMod("sodium", "1.20.4", LoaderType.FABRIC, modsDir)
        assertEquals("primary.jar", installed.name)
        assertTrue(installed.readBytes().contentEquals(bytes))
        assertFalse(File(modsDir, "secondary.jar").exists())
    }

    @Test
    fun `rejects corrupt download without leaving a jar`() = runBlocking {
        val versions = """[{"files":[{"filename":"sodium.jar","url":"https://files.example/mod.jar","primary":true,"hashes":{"sha512":"bad"}}]}]"""
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "api.modrinth.com") respond(versions) else respond("corrupt")
        })
        assertFailsWith<IllegalStateException> {
            ModrinthApi(client).downloadMod("sodium", "1.20.4", LoaderType.FABRIC, modsDir)
        }
        assertFalse(File(modsDir, "sodium.jar").exists())
    }

    @Test
    fun `reports missing compatible version`() = runBlocking {
        val client = HttpClient(MockEngine { respond("[]") })
        val error = assertFailsWith<IllegalStateException> {
            ModrinthApi(client).downloadMod("sodium", "26.4-snapshot", LoaderType.FABRIC, modsDir)
        }
        assertTrue(error.message.orEmpty().contains("26.4-snapshot"))
    }

    @Test
    fun `replaces the previous managed version without touching other mods`() = runBlocking {
        val requests = AtomicInteger()
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "api.modrinth.com") {
                val version = requests.incrementAndGet()
                val bytes = "version $version".toByteArray()
                respond("""[{"files":[{"filename":"sodium-$version.jar","url":"https://files.example/$version.jar","primary":true,"hashes":{"sha512":"${sha512(bytes)}"}}]}]""")
            } else {
                respond("version ${request.url.encodedPath.substringAfterLast('/').substringBefore('.')}" )
            }
        })
        val personalMod = File(modsDir, "personal.jar").apply { writeText("keep me") }
        val api = ModrinthApi(client)
        api.downloadMod("sodium", "1.20.4", LoaderType.FABRIC, modsDir)
        api.downloadMod("sodium", "1.20.5", LoaderType.FABRIC, modsDir)
        assertFalse(File(modsDir, "sodium-1.jar").exists())
        assertTrue(File(modsDir, "sodium-2.jar").exists())
        assertEquals("keep me", personalMod.readText())
    }

    @Test
    fun `uses verified cache without network on the next launch`() = runBlocking {
        val bytes = "cached mod".toByteArray()
        val requests = AtomicInteger()
        val versions = """[{"files":[{"filename":"cached.jar","url":"https://files.example/cached.jar","primary":true,"hashes":{"sha512":"${sha512(bytes)}"}}]}]"""
        val client = HttpClient(MockEngine { request ->
            requests.incrementAndGet()
            if (request.url.host == "api.modrinth.com") respond(versions) else respond("cached mod")
        })
        val api = ModrinthApi(client)
        api.downloadMod("sodium", "1.20.4", LoaderType.FABRIC, modsDir)
        val afterFirstLaunch = requests.get()
        api.downloadMod("sodium", "1.20.4", LoaderType.FABRIC, modsDir)
        assertEquals(afterFirstLaunch, requests.get())
    }

    private fun sha512(bytes: ByteArray): String = MessageDigest.getInstance("SHA-512")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
