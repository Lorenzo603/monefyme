package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.component.TransactionEditorDialog
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenTransactions: () -> Unit,
    onOpenRestore: () -> Unit,
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val previousMonthLabel = formatMonth(month.minusMonths(1)).substringBefore(' ')
    val currentMonthLabel = formatMonth(month).substringBefore(' ')
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All accounts",
                        color = Color(0xFFE7F4EC),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = Color(0xFF8BCFAE),
                    )
                }
                Text(
                    text = previousMonthLabel,
                    color = Color(0xFFA8DABD),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentMonthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF66C290),
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next month",
                        tint = Color(0xFF8BCFAE),
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

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onOpenTransactions) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open transaction list",
                    tint = Color(0xFF7CC8A4),
                    modifier = Modifier.size(40.dp),
                )
            }
            Surface(
                color = if (balance < 0) Color(0xFFF27982) else Color(0xFF70C889),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(74.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Balance ${if (balance < 0) "-" else ""}€${centsToDisplay(abs(balance))}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            IconButton(onClick = onOpenRestore) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open restore",
                    tint = Color(0xFF7CC8A4),
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundActionButton(
                symbol = "−",
                borderColor = Color(0xFFF07F86),
                symbolColor = Color(0xFFF07F86),
                tag = "add_expense_button",
                onClick = {
                    newTransactionType = TransactionType.EXPENSE
                    editorVisible = true
                },
            )
            RoundActionButton(
                symbol = "+",
                borderColor = Color(0xFF64D09B),
                symbolColor = Color(0xFF64D09B),
                tag = "add_income_button",
                onClick = {
                    newTransactionType = TransactionType.INCOME
                    editorVisible = true
                },
            )
        }
    }

    if (editorVisible) {
        TransactionEditorDialog(
            existing = null,
            categories = categories,
            initialType = newTransactionType,
            onDismiss = { editorVisible = false },
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
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFFDFDFD),
        shadowElevation = 0.dp,
        modifier = Modifier
            .testTag(tag)
            .size(132.dp)
            .shadow(2.dp, shape = CircleShape),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.dp)
                .drawBehind {
                    drawCircle(
                        color = borderColor,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 9f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                color = symbolColor,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Modifier.drawMonefyBackground(): Modifier {
    return drawBehind {
        drawRect(Color(0xFFEAF8F2))
    }
}
