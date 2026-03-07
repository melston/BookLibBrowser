package org.elsoft.bkdb.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object ConfigManager {
    val db_url = "db.url"
    val db_user = "db.user"
    val db_password = "db.password"
    val viewer_command = "viewer.command"
    val dropbox_token = "dropbox.token"
    val dropbox_refresh_token = "dropbox.refresh_token"
    val dropbox_app_key = "dropbox.app_key"
    val dropbox_app_secret = "dropbox.app_secret"


    private val properties = Properties()
    private val configDir = Platform.getConfigDir()
    private val configFile =
        File(configDir, "config.properties")

    private val cacheDir = Platform.getCacheDir()

    init {
        if (configFile.exists()) {
            FileInputStream(configFile).use { properties.load(it) }
        }
    }

    fun isConfigured() =
        properties.containsKey(db_url) &&
        properties.containsKey(db_user)

    fun saveConfig(url: String, user: String, pass: String, viewer: String, dropboxToken: String) {
        if (!configDir.exists()) configDir.mkdirs()

        properties.setProperty(db_url, url)
        properties.setProperty(db_user, user)
        properties.setProperty(db_password, pass)
        properties.setProperty(viewer_command, viewer)
        properties.setProperty(dropbox_token, dropboxToken)

        FileOutputStream(configFile).use {
            properties.store(it, "My Linux Library Configuration")
        }

        // Linux security: restrict to current user (600)
        configFile.setReadable(false, false)
        configFile.setReadable(true, true)
        configFile.setWritable(true, true)
    }

    fun get(key: String): String = properties.getProperty(key, "")
    fun get(key: String, default: String): String = properties.getProperty(key, default)
}