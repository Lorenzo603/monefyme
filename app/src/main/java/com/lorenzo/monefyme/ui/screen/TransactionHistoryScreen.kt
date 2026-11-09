package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.ui.component.TransactionEditorDialog
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import com.lorenzo.monefyme.ui.category.resolveColor
import com.lorenzo.monefyme.ui.category.resolveIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenRestore: () -> Unit,
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val expansionRequest by viewModel.categoryExpansionRequest.collectAsStateWithLifecycle()
    val categoriesMap = remember(allCategories) {
        allCategories.associateBy { it.id }
    }

    // Category whose accordion the pie chart click asked to open, taken out of the ViewModel here so
    // it is applied exactly once.
    var categoryToExpand by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(expansionRequest) {
        val requested = viewModel.consumeCategoryExpansionRequest()
        if (requested != null) {
            categoryToExpand = requested
        }
    }

    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by rememberSaveable { mutableStateOf<TransactionListItem?>(null) }
    val editingTransactionId = editingTransaction?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRestore) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Open restore")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF74C792),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Month selector row
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

            HorizontalDivider(color = Color(0xFFD4EDE0))

            // Toggle grouping button
            var isGroupedByCategory by rememberSaveable { mutableStateOf(true) }
            // Category accordions only exist in the category grouping, so a request to expand one
            // switches back to it.
            LaunchedEffect(categoryToExpand) {
                isGroupedByCategory = groupedByCategoryAfterExpansionRequest(
                    isGroupedByCategory = isGroupedByCategory,
                    requestedCategory = categoryToExpand,
                )
            }
            GroupingToggle(
                isGroupedByCategory = isGroupedByCategory,
                onToggle = { isGroupedByCategory = !isGroupedByCategory },
            )

            // Transaction list grouped by category or date
            TransactionGroupedList(
                transactions = transactions,
                categoriesMap = categoriesMap,
                isGroupedByCategory = isGroupedByCategory,
                expandCategory = categoryToExpand,
                onTransactionClick = { transaction ->
                    editingTransaction = transaction
                    editorVisible = true
                },
            )
        }
    }

    if (editorVisible) {
        TransactionEditorDialog(
            existing = editingTransaction,
            categories = allCategories.map { it.displayName },
            onDismiss = {
                editorVisible = false
                editingTransaction = null
            },
            onSave = viewModel::saveTransaction,
            onDelete = viewModel::deleteTransaction,
        )
    }
}

@Composable
private fun GroupingToggle(
    isGroupedByCategory: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isGroupedByCategory) "By category" else "By date",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4B5558),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = if (isGroupedByCategory) Color(0xFF74C792) else Color(0xFFD4EDE0),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .size(52.dp, 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onToggle),
            )
        }
    }
}

@Composable
internal fun TransactionGroupedList(
    transactions: List<TransactionListItem>,
    categoriesMap: Map<Long, CategoryItem>,
    isGroupedByCategory: Boolean,
    expandCategory: String? = null,
    onTransactionClick: (TransactionListItem) -> Unit,
) {
    val listState = rememberLazyListState()

    // Group transactions by category or date
    val groupedTransactions = remember(transactions, isGroupedByCategory) {
        if (isGroupedByCategory) {
            transactions.groupBy { it.categoryName }
        } else {
            transactions.groupBy { formatDate(it.date) }
        }
    }

    // Which groups the user opened. A group with no entry is collapsed, so every group - also the
    // ones that appear later (new month, new group) - starts collapsed.
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // [expandCategory] - a click on that category's pie chart section - opens its accordion without
    // the user touching anything. It is applied once per requested category, so it never overrides a
    // header the user collapsed (or a group they opened) afterwards.
    val appliedExpansion = remember { mutableStateOf<String?>(null) }
    val groupNames = groupedTransactions.keys.toList()
    LaunchedEffect(expandCategory, groupNames) {
        val category = expandCategory ?: return@LaunchedEffect
        if (appliedExpansion.value == category) return@LaunchedEffect
        appliedExpansion.value = category
        groupsToExpand(groupNames, category, isGroupedByCategory).forEach { groupName ->
            expandedGroups[groupName] = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groupedTransactions.forEach { (groupName, groupTransactions) ->
            val isExpanded = isGroupExpanded(expandedGroups, groupName)

            item(key = "group_header_$groupName") {
                GroupHeader(
                    groupName = groupName,
                    groupTransactions = groupTransactions,
                    categoriesMap = categoriesMap,
                    isGroupedByCategory = isGroupedByCategory,
                    isExpanded = isExpanded,
                    onClick = {
                        expandedGroups[groupName] = expandedGroupsAfterToggle(expandedGroups, groupName)
                    },
                )
            }

            // A collapsed accordion shows its header only; clicking the header above expands it.
            if (isExpanded) {
                items(
                    items = groupTransactions,
                    key = { it.id },
                ) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                    )
                }
            }
        }
    }
}

