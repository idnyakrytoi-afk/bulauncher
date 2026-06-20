package net.bullmc.client.core.anticheat

import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

object GameAgent {

    @Volatile
    private var instrumentation: Instrumentation? = null

    @Volatile
    private var classLoadCount = 0

    @Volatile
    private var suspiciousClassCount = 0

    private val loadedClassHashes = mutableSetOf<String>()

    @JvmStatic
    fun premain(args: String?, inst: Instrumentation) {
        instrumentation = inst
        registerTransformer(inst)
        scanLoadedClasses(inst)
        startRuntimeAIDetection(inst)
    }

    @JvmStatic
    fun agentmain(args: String?, inst: Instrumentation) {
        instrumentation = inst
        registerTransformer(inst)
        scanLoadedClasses(inst)
        startRuntimeAIDetection(inst)
    }

    private fun registerTransformer(inst: Instrumentation) {
        inst.addTransformer(object : ClassFileTransformer {
            override fun transform(
                loader: ClassLoader?,
                className: String?,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray?
            ): ByteArray? {
                if (className == null || classfileBuffer == null) return null

                classLoadCount++

                val lower = className.lowercase()

                if (isBlacklistedClassName(lower)) {
                    log("[BLOCKED] Запрещённый класс: $className")
                    throw SecurityException("Security violation")
                }

                if (isVanillaClass(className) && classBeingRedefined != null) {
                    log("[BLOCKED] Попытка модификации ванильного класса: $className")
                    throw SecurityException("Security violation")
                }

                val aiScore = analyzeClassWithAI(classfileBuffer, className, loader)
                if (aiScore >= AI_THRESHOLD_CRITICAL) {
                    log("[AI-BLOCK] Критический AI-скор для $className: $aiScore")
                    throw SecurityException("Security violation")
                }
                if (aiScore >= AI_THRESHOLD_HIGH) {
                    suspiciousClassCount++
                    log("[AI-WARN] Подозрительный AI-скор для $className: $aiScore (подозрительных: $suspiciousClassCount)")
                    if (suspiciousClassCount >= 5) {
                        throw SecurityException("Security violation")
                    }
                }

                if (hasCheatBytecodePatterns(classfileBuffer, className)) {
                    log("[BLOCKED] Подозрительный bytecode в: $className")
                    throw SecurityException("Security violation")
                }

                if (classBeingRedefined == null) {
                    val classHash = computeClassHash(classfileBuffer)
                    if (loadedClassHashes.contains(classHash)) {
                        log("[AI-WARN] Дублирующийся класс: $className")
                    }
                    loadedClassHashes.add(classHash)
                }

                return null
            }
        }, true)
    }

    private fun scanLoadedClasses(inst: Instrumentation) {
        if (!inst.isRetransformClassesSupported) return

        try {
            val toRetransform = inst.allLoadedClasses.filter { clazz ->
                isBlacklistedClassName(clazz.name.lowercase())
            }.toTypedArray()

            if (toRetransform.isNotEmpty()) {
                inst.retransformClasses(*toRetransform)
            }
        } catch (e: Exception) {
            log("Ошибка retransform: ${e.message}")
        }
    }

