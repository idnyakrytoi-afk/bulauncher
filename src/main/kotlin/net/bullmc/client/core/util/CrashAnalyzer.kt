package net.bullmc.client.core.util

import java.io.File

data class CrashReport(
    val summary: String,
    val cause: String,
    val suggestion: String,
    val severity: CrashSeverity,
    val rawLines: List<String>
)

enum class CrashSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

object CrashAnalyzer {

    fun analyzeLog(gameDir: File): CrashReport? {
        val logFile = File(gameDir, "logs/latest.log")
        if (!logFile.exists()) return null

        val lines = logFile.readLines()
        if (lines.isEmpty()) return null

        val crashLines = mutableListOf<String>()
        var inCrash = false

        for (line in lines) {
            if (line.contains("---- Minecraft Crash Report ----") || line.contains("FATAL ERROR")) {
                inCrash = true
            }
            if (inCrash) crashLines.add(line)
            if (line.contains("java.lang.OutOfMemoryError")) {
                return CrashReport(
                    summary = "Не хватает оперативной памяти",
                    cause = "Minecraft потребовал больше RAM, чем выделено",
                    suggestion = "Увеличьте объём ОЗУ в настройках (текущий лимит недостаточен)",
                    severity = CrashSeverity.CRITICAL,
                    rawLines = listOf(line)
                )
            }
        }

        if (crashLines.isEmpty()) {
            val fatalLines = lines.filter {
                it.contains("FATAL") || it.contains("Exception") || it.contains("Error")
            }
            if (fatalLines.isEmpty()) return null
            crashLines.addAll(fatalLines.takeLast(30))
        }

        val fullCrash = crashLines.joinToString("\n")

        when {
            fullCrash.contains("OutOfMemoryError") -> return CrashReport(
                summary = "Не хватает оперативной памяти",
                cause = "Minecraft потребовал больше RAM",
                suggestion = "Увеличьте ОЗУ в настройках лаунчера",
                severity = CrashSeverity.CRITICAL,
                rawLines = crashLines
            )
            fullCrash.contains("NoSuchMethodError") || fullCrash.contains("ClassNotFoundException") -> {
                val className = Regex("class (.+?) ").find(fullCrash)?.groupValues?.get(1) ?: "неизвестный класс"
                return CrashReport(
                    summary = "Конфликт модов",
                    cause = "Несовместимые моды: $className",
                    suggestion = "Попробуйте удалить недавно добавленные моды или обновите все моды",
                    severity = CrashSeverity.ERROR,
                    rawLines = crashLines
                )
            }
            fullCrash.contains("ModResolutionException") || fullCrash.contains("IncompatibleModError") -> CrashReport(
                summary = "Несовместимые моды",
                cause = "Один или несколько модов несовместимы друг с другом",
                suggestion = "Проверьте совместимость модов. Удалите конфликтующие моды.",
                severity = CrashSeverity.ERROR,
                rawLines = crashLines
            ).let { return it }
            fullCrash.contains("GLFW") || fullCrash.contains("OpenGL") -> return CrashReport(
                summary = "Ошибка графики",
                cause = "Проблема с видеодрайвером или OpenGL",
                suggestion = "Обновите видеодрайвер. Попробуйте добавить -Dfml.earlyWindowSkipGLVersions в JVM аргументы.",
                severity = CrashSeverity.ERROR,
                rawLines = crashLines
            )
            fullCrash.contains("StackOverflowError") -> return CrashReport(
                summary = "Stack Overflow",
                cause = "Слишком глубокая рекурсия (баг мода)",
                suggestion = "Попробуйте удалить моды, которые добавляют новые блоки/сущности",
                severity = CrashSeverity.ERROR,
                rawLines = crashLines
            )
            fullCrash.contains("ConnectionRefused") || fullCrash.contains("ConnectException") -> return CrashReport(
                summary = "Сервер недоступен",
                cause = "Не удалось подключиться к серверу",
                suggestion = "Проверьте, запущен ли сервер. Проверьте IP и порт.",
                severity = CrashSeverity.WARNING,
                rawLines = crashLines
            )
            fullCrash.contains("IOException") || fullCrash.contains("ZipException") -> return CrashReport(
                summary = "Повреждённые файлы",
                cause = "Файлы игры или модов повреждены",
                suggestion = "Попробуйте переустановить версию Minecraft и моды",
                severity = CrashSeverity.ERROR,
                rawLines = crashLines
            )
        }

        val lastException = crashLines.lastOrNull { it.contains("Exception") || it.contains("Error") }
            ?: crashLines.lastOrNull()

        return CrashReport(
            summary = "Краш",
            cause = lastException?.take(200) ?: "Неизвестная ошибка",
            suggestion = "Проверьте логи и попробуйте переустановить версию Minecraft",
            severity = CrashSeverity.ERROR,
            rawLines = crashLines.takeLast(30)
        )
    }

    fun getRecentCrashCount(gameDir: File): Int {
        val logDir = File(gameDir, "logs")
        if (!logDir.exists()) return 0
        return logDir.listFiles()?.count {
            it.name.endsWith(".log") && it.readText().let { text ->
                text.contains("---- Minecraft Crash Report ----") || text.contains("FATAL ERROR")
            }
        } ?: 0
    }
}
