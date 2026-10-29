package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.component.TransactionEditorDialog
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val expenseSlices by viewModel.expenseSlices.collectAsStateWithLifecycle()
    var editingTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }

    val editingTransaction = transactions.firstOrNull { it.id == editingTransactionId }
    val totalExpense = transactions
        .filter { it.transactionType == TransactionType.EXPENSE }
        .sumOf { it.amountCents }
    val totalIncome = transactions
        .filter { it.transactionType == TransactionType.INCOME }
        .sumOf { it.amountCents }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransactionId = null
                    editorVisible = true
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = formatMonth(month),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    ExpensePieChart(slices = expenseSlices)
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Month summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("Income: +${centsToDisplay(totalIncome)}")
                        Text("Expenses: -${centsToDisplay(totalExpense)}")
                        Text("Balance: ${centsToDisplay(totalIncome - totalExpense)}")
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "No transactions for this month.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = {
                            editingTransactionId = transaction.id
                            editorVisible = true
                        },
                    )
                }
            }
        }
    }

    if (editorVisible) {
        TransactionEditorDialog(
            existing = editingTransaction,
            categories = categories,
            onDismiss = {
                editorVisible = false
                editingTransactionId = null
            },
            onSave = viewModel::saveTransaction,
            onDelete = viewModel::deleteTransaction,
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionListItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(formatDate(transaction.date), style = MaterialTheme.typography.bodySmall)
                if (transaction.description.isNotBlank()) {
                    Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (transaction.account.isNotBlank()) {
                    Text("Account: ${transaction.account}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = centsToDisplay(
                    transaction.amountCents,
                    transaction.transactionType,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
