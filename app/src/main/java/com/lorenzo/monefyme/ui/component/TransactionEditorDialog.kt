package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.util.centsToEditableAmountText
import com.lorenzo.monefyme.ui.util.formatDate
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun TransactionEditorDialog(
    existing: TransactionListItem?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (
        transactionId: Long?,
        dateText: String,
        amountText: String,
        type: TransactionType,
        categoryName: String,
        description: String,
        account: String,
    ) -> Boolean,
    onDelete: suspend (transactionId: Long) -> Unit,
) {
    var dateText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.date?.let(::formatDate) ?: formatDate(LocalDate.now()))
    }
    var amountText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.amountCents?.let(::centsToEditableAmountText) ?: "")
    }
    var type by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.transactionType ?: TransactionType.EXPENSE)
    }
    var category by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.categoryName ?: "")
    }
    var description by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.description ?: "")
    }
    var account by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.account ?: "")
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sortedCategories = remember(categories) { categories.sortedBy { it.lowercase() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Add transaction" else "Edit transaction",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { currentType ->
                        FilterChip(
                            selected = currentType == type,
                            onClick = { type = currentType },
                            label = {
                                Text(
                                    text = currentType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (currentType == type) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date (dd/MM/yyyy)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category") },
                    singleLine = true,
                )
                Text(
                    modifier = Modifier
                        .clickable { categoryMenuExpanded = true }
                        .padding(top = 2.dp),
                    text = "Pick existing category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    sortedCategories.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                category = suggestion
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val success = onSave(
                            existing?.id,
                            dateText,
                            amountText,
                            type,
                            category,
                            description,
                            account,
                        )
                        if (success) {
                            onDismiss()
                        }
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                onDelete(existing.id)
                                onDismiss()
                            }
                        },
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
