package org.elsoft.bkdb

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object ConfigManager {
    private val properties = Properties()
    private val configFile = File(
        System.getProperty("user.home"),
        ".ebook-lib-browser/config.properties")

    init {
        if (configFile.exists()) {
            FileInputStream(configFile).use { properties.load(it) }
        } else {
            // Optional: Create the directory and a template file if it doesn't exist
            configFile.parentFile.mkdirs()
            println("Config file not found at ${configFile.absolutePath}")
        }
    }

    fun get(key: String, default: String = ""): String {
        return properties.getProperty(key, default)
    }
}