/**
 * Accordion state of one transaction-history group: only groups the user clicked have an entry in
 * [expandedGroups], so an unknown (never clicked) group is collapsed.
 */
internal fun isGroupExpanded(expandedGroups: Map<String, Boolean>, groupName: String): Boolean =
    expandedGroups[groupName] == true

/**
 * State of [groupName] after its header was clicked: that group's accordion flips on its own,
 * every other group keeps its state.
 */
internal fun expandedGroupsAfterToggle(expandedGroups: Map<String, Boolean>, groupName: String): Boolean =
    !isGroupExpanded(expandedGroups, groupName)

/**
 * The group headers a click on [requestedCategory] opens: the group whose name is that category,
 * compared case- and whitespace-insensitively (the pie chart and the transactions may spell it
 * differently). Nothing is expanded when the list is grouped by date, when no category was requested,
 * or when no group matches it.
 */
internal fun groupsToExpand(
    groupNames: List<String>,
    requestedCategory: String?,
    isGroupedByCategory: Boolean,
): List<String> {
    if (!isGroupedByCategory || requestedCategory.isNullOrBlank()) return emptyList()
    val target = normalizedGroupName(requestedCategory)
    return groupNames.filter { normalizedGroupName(it) == target }
}

private fun normalizedGroupName(name: String): String = name.trim().lowercase()

/**
 * Grouping to show once an expansion request arrives: category accordions only exist in the category
 * grouping, so a request for [requestedCategory] switches back to it. Without a request the grouping
 * the user chose stays as it is.
 */
internal fun groupedByCategoryAfterExpansionRequest(
    isGroupedByCategory: Boolean,
    requestedCategory: String?,
): Boolean = if (requestedCategory != null) true else isGroupedByCategory

@Composable
private fun GroupHeader(
    groupName: String,
    groupTransactions: List<TransactionListItem>,
    categoriesMap: Map<Long, CategoryItem>,
    isGroupedByCategory: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val totalAmount = groupTransactions.sumOf { it.amountCents }
    val transactionCount = groupTransactions.size
    val categoryItem = if (isGroupedByCategory) {
        groupTransactions.firstOrNull()?.let { t ->
            categoriesMap[t.categoryId]
        }
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("group_header_$groupName"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAF8F2),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isGroupedByCategory && categoryItem != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(categoryItem.resolveColor()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryItem.resolveIcon(),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF95A2A3),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "$transactionCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF95A2A3),
                    )
                    Text(
                        text = currencyAnnotatedString(
                            text = centsToDisplay(totalAmount),
                            baseFontSize = MaterialTheme.typography.titleMedium.fontSize,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("transaction_row_${transaction.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                text = currencyAnnotatedString(
                    text = centsToDisplay(transaction.amountCents, transaction.transactionType),
                    baseFontSize = MaterialTheme.typography.titleMedium.fontSize,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
