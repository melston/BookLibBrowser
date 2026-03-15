package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.elsoft.bkdb.LocalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown() {
    val vm = LocalViewModel.current

    // 1. Collect the states from the ViewModel
    val categories by vm.allCategories.collectAsState()
    val selectedCategory by vm.categoryFilter.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // 2. The Material 3 Wrapper for Dropdowns
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        OutlinedTextField(
            // Show "All Categories" if the selection is null
            value = selectedCategory?.name ?: "All Categories",
            onValueChange = {},
            readOnly = true, // User picks from list, doesn't type
            label = { Text("Filter by Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Option to reset the filter
            DropdownMenuItem(
                text = { Text("All Categories") },
                onClick = {
                    vm.setCategoryFilter(null)
                    expanded = false
                }
            )

            // Map the categories from your DB into menu items
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        vm.setCategoryFilter(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
