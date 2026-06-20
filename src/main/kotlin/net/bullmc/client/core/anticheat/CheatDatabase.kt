package net.bullmc.client.core.anticheat

object CheatDatabase {

    val blacklistedModHashes: Set<String> = setOf(
        "a1b2c3d4e5f6",
    )

    val blacklistedModNames: Set<String> = setOf(
        "wurst",
        "impact",
        "meteor-client",
        "baritone",
        "rusherhack",
        "aristois",
        "futureclient",
        "phoenixclient",
        "vape",
        "novoline",
        "huzuni",
        "kiddion",
        "bleachhack",
        "ghostly",
        "moonsworth",
    )

    val blacklistedModFilePatterns: List<Regex> = listOf(
        Regex("(?i)^(wurst|impact|baritone|rusherhack|aristois|futureclient|phoenixclient|vape|novoline|huzuni|kiddion|bleachhack|ghostly).*\\.jar$"),
        Regex("(?i)^(killaura|aimbot|autoaim|wallhack|xray|bhop|autoclicker|nofall|scaffold|autocrystal).*\\.jar$"),
    )

    val blacklistedProcesses: Set<String> = setOf(
        "cheatengine-x86_64.exe",
        "cheatengine-i386.exe",
        "cheatengine.exe",
        "ollydbg.exe",
        "x64dbg.exe",
        "x32dbg.exe",
    )

    val suspiciousJvmArgs: List<Regex> = listOf(
        Regex("(?i)-javaagent:(?!.*bullmc-anticheat).*"),
        Regex("(?i)-agentlib:(?!.*jna|.*jtreg|.*jcov|.*j2pcsc|.*j2gss|.*jaas|.*sunmscapi|.*net).*$"),
        Regex("(?i)-agentpath:(?!.*bullmc).*$"),
        Regex("(?i)-Xbootclasspath.*"),
        Regex("(?i)-XXaltjvm.*"),
    )

    val allowedModIds: Set<String> = setOf(
        "fabric-api",
        "sodium",
        "lithium",
        "starlight",
        "ferrite-core",
        "memoryleakfix",
        "lazydfu",
        "entityculling",
        "iris",
        "modmenu",
        "cloth-config",
        "embeddium",
        "oculus",
        "rubidium",
        "sodium-quilt",
        "iris-quilt",
        "bulltweaks",
        "bullobjects",
        "bullassets",
        "bullicons",
        "shulkerboxtooltip",
        "appleskin",
        "jei",
        "jeiintegration",
        "roughlyenoughitems",
        "xaeros-minimap",
        "xaeros-world-map",
        "journeymap",
        "optifine",
        "capes",
        "continuity",
        " lambdynamiclights",
        "lithium",
        "plasmo-voice",
        "proximity-chat",
        "simple-voice-chat",
        "presence-footsteps",
        "sound-physics-remastered",
        "not-enoughAnimations",
        "betterThirdPerson",
        "betterf3",
        "tweakeroo",
        "minihud",
        "litematica",
        "item-scroller",
        "tweakmyclient",
        "_inventory-hud-forge",
        "inventory-hud",
        "wthit",
        "hwyla",
        "jade",
        "neat",
        "durability-viewer",
        "armor-statues",
        "chisel",
        "bits-and-chisels",
        "canvas-renderer",
        "indium",
        "modelfix",
        "smooth-boot",
        "auth-me",
        "presencefootsteps",
        "fallingtree",
        "tree-chop",
        "harvest-with-ease",
        "easy-mining",
        "netherite-fireproof",
        "netherite-fire-resistance",
        "inv-move",
        "inv-move-reforged",
        "inventory-sorting",
        "quick-pickup",
        "fast-leaf-decay",
        "leaf-me-alone",
        "better-grass",
        "cull-less-leaves",
        "cull-particles",
        "more-culling",
        "sodium-extra",
        "replay-mod",
        "spark",
        "phosphor",
        "render-morph",
        "lambdynamiclights",
        "fabric-renderer-api-v1",
        "fabric-renderer-indigo",
        "fabric-renderer-loader",
        "fabric-rendering-v0",
        "fabric-rendering-v1",
        "fabric-rendering-data-attachment-v1",
        "fabric-resource-loader-v0",
        "fabric-networking-api-v1",
        "fabric-networking-v0",
        "fabric-screen-handler-api-v1",
        "fabric-events-lifecycle-v0",
        "fabric-events-rendering-v0",
        "fabric-events-input-v0",
        "fabric-key-binding-api-v1",
        "fabric-model-loading-api-v1",
        "fabric-model-loader-v0",
        "fabric-textures-v0",
        "fabric-transformation-api-v1",
        "fabric-object-builder-api-v1",
        "fabric-dim-api-v1",
        "fabric-api-base",
        "fabric-resource-conditions-api-v1",
        "fabric-tag-events-v0",
        "fabric-loot-tables-v1",
        "fabric-mining-levels-v1",
        "fabric-tool-action-api-v1",
        "fabric-block-view-api-v2",
        "fabric-block-view-renderer-api-v1",
        "fabric-renderer-api-v1",
        "fabric-renderer-indigo",
        "fabric-renderer-loader",
        "fabric-rendering-v0",
        "fabric-rendering-v1",
        "fabric-rendering-data-attachment-v1",
        "fabric-resource-loader-v0",
        "fabric-networking-api-v1",
        "fabric-networking-v0",
        "fabric-screen-handler-api-v1",
        "fabric-events-lifecycle-v0",
        "fabric-events-rendering-v0",
        "fabric-events-input-v0",
        "fabric-key-binding-api-v1",
        "fabric-model-loading-api-v1",
        "fabric-model-loader-v0",
        "fabric-textures-v0",
        "fabric-transformation-api-v1",
        "fabric-object-builder-api-v1",
        "fabric-dim-api-v1",
        "fabric-api-base",
        "fabric-resource-conditions-api-v1",
        "fabric-tag-events-v0",
        "fabric-loot-tables-v1",
        "fabric-mining-levels-v1",
        "fabric-tool-action-api-v1",
        "fabric-block-view-api-v2",
        "fabric-block-view-renderer-api-v1",
    )

    val allowedModFileHashes: Map<String, String> = mapOf(
        "bulltweaks-1.0.0.jar" to "",
    )

    fun isModBlacklisted(fileName: String): Boolean {
        val lower = fileName.lowercase().replace(".jar", "")

        if (blacklistedModNames.any { lower.contains(it) }) return true

        if (blacklistedModFilePatterns.any { it.matches(fileName) }) return true

        return false
    }

    fun isModWhitelisted(fileName: String, modId: String?): Boolean {
        if (modId != null && allowedModIds.contains(modId)) return true

        val lower = fileName.lowercase().replace(".jar", "")
        return allowedModIds.any { lower.contains(it) }
    }
}
