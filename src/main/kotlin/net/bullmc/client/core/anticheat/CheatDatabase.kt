package net.bullmc.client.core.anticheat

import java.io.File
import java.util.zip.ZipFile

object CheatDatabase {

    val blacklistedModNames: Set<String> = setOf(
        "wurst", "impact", "meteor-client", "meteorclient", "baritone",
        "rusherhack", "aristois", "futureclient", "phoenixclient", "vape",
        "novoline", "huzuni", "kiddion", "bleachhack", "ghostly",
        "moonsworth", "liquidbounce", "konas", "strafe", "rdpraxis",
        "xave", "jello", "seppuku", "halcyon", "astolfo",
        "wamy", "vortext", "exhi", "dream", "jigsaw",
        "hanabi", "drip", "wyld", "dankpvp", "brutal",
        "brutality", "nightx", "sensei", "aurora", "novus",
        "firedump", "moondump", "hackphoenix", "miningplus",
        "godmode", "killswitch", "exploit", "exploiter", "hackclient",
        "assault", "kamikaze", "ragebot", "triggerbot", "autoclicker",
        "killaura", "aimbot", "wallhack", "scaffold", "autocrystal",
        "blink", "phase", "hitbox", "expandhitbox", "combo",
        "nofall", "speed", "fly", "noclip", "timer",
        "noslow", "fastplace", "fastbreak", "autotool", "cheststealer",
        "invsee", "esp", "tracers", "nametags", "storageesp",
        "fastswing", "autoarmor", "autosoup", "autoeat", "autototem",
        "entityspeed", "boatfly", "packetfly", "packetspeed",
        "autobow", "bowaimbot", "arrowaura", "bedaura",
        "totemswap", "hotbarswap", "mass", "massai", "massaiware",
        "hledej", "hledejcz", "hackgaming", "hacklaby",
        "reach", "reachmod", "anticheck", "anticheat",
        "nocheatplus", "ncp-bypass", "vulcan-bypass", "matrix-bypass",
        "capes", "freecape", "capemod",

        "bedrock-miner", "bedrockminer",
        "betterpvp", "better-pvp",
        "clientcommands",
        "chestlocator", "chest-locator", "chestlocatormod",
        "replay-mod", "replaymod", "flashback",
        "elytra-utilities", "elytrautilities",
        "findme",
        "forgehax", "forge-hax",
        "worlddownloader", "world-downloader", "wdl",
        "freecam",
        "seedcracker", "seed-cracker",
        "step",
        "squake",
        "tweakeroo",
        "walljump", "wall-jump",
        "xray",
        "inventoryprofilesnext", "inventory-profiles-next", "ipn",
        "marlowcrystaloptimizer", "marlow-crystal-optimizer", "marlow",
        "inventorymove", "inventory-move"
    )

    val blacklistedModFilePatterns: List<Regex> = listOf(
        Regex("(?i)^(wurst|impact|baritone|rusherhack|aristois|futureclient|phoenixclient|vape|novoline|huzuni|kiddion|bleachhack|ghostly|liquidbounce|konas|strafe|astolfo|seppuku|halcyon|jello|moonsworth)(?:-|_| ).*\\.jar$"),
        Regex("(?i)^(bedrock.?miner|better.?pvp|clientcommands|chest.?locator|replay.?mod|replaymod|flashback|elytra.?utilities|findme|forgehax|world.?downloader|freecam|seedcracker|step|squake|tweakeroo|wall.?jump|xray|inventory.?profiles.?next|inventorymove|marlow)(?:-|_| ).*\\.jar$"),
        Regex("(?i)^(killaura|aimbot|autoaim|wallhack|bhop|autoclicker|nofall|scaffold|autocrystal|blink|phase|velocity|hitbox|combo|speed|fly|noclip|timer|noslow|fastplace|fastbreak|autotool|cheststeal|invsee|esp|tracer|nametag)(?:-|_| ).*\\.jar$"),
    )

