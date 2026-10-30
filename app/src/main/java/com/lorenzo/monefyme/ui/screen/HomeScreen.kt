package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import com.lorenzo.monefyme.ui.util.centsToDisplay
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenTransactions: () -> Unit,
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
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
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

        MonthTicker(
            pagerState = pagerState,
            pageCount = pageCount,
            monthForPage = monthForPage,
            canMoveToNextMonth = canMoveToNextMonth,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
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
            ExpensePieChart(
                slices = pageExpenseSlices,
                incomeCents = pageTotalIncome,
                expenseCents = pageTotalExpense,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

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
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Previous month",
                tint = Color(0xFF8BCFAE),
            )
        }

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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal,
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

        IconButton(
            onClick = onNextMonth,
            enabled = canMoveToNextMonth,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Next month",
                tint = if (canMoveToNextMonth) Color(0xFF8BCFAE) else Color(0xFFCFE7DA),
            )
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
