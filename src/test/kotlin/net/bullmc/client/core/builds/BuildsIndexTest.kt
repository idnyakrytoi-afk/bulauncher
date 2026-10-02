package net.bullmc.client.core.builds

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверяет публичный индекс сборок builds.json из корня репозитория:
 * он грузится той же схемой, что и лаунчер, без дублей id и пустых полей.
 * Заодно покрывает утилиты модели (formatSize / shortInfo).
 */
class BuildsIndexTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadIndex(): List<CommunityBuild> {
        val file = listOf(File("builds.json"), File("../builds.json"))
            .firstOrNull { it.isFile }
            ?: error("builds.json не найден рядом с проектом")
        return json.decodeFromString(ListSerializer(CommunityBuild.serializer()), file.readText())
    }

    @Test
    fun `index parses into community builds`() {
        val builds = loadIndex()
        assertTrue(builds.isNotEmpty(), "индекс пустой")
        println("Проиндексировано сборок: ${builds.size}")
        builds.forEach { println(" - ${it.name} by ${it.author} [${it.loader}] ${it.mcVersion} mods=${it.mods.size}") }
    }

    @Test
    fun `index has no duplicate ids or empty required fields`() {
        val builds = loadIndex()
        val ids = builds.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "дубли id в builds.json")
        builds.forEach { build ->
            assertTrue(build.name.isNotBlank(), "пустое имя у ${build.id}")
            assertTrue(build.author.isNotBlank(), "пустой автор у ${build.id}")
            assertTrue(build.mcVersion.isNotBlank(), "пустая версия MC у ${build.id}")
            assertTrue(build.mods.isNotEmpty(), "пустой список модов у ${build.id}")
            assertTrue(!build.local, "в публичном индексе сборки не должны быть локальными")
        }
    }

    @Test
    fun `formatSize uses russian pluralization`() {
        fun of(count: Int) = CommunityBuild(name = "n", author = "a", mods = List(count) { "m" }).formatSize()
        assertEquals("1 мод", of(1))
        assertEquals("2 мода", of(2))
        assertEquals("5 модов", of(5))
        assertEquals("11 модов", of(11))
        assertEquals("21 мод", of(21))
        assertEquals("24 мода", of(24))
    }

    @Test
    fun `shortInfo contains version and mod count`() {
        val build = CommunityBuild(name = "n", author = "a", mcVersion = "1.20.4", mods = listOf("sodium"))
        val info = build.shortInfo()
        assertTrue(info.contains("1.20.4"), info)
        assertTrue(info.contains("1 мод"), info)
    }
}
