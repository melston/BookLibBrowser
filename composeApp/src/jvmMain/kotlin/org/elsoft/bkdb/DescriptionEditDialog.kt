package org.elsoft.bkdb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*

@Composable
fun DescriptionEditDialog(
    book: EBook,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    // Local state for the text field
    var currentText by remember { mutableStateOf(book.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Description") },
        text = {
            Column {
                Text(book.title, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentText,
                    // Name the parameter explicitly as 'newString' to avoid 'it' resolution errors
                    onValueChange = { newString -> currentText = newString },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") }, // Ensure material3.Text is imported
                    minLines = 3,
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Using .trim() and checking isEmpty() if ifBlank() is acting up
                val result = if (currentText.trim().isEmpty()) null else currentText
                onConfirm(result)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
