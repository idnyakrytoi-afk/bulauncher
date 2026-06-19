package net.bullmc.client.core

import java.io.File

class Auth {
    private val credentialsFile: File = LauncherPaths.credentials

    init {
        LauncherPaths.init()
    }

    fun savePlayerNick(nick: String) {
        val lines = readLines().filter { !it.startsWith("nick=") }
        writeLines(lines + "nick=$nick")
        
        // Добавляем в профили если его нет
        val profiles = getProfiles()
        if (!profiles.contains(nick)) {
            val profileLines = readLines().filter { !it.startsWith("profiles=") }
            val newProfiles = profiles + nick
            writeLines(profileLines + "profiles=${newProfiles.joinToString("|")}")
        }
    }

    fun getPlayerNick(): String? = readLines().find { it.startsWith("nick=") }?.substringAfter("nick=")
    
    fun getProfiles(): List<String> {
        val profiles = readLines().find { it.startsWith("profiles=") }?.substringAfter("profiles=") ?: ""
        return if (profiles.isEmpty()) emptyList() else profiles.split("|")
    }
    
    fun setPlayerNickFromProfile(nick: String) {
        val lines = readLines().filter { !it.startsWith("nick=") }
        writeLines(lines + "nick=$nick")
    }

    fun saveLicense(license: String) {
        val lines = readLines().filter { !it.startsWith("license=") }
        writeLines(lines + "license=$license")
    }

    fun getLicense(): String? = readLines().find { it.startsWith("license=") }?.substringAfter("license=")

    fun saveSettings(ramMb: Int, javaPath: String) {
        val lines = readLines().filter { !it.startsWith("ram=") && !it.startsWith("java=") }
        writeLines(lines + "ram=$ramMb" + "java=$javaPath")
    }

    fun getRamMb(): Int {
        return readLines().find { it.startsWith("ram=") }?.substringAfter("ram=")?.toIntOrNull() ?: 4096
    }

    fun getJavaPath(): String {
        val bundled = File(LauncherPaths.jre, "jdk-21.0.3/bin/java.exe")
        if (bundled.exists()) return bundled.absolutePath

        val bundledUnix = File(LauncherPaths.jre, "jdk-21.0.3/bin/java")
        if (bundledUnix.exists()) return bundledUnix.absolutePath

        val saved = readLines().find { it.startsWith("java=") }?.substringAfter("java=")
        if (!saved.isNullOrEmpty()) return saved

        return "java"
    }

    fun saveJavaPath(javaPath: String) {
        val lines = readLines().filter { !it.startsWith("java=") }
        writeLines(lines + "java=$javaPath")
    }

    fun saveTheme(themeName: String) {
        val lines = readLines().filter { !it.startsWith("theme=") }
        writeLines(lines + "theme=$themeName")
    }

    fun getTheme(): String {
        return readLines().find { it.startsWith("theme=") }?.substringAfter("theme=") ?: "DARK"
    }

    fun setDefaultServer(ip: String) {
        val lines = readLines().filter { !it.startsWith("defaultServer=") }
        writeLines(lines + "defaultServer=$ip")
    }

    fun getDefaultServer(): String {
        return readLines().find { it.startsWith("defaultServer=") }?.substringAfter("defaultServer=") ?: "play.bullmc.net"
    }

    fun getConfigDir(): File = LauncherPaths.root

    private fun readLines(): List<String> {
        return try {
            if (credentialsFile.exists()) credentialsFile.readLines() else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun writeLines(lines: List<String>) {
        try { credentialsFile.writeText(lines.joinToString("\n")) } catch (_: Exception) {}
    }
}