    val blacklistedProcesses: Set<String> = setOf(
        "cheatengine-x86_64.exe", "cheatengine-i386.exe", "cheatengine.exe",
        "cheatengine-x86.exe", "cheatengine-x32.exe",
        "ollydbg.exe", "ollydbg64.exe",
        "x64dbg.exe", "x32dbg.exe", "x96dbg.exe",
        "idaq.exe", "idaq64.exe", "idag.exe", "idag64.exe", "ida64.exe",
        "dnspy.exe", "dnspy64.exe", "dnSpy.exe", "dnSpy-x64.exe",
        "processhacker.exe", "processhacker64.exe",
        "wireshark.exe", "fiddler.exe",
        "dumpcap.exe", "tcpdump.exe",
        "httpdebuggerpro.exe", "httpdebugger.exe",
        "mempatch.exe", "memhack.exe",
        "inject.exe", "injector.exe", "dllinjector.exe", "inject-dll.exe",
        "extreme injector.exe", "minject.exe", "xenos.exe",
        "sandboxie.exe", "sandboxiedcomlaunch.exe",
        "windbg.exe", "cdb.exe", "ntsd.exe",
        "frida.exe", "frida-trace.exe", "frida-server.exe",
        "cain.exe", "john.exe", "hashcat.exe",
        "immunity debugger.exe", "immdbg.exe",
        "bytecodeviewer.exe", "jad.exe", "cfr.exe",
        "procyon.exe", "fernflower.exe",
        "mc-agent.exe", "mcagent.exe",
        "minecraft-hack.exe", "mchack.exe"
    )

    val blacklistedNativeLibraries: Set<String> = setOf(
        "cheatengine", "cetrainer", "trainer",
        "inject", "hook", "detour",
        "frida", "xposed", "substrate",
        "minhook", "easyhook", "detours",
        "frida-agent", "frida-gadget",
        "edxposed", "lsposed"
    )

    val suspiciousJvmArgs: List<Regex> = listOf(
        Regex("(?i)-javaagent:(?!.*bullmc-anticheat).*"),
        Regex("(?i)-agentlib:(?!.*jna|.*jtreg|.*jcov|.*j2pcsc|.*j2gss|.*jaas|.*sunmscapi|.*net).*$"),
        Regex("(?i)-agentpath:(?!.*bullmc).*$"),
        Regex("(?i)-Xbootclasspath.*"),
        Regex("(?i)-XXaltjvm.*"),
        Regex("(?i)-XX:\\+UnlockDiagnosticVMOptions"),
        Regex("(?i)-XX:\\+UnlockExperimentalVMOptions"),
        Regex("(?i)-Xdebug"),
        Regex("(?i)-XX:JDWPTransport"),
        Regex("(?i)-agentlib:jdwp"),
        Regex("(?i)-Xrunjdwp"),
        Regex("(?i)-noverify"),
        Regex("(?i)-Xverify:none"),
        Regex("(?i)--add-opens.*sun/reflect"),
        Regex("(?i)--add-opens.*java/lang"),
        Regex("(?i)--add-opens.*jdk/internal"),
    )

    val allowedModIds: Set<String> = setOf(
        "fabric-api", "sodium", "lithium", "starlight", "ferrite-core",
        "memoryleakfix", "lazydfu", "entityculling", "iris", "modmenu",
        "cloth-config", "embeddium", "oculus", "rubidium", "sodium-quilt",
        "iris-quilt", "bulltweaks", "bullobjects", "bullassets", "bullicons",
        "shulkerboxtooltip", "appleskin", "jei", "jeiintegration",
        "roughlyenoughitems", "xaeros-minimap", "xaeros-world-map",
        "journeymap", "optifine", "continuity", "lambdynamiclights",
        "plasmo-voice", "proximity-chat", "simple-voice-chat",
        "presence-footsteps", "sound-physics-remastered", "not-enoughanimations",
        "betterthirdperson", "betterf3", "minihud", "litematica",
        "item-scroller", "tweakmyclient", "inventory-hud", "wthit", "hwyla",
        "jade", "neat", "durability-viewer", "armor-statues", "chisel",
        "bits-and-chisels", "canvas-renderer", "indium", "modelfix",
        "smooth-boot", "auth-me", "fallingtree", "tree-chop",
        "harvest-with-ease", "easy-mining", "netherite-fireproof",
        "netherite-fire-resistance", "inventory-sorting", "quick-pickup",
        "fast-leaf-decay", "leaf-me-alone", "better-grass", "cull-less-leaves",
        "cull-particles", "more-culling", "sodium-extra", "spark", "phosphor",
        "render-morph", "fabric-renderer-api-v1", "fabric-renderer-indigo",
        "fabric-renderer-loader", "fabric-rendering-v0", "fabric-rendering-v1",
        "fabric-rendering-data-attachment-v1", "fabric-resource-loader-v0",
        "fabric-networking-api-v1", "fabric-networking-v0",
        "fabric-screen-handler-api-v1", "fabric-events-lifecycle-v0",
        "fabric-events-rendering-v0", "fabric-events-input-v0",
        "fabric-key-binding-api-v1", "fabric-model-loading-api-v1",
        "fabric-model-loader-v0", "fabric-textures-v0",
        "fabric-transformation-api-v1", "fabric-object-builder-api-v1",
        "fabric-dim-api-v1", "fabric-api-base",
        "fabric-resource-conditions-api-v1", "fabric-tag-events-v0",
        "fabric-loot-tables-v1", "fabric-mining-levels-v1",
        "fabric-tool-action-api-v1", "fabric-block-view-api-v2",
        "fabric-block-view-renderer-api-v1",
        "fullbrightnesstoggle",
    )

