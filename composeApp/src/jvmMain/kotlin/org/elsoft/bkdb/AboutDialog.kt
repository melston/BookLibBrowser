package org.elsoft.bkdb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val vm = viewModel<EBookViewModel>()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ebook Library Browser") },
        text = {
            Column {
                Text("Version: ${AppInfo.VERSION}", style = MaterialTheme.typography.bodyMedium)
                Text(AppInfo.COPYRIGHT, style = MaterialTheme.typography.bodySmall)

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("A streamlined bridge between Dropbox and your local readers.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}