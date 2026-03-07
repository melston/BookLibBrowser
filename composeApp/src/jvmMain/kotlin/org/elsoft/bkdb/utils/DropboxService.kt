package org.elsoft.bkdb.utils

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import java.io.File
import java.io.FileOutputStream

object DropboxService {
    private val cacheDir = Platform.getCacheDir()

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }
    private val client: DbxClientV2 by lazy {
        val config = DbxRequestConfig.newBuilder("elsoft.EBookLib").build()

        // Use the credential constructor instead of a simple string
        val credential = DbxCredential(
            "", // Access token (can be empty, it will auto-refresh)
            -1L, // Expires in (auto-handled)
            ConfigManager.get(ConfigManager.dropbox_refresh_token),
            ConfigManager.get(ConfigManager.dropbox_app_key),
            ConfigManager.get(ConfigManager.dropbox_app_secret)
        )

        // Create the client
        val clientV2 = DbxClientV2(config, credential)

        // MANDATORY: Force a refresh if the token is currently empty
        // This populates the internal 'Bearer' header correctly.
        try {
            credential.refresh(config)
        } catch (e: Exception) {
            println("Initial Dropbox token refresh failed: ${e.message}")
        }

        clientV2
    }

    fun download(dropboxPath: String, outFileName: String): String {
        // 1. Create a cache file in the cache directory
        val outFile = File(cacheDir, outFileName)

        if (!outFile.exists()) {
            // 2. Download from Dropbox
            // This will throw if an exception happens.
            FileOutputStream(outFile).use { outputStream ->
                client.files().downloadBuilder(dropboxPath).download(outputStream)
                outputStream.flush();
            }
            Thread.sleep(100)
        }

        outFile.setReadable(true, false)
        return outFile.path
    }
}