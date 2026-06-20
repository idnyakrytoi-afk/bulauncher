package net.bullmc.client.core.mod

enum class ModSource(val displayName: String) {
    MODRINTH("Modrinth"),
    CURSEFORGE("CurseForge")
}

data class BrowserMod(
    val id: String,
    val slug: String = "",
    val name: String,
    val description: String = "",
    val body: String = "",
    val iconUrl: String? = null,
    val source: ModSource = ModSource.MODRINTH,
    val downloads: Long = 0,
    val author: String = "",
    val categories: List<String> = emptyList(),
    val mcVersions: List<String> = emptyList(),
    val dateModified: String = "",
    val clientSide: String = "",
    val serverSide: String = "",
    val installed: Boolean = false,
    val downloadUrl: String? = null,
    val fileName: String? = null
)