    val allowedModFileHashes: Map<String, String> = mapOf(
        "bulltweaks-1.0.0.jar" to "",
    )

    val blacklistedTweakClasses: Set<String> = setOf(
        "wurst", "impact", "meteor", "baritone", "rusherhack",
        "aristois", "futureclient", "phoenixclient", "vape", "novoline",
        "huzuni", "liquidbounce", "konas", "strafe", "astolfo",
        "seppuku", "halcyon", "jello", "moonsworth", "catalyst",
        "forgehax", "freecam", "xray", "seedcracker"
    )

    val blacklistedModMetadataNames: Set<String> = setOf(
        "wurst", "impact", "meteor-client", "meteorclient", "baritone",
        "rusherhack", "aristois", "futureclient", "phoenixclient", "vape",
        "novoline", "huzuni", "liquidbounce", "konas", "strafe",
        "astolfo", "seppuku", "halcyon", "jello", "moonsworth",
        "catalyst", "mass", "massai", "brutal", "nightx",
        "sensei", "aurora", "novus", "firedump", "moondump",
        "forgehax", "freecam", "xray", "seedcracker",
        "bedrock-miner", "betterpvp", "clientcommands",
        "replay-mod", "flashback", "elytra-utilities",
        "findme", "world-downloader", "tweakeroo",
        "walljump", "inventory-profiles-next", "inventorymove", "marlow"
    )

    fun isModBlacklisted(fileName: String): Boolean {
        val lower = fileName.lowercase().replace(".jar", "")

        val segments = lower.split("-", "_", " ")
        if (segments.any { it in blacklistedModNames }) return true

        for (i in 0 until segments.size - 1) {
            val compound = segments[i] + "-" + segments[i + 1]
            if (compound in blacklistedModNames) return true
        }

        if (blacklistedModFilePatterns.any { it.matches(fileName) }) return true

        return false
    }

    fun isModWhitelisted(fileName: String, modId: String?): Boolean {
        if (modId != null && allowedModIds.contains(modId)) return true

        val lower = fileName.lowercase().replace(".jar", "")
        return allowedModIds.any { lower.contains(it) }
    }

    fun isNativeLibBlacklisted(libName: String): Boolean {
        val lower = libName.lowercase()
        return blacklistedNativeLibraries.any { lower.contains(it) }
    }

    fun isTweakClassBlacklisted(tweakClass: String): Boolean {
        val lower = tweakClass.lowercase()
        return blacklistedTweakClasses.any { lower.contains(it) }
    }

    fun scanModJarInternals(jarFile: File): List<String> {
        val violations = mutableListOf<String>()

        try {
            ZipFile(jarFile).use { zip ->
                val fabricModJson = zip.getEntry("fabric.mod.json")
                if (fabricModJson != null) {
                    val content = zip.getInputStream(fabricModJson).bufferedReader().readText()
                    for (name in blacklistedModMetadataNames) {
                        if (content.lowercase().contains(name)) {
                            violations.add("fabric.mod.json содержит запрещённое имя: $name")
                        }
                    }
                }

                val modsToml = zip.getEntry("META-INF/mods.toml")
                if (modsToml != null) {
                    val content = zip.getInputStream(modsToml).bufferedReader().readText()
                    for (name in blacklistedModMetadataNames) {
                        if (content.lowercase().contains(name)) {
                            violations.add("mods.toml содержит запрещённое имя: $name")
                        }
                    }
                }

                val mcmodInfo = zip.getEntry("mcmod.info")
                if (mcmodInfo != null) {
                    val content = zip.getInputStream(mcmodInfo).bufferedReader().readText()
                    for (name in blacklistedModMetadataNames) {
                        if (content.lowercase().contains(name)) {
                            violations.add("mcmod.info содержит запрещённое имя: $name")
                        }
                    }
                }

                zip.entries().asSequence().forEach { entry ->
                    if (entry.name.endsWith(".class")) {
                        val className = entry.name
                            .replace("/", ".")
                            .replace("\\", ".")
                            .removeSuffix(".class")
                            .lowercase()

                        val segments = className.split(".")

                        for (tweakClass in blacklistedTweakClasses) {
                            if (segments.any { it == tweakClass }) {
                                violations.add("Запрещённый tweak-класс: ${entry.name}")
                            }
                        }

                        for (name in blacklistedModNames) {
                            if (segments.any { it == name }) {
                                violations.add("Запрещённый класс в моде: ${entry.name}")
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return violations
    }
}
