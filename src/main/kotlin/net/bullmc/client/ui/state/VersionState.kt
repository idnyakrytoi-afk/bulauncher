package net.bullmc.client.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.bullmc.client.core.launcher.VersionEntry

/** Фильтр списка версий — как в Legacy Launcher / TLauncher. */
enum class VersionFilter(val displayName: String, val manifestType: String?) {
    ALL("Все", null),
    RELEASE("Release", "release"),
    SNAPSHOT("Snapshot", "snapshot"),
    OLD_BETA("Beta", "old_beta"),
    OLD_ALPHA("Alpha", "old_alpha")
}

class VersionState(
    initialVersion: String = "1.20.4"
) {
    var selectedVersion by mutableStateOf(initialVersion)

    /** Полный список версий из манифеста Mojang. */
    var allVersions by mutableStateOf<List<VersionEntry>>(emptyList())
        private set

    /** Только идентификаторы — для мест, которым не нужен тип версии. */
    val availableVersions: List<String> get() = allVersions.map { it.id }

    fun updateVersions(versions: List<VersionEntry>) {
        allVersions = versions
        if (versions.isNotEmpty() && versions.none { it.id == selectedVersion }) {
            selectedVersion = versions.firstOrNull { it.type == "release" }?.id ?: versions.first().id
        }
    }

    /** Позволяет выбрать любую версию, даже отсутствующую в загруженном списке. */
    fun selectVersion(version: String) {
        if (version.isNotBlank()) selectedVersion = version
    }

    fun typeOf(versionId: String): String =
        allVersions.firstOrNull { it.id == versionId }?.type ?: "release"

    /** Отфильтрованный и отсортированный список для диалога выбора версии. */
    fun filtered(filter: VersionFilter, query: String): List<VersionEntry> {
        val q = query.trim().lowercase()
        return allVersions.filter { entry ->
            val matchesFilter = filter.manifestType == null || entry.type == filter.manifestType
            val matchesQuery = q.isEmpty() || entry.id.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }
}
