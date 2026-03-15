package org.elsoft.bkdb.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.elsoft.bkdb.utils.ConfigManager

@Composable
fun EBookApp() {
    var isConfigured by remember { mutableStateOf(ConfigManager.isConfigured()) }

    MaterialTheme {
        if (!isConfigured) {
            SetupScreen(onConfigSaved = { isConfigured = true })
        } else {
            // Your existing Library UI (Tabs, List, etc.)
            MainScreen()
        }
    }
}