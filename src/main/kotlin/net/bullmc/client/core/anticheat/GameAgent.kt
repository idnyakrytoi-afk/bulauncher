package net.bullmc.client.core.anticheat

import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

object GameAgent {
    @JvmStatic
    fun agentmain(args: String?, inst: Instrumentation) {
        println("[ANTICHEAT-AGENT] Агент загружен в Minecraft процесс")

        inst.addTransformer(object : ClassFileTransformer {
            override fun transform(
                loader: ClassLoader?,
                className: String?,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray?
            ): ByteArray? {
                if (className != null) {
                    val lower = className.lowercase()
                    if (isBlacklistedClass(lower)) {
                        println("[ANTICHEAT-AGENT] ЗАБЛОКИРОВАН: $className")
                        throw SecurityException(
                            "[BullMC AntiCheat] Обнаружен запрещённый класс: $className. Игра будет закрыта."
                        )
                    }
                }
                return null
            }
        })

        val loaded = inst.allLoadedClasses.filter { clazz ->
            isBlacklistedClass(clazz.name.lowercase())
        }.toTypedArray()

        if (loaded.isNotEmpty()) {
            inst.retransformClasses(*loaded)
        }
    }

    @JvmStatic
    fun premain(args: String?, inst: Instrumentation) {
        agentmain(args, inst)
    }

    private fun isBlacklistedClass(className: String): Boolean {
        val blacklist = listOf(
            "wurst",
            "impact",
            "meteorclient",
            "meteor.client",
            "baritone",
            "rusherhack",
            "aristois",
            "futureclient",
            "phoenixclient",
            "vape",
            "novoline",
            "huzuni",
            "kiddion",
            "sigma",
            "bleachhack",
            "inertia",
            "ghostly",
            "sudden",
            "olympus",
            "moonsworth",
            "cef",
            "catalyst",
            "cheatengine",
            "aimbot",
            "killaura",
            "reachmod",
            "wallhack",
            "xray",
            "bhop",
            "autoclicker",
            "nofall",
            "scaffold",
            "autocrystal",
            "blink",
            "phase",
            "step",
            "timer",
            "velocity",
            "antiknockback",
            "hitbox",
            "expandhitbox",
            "combo",
        )

        return blacklist.any { className.contains(it) }
    }
}
