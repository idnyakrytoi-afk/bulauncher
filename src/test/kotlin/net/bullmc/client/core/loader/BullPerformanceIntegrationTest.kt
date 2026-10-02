package net.bullmc.client.core.loader

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.bullmc.client.core.mod.ManagedModRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "BULLMC_INTEGRATION", matches = "true")
class BullPerformanceIntegrationTest {
    @TempDir lateinit var gameDir: File

    @Test
    fun `installs Fabric and the performance preset for Minecraft 1 20 4`() = runBlocking {
        val manager = LoaderManager(gameDir)
        val versionId = manager.ensureLoaderInstalled("1.20.4", LoaderType.FABRIC, "", BullPerformancePreset.modIds)
        assertTrue(File(gameDir, "versions/$versionId/$versionId.json").isFile)
        val modsDir = File(gameDir, "mods")
        val index = Json.decodeFromString<Map<String, ManagedModRecord>>(File(modsDir, ".bullmc-managed-mods.json").readText())
        assertEquals(BullPerformancePreset.modIds.size + 1, index.size)
        assertTrue(index.values.all { File(modsDir, it.fileName).isFile })
    }
}
