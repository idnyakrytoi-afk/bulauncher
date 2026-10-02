package net.bullmc.client.core.builds

import net.bullmc.client.core.loader.LoaderType
import net.bullmc.client.core.mod.ModrinthApi
import net.bullmc.client.core.profile.GameProfile
import net.bullmc.client.core.profile.ProfileManager
import java.io.File

/**
 * Установка сборки: создаёт/обновляет профиль и докачивает недостающие моды из Modrinth.
 */
object BuildInstaller {
    private val api = ModrinthApi()

    /**
     * Устанавливает сборку в отдельный профиль (или в существующий, если сборка уже ставилась).
     *
     * @param onProgress (сообщение, доля 0..1)
     */
    suspend fun install(
        build: CommunityBuild,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): BuildInstallResult {
        return try {
            onProgress("Подготовка профиля...", 0.02f)

            val profileName = build.name
            val existing = ProfileManager.getProfiles().find { it.name == profileName }
            val profile: GameProfile = if (existing != null) {
                existing
            } else {
                val created = GameProfile(
                    name = profileName,
                    mcVersion = build.mcVersion,
                    loaderType = build.loader,
                    loaderVersion = build.loaderVersion,
                    enabledMods = build.mods,
                    ramMb = build.ramMb
                )
                ProfileManager.createProfile(created)
                created
            }

            // Синхронизируем настройки профиля со сборкой
            ProfileManager.updateProfile(profile.id) {
                mcVersion = build.mcVersion
                loaderType = build.loader
                loaderVersion = build.loaderVersion
                enabledMods = build.mods
                ramMb = build.ramMb
                name = profileName
            }
            ProfileManager.setActiveProfile(profile.id)

            val modsDir: File = profile.getModsDir()
            modsDir.mkdirs()

            if (build.mods.isEmpty()) {
                onProgress("Сборка без модов", 1f)
                return BuildInstallResult.Success(0, emptyList())
            }

            if (build.loader == LoaderType.VANILLA) {
                onProgress("Vanilla-сборка: моды не скачиваются", 1f)
                return BuildInstallResult.Success(0, emptyList())
            }

            val failed = mutableListOf<String>()
            var installed = 0
            val total = build.mods.size

            build.mods.forEachIndexed { index, slug ->
                onProgress("Скачивание ${index + 1}/$total: $slug", 0.05f + (index.toFloat() / total) * 0.9f)
                val ok = try {
                    api.downloadModById(slug, build.mcVersion, build.loader, modsDir)
                } catch (e: Exception) {
                    println("[BUILDS] Ошибка мода $slug: ${e.message}")
                    false
                }
                if (ok) installed++ else failed.add(slug)
            }

            BuildsRepository.incrementDownloads(build.id)
            onProgress(if (failed.isEmpty()) "Сборка установлена" else "Установлено с ошибками", 1f)
            BuildInstallResult.Success(installed, failed)
        } catch (e: Exception) {
            println("[BUILDS] Ошибка установки: ${e.message}")
            BuildInstallResult.Error(e.message ?: "неизвестная ошибка")
        }
    }
}
