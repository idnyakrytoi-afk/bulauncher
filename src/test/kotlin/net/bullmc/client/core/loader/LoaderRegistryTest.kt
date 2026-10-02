package net.bullmc.client.core.loader

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoaderRegistryTest {
    @Test
    fun `Fabric stable version comes from fetched metadata`() {
        val versions = listOf(LoaderVersionEntry("0.19.6-beta", false), LoaderVersionEntry("0.19.5", true))
        assertEquals("0.19.5", LoaderRegistry.resolveVersion(LoaderChannel.STABLE, LoaderType.FABRIC, "1.20.4", "", versions))
        assertEquals("", LoaderRegistry.resolveVersion(LoaderChannel.STABLE, LoaderType.FABRIC, "1.20.4", "", emptyList()))
    }

    @Test
    fun `performance preset only contains unique Fabric mods`() {
        assertEquals(BullPerformancePreset.modIds.size, BullPerformancePreset.modIds.distinct().size)
        val fabricIds = LoaderRegistry.getModsForLoader(LoaderType.FABRIC).map { it.id }.toSet()
        assertTrue(BullPerformancePreset.modIds.all { it in fabricIds })
    }
}