    private fun startRuntimeAIDetection(inst: Instrumentation) {
        val monitorThread = Thread({
            log("AI Runtime-мониторинг запущен")
            while (true) {
                try {
                    Thread.sleep(60_000)
                    if (inst.allLoadedClasses.isEmpty()) break

                    val allLoaded = inst.allLoadedClasses
                    val suspiciousClasses = mutableListOf<String>()

                    for (clazz in allLoaded) {
                        val lower = clazz.name.lowercase()
                        if (isBlacklistedClassName(lower)) {
                            suspiciousClasses.add(clazz.name)
                        }
                    }

                    if (suspiciousClasses.isNotEmpty()) {
                        log("[AI-RUNTIME] Обнаружено ${suspiciousClasses.size} подозрительных загруженных классов")
                        for (cls in suspiciousClasses.take(5)) {
                            log("  - $cls")
                        }
                        if (inst.isRetransformClassesSupported) {
                            try {
                                val toRetransform = allLoaded.filter { isBlacklistedClassName(it.name.lowercase()) }
                                    .filter { inst.isModifiableClass(it) }
                                    .toTypedArray()
                                if (toRetransform.isNotEmpty()) {
                                    inst.retransformClasses(*toRetransform)
                                }
                            } catch (e: Exception) {
                                log("Ошибка retransform в runtime: ${e.message}")
                            }
                        }
                    }

                    val totalClasses = allLoaded.size
                    if (totalClasses > 10000) {
                        log("[AI-RUNTIME] Подозрительно много классов: $totalClasses")
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    log("Ошибка AI runtime мониторинга: ${e.message}")
                }
            }
            log("AI Runtime-мониторинг остановлен")
        }, "AntiCheat-AI-RuntimeMonitor")
        monitorThread.isDaemon = true
        monitorThread.priority = Thread.MIN_PRIORITY
        monitorThread.start()
    }

    private fun isBlacklistedClassName(className: String): Boolean {
        for (name in BLACKLISTED_NAMES) {
            if (className.contains(name)) return true
        }

        for (pattern in BLACKLISTED_CLASS_PATTERNS) {
            if (pattern.containsMatchIn(className)) return true
        }

        return false
    }

    private fun isVanillaClass(className: String): Boolean {
        return className.startsWith("net/minecraft/") ||
               className.startsWith("com/mojang/") ||
               className.startsWith("org/lwjgl/") ||
               className.startsWith("org/apache/") ||
               className.startsWith("java/") ||
               className.startsWith("javax/")
    }

    private fun hasCheatBytecodePatterns(bytes: ByteArray, className: String): Boolean {
        if (isVanillaClass(className)) return false

        val content = String(bytes, Charsets.ISO_8859_1)

        for (pattern in SUSPICIOUS_BYTECODE_STRINGS) {
            if (content.contains(pattern)) {
                log("[SCAN] Подозрительный паттерн '$pattern' в $className")
                return true
            }
        }

        if (hasSuspiciousNativeMethods(bytes)) {
            return true
        }

        return false
    }

    private fun analyzeClassWithAI(bytes: ByteArray, className: String, loader: ClassLoader?): Int {
        if (isVanillaClass(className)) return 0

        var score = 0

        val content = String(bytes, Charsets.ISO_8859_1)

        score += analyzeInjectionPatterns(content, className)
        score += analyzeReflectivePatterns(content, className)
        score += analyzeMemoryManipulation(content, className)
        score += analyzeAntiDetection(content, className)
        score += analyzeNetworkPatterns(content, className)
        score += analyzeObfuscationPatterns(content, className)
        score += analyzeTimingPatterns(content, className)

        if (loader != null && loader.javaClass.name.contains("URLClassLoader").not()) {
            val loaderName = loader.javaClass.name.lowercase()
            if (loaderName.contains("reflect") || loaderName.contains("dynamic")) {
                score += 30
                log("[AI] Подозрительный ClassLoader: $loaderName")
            }
        }

        return score
    }

    private fun analyzeInjectionPatterns(content: String, className: String): Int {
        var score = 0

        val injectionStrings = listOf(
            "loadClass", "defineClass", "findClass",
            "ClassLoader", "URLClassLoader", "ChildFirst",
            "loadLibrary", "System.loadLibrary", "Runtime.getRuntime().exec"
        )

        for (str in injectionStrings) {
            if (content.contains(str, ignoreCase = true)) {
                score += 5
            }
        }

        if (content.contains("defineClass") && content.contains("byte")) {
            score += 25
        }

        if (content.contains("Class.forName") && content.contains("newInstance")) {
            score += 15
        }

        return score
    }

    private fun analyzeReflectivePatterns(content: String, className: String): Int {
        var score = 0

        val reflectiveStrings = listOf(
            "Method.invoke", "Field.set", "Field.get",
            "Constructor.newInstance", "setAccessible",
            "getDeclaredMethod", "getDeclaredField",
            "getDeclaredConstructor", "getModifiers",
            "setAccessible(true)"
        )

        for (str in reflectiveStrings) {
            if (content.contains(str, ignoreCase = true)) {
                score += 3
            }
        }

        val reflectionCount = reflectiveStrings.count { content.contains(it, ignoreCase = true) }
        if (reflectionCount >= 4) {
            score += 20
        }

        if (content.contains("sun.misc.Unsafe") || content.contains("jdk.internal.misc.Unsafe")) {
            score += 30
        }

        return score
    }

    private fun analyzeMemoryManipulation(content: String, className: String): Int {
        var score = 0

        val memoryStrings = listOf(
            "ByteBuffer.allocateDirect", "DirectByteBuffer",
            "MappedByteBuffer", "FileChannel.map",
            "sun.nio.ch.DirectBuffer", "java.nio.Buffer",
            "Unsafe.putByte", "Unsafe.getByte",
            "Unsafe.putLong", "Unsafe.getLong",
            "Unsafe.putInt", "Unsafe.getInt",
            "MemorySegment", "MemoryLayout",
            "VarHandle", "MethodHandles.Lookup"
        )

        for (str in memoryStrings) {
            if (content.contains(str, ignoreCase = true)) {
                score += 5
            }
        }

        val memoryCount = memoryStrings.count { content.contains(it, ignoreCase = true) }
        if (memoryCount >= 3) {
            score += 15
        }

        return score
    }

    private fun analyzeAntiDetection(content: String, className: String): Int {
        var score = 0

        val antiDebugStrings = listOf(
            "isDebuggerPresent", "CheckRemoteDebuggerPresent",
            "Thread.sleep", "System.nanoTime", "System.currentTimeMillis",
            "Runtime.getRuntime().exec", "ProcessBuilder",
            "File.delete", "File.renameTo",
            "System.exit", "Runtime.halt"
        )

        for (str in antiDebugStrings) {
            if (content.contains(str, ignoreCase = true)) {
                score += 3
            }
        }

        if (content.contains("Thread.sleep") && content.contains("System.nanoTime")) {
            score += 15
        }

        if (content.contains("Runtime.getRuntime().exec") && content.contains("tasklist")) {
            score += 25
        }

        if (content.contains("File.delete") && content.contains(".class")) {
            score += 10
        }

        return score
    }

    private fun analyzeNetworkPatterns(content: String, className: String): Int {
        var score = 0

        val networkStrings = listOf(
            "java.net.Socket", "java.net.ServerSocket",
            "java.net.HttpURLConnection", "java.net.URL",
            "java.io.ObjectInputStream", "java.io.ObjectOutputStream",
            "javax.net.ssl.SSLSocket",
            "java.nio.channels.SocketChannel",
            "java.nio.channels.DatagramChannel",
            "io.netty", "org.apache.http",
            "okhttp3", "retrofit2"
        )

        for (str in networkStrings) {
            if (content.contains(str, ignoreCase = true)) {
                score += 2
            }
        }

        val networkCount = networkStrings.count { content.contains(it, ignoreCase = true) }
        if (networkCount >= 3) {
            score += 10
        }

        if (content.contains("ObjectInputStream") && content.contains("readObject")) {
            score += 20
        }

        return score
    }

    private fun analyzeObfuscationPatterns(content: String, className: String): Int {
        var score = 0

        if (className.matches(Regex("^[a-zA-Z]{1,3}$"))) {
            score += 15
        }

        if (className.contains("$") && className.split("$").size > 3) {
            score += 10
        }

        val obfuscationIndicators = listOf(
            "\\x00", "\\u0000",
            "javax.crypto", "java.security.MessageDigest",
            "javax.crypto.Cipher", "SecretKeySpec"
        )

        for (indicator in obfuscationIndicators) {
            if (content.contains(indicator, ignoreCase = true)) {
                score += 5
            }
        }

        val bytePatternCount = Regex("\\\\x[0-9a-fA-F]{2}").findAll(content).count()
        if (bytePatternCount > 10) {
            score += 20
        }

        return score
    }

    private fun analyzeTimingPatterns(content: String, className: String): Int {
        var score = 0

        if (content.contains("System.nanoTime") && content.contains("Thread.sleep")) {
            score += 10
        }

        if (content.contains("ScheduledExecutorService") || content.contains("Timer")) {
            score += 5
        }

        if (content.contains("CountDownLatch") || content.contains("CyclicBarrier")) {
            score += 5
        }

        if (content.contains("Phaser")) {
            score += 5
        }

        return score
    }

    private fun computeClassHash(bytes: ByteArray): String {
        try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            val hash = digest.digest(bytes)
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            return bytes.hashCode().toString()
        }
    }

