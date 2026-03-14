package org.elsoft.bkdb.utils

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest  // Alias to avoid conflict


// Remove: import com.dropbox.core.DbxWebAuth.Request (if present)

object DropboxService {
    private val cacheDir = Platform.getCacheDir()
    private val httpClient = OkHttpClient()
    private val gson = GsonBuilder().setPrettyPrinting().create()
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

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    /**
     * Download a file from the specified path in Dropbox and write it
     * to the filename provided.  The filename is expected to have no
     * directory information and the file will be written in the
     * cacheDir directory. If there is already a file at that
     * path then skip the download.
     *
     * @return the full path to the file.
     */
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

    suspend fun delete(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Get Token (Throw exception if null to trigger runCatching failure)
            val token = getFreshAccessToken()
                ?: throw Exception("Dropbox authorization failed")

            // 2. Setup Client
            val config = DbxRequestConfig.newBuilder("ebook-manager").build()
            val client = DbxClientV2(config, token)

            // 3. Format Path
            val dropboxPath = remotePath.substringAfter("dropbox:")
            val formattedPath = if (dropboxPath.startsWith("/")) dropboxPath else "/$dropboxPath"

            println("Dropbox: Attempting to delete $formattedPath")

            // 4. Execute
            client.files().deleteV2(formattedPath)

            // 5. Return success unit
            Unit
        }
    }

    private suspend fun getFreshAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val clientid = ConfigManager.get(ConfigManager.dropbox_app_key).trim()
            val secret = ConfigManager.get(ConfigManager.dropbox_app_secret).trim()
            val refreshToken = ConfigManager.get(ConfigManager.dropbox_refresh_token).trim()

            val requestBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken) // REFRESH_TOKEN_FROM_DB) // Your new token
                .add("client_id", clientid) // APP_KEY)
                .add("client_secret", secret) // ) APP_SECRET)
                .build()

            val request = OkHttpRequest.Builder()
                .url("https://api.dropbox.com/oauth2/token")
                .post(requestBody)
                .build()

            // Replace the JSONObject logic with this:
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    println("Dropbox API Error: Code ${response.code}, Body: $errorBody")
                    return@withContext null
                }

                val responseData = response.body?.string() ?: ""

                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(responseData, mapType)

                data["access_token"]?.toString()
            }
        } catch (e: Exception) {
            println("Dropbox Auth Error: ${e.message}")
            null
        }
    }
}