package net.bullmc.client.core.anticheat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class CheatDatabaseTest {

    @Test
    fun `blacklisted mod names contain known cheats`() {
        assertTrue("wurst" in CheatDatabase.blacklistedModNames)
        assertTrue("meteor-client" in CheatDatabase.blacklistedModNames)
        assertTrue("baritone" in CheatDatabase.blacklistedModNames)
        assertTrue("impact" in CheatDatabase.blacklistedModNames)
        assertTrue("vape" in CheatDatabase.blacklistedModNames)
    }

    @Test
    fun `isModBlacklisted detects cheat jar names via segment match`() {
        assertTrue(CheatDatabase.isModBlacklisted("wurst-3.22.1.jar"))
        assertTrue(CheatDatabase.isModBlacklisted("meteor-client-0.5.8.jar"))
        assertTrue(CheatDatabase.isModBlacklisted("baritone-1.10.jar"))
        assertTrue(CheatDatabase.isModBlacklisted("impact-1.12.jar"))
    }

    @Test
    fun `isModBlacklisted detects cheat via regex patterns`() {
        assertTrue(CheatDatabase.isModBlacklisted("wurst-client.jar"))
        assertTrue(CheatDatabase.isModBlacklisted("meteor-client-mod.jar"))
        assertTrue(CheatDatabase.isModBlacklisted("vape-lite.jar"))
    }

    @Test
    fun `isModBlacklisted allows legit mods`() {
        assertFalse(CheatDatabase.isModBlacklisted("sodium-fabric-0.6.13+mc1.21.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("fabric-api-0.119.4+1.21.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("iris-fabric-1.8.8+mc1.21.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("lithium-fabric-0.15.3+mc1.21.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("entityculling-fabric-1.10.4-mc1.21.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("cloth-config-17.0.144-fabric.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("modmenu-13.0.4.jar"))
        assertFalse(CheatDatabase.isModBlacklisted("fullbrightnesstoggle-1.21.4-4.3.jar"))
    }

    @Test
    fun `isModWhitelisted detects allowed mods`() {
        assertTrue(CheatDatabase.isModWhitelisted("sodium-fabric-0.6.13.jar", "sodium"))
        assertTrue(CheatDatabase.isModWhitelisted("fabric-api-0.119.4.jar", "fabric-api"))
        assertTrue(CheatDatabase.isModWhitelisted("iris-fabric-1.8.8.jar", "iris"))
        assertTrue(CheatDatabase.isModWhitelisted("lithium-fabric.jar", "lithium"))
        assertTrue(CheatDatabase.isModWhitelisted("bulltweaks-1.0.0.jar", "bulltweaks"))
    }

    @Test
    fun `isModWhitelisted rejects unknown mods`() {
        assertFalse(CheatDatabase.isModWhitelisted("some-random-mod.jar", null))
        assertFalse(CheatDatabase.isModWhitelisted("unknown-mod.jar", "unknown"))
    }

    @Test
    fun `isNativeLibBlacklisted detects cheat libraries`() {
        assertTrue(CheatDatabase.isNativeLibBlacklisted("cheatengine-x86_64.dll"))
        assertTrue(CheatDatabase.isNativeLibBlacklisted("frida-agent.dll"))
        assertTrue(CheatDatabase.isNativeLibBlacklisted("minhook.dll"))
        assertTrue(CheatDatabase.isNativeLibBlacklisted("easyhook.dll"))
        assertTrue(CheatDatabase.isNativeLibBlacklisted("xposed-hook.so"))
    }

    @Test
    fun `isNativeLibBlacklisted allows system libraries`() {
        assertFalse(CheatDatabase.isNativeLibBlacklisted("kernel32.dll"))
        assertFalse(CheatDatabase.isNativeLibBlacklisted("opengl32.dll"))
        assertFalse(CheatDatabase.isNativeLibBlacklisted("lwjgl.dll"))
    }

    @Test
    fun `isTweakClassBlacklisted detects cheat tweak classes`() {
        assertTrue(CheatDatabase.isTweakClassBlacklisted("wurst.client.modules"))
        assertTrue(CheatDatabase.isTweakClassBlacklisted("meteor.client"))
        assertTrue(CheatDatabase.isTweakClassBlacklisted("impact"))
        assertTrue(CheatDatabase.isTweakClassBlacklisted("baritone"))
    }

    @Test
    fun `isTweakClassBlacklisted allows legit tweak classes`() {
        assertFalse(CheatDatabase.isTweakClassBlacklisted("fabric.rendering"))
        assertFalse(CheatDatabase.isTweakClassBlacklisted("lithium.ai"))
    }

    @Test
    fun `suspiciousJvmArgs blocks debug agents`() {
        assertTrue(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-javaagent:/some/cheat.jar") })
        assertTrue(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-Xdebug") })
        assertTrue(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-noverify") })
        assertTrue(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-Xverify:none") })
    }

    @Test
    fun `suspiciousJvmArgs allows bullmc agent`() {
        assertFalse(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-javaagent:/game/anticheat/bullmc-anticheat-agent.jar") })
    }

    @Test
    fun `suspiciousJvmArgs allows standard JVM args`() {
        assertFalse(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-Xmx4G") })
        assertFalse(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-Xms2G") })
        assertFalse(CheatDatabase.suspiciousJvmArgs.any { it.containsMatchIn("-Dfile.encoding=UTF-8") })
    }

    @Test
    fun `scanModJarInternals detects cheat classes in jar`() {
        val tempJar = File.createTempFile("test-cheat", ".jar")
        try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(tempJar)).use { zos ->
                val entry = java.util.zip.ZipEntry("net/wurst/client/modules/KillAura.class")
                zos.putNextEntry(entry)
                zos.write(ByteArray(100))
                zos.closeEntry()
            }
            val violations = CheatDatabase.scanModJarInternals(tempJar)
            assertTrue(violations.isNotEmpty())
            assertTrue(violations.any { it.contains("KillAura") || it.contains("wurst") })
        } finally {
            tempJar.delete()
        }
    }

    @Test
    fun `scanModJarInternals allows legit mod internals`() {
        val tempJar = File.createTempFile("test-legit", ".jar")
        try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(tempJar)).use { zos ->
                val entry = java.util.zip.ZipEntry("net/fabricmc/impl/FabricLoaderImpl.class")
                zos.putNextEntry(entry)
                zos.write(ByteArray(100))
                zos.closeEntry()
            }
            val violations = CheatDatabase.scanModJarInternals(tempJar)
            assertTrue(violations.isEmpty())
        } finally {
            tempJar.delete()
        }
    }

    @Test
    fun `scanModJarInternals detects blacklisted metadata names`() {
        val tempJar = File.createTempFile("test-metadata", ".jar")
        try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(tempJar)).use { zos ->
                val entry = java.util.zip.ZipEntry("fabric.mod.json")
                zos.putNextEntry(entry)
                zos.write("""{"id":"wurst","name":"Wurst Client"}""".toByteArray())
                zos.closeEntry()
            }
            val violations = CheatDatabase.scanModJarInternals(tempJar)
            assertTrue(violations.isNotEmpty())
            assertTrue(violations.any { it.contains("fabric.mod.json") || it.contains("wurst") })
        } finally {
            tempJar.delete()
        }
    }

    @Test
    fun `allowed mod IDs cover common performance mods`() {
        assertTrue("sodium" in CheatDatabase.allowedModIds)
        assertTrue("lithium" in CheatDatabase.allowedModIds)
        assertTrue("iris" in CheatDatabase.allowedModIds)
        assertTrue("fabric-api" in CheatDatabase.allowedModIds)
        assertTrue("modmenu" in CheatDatabase.allowedModIds)
        assertTrue("bulltweaks" in CheatDatabase.allowedModIds)
        assertTrue("bullobjects" in CheatDatabase.allowedModIds)
    }
}
