package net.bullmc.client.core.launcher

import net.bullmc.client.core.auth.Auth
import kotlinx.serialization.json.*
import java.io.File

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
        serverPort: Int = 25565
    ): Process? {
        return try {
            auth.savePlayerNick(playerNick)

            val versionDir = File(versionsDir, version)
            val versionJsonFile = File(versionDir, "$version.json")

            if (!versionJsonFile.exists()) {
                println("[LAUNCH] Файлы версии $version не найдены")
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
                    println("[LAUNCH] InheritsFrom: $inheritsFrom")
                }

                if (!clientJar.exists() && parentJarFile.exists()) {
                    clientJar = parentJarFile
                }
            }

            if (!clientJar.exists()) {
                println("[LAUNCH] Client jar не найден: ${clientJar.absolutePath}")
                return null
            }

            val mainClass = rawJson["mainClass"]?.jsonPrimitive?.content
                ?: parentJson?.get("mainClass")?.jsonPrimitive?.content
                ?: throw IllegalStateException("mainClass не найден в version.json")

            val classpath = buildClasspath(version, inheritsFrom)
            val nativesDir = findOrExtractNatives(version)

            val ramMB = ramMb.coerceIn(1024, 16384)
            val xmx = "${ramMB}M"
            val xms = "${(ramMB / 2).coerceAtLeast(512)}M"

            val replacements = mapOf(
                "natives_directory" to (nativesDir ?: ""),
                "launcher_name" to "BullMC",
                "launcher_version" to "1.0"
            )

            val jvmArgs = mutableListOf<String>()
            jvmArgs.add(javaPath)
            jvmArgs.addAll(listOf("-Xmx$xmx", "-Xms$xms"))

            val allJvmArgs = mutableListOf<JsonElement>()
            rawJson["arguments"]?.jsonObject?.get("jvm")?.let { if (it is JsonArray) allJvmArgs.addAll(it) }
            parentJson?.get("arguments")?.jsonObject?.get("jvm")?.let { if (it is JsonArray) allJvmArgs.addAll(it) }

            var skipNext = false
            for (element in allJvmArgs) {
                if (skipNext) {
                    skipNext = false
                    continue
                }
                when (element) {
                        is JsonPrimitive -> {
                            val content = element.content
                            if (content == "-cp" || content == "\${classpath}") {
                                if (content == "-cp") skipNext = true
                                continue
                            }
                            jvmArgs.add(resolvePlaceholders(content, replacements))
                        }
                        is JsonObject -> {
                            if (!rulesMatch(element)) continue
                            val value = element["value"]
                            when (value) {
                                is JsonPrimitive -> {
                                    val content = value.content
                                    if (content == "-cp" || content == "\${classpath}") {
                                        if (content == "-cp") skipNext = true
                                        continue
                                    }
                                    jvmArgs.add(resolvePlaceholders(content, replacements))
                                }
                                is JsonArray -> {
                                    var innerSkip = false
                                    for (v in value) {
                                        if (innerSkip) {
                                            innerSkip = false
                                            continue
                                        }
                                        if (v is JsonPrimitive) {
                                            val content = v.content
                                            if (content == "-cp" || content == "\${classpath}") {
                                                if (content == "-cp") innerSkip = true
                                                continue
                                            }
                                            jvmArgs.add(resolvePlaceholders(content, replacements))
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
                jvmArgs.removeAll { it.startsWith("-Djava.library.path=") ||
                    it.startsWith("-Djna.tmpdir=") ||
                    it.startsWith("-Dorg.lwjgl.system.SharedLibraryExtractPath=") ||
                    it.startsWith("-Dio.netty.native.workdir=") }
            }

            jvmArgs.addAll(listOf("-cp", classpath))

            val gameArgs = mutableListOf(
                "--username=$playerNick",
                "--version=$version",
                "--gameDir=${gameDir.absolutePath}",
                "--assetsDir=${assetsDir.absolutePath}",
                "--accessToken=0",
                "--uuid=${java.util.UUID.randomUUID().toString().replace("-", "")}",
                "--userType=offline",
                "--userProperties={}"
            )

            val assetIndex = rawJson["assets"]?.jsonPrimitive?.content
            if (assetIndex != null) {
                gameArgs.add(3, "--assetIndex=$assetIndex")
            }

            if (serverIp != null) {
                gameArgs.add("--server=$serverIp")
                gameArgs.add("--port=$serverPort")
            }

            val allArgs = jvmArgs + listOf(mainClass) + gameArgs

            println("[LAUNCH] Command (${allArgs.size} args):")
            println("[LAUNCH] ${allArgs.joinToString(" ")}")

            val processBuilder = ProcessBuilder(allArgs)
            processBuilder.directory(gameDir)
            processBuilder.redirectErrorStream(true)
            processBuilder.start()
        } catch (e: Exception) {
            println("[LAUNCH] Ошибка: ${e.message}")
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

                println("[LAUNCH] Extracting natives from: $path")
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
                } catch (e: Exception) {
                    println("[LAUNCH] Error extracting natives from $path: ${e.message}")
                }
            }
        }

        if (nativesDir.exists() && nativesDir.listFiles()?.isNotEmpty() == true) {
            return nativesDir.absolutePath
        }
        nativesDir.delete()
        return null
    }

    private fun buildClasspath(version: String, inheritsFrom: String? = null): String {
        val jars = mutableListOf<String>()

        val versionDir = File(versionsDir, version)
        val clientJar = File(versionDir, "$version.jar")
        if (clientJar.exists()) jars.add(clientJar.absolutePath)

        val allLibraries = mutableListOf<Pair<String, JsonObject>>()

        val versionJsonFile = File(versionsDir, "$version/$version.json")
        if (versionJsonFile.exists()) {
            val rawJson = Json.parseToJsonElement(versionJsonFile.readText()).jsonObject
            val libraries = rawJson["libraries"] as? JsonArray
            if (libraries != null) {
                for (el in libraries) {
                    val obj = el as? JsonObject ?: continue
                    val name = obj["name"]?.jsonPrimitive?.content ?: continue
                    allLibraries.add(name to obj)
                }
            }
        }

        if (inheritsFrom != null) {
            val parentJsonFile = File(versionsDir, "$inheritsFrom/$inheritsFrom.json")
            if (parentJsonFile.exists()) {
                val parentJson = Json.parseToJsonElement(parentJsonFile.readText()).jsonObject
                val parentLibs = parentJson["libraries"] as? JsonArray
                if (parentLibs != null) {
                    for (el in parentLibs) {
                        val obj = el as? JsonObject ?: continue
                        val name = obj["name"]?.jsonPrimitive?.content ?: continue
                        allLibraries.add(name to obj)
                    }
                }

                val parentJar = File(versionsDir, "$inheritsFrom/$inheritsFrom.jar")
                if (parentJar.exists() && !jars.contains(parentJar.absolutePath)) {
                    jars.add(0, parentJar.absolutePath)
                }
            }
        }

        val deduped = linkedMapOf<String, JsonObject>()
        for ((name, lib) in allLibraries) {
            val parts = name.split(":")
            if (parts.size >= 3) {
                val artifactKey = "${parts[0]}:${parts[1]}"
                deduped[artifactKey] = lib
            }
        }

        for ((_, lib) in deduped) {
            if (!rulesMatchLibrary(lib)) continue
            val path = resolveLibraryPath(lib)
            if (path != null) {
                val file = File(librariesDir, path)
                if (file.exists()) {
                    jars.add(file.absolutePath)
                } else {
                    println("[LAUNCH] Missing library: $path")
                }
            }
        }

        return jars.joinToString(File.pathSeparator)
    }

    private fun resolveLibraryPath(lib: JsonObject): String? {
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