    private fun hasSuspiciousNativeMethods(bytes: ByteArray): Boolean {
        val content = String(bytes, Charsets.ISO_8859_1)
        val nativeCount = "native".let { keyword ->
            var count = 0
            var idx = 0
            while (idx < content.length) {
                val found = content.indexOf(keyword, idx, ignoreCase = true)
                if (found == -1) break
                count++
                idx = found + keyword.length
                if (count > 3) return true
            }
            count
        }
        return false
    }

    private fun log(message: String) {
    }

    private const val AI_THRESHOLD_CRITICAL = 80
    private const val AI_THRESHOLD_HIGH = 50

    private val BLACKLISTED_NAMES = setOf(
        "wurst", "impact", "meteorclient", "meteor.client", "baritone",
        "rusherhack", "aristois", "futureclient", "phoenixclient", "vape",
        "novoline", "huzuni", "kiddion", "sigma", "bleachhack", "inertia",
        "ghostly", "sudden", "olympus", "moonsworth", "catalyst", "cef",
        "liquidbounce", "konas", "strafe", "rdpraxis", "xave", "jello",
        "seppuku", "halcyon", "astolfo", "wamy", "vortext", "exhi",
        "dream", "bhop", "nuker", "reach", "killaura", "autoclicker",
        "aimbot", "scaffold", "phase", "blink", "velocity", "antiknockback",
        "hitbox", "expandhitbox", "combo", "xray", "wallhack",
        "noclip", "fly", "speed", "sprint", "timer", "slow",
        "fastplace", "fastbreak", "autotool", "cheststealer", "invsee",
        "esp", "tracers", "nametags", "storageesp",
        "cheatengine", "inject", "hooker",
        "jigsaw", "hanabi", "drip", "wyld", "dankpvp",
        "brutal", "brutality", "nightx", "sensei", "aurora",
        "novus", "firedump", "moondump", "hackphoenix",
        "miningplus", "godmode", "creative", "spawnkill",
        "killswitch", "exploit", "exploiter", "hackclient",
        "assault", "kamikaze", "ragebot", "triggerbot",
        "autoarmor", "autosoup", "autoeat", "autototem",
        "entityspeed", "boatfly", "packetfly", "packetspeed",
        "noslow", "noslowdown", "fastswing", "autobow",
        "bowaimbot", "arrowaura", "crystalaura", "surround",
        "bedaura", "totemswap", "hotbarswap",
        "irc", "mass", "massai", "massaiware",
        "hledej", "hledejcz", "hackgaming", "hacklaby",
        "hackgaming2", "hacklaby2", "hacklaby3",
        "bedrockminer", "betterpvp", "clientcommands",
        "chestlocator", "replaymod", "flashback", "elytrautilities",
        "findme", "forgehax", "worlddownloader", "freecam",
        "seedcracker", "squake", "tweakeroo", "walljump",
        "inventoryprofilesnext", "marlowcrystaloptimizer", "inventorymove"
    )

