package org.elsoft.bkdb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.DriverManager

@Composable
fun SetupScreen(onConfigSaved: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("jdbc:mysql://localhost:3306/bookdb") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
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
                label = { Text("Username") }
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation() // Hides characters
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
                            ConfigManager.saveConfig(url, user, pass)
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
