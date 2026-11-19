package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.screen.AddTransactionScreen
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.io.InputStream
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for opening the add-expense flow with a category already preselected.
 *
 * Clicking a category icon slot on the home screen's pie chart reports the category through
 * ExpensePieChart.onCategorySlotClick (covered by PieChartSectionClickUiTest); HomeScreen/BudgetApp
 * record it with MainViewModel.requestAddExpense and open the add-expense route. This test starts the
 * add-expense screen with such a request pending and asserts the screen preselects the category: it
 * shows the category, its bottom button confirms that category instead of opening the picker, and
 * confirming after entering an amount saves an EXPENSE for that category and returns to Home.
 * Without a pending request the screen keeps its original CHOOSE CATEGORY flow.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class AddExpensePreselectedCategoryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = RecordingRepository()

    private fun showAddExpenseScreen(pendingCategory: String?) {
        val viewModel = MainViewModel(repository)
        if (pendingCategory != null) {
            // What BudgetApp does when a pie chart icon slot is clicked, just before navigating here.
            viewModel.requestAddExpense(pendingCategory)
        }
        composeTestRule.setContent {
            AddTransactionScreen(
                viewModel = viewModel,
                type = TransactionType.EXPENSE,
                onBack = { },
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun pendingCategoryIsPreselectedOnTheAddExpenseScreen() {
        showAddExpenseScreen(pendingCategory = "Food")

        composeTestRule.onNodeWithTag("add_transaction_selected_category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category: Food").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_transaction_confirm_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADD \"Food\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("CHOOSE CATEGORY").assertDoesNotExist()
    }

    @Test
    fun noPendingCategoryKeepsTheOriginalChooseCategoryFlow() {
        showAddExpenseScreen(pendingCategory = null)

        composeTestRule.onNodeWithTag("add_transaction_selected_category").assertDoesNotExist()
        composeTestRule.onNodeWithText("CHOOSE CATEGORY").assertIsDisplayed()
    }

    @Test
    fun confirmingWithAPreselectedCategorySavesAnExpenseForIt() {
        showAddExpenseScreen(pendingCategory = "Food")

        // Type an amount on the calculator pad, then confirm with the preselected category.
        // ("5" is unambiguous: the amount display itself only shows "0" at the start.)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithTag("add_transaction_confirm_button").performClick()
        composeTestRule.waitForIdle()

        val draft = repository.savedDrafts.single()
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals("Food", draft.categoryName)
        assertEquals(500L, draft.amountCents)
    }

    @Test
    fun confirmingWithoutAnAmountDoesNotSaveAnything() {
        showAddExpenseScreen(pendingCategory = "Food")

        composeTestRule.onNodeWithTag("add_transaction_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "The amount is still 0, so nothing may be saved.",
            repository.savedDrafts.isEmpty(),
        )
    }

    /** Repository stub: records saved drafts, everything else is unused by the add screen. */
    private class RecordingRepository : BudgetRepository {
        val savedDrafts = mutableListOf<TransactionDraft>()

        override fun observeCategoryNames(): Flow<List<String>> = MutableStateFlow(emptyList())

        override fun observeCategories(): Flow<List<CategoryItem>> = MutableStateFlow(emptyList())

        override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> =
            MutableStateFlow(emptyList())

        override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> =
            MutableStateFlow(emptyList())

        override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean =
            throw UnsupportedOperationException("not used by the add-expense screen")

        override suspend fun updateCategory(
            categoryId: Long,
            displayName: String,
            iconName: String,
            colorArgb: Int,
        ): Boolean = throw UnsupportedOperationException("not used by the add-expense screen")

        override suspend fun deleteCategory(categoryId: Long): Boolean =
            throw UnsupportedOperationException("not used by the add-expense screen")

        override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft): Boolean {
            savedDrafts += draft
            return true
        }

        override suspend fun deleteTransaction(transactionId: Long) {
            throw UnsupportedOperationException("not used by the add-expense screen")
        }

        override suspend fun importCsv(inputStream: InputStream): CsvImportResult =
            throw UnsupportedOperationException("not used by the add-expense screen")
    }
}
