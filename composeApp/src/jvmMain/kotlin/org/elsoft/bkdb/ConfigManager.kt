package org.elsoft.bkdb

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object ConfigManager {
    private val properties = Properties()
    private val configDir =
        File(System.getProperty("user.home"), ".ebook-lib-browser")
    private val configFile =
        File(configDir, "config.properties")

    init {
        if (configFile.exists()) {
            FileInputStream(configFile).use { properties.load(it) }
        }
    }

    fun isConfigured() =
        properties.containsKey("db.url") &&
        properties.containsKey("db.user")

    fun saveConfig(url: String, user: String, pass: String) {
        if (!configDir.exists()) configDir.mkdirs()

        properties.setProperty("db.url", url)
        properties.setProperty("db.user", user)
        properties.setProperty("db.password", pass)

        FileOutputStream(configFile).use {
            properties.store(it, "My Linux Library Configuration")
        }

        // Linux security: restrict to current user (600)
        configFile.setReadable(false, false)
        configFile.setReadable(true, true)
        configFile.setWritable(true, true)
    }

    fun get(key: String) = properties.getProperty(key, "")
}
