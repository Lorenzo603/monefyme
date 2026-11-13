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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.lorenzo.monefyme.ui.component.CategoriesSidebar
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatMonth
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
                        .graphicsLayer { alpha = 1f - historyRevealOffset }
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
                                .padding(12.dp),
                            // Clicking a pie chart section expands the inline transaction history with that
                            // category's accordion already expanded.
                            onCategoryClick = { categoryName ->
                                viewModel.requestCategoryExpansion(categoryName)
                                onToggleHistoryFull(true)
                            },
                        )
                        
                        // Balance box for this month's transactions - now part of scrollable month
                        BalanceAndHistorySheet(
                            viewModel = viewModel,
                            balance = pageBalance,
                            transactions = pageTransactions,
                            historyRevealOffset = historyRevealOffset,
                            onHistoryOffsetChange = onHistoryOffsetChange,
                            onToggleHistoryFull = onToggleHistoryFull,
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
    modifier: Modifier = Modifier,
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categoriesMap = remember(allCategories) {
        allCategories.associateBy { it.id }
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
                        contentDescription = "Toggle transaction history",
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
                IconButton(
                    onClick = { onToggleHistoryFull(currentRevealOffset <= 0.5f) },
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("balance_history_right"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Toggle transaction history",
                        tint = Color(0xFF7CC8A4),
                        modifier = Modifier.size(40.dp),
                    )
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
                        // Transaction list
                        Box(modifier = Modifier.weight(1f)) {
                            TransactionGroupedList(
                                transactions = transactions,
                                categoriesMap = categoriesMap,
                                isGroupedByCategory = true,
                                expandCategory = null,
                                onTransactionClick = { },
                            )
                        }
                    }
                }
            }
        }
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