    private val BLACKLISTED_CLASS_PATTERNS = listOf(
        Regex("(?i)(cheat|hack|inject|exploit|trigger|aura|kill|reach|speed|fly|xray|wall|freecam|noclip|scaffold|autoclick|autobow|autocrystal|bhop|phase|blink|velocity|hitbox|combo|noslow|fastplace|fastbreak|autotool|cheststeal|invsee|esp|tracer|nametag|seedcracker|forgehax|bedrockminer|freecam|tweakeroo|walljump|squake)"),
        Regex("(?i)^(com|net|org|io)\\..*\\.(cheat|hack|exploit|mod)\\."),
    )

    private val SUSPICIOUS_BYTECODE_STRINGS = setOf(
        "makeClientPlayer",
        "sendChatMessage",
        "playerController",
        "connection/sendPacket",
        "net/minecraft/client/multiplayer/PlayerController",
        "setSprinting",
        "setHealth",
        "setAbsorptionAmount",
        "attackEntity",
        "onLivingUpdate",
        "EntityPlayerSP",
        "GameRenderer/renderLevel",
        "LevelRenderer/renderLevel",
        "MatrixStack",
        "GL11/glEnable",
        "GlStateManager",
        "Minecraft.getInstance",
        "getMinecraft",
        "sendQueue",
        "getNetHandler",
        "PacketListener",
        "playerNetClientHandler",
        "networkManager",
        "addToSendQueue",
        "playerControllerMP",
        "rightClickMouse",
        "clickMouse",
        "middleClickMouse",
        "leftClickMouse",
        "sendClickBlockToController",
        "ServerData",
        "currentServerData",
        "serverIP",
        "serverMOTD",
        "ThreadDownloadSkin",
        "AbstractGui",
        "drawString",
        "drawRect",
        "drawCenteredString",
        "FontRenderer",
        "font",
        "InGameHud",
        "GameMenuScreen"
    )
}
