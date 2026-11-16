package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.ui.screen.TransactionHistoryScreen
import com.lorenzo.monefyme.ui.util.formatMonth
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for the header of the Transaction History screen.
 *
 * The screen has no "Transactions" title: the top bar carries only its Back button and its restore
 * entry point, and the only heading on the screen is the month the transactions belong to. Removing
 * the title must not remove the bar itself - the user still needs to get back and to reach Restore -
 * and the grouped transaction list below it still renders.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class TransactionHistoryTitleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        CategoryItem(id = 1L, displayName = "Food", iconName = "Restaurant", colorArgb = null),
        CategoryItem(id = 2L, displayName = "Transport", iconName = "LocalTaxi", colorArgb = null),
    )

    private val transactions = listOf(
        TransactionListItem(
            id = 11L,
            dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
            categoryId = 1L,
            categoryName = "Food",
            amountCents = 1000L,
            type = "EXPENSE",
            description = "Lunch",
            account = "Cash",
            currency = "EUR",
        ),
        TransactionListItem(
            id = 12L,
            dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
            categoryId = 2L,
            categoryName = "Transport",
            amountCents = 500L,
            type = "EXPENSE",
            description = "Taxi",
            account = "Card",
            currency = "EUR",
        ),
    )

    private lateinit var viewModel: MainViewModel

    private fun showTransactionHistory() {
        viewModel = MainViewModel(FakeBudgetRepository(categories, transactions))
        composeTestRule.setContent {
            TransactionHistoryScreen(
                viewModel = viewModel,
                onBack = { },
                onOpenRestore = { },
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun transactionHistoryScreenShowsNoTransactionsTitle() {
        showTransactionHistory()

        composeTestRule.onNodeWithText("Transactions").assertDoesNotExist()
    }

    @Test
    fun topBarKeepsItsButtonsAfterTheTitleWasRemoved() {
        showTransactionHistory()

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open restore").assertIsDisplayed()
    }

    @Test
    fun monthSelectorAndGroupedHistoryAreStillOnScreen() {
        showTransactionHistory()

        // The month selector is the heading left on the screen once the title is gone.
        composeTestRule.onNodeWithText(formatMonth(viewModel.selectedMonth.value)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    /** Repository stub: the history screen only reads categories and the month's transactions. */
    private class FakeBudgetRepository(
        initialCategories: List<CategoryItem>,
        initialTransactions: List<TransactionListItem>,
    ) : BudgetRepository {
        private val categoriesFlow = MutableStateFlow(initialCategories)
        private val categoryNamesFlow = MutableStateFlow(initialCategories.map { it.displayName })
        private val transactionsFlow = MutableStateFlow(initialTransactions)

        override fun observeCategoryNames(): Flow<List<String>> = categoryNamesFlow

        override fun observeCategories(): Flow<List<CategoryItem>> = categoriesFlow

        override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> =
            transactionsFlow

        override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> =
            MutableStateFlow(emptyList())

        override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean =
            throw UnsupportedOperationException("not used by the transaction history screen")

        override suspend fun updateCategory(
            categoryId: Long,
            displayName: String,
            iconName: String,
            colorArgb: Int,
        ): Boolean = throw UnsupportedOperationException("not used by the transaction history screen")

        override suspend fun deleteCategory(categoryId: Long): Boolean =
            throw UnsupportedOperationException("not used by the transaction history screen")

        override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft): Boolean =
            throw UnsupportedOperationException("not used by the transaction history screen")

        override suspend fun deleteTransaction(transactionId: Long) {
            throw UnsupportedOperationException("not used by the transaction history screen")
        }

        override suspend fun importCsv(inputStream: InputStream): CsvImportResult =
            throw UnsupportedOperationException("not used by the transaction history screen")
    }
}
