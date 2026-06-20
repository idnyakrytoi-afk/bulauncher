package net.bullmc.client.core.launcher

import net.bullmc.client.core.auth.Auth
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL

class MinecraftLauncher(
    private val gameDir: File,
    private val auth: Auth
) {
    private val versionsDir = File(gameDir, "versions")
    private val librariesDir = File(gameDir, "libraries")
    private val assetsDir = File(gameDir, "assets")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun launch(
        version: String,
        playerNick: String,
        javaPath: String = "java",
        ramMb: Int = 4096,
        serverIp: String? = null,
        serverPort: Int = 25565,
        extraJvmArgs: List<String> = emptyList()
    ): Process? {
        return try {
            auth.savePlayerNick(playerNick)

            val versionDir = File(versionsDir, version)
            val versionJsonFile = File(versionDir, "$version.json")

            if (!versionJsonFile.exists()) {
                return null
            }

            var rawJson = Json.parseToJsonElement(versionJsonFile.readText()).jsonObject

            val inheritsFrom = rawJson["inheritsFrom"]?.jsonPrimitive?.content
            var parentJson: JsonObject? = null
            var clientJar = File(versionDir, "$version.jar")

            if (inheritsFrom != null) {
                val parentJsonFile = File(versionsDir, "$inheritsFrom/$inheritsFrom.json")
                val parentJarFile = File(versionsDir, "$inheritsFrom/$inheritsFrom.jar")

                if (parentJsonFile.exists()) {
                    parentJson = Json.parseToJsonElement(parentJsonFile.readText()).jsonObject
                }

                if (!clientJar.exists() && parentJarFile.exists()) {
                    clientJar = parentJarFile
                }
            }

            if (!clientJar.exists()) {
                return null
            }

            val mainClass = rawJson["mainClass"]?.jsonPrimitive?.content
                ?: parentJson?.get("mainClass")?.jsonPrimitive?.content
                ?: throw IllegalStateException("mainClass не найден в version.json")

            val classpath = buildClasspath(version, inheritsFrom)
            val nativesDir = findOrExtractNatives(version)

            val ramMB = ramMb.coerceIn(1024, 16384)
            
            val offlineUuid = java.util.UUID.nameUUIDFromBytes("OfflinePlayer:$playerNick".toByteArray()).toString()

            val fakeToken = java.util.UUID.nameUUIDFromBytes("AccessToken:$playerNick".toByteArray()).toString()

            val replacements = mapOf(
                "auth_player_name" to playerNick,
                "version_name" to version,
                "game_directory" to gameDir.absolutePath,
                "assets_root" to assetsDir.absolutePath,
                "assets_index_name" to (rawJson["assets"]?.jsonPrimitive?.content ?: parentJson?.get("assets")?.jsonPrimitive?.content ?: "1.21"),
                "auth_uuid" to offlineUuid,
                "auth_access_token" to fakeToken,
                "clientid" to offlineUuid,
                "auth_xuid" to "0",
                "user_type" to "legacy",
                "version_type" to "release",
                "resolution_width" to "854",
                "resolution_height" to "480",
                "natives_directory" to (nativesDir ?: ""),
                "launcher_name" to "BullMC",
                "launcher_version" to "1.0"
            )

            val jvmArgs = mutableListOf<String>()
            jvmArgs.add(javaPath)
            jvmArgs.add("-Xmx${ramMB}M")
            jvmArgs.add("-Xms${(ramMB / 2).coerceAtLeast(512)}M")

            jvmArgs.add("-XX:+DisableAttachMechanism")
            jvmArgs.add("-Djdk.attach.allowAttachSelf=false")

            // Парсинг аргументов JVM
            val allJvmArgs = mutableListOf<JsonElement>()
            parentJson?.get("arguments")?.jsonObject?.get("jvm")?.let { if (it is JsonArray) allJvmArgs.addAll(it) }
            rawJson["arguments"]?.jsonObject?.get("jvm")?.let { if (it is JsonArray) allJvmArgs.addAll(it) }

            var skipNextJvm = false
            for (element in allJvmArgs) {
                if (skipNextJvm) { skipNextJvm = false; continue }
                when (element) {
                    is JsonPrimitive -> {
                        val content = element.content
                        if (content == "-cp" || content == "\${classpath}") {
                            if (content == "-cp") skipNextJvm = true
                            continue
                        }
                        val resolved = resolvePlaceholders(content, replacements)
                        if (!isSuspiciousJvmArg(resolved)) {
                            jvmArgs.add(resolved)
                        }
                    }
                    is JsonObject -> {
                        if (!rulesMatch(element)) continue
                        val value = element["value"]
                        when (value) {
                            is JsonPrimitive -> {
                                val content = value.content
                                if (content == "-cp" || content == "\${classpath}") {
                                    if (content == "-cp") skipNextJvm = true
                                    continue
                                }
                                val resolved = resolvePlaceholders(content, replacements)
                                if (!isSuspiciousJvmArg(resolved)) {
                                    jvmArgs.add(resolved)
                                }
                            }
                            is JsonArray -> {
                                var innerSkip = false
                                for (v in value) {
                                    if (innerSkip) { innerSkip = false; continue }
                                    if (v is JsonPrimitive) {
                                        val content = v.content
                                        if (content == "-cp" || content == "\${classpath}") {
                                            if (content == "-cp") innerSkip = true
                                            continue
                                        }
                                        val resolved = resolvePlaceholders(content, replacements)
                                        if (!isSuspiciousJvmArg(resolved)) {
                                            jvmArgs.add(resolved)
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                    else -> {}
                }
            }

            val tempDir = System.getProperty("java.io.tmpdir") ?: System.getenv("TEMP") ?: gameDir.absolutePath
            jvmArgs.add("-Djava.io.tmpdir=$tempDir")

            if (nativesDir == null) {
                jvmArgs.removeAll { it.startsWith("-Djava.library.path=") || it.startsWith("-Djna.tmpdir=") || it.startsWith("-Dorg.lwjgl.system.SharedLibraryExtractPath=") || it.startsWith("-Dio.netty.native.workdir=") }
            }

            for (extraArg in extraJvmArgs) {
                if (!isSuspiciousJvmArg(extraArg)) {
                    jvmArgs.add(extraArg)
                }
            }

            jvmArgs.addAll(listOf("-cp", classpath))

            // Парсинг аргументов ИГРЫ
            val rawGameArgs = mutableListOf<String>()
            val allGameArgs = mutableListOf<JsonElement>()
            parentJson?.get("arguments")?.jsonObject?.get("game")?.let { if (it is JsonArray) allGameArgs.addAll(it) }
            rawJson["arguments"]?.jsonObject?.get("game")?.let { if (it is JsonArray) allGameArgs.addAll(it) }

            if (allGameArgs.isEmpty()) {
                 val mcArgs = rawJson["minecraftArguments"]?.jsonPrimitive?.content ?: parentJson?.get("minecraftArguments")?.jsonPrimitive?.content
                 if (mcArgs != null) {
                     rawGameArgs.addAll(mcArgs.split(" ").map { resolvePlaceholders(it, replacements) })
                 } else {
                     // Страховочный вариант для старых версий
                     rawGameArgs.addAll(listOf(
                         "--username", playerNick, "--version", version,
                         "--gameDir", gameDir.absolutePath, "--assetsDir", assetsDir.absolutePath,
                         "--assetIndex", (rawJson["assets"]?.jsonPrimitive?.content ?: "1.21"),
                          "--uuid", replacements["auth_uuid"]!!, "--accessToken", fakeToken,
                          "--userType", "legacy", "--versionType", "release"
                     ))
                 }
            } else {
                for (element in allGameArgs) {
                    when (element) {
                        is JsonPrimitive -> rawGameArgs.add(resolvePlaceholders(element.content, replacements))
                        is JsonObject -> {
                            if (!rulesMatch(element)) continue
                            val value = element["value"]
                            when (value) {
                                is JsonPrimitive -> rawGameArgs.add(resolvePlaceholders(value.content, replacements))
                                is JsonArray -> {
                                    for (v in value) {
                                        if (v is JsonPrimitive) rawGameArgs.add(resolvePlaceholders(v.content, replacements))
                                    }
                                }
                                else -> {} // Добавлено для компилятора
                            }
                        }
                        else -> {} // Добавлено для компилятора
                    }
                }
            }

            if (serverIp != null) {
                rawGameArgs.add("--server")
                rawGameArgs.add(serverIp)
                rawGameArgs.add("--port")
                rawGameArgs.add(serverPort.toString())
            }

            rawGameArgs.removeAll { it == "--demo" }

            // УМНОЕ УДАЛЕНИЕ ДУБЛИКАТОВ АРГУМЕНТОВ
            val finalGameArgs = mutableListOf<String>()
            var i = 0
            while (i < rawGameArgs.size) {
                val arg = rawGameArgs[i]
                if (arg.startsWith("--")) {
                    val existingIndex = finalGameArgs.indexOf(arg)
                    if (existingIndex != -1) {
                        // Если аргумент уже есть, обновляем его значение
                        if (i + 1 < rawGameArgs.size && !rawGameArgs[i + 1].startsWith("--")) {
                            if (existingIndex + 1 < finalGameArgs.size && !finalGameArgs[existingIndex + 1].startsWith("--")) {
                                finalGameArgs[existingIndex + 1] = rawGameArgs[i + 1]
                            } else {
                                finalGameArgs.add(existingIndex + 1, rawGameArgs[i + 1])
                            }
                            i += 2
                        } else {
                            i += 1
                        }
                        continue
                    }
                }
                finalGameArgs.add(arg)
                i++
            }

            val allArgs = jvmArgs + listOf(mainClass) + finalGameArgs

            val processBuilder = ProcessBuilder(allArgs)
            processBuilder.directory(gameDir)
            processBuilder.redirectErrorStream(true)
            processBuilder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolvePlaceholders(arg: String, replacements: Map<String, String>): String {
        var result = arg
        for ((key, value) in replacements) {
            result = result.replace("\${$key}", value)
        }
        return result
    }

    private fun rulesMatch(element: JsonObject): Boolean {
        val rules = element["rules"] as? JsonArray ?: return true
        for (rule in rules) {
            val ruleObj = rule as? JsonObject ?: continue
            val action = ruleObj["action"]?.jsonPrimitive?.content ?: "allow"
            val os = ruleObj["os"]?.jsonObject
            if (os != null) {
                val osName = os["name"]?.jsonPrimitive?.content
                val currentOs = when {
                    System.getProperty("os.name").lowercase().contains("windows") -> "windows"
                    System.getProperty("os.name").lowercase().contains("mac") -> "osx"
                    else -> "linux"
                }
                val matches = osName == null || osName == currentOs
                if (action == "allow" && !matches) return false
                if (action == "disallow" && matches) return false
            }
        }
        return true
    }

    private fun findOrExtractNatives(version: String): String? {
        val candidates = listOf(
            File(librariesDir, "natives"),
            File(versionsDir, "$version/natives"),
            File(gameDir, "natives/$version")
        )
        for (dir in candidates) {
            if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) {
                return dir.absolutePath
            }
        }

        val nativesDir = File(gameDir, "natives-extracted/$version")
        nativesDir.mkdirs()

        val versionJsonFile = File(versionsDir, "$version/$version.json")
        if (versionJsonFile.exists()) {
            val rawJson = Json.parseToJsonElement(versionJsonFile.readText()).jsonObject
            val libraries = rawJson["libraries"] as? JsonArray ?: return null

            val currentOs = when {
                System.getProperty("os.name").lowercase().contains("windows") -> "windows"
                System.getProperty("os.name").lowercase().contains("mac") -> "osx"
                else -> "linux"
            }

            for (libElement in libraries) {
                val lib = libElement as? JsonObject ?: continue
                if (!rulesMatchLibrary(lib)) continue

                val classifiers = lib["downloads"]?.jsonObject?.get("classifiers")?.jsonObject ?: continue
                val nativeKey = "natives-$currentOs"
                val nativeClassifier = classifiers[nativeKey] ?: continue
                val path = nativeClassifier.jsonObject["path"]?.jsonPrimitive?.content ?: continue

                val jarFile = File(librariesDir, path)
                if (!jarFile.exists()) continue

                try {
                    java.util.zip.ZipFile(jarFile).use { zip ->
                        zip.entries().asSequence().forEach { entry ->
                            if (!entry.isDirectory && !entry.name.startsWith("META-INF")) {
                                val outFile = File(nativesDir, entry.name)
                                outFile.parentFile?.mkdirs()
                                zip.getInputStream(entry).use { input ->
                                    outFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        if (nativesDir.exists() && nativesDir.listFiles()?.isNotEmpty() == true) {
            return nativesDir.absolutePath
        }
        nativesDir.delete()
        return null
    }

    private fun buildClasspath(version: String, inheritsFrom: String? = null): String {
        val classpathList = mutableListOf<String>()

        if (inheritsFrom != null) {
            val parentJar = File(versionsDir, "$inheritsFrom/$inheritsFrom.jar")
            if (parentJar.exists()) classpathList.add(parentJar.absolutePath)
        }
        val childJar = File(versionsDir, "$version/$version.jar")
        if (childJar.exists()) classpathList.add(childJar.absolutePath)

        if (inheritsFrom != null) {
            val vanillaJsonFile = File(versionsDir, "$inheritsFrom/$inheritsFrom.json")
            if (vanillaJsonFile.exists()) {
                val vanillaJson = Json.parseToJsonElement(vanillaJsonFile.readText()).jsonObject
                val vanillaLibs = vanillaJson["libraries"] as? JsonArray ?: JsonArray(emptyList())
                for (el in vanillaLibs) {
                    val obj = el as? JsonObject ?: continue
                    if (!rulesMatchLibrary(obj)) continue
                    val libPath = resolveLibPath(obj) ?: continue
                    val jarFile = File(librariesDir, libPath)

                    if (!jarFile.exists()) {
                        val url = obj["downloads"]?.jsonObject?.get("artifact")?.jsonObject?.get("url")?.jsonPrimitive?.content
                        val dlUrl = url ?: "https://libraries.minecraft.net/$libPath"
                        try {
                            jarFile.parentFile?.mkdirs()
                            URL(dlUrl).openStream().use { input ->
                                jarFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        } catch (e: Exception) {}
                    }
                    if (jarFile.exists()) classpathList.add(jarFile.absolutePath)
                }
            }
        }

        val loaderJsonFile = File(versionsDir, "$version/$version.json")
        if (loaderJsonFile.exists()) {
            val loaderJson = Json.parseToJsonElement(loaderJsonFile.readText()).jsonObject
            val loaderLibs = loaderJson["libraries"] as? JsonArray ?: JsonArray(emptyList())
            for (el in loaderLibs) {
                val obj = el as? JsonObject ?: continue
                if (!rulesMatchLibrary(obj)) continue
                val libPath = resolveLibPath(obj) ?: continue
                val jarFile = File(librariesDir, libPath)

                if (!jarFile.exists()) {
                    val baseUrl = obj["url"]?.jsonPrimitive?.content ?: "https://maven.fabricmc.net/"
                    val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                    try {
                        jarFile.parentFile?.mkdirs()
                        URL("$cleanUrl$libPath").openStream().use { input ->
                            jarFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    } catch (e: Exception) {
                        try {
                            URL("https://repo1.maven.org/maven2/$libPath").openStream().use { input ->
                                jarFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        } catch (e2: Exception) {}
                    }
                }
                if (jarFile.exists()) classpathList.add(jarFile.absolutePath)
            }
        }

        val finalClasspath = mutableListOf<String>()
        val libraryMap = mutableMapOf<String, String>()

        for (path in classpathList) {
            val file = File(path)
            if (!file.exists()) continue

            if (path.replace("\\", "/").contains("/libraries/")) {
                val ver = file.parentFile.name
                val artifactDir = file.parentFile.parentFile.absolutePath
                val suffix = file.name.substringAfter(ver, "")
                val uniqueKey = "$artifactDir|$suffix"

                val existingPath = libraryMap[uniqueKey]
                if (existingPath != null) {
                    val existingVer = File(existingPath).parentFile.name
                    val scoreOld = existingVer.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                    val scoreNew = ver.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                    if (scoreNew >= scoreOld) libraryMap[uniqueKey] = path
                } else {
                    libraryMap[uniqueKey] = path
                }
            } else {
                if (!finalClasspath.contains(path)) finalClasspath.add(path)
            }
        }

        finalClasspath.addAll(libraryMap.values)
        return finalClasspath.joinToString(File.pathSeparator)
    }

    private fun resolveLibPath(lib: JsonObject): String? {
        val downloads = lib["downloads"] as? JsonObject
        val artifact = downloads?.get("artifact") as? JsonObject
        val downloadPath = artifact?.get("path")?.jsonPrimitive?.content
        if (downloadPath != null) return downloadPath

        val name = lib["name"]?.jsonPrimitive?.content ?: return null
        val parts = name.split(":")
        if (parts.size < 3) return null
        val group = parts[0].replace('.', '/')
        val artifactId = parts[1]
        val version = parts[2]
        val classifier = parts.getOrNull(3)
        val fileName = if (classifier != null) "$artifactId-$version-$classifier.jar" else "$artifactId-$version.jar"
        return "$group/$artifactId/$version/$fileName"
    }

    private fun isSuspiciousJvmArg(arg: String): Boolean {
        val suspiciousPatterns = listOf(
            Regex("(?i)^-javaagent:(?!.*bullmc-anticheat)"),
            Regex("(?i)^-agentlib:(?!.*jna|.*jtreg|.*jcov|.*j2pcsc|.*j2gss|.*jaas|.*sunmscapi|.*net)"),
            Regex("(?i)^-agentpath:(?!.*bullmc)"),
            Regex("(?i)^-Xbootclasspath"),
            Regex("(?i)^-XXaltjvm"),
            Regex("(?i)^-XX:\\+UnlockDiagnosticVMOptions"),
            Regex("(?i)^-XX:\\+UnlockExperimentalVMOptions"),
            Regex("(?i)^-Xdebug"),
            Regex("(?i)^-XX:JDWPTransport"),
            Regex("(?i)^-agentlib:jdwp"),
            Regex("(?i)^-Xrunjdwp"),
            Regex("(?i)^-noverify"),
            Regex("(?i)^-Xverify:none"),
        )
        return suspiciousPatterns.any { it.containsMatchIn(arg) }
    }

    private fun rulesMatchLibrary(lib: JsonObject): Boolean {
        val rules = lib["rules"] as? JsonArray ?: return true
        for (rule in rules) {
            val ruleObj = rule as? JsonObject ?: continue
            val action = ruleObj["action"]?.jsonPrimitive?.content ?: "allow"
            val os = ruleObj["os"]?.jsonObject
            if (os != null) {
                val osName = os["name"]?.jsonPrimitive?.content
                val currentOs = when {
                    System.getProperty("os.name").lowercase().contains("windows") -> "windows"
                    System.getProperty("os.name").lowercase().contains("mac") -> "osx"
                    else -> "linux"
                }
                val matches = osName == null || osName == currentOs
                if (action == "allow" && !matches) return false
                if (action == "disallow" && matches) return false
            }
            val features = ruleObj["features"] as? JsonObject
            if (features != null) {
                return false
            }
        }
        return true
    }
}