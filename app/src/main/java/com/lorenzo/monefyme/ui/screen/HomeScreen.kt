package com.lorenzo.monefyme.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.category.resolveColor
import com.lorenzo.monefyme.ui.category.resolveIcon
import com.lorenzo.monefyme.ui.component.CategoriesSidebar
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.component.TransactionEditorDialog
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenRestore: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    // Opens the add-expense flow with the clicked pie chart icon slot's category preselected.
    onAddExpenseForCategory: (String) -> Unit,
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val currentMonth = remember { YearMonth.now() }
    val anchorPage = remember { 1_000_000 }
    val pageCount = remember { anchorPage + 1 }
    val monthForPage: (Int) -> YearMonth = { page ->
        currentMonth.minusMonths((anchorPage - page).toLong())
    }
    val pageForMonth: (YearMonth) -> Int = { targetMonth ->
        val diff = ChronoUnit.MONTHS.between(targetMonth, currentMonth)
            .coerceAtLeast(0L)
            .toInt()
        (anchorPage - diff).coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = pageForMonth(month),
        pageCount = { pageCount },
    )

    LaunchedEffect(pagerState, currentMonth, anchorPage) {
        snapshotFlow { pagerState.settledPage }
            .map(monthForPage)
            .distinctUntilChanged()
            .collect { settledMonth ->
                viewModel.selectMonth(settledMonth)
            }
    }

    LaunchedEffect(month) {
        val targetPage = pageForMonth(month)
        if (!pagerState.isScrollInProgress && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val canMoveToNextMonth = month.isBefore(currentMonth)
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var sidebarOpen by rememberSaveable { mutableStateOf(false) }
    var historyRevealOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
        // Transaction editor state
        var editorVisible by rememberSaveable { mutableStateOf(false) }
        var editingTransaction by rememberSaveable { mutableStateOf<com.lorenzo.monefyme.data.model.TransactionListItem?>(null) }
    
    // Callbacks for expanding the transaction history sheet (used by pie chart clicks)
    val onHistoryOffsetChange: (Float) -> Unit = { delta ->
        historyRevealOffset = (historyRevealOffset + delta).coerceIn(0f, 1f)
    }
    val onToggleHistoryFull: (Boolean) -> Unit = { isOpen ->
        scope.launch {
            animateRevealOffset(historyRevealOffset, if (isOpen) 1f else 0f, density) { offset ->
                historyRevealOffset = offset
            }
        }
    }

    BackHandler(enabled = sidebarOpen) {
        sidebarOpen = false
    }

    BackHandler(enabled = historyRevealOffset > 0f) {
        scope.launch {
            animateRevealOffset(historyRevealOffset, 0f, density) { offset ->
                historyRevealOffset = offset
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawMonefyBackground(),
        ) {
            Surface(color = Color(0xFF74C792)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { sidebarOpen = true }) {
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

            MonthTicker(
                pagerState = pagerState,
                pageCount = pageCount,
                monthForPage = monthForPage,
                canMoveToNextMonth = canMoveToNextMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .testTag("month_pager"),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val pageMonth = monthForPage(page)
                    val pageTransactions by viewModel.observeMonthTransactions(pageMonth)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val pageExpenseSlices by viewModel.observeMonthExpenseSlices(pageMonth)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val pageTotalExpense = pageTransactions
                        .filter { transaction -> transaction.transactionType == TransactionType.EXPENSE }
                        .sumOf { transaction -> transaction.amountCents }
                    val pageTotalIncome = pageTransactions
                        .filter { transaction -> transaction.transactionType == TransactionType.INCOME }
                        .sumOf { transaction -> transaction.amountCents }
                    val pageBalance = pageTotalIncome - pageTotalExpense

                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ExpensePieChart(
                            slices = pageExpenseSlices,
                            incomeCents = pageTotalIncome,
                            expenseCents = pageTotalExpense,
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp)
                                .graphicsLayer { alpha = 1f - historyRevealOffset },
                            onCategoryClick = { categoryName ->
                                viewModel.requestCategoryExpansion(categoryName)
                                onToggleHistoryFull(true)
                            },
                            // A click on a category's icon slot opens the add-expense flow with that
                            // category already preselected.
                            onCategorySlotClick = { categoryName ->
                                onAddExpenseForCategory(categoryName)
                            },
                        )

                        BalanceAndHistorySheet(
                            viewModel = viewModel,
                            balance = pageBalance,
                            transactions = pageTransactions,
                            historyRevealOffset = historyRevealOffset,
                            onHistoryOffsetChange = onHistoryOffsetChange,
                            onToggleHistoryFull = onToggleHistoryFull,
                                                    month = month,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Action buttons (always visible, below the sheet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundActionButton(
                    symbol = "−",
                    borderColor = Color(0xFFF07F86),
                    symbolColor = Color(0xFFF07F86),
                    tag = "add_expense_button",
                    onClick = {
                        onAddTransaction(TransactionType.EXPENSE)
                    },
                )
                RoundActionButton(
                    symbol = "+",
                    borderColor = Color(0xFF64D09B),
                    symbolColor = Color(0xFF64D09B),
                    tag = "add_income_button",
                    onClick = {
                        onAddTransaction(TransactionType.INCOME)
                    },
                )
            }
        } // end Column (main content)

        // Scrim
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { sidebarOpen = false },
                    ),
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            CategoriesSidebar(
                categories = allCategories,
                onAddCategory = viewModel::saveCategory,
                onEditCategory = viewModel::updateCategory,
                onDeleteCategory = viewModel::deleteCategory,
                onClose = { sidebarOpen = false },
                onOpenRestore = {
                    sidebarOpen = false
                    onOpenRestore()
                },
            )
        }
    } // end Box
}

@Composable
private fun BalanceAndHistorySheet(
    viewModel: MainViewModel,
    balance: Long,
    transactions: List<com.lorenzo.monefyme.data.model.TransactionListItem>,
    historyRevealOffset: Float,
    onHistoryOffsetChange: (Float) -> Unit,
    onToggleHistoryFull: (Boolean) -> Unit,
    month: YearMonth,
    modifier: Modifier = Modifier,
) {
    val groupByCategory by viewModel.groupByCategory.collectAsStateWithLifecycle()
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by rememberSaveable { mutableStateOf<com.lorenzo.monefyme.data.model.TransactionListItem?>(null) }
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categoriesMap = remember(allCategories) {
        allCategories.associateBy { it.id }
    }
    
    val expansionRequest by viewModel.categoryExpansionRequest.collectAsStateWithLifecycle()
    var categoryToExpand by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(expansionRequest) {
        val requested = viewModel.consumeCategoryExpansionRequest()
        if (requested != null) {
            categoryToExpand = requested
        }
    }

    val balanceBoxHeightDp = 70.dp
    val sheetHeightDp = 500.dp

    // pointerInput(Unit) below runs once and captures its surroundings; reading the plain
    // historyRevealOffset parameter there would see the stale value from the first composition,
    // so dragging the sheet to the top always snapped closed on release. rememberUpdatedState
    // gives the gesture lambdas the current reveal offset so a dragged-open sheet sticks open.
    val currentRevealOffset by rememberUpdatedState(historyRevealOffset)

    Box(
        modifier = modifier
            .height(balanceBoxHeightDp + (sheetHeightDp * historyRevealOffset))
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Balance box section (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF8F2))
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                // If history is fully open (offset >= 1.0), only allow dragging down
                                if (currentRevealOffset >= 1f && dragAmount < 0f) {
                                    // Trying to drag up while fully open - don't move
                                    return@detectVerticalDragGestures
                                }
                                
                                val normalizedDelta = -dragAmount / 500f
                                onHistoryOffsetChange(normalizedDelta)
                            },
                            onDragEnd = {
                                if (currentRevealOffset > 0.3f) {
                                    onToggleHistoryFull(true)
                                } else {
                                    onToggleHistoryFull(false)
                                }
                            },
                            onDragCancel = {
                                // A cancelled drag snaps the same way, so a sheet dragged to the
                                // top never stays half-open.
                                if (currentRevealOffset > 0.3f) {
                                    onToggleHistoryFull(true)
                                } else {
                                    onToggleHistoryFull(false)
                                }
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { onToggleHistoryFull(currentRevealOffset <= 0.5f) },
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("balance_history_left"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Open transaction history",
                        tint = Color(0xFF7CC8A4),
                        modifier = Modifier.size(40.dp),
                    )
                }
                Surface(
                    color = if (balance < 0) Color(0xFFF27982) else Color(0xFF70C889),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(45.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onToggleHistoryFull(currentRevealOffset <= 0.5f)
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currencyAnnotatedString(
                                text = "Balance ${if (balance < 0) "-" else ""}€${centsToDisplay(abs(balance))}",
                                baseFontSize = 18.sp,
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                        )
                    }
                }
                // Show grouping toggle when history is open, otherwise show right hamburger icon
                if (historyRevealOffset > 0f) {
                    IconButton(
                        onClick = { viewModel.toggleGroupByCategory() },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("grouping_toggle_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Toggle grouping",
                            tint = Color(0xFF7CC8A4),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onToggleHistoryFull(currentRevealOffset <= 0.5f) },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("balance_history_right"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Open transaction history",
                            tint = Color(0xFF7CC8A4),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            // Transaction history content
            if (historyRevealOffset > 0f) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sheetHeightDp)
                        .testTag("history_sheet"),
                    color = Color.White,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                                    HorizontalDivider(color = Color(0xFFD4EDE0))

                        Box(modifier = Modifier.weight(1f)) {
                            TransactionGroupedList(
                                transactions = transactions,
                                categoriesMap = categoriesMap,
                                isGroupedByCategory = groupByCategory,
                                expandCategory = categoryToExpand,
                                onTransactionClick = { transaction ->
                                    editingTransaction = transaction
                                    editorVisible = true
                                },
                            )
                        }
                    }
                }
            }
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
                                    onSave = { transactionId, dateText, amountText, type, categoryName, description, account ->
                                        viewModel.saveTransaction(transactionId, dateText, amountText, type, categoryName, description, account)
                                    },
                                    onDelete = { transactionId ->
                                        viewModel.deleteTransaction(transactionId)
                                    },
                                )
                            }
}

private suspend fun animateRevealOffset(
    from: Float,
    to: Float,
    density: androidx.compose.ui.unit.Density,
    onOffset: (Float) -> Unit,
) {
    val startTime = System.currentTimeMillis()
    val duration = 300L
    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed >= duration) {
            onOffset(to)
            break
        }
        val progress = elapsed.toFloat() / duration
        val eased = progress * progress * (3 - 2 * progress) // cubic easing
        val value = from + (to - from) * eased
        onOffset(value)
        delay(16)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthTicker(
    pagerState: PagerState,
    pageCount: Int,
    monthForPage: (Int) -> YearMonth,
    canMoveToNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clipToBounds()
                .testTag("month_ticker"),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val stepPx = widthPx * 0.52f
            val sideWindow = 96.dp
            val scrollRate = 0.78f
            val progress = pagerState.currentPage + (pagerState.currentPageOffsetFraction * scrollRate)
            val visiblePages = listOf(
                pagerState.currentPage - 1,
                pagerState.currentPage,
                pagerState.currentPage + 1,
            ).filter { it in 0 until pageCount }

            visiblePages.forEach { page ->
                val distance = page - progress
                val isCentered = abs(distance) < 0.35f
                val monthLabel = formatMonth(monthForPage(page)).substringBefore(' ')
                val isLeftLabel = distance < 0f
                val canClick = if (isLeftLabel) true else canMoveToNextMonth

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = distance * stepPx
                        }
                        .width(if (isCentered) 180.dp else sideWindow)
                        .clickable(
                            enabled = !isCentered && canClick,
                            onClick = if (isLeftLabel) onPreviousMonth else onNextMonth,
                        ),
                    contentAlignment = when {
                        isCentered -> Alignment.Center
                        isLeftLabel -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    },
                ) {
                    Text(
                        text = monthLabel,
                        color = if (isCentered) Color(0xFF66C290) else Color(0xFFA8DABD),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = when {
                            isCentered -> TextAlign.Center
                            isLeftLabel -> TextAlign.End
                            else -> TextAlign.Start
                        },
                    )
                }
            }
        }
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
        color = borderColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .testTag(tag)
            .size(118.dp)
            .shadow(0.dp, shape = CircleShape),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = Color(0xFFFDFDFD),
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
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
    }
}

private fun Modifier.drawMonefyBackground(): Modifier {
    return drawBehind {
        drawRect(Color(0xFFEAF8F2))
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

/**
 * State of [groupName] after its header was clicked: that group's accordion flips on its own,
 * every other group keeps its state.
 */
internal fun expandedGroupsAfterToggle(expandedGroups: Map<String, Boolean>, groupName: String): Boolean =
    !isGroupExpanded(expandedGroups, groupName)

/**
 * Accordion state of one transaction-history group: only groups the user clicked have an entry in
 * [expandedGroups], so an unknown (never clicked) group is collapsed.
 */
internal fun isGroupExpanded(expandedGroups: Map<String, Boolean>, groupName: String): Boolean =
    expandedGroups[groupName] == true

/**
 * The group headers a click on [requestedCategory] opens:
 * - When grouped by category: the group whose name is that category.
 * - When grouped by date: the date group containing the latest transaction of that category.
 * Nothing is expanded when no category was requested, or when no group matches it.
 */
internal fun groupsToExpand(
    groupNames: List<String>,
    requestedCategory: String?,
    isGroupedByCategory: Boolean,
    transactions: List<TransactionListItem>,
): List<String> {
    if (requestedCategory.isNullOrBlank()) return emptyList()
    
    val target = normalizedGroupName(requestedCategory)
    
    return if (isGroupedByCategory) {
        // Expand the category group matching the requested category
        groupNames.filter { normalizedGroupName(it) == target }
    } else {
        // Find the latest transaction of the requested category, then expand its date group
        val latestTransactionOfCategory = transactions
            .filter { normalizedGroupName(it.categoryName) == target }
            .maxByOrNull { it.dateEpochDay }
        
        if (latestTransactionOfCategory == null) return emptyList()
        
        val expectedDateGroup = formatDate(latestTransactionOfCategory.date)
        groupNames.filter { normalizedGroupName(it) == normalizedGroupName(expectedDateGroup) }
    }
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
internal fun TransactionGroupedList(
    transactions: List<com.lorenzo.monefyme.data.model.TransactionListItem>,
    categoriesMap: Map<Long, com.lorenzo.monefyme.data.model.CategoryItem>,
    isGroupedByCategory: Boolean,
    expandCategory: String? = null,
    onTransactionClick: (com.lorenzo.monefyme.data.model.TransactionListItem) -> Unit,
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
    LaunchedEffect(expandCategory, groupNames, transactions, isGroupedByCategory) {
        val category = expandCategory ?: return@LaunchedEffect
        if (appliedExpansion.value == category) return@LaunchedEffect
        appliedExpansion.value = category
        groupsToExpand(groupNames, category, isGroupedByCategory, transactions).forEach { groupName ->
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

@Composable
private fun GroupHeader(
    groupName: String,
    groupTransactions: List<com.lorenzo.monefyme.data.model.TransactionListItem>,
    categoriesMap: Map<Long, com.lorenzo.monefyme.data.model.CategoryItem>,
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
    transaction: com.lorenzo.monefyme.data.model.TransactionListItem,
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
