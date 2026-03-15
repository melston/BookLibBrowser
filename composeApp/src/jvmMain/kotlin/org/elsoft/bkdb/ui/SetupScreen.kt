package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.elsoft.bkdb.data.remote.DatabaseManager
import org.elsoft.bkdb.utils.ConfigManager

@Composable
fun SetupScreen(onConfigSaved: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("jdbc:mysql://localhost:3306/bookdb") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var viewer by remember { mutableStateOf("") }
    var dropboxToken by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Database Setup",
                style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("JDBC URL") }
            )
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("DB Username") }
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("DB Password") },
                visualTransformation = PasswordVisualTransformation() // Hides characters
            )
            OutlinedTextField(
                value = viewer,
                onValueChange = { viewer = it },
                label = { Text("Viewer Application") }
            )
            OutlinedTextField(
                value = dropboxToken,
                onValueChange = { dropboxToken = it },
                label = { Text("Dropbox Token") }
            )

            Spacer(Modifier.height(24.dp))
            Button(
                enabled = !isTesting,
                onClick = {
                    scope.launch {
                        isTesting = true
                        val success = withContext(Dispatchers.IO) {
                            DatabaseManager.testConnection(url, user, pass)
                        }

                        if (success) {
                            ConfigManager.saveConfig(url, user, pass, viewer, dropboxToken)
                            onConfigSaved()
                        } else {
                            isTesting = false
                            snackbarHostState.showSnackbar(
                                message = "Connection failed. Check your URL, user, or password.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Save and Connect")
                }
            }
        }
    }
}
