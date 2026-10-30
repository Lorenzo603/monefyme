package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.component.TransactionEditorDialog
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val previousMonthLabel = formatMonth(month.minusMonths(1))
    val currentMonthLabel = formatMonth(month)
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val expenseSlices by viewModel.expenseSlices.collectAsStateWithLifecycle()
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var newTransactionType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }

    val totalExpense = transactions
        .filter { it.transactionType == TransactionType.EXPENSE }
        .sumOf { it.amountCents }
    val totalIncome = transactions
        .filter { it.transactionType == TransactionType.INCOME }
        .sumOf { it.amountCents }

    val balance = totalIncome - totalExpense

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .drawMonefyBackground(),
        ) {
            Surface(color = Color(0xFF74C792)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White)
                            Icon(Icons.Outlined.SwapHoriz, contentDescription = "Switch account", tint = Color.White)
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Monefy",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "  All accounts",
                            color = Color(0xFFE7F4EC),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = Color(0xFF5EBE8E),
                        )
                    }
                    Text(
                        text = previousMonthLabel.substringBefore(' '),
                        color = Color(0xFF8BCFAE),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentMonthLabel.substringBefore(' '),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5EBE8E),
                    )
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next month",
                            tint = Color(0xFF5EBE8E),
                        )
                    }
                }
            }

            ExpensePieChart(
                slices = expenseSlices,
                incomeCents = totalIncome,
                expenseCents = totalExpense,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "☰",
                    color = Color(0xFF7CC8A4),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                )
                Surface(
                    color = if (balance < 0) Color(0xFFF27982) else Color(0xFF70C889),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .height(76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Balance ${if (balance < 0) "-" else ""}${centsToDisplay(kotlin.math.abs(balance))}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = "☰",
                    color = Color(0xFF7CC8A4),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundActionButton(
                    symbol = "−",
                    borderColor = Color(0xFFF07F86),
                    symbolColor = Color(0xFFF07F86),
                    onClick = {
                        newTransactionType = TransactionType.EXPENSE
                        editorVisible = true
                    },
                )
                RoundActionButton(
                    symbol = "+",
                    borderColor = Color(0xFF64D09B),
                    symbolColor = Color(0xFF64D09B),
                    onClick = {
                        newTransactionType = TransactionType.INCOME
                        editorVisible = true
                    },
                )
            }
        }
    }

    if (editorVisible) {
        TransactionEditorDialog(
            existing = null,
            categories = categories,
            initialType = newTransactionType,
            onDismiss = {
                editorVisible = false
            },
            onSave = viewModel::saveTransaction,
            onDelete = viewModel::deleteTransaction,
        )
    }
}

@Composable
private fun RoundActionButton(
    symbol: String,
    borderColor: Color,
    symbolColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFFDFDFD),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(144.dp)
            .shadow(2.dp, shape = CircleShape),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .drawBehind {
                    drawCircle(
                        color = borderColor,
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                color = symbolColor,
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Modifier.drawMonefyBackground(): Modifier {
    return this.drawBehind {
        drawRect(Color(0xFFEAF8F2))
    }
}
