package org.elsoft.bkdb

import java.io.File

enum class OS {
    WINDOWS, LINUX, MACOS, UNKNOWN
}

object Platform {
    val current: OS = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("win") -> OS.WINDOWS
            it.contains("linux") -> OS.LINUX
            it.contains("mac") -> OS.MACOS
            else -> OS.UNKNOWN
        }
    }

    /**
     * Returns a safe, non-hidden directory for the ebook cache.
     * Linux: ~/Documents/EBookLibrary/Cache
     * Windows: C:\Users\Name\AppData\Local\EBookLibrary\Cache
     */
    fun getCacheDir(): File {
        val baseDir = when (current) {
            OS.WINDOWS -> File(System.getenv("LOCALAPPDATA"),
                "EBookLibrary")
            OS.LINUX -> File(System.getProperty("user.home"),
                "Documents/EBookLibrary")
            else -> File(System.getProperty("user.home"),
                "EBookLibrary")
        }
        return File(baseDir, "Cache").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Returns the directory for config.properties.
     * We can keep this hidden as only our app reads it.
     */
    fun getConfigDir(): File {
        return when (current) {
            OS.WINDOWS -> File(System.getenv("APPDATA"),
                "EBookLibrary")
            else -> File(System.getProperty("user.home"),
                ".config/ebooklibrary")
        }.apply { if (!exists()) mkdirs() }
    }

    /**
     * Provides a sensible default command if the config is empty.
     */
    fun getDefaultViewerCommand(path: String): String {
        return when (current) {
            OS.WINDOWS -> "cmd /c start \"\" \"$path\""
            OS.LINUX -> "xdg-open \"$path\""
            else -> ""
        }
    }

    /**
     * Cross-platform way to launch a process.
     */
    fun openFile(file: File, customCommand: String?): Result<Unit> {
        val absolutePath = file.absolutePath

        // Use custom command, or fall back to the platform default
        val commandToUse = when {
            !customCommand.isNullOrBlank() -> customCommand.replace("%f", absolutePath)
            else -> getDefaultViewerCommand(absolutePath)
        }

        if (commandToUse.isEmpty()) {
            return Result.failure(Exception("No viewer command available for this platform."))
        }

        return try {
            val parts = parseCommand(commandToUse)
            ProcessBuilder(parts)
                .directory(File(System.getProperty("user.home")))
                .start()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun parseCommand(command: String): List<String> {
    val regex = """[^\s"']+|"([^"]*)"|'([^']*)'""".toRegex()
    return regex.findAll(command).map {
        it.groupValues[1].ifEmpty {
            it.groupValues[2].ifEmpty { it.groupValues[0] }
        }
    }.toList()
}