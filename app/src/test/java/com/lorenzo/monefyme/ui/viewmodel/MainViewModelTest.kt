package com.lorenzo.monefyme.ui.viewmodel

import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val repository = FakeBudgetRepository()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun viewModel(): MainViewModel = MainViewModel(repository)

    // --- month navigation ------------------------------------------------------

    @Test
    fun initialMonthIsTheCurrentMonth() = runTest(testDispatcher) {
        assertEquals(YearMonth.now(), viewModel().selectedMonth.value)
    }

    @Test
    fun previousMonthAndNextMonthMoveByOneMonth() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.previousMonth()
        assertEquals(YearMonth.now().minusMonths(1), vm.selectedMonth.value)

        vm.nextMonth()
        assertEquals(YearMonth.now(), vm.selectedMonth.value)
    }

    @Test
    fun futureMonthsCannotBeSelected() = runTest(testDispatcher) {
        val vm = viewModel()
        val current = vm.selectedMonth.value

        vm.selectMonth(current.plusMonths(1))
        assertEquals(current, vm.selectedMonth.value)

        vm.nextMonth()
        assertEquals(current, vm.selectedMonth.value)
    }

    @Test
    fun pastMonthsCanBeSelectedDirectly() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.transactions.collect {} }
        val january2020 = YearMonth.of(2020, 1)

        vm.selectMonth(january2020)

        assertEquals(january2020, vm.selectedMonth.value)
        assertTrue(repository.requestedTransactionMonths.contains(january2020))
    }

    @Test
    fun monthFlowsFollowTheSelectedMonth() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.transactions.collect {} }
        backgroundScope.launch { vm.expenseSlices.collect {} }
        val january2020 = YearMonth.of(2020, 1)
        repository.transactionsFlow(january2020).value = listOf(transactionItem())
        repository.slicesFlow(january2020).value = listOf(ExpenseSlice("Food", 1_250L))

        vm.selectMonth(january2020)

        assertEquals(1, vm.transactions.value.size)
        assertEquals("Groceries", vm.transactions.value.single().description)
        assertEquals(1_250L, vm.expenseSlices.value.single().totalCents)
    }

    @Test
    fun categoryFlowsReflectRepository() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.categories.collect {} }
        backgroundScope.launch { vm.allCategories.collect {} }
        repository.categoryNamesFlow.value = listOf("Food", "House")
        repository.categoriesFlow.value = listOf(CategoryItem(id = 1L, displayName = "Food", iconName = null, colorArgb = null))

        assertEquals(listOf("Food", "House"), vm.categories.value)
        assertEquals("Food", vm.allCategories.value.single().displayName)
        // The picker always shows every seeded category even with an empty database.
        assertTrue(vm.pickerCategories.value.any { it.displayName == "Breakfast out" })
    }

    // --- saving transactions ---------------------------------------------------

    @Test
    fun saveTransactionRejectsInvalidDateWithoutTouchingRepository() = runTest(testDispatcher) {
        val vm = viewModel()

        val saved = vm.saveTransaction(null, "31/02/2026", "12.50", TransactionType.EXPENSE, "Food", "d", "Cash")

        assertFalse(saved)
        assertEquals("Invalid date. Expected format dd/MM/yyyy.", vm.message.value)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test
    fun saveTransactionRejectsInvalidAmount() = runTest(testDispatcher) {
        val vm = viewModel()

        val saved = vm.saveTransaction(null, "01/02/2026", "-3", TransactionType.EXPENSE, "Food", "d", "Cash")

        assertFalse(saved)
        assertEquals("Invalid amount. Enter a positive numeric value.", vm.message.value)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test
    fun saveTransactionRequiresACategory() = runTest(testDispatcher) {
        val vm = viewModel()

        val saved = vm.saveTransaction(null, "01/02/2026", "3", TransactionType.EXPENSE, "  ", "d", "Cash")

        assertFalse(saved)
        assertEquals("Category is required.", vm.message.value)
    }

    @Test
    fun saveTransactionSuccessBuildsDraftAndReportsAdded() = runTest(testDispatcher) {
        val vm = viewModel()

        val saved = vm.saveTransaction(null, "01/02/2026", "12,50", TransactionType.INCOME, "Salary", "bonus", "Bank")

        assertTrue(saved)
        assertEquals("Transaction added.", vm.message.value)

        val (id, draft) = repository.savedDrafts.single()
        assertNull(id)
        assertEquals(YearMonth.from(draft.date).atDay(1).toString(), "2026-02-01")
        assertEquals(TransactionType.INCOME, draft.type)
        assertEquals(1_250L, draft.amountCents)
        assertEquals("Salary", draft.categoryName)
    }

    @Test
    fun saveTransactionUpdateReportsUpdated() = runTest(testDispatcher) {
        val vm = viewModel()

        val saved = vm.saveTransaction(7L, "01/02/2026", "10", TransactionType.EXPENSE, "Food", "d", "Cash")

        assertTrue(saved)
        assertEquals("Transaction updated.", vm.message.value)
        assertEquals(7L, repository.savedDrafts.single().first)
    }

    @Test
    fun saveTransactionRepositoryRejectionIsReported() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.saveTransactionResult = false

        assertFalse(vm.saveTransaction(null, "01/02/2026", "10", TransactionType.EXPENSE, "Food", "d", "Cash"))
        assertEquals("Could not save transaction.", vm.message.value)
    }

    @Test
    fun saveTransactionReportsIllegalArgumentFromRepository() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.saveTransactionError = IllegalArgumentException("Amount must be greater than zero.")

        assertFalse(vm.saveTransaction(null, "01/02/2026", "10", TransactionType.EXPENSE, "Food", "d", "Cash"))
        assertEquals("Amount must be greater than zero.", vm.message.value)
    }

    @Test
    fun saveTransactionReportsDatabaseError() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.saveTransactionError = android.database.SQLException("db boom")

        assertFalse(vm.saveTransaction(null, "01/02/2026", "10", TransactionType.EXPENSE, "Food", "d", "Cash"))
        assertEquals("Database error while saving transaction.", vm.message.value)
    }

    // --- deleting transactions -------------------------------------------------

    @Test
    fun deleteTransactionDelegatesAndConfirms() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.deleteTransaction(42L)

        assertEquals(listOf(42L), repository.deletedIds)
        assertEquals("Transaction deleted.", vm.message.value)
    }

    // --- categories ------------------------------------------------------------

    @Test
    fun saveCategoryRejectsBlankName() = runTest(testDispatcher) {
        val vm = viewModel()

        assertFalse(vm.saveCategory("  ", "Star", 0))
        assertEquals("Category name cannot be blank.", vm.message.value)
        assertTrue(repository.savedCategories.isEmpty())
    }

    @Test
    fun saveCategorySuccessAndDuplicateProduceDistinctMessages() = runTest(testDispatcher) {
        val vm = viewModel()

        assertTrue(vm.saveCategory("Food", "ShoppingCart", 1))
        assertEquals("Category \"Food\" saved.", vm.message.value)

        repository.saveCategoryResult = false
        assertFalse(vm.saveCategory("Food", "ShoppingCart", 1))
        assertEquals("Category \"Food\" already exists.", vm.message.value)
    }

    @Test
    fun saveCategoryReportsUnexpectedFailure() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.saveCategoryError = IllegalStateException("boom")

        assertFalse(vm.saveCategory("Food", "ShoppingCart", 1))
        assertEquals("Error saving category.", vm.message.value)
    }

    // --- update category -------------------------------------------------------

    @Test
    fun updateCategoryRejectsBlankName() = runTest(testDispatcher) {
        val vm = viewModel()
        assertFalse(vm.updateCategory(1L, "  ", "Star", 0))
        assertEquals("Category name cannot be blank.", vm.message.value)
    }

    @Test
    fun updateCategorySuccessProducesMessage() = runTest(testDispatcher) {
        val vm = viewModel()
        assertTrue(vm.updateCategory(1L, "Groceries", "ShoppingCart", 1))
        assertEquals("Category updated.", vm.message.value)
        assertEquals(1, repository.updatedCategories.single().categoryId)
    }

    @Test
    fun updateCategoryReportsUnexpectedFailure() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.updateCategoryError = IllegalStateException("boom")

        assertFalse(vm.updateCategory(1L, "Food", "ShoppingCart", 1))
        assertEquals("Error updating category.", vm.message.value)
    }

    // --- delete category -------------------------------------------------------

    @Test
    fun deleteCategorySuccessProducesMessage() = runTest(testDispatcher) {
        val vm = viewModel()
        assertTrue(vm.deleteCategory(42L))
        assertEquals("Category deleted.", vm.message.value)
        assertEquals(listOf(42L), repository.deletedCategoryIds)
    }

    @Test
    fun deleteCategoryReportsUnexpectedFailure() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.deleteCategoryError = IllegalStateException("boom")

        assertFalse(vm.deleteCategory(42L))
        assertEquals("Error deleting category.", vm.message.value)
    }

    // --- CSV import ------------------------------------------------------------

    @Test
    fun importCsvExposesResultAndMessage() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.csvImportResult = CsvImportResult(processedRows = 5, insertedRows = 3, duplicateRows = 1, invalidRows = 1)

        vm.importCsv("whatever".toByteArray())

        assertEquals(repository.csvImportResult, vm.lastImportResult.value)
        assertEquals("Import complete: 3 inserted, 1 duplicates, 1 invalid.", vm.message.value)
        assertFalse(vm.isImporting.value)
        assertNotNull(repository.importedContent.single())
    }

    @Test
    fun importCsvReportsDatabaseError() = runTest(testDispatcher) {
        val vm = viewModel()
        repository.csvImportError = android.database.SQLException("db boom")

        vm.importCsv("whatever".toByteArray())

        assertNull(vm.lastImportResult.value)
        assertEquals("Database error while importing CSV.", vm.message.value)
        assertFalse(vm.isImporting.value)
    }

    // --- messages --------------------------------------------------------------

    @Test
    fun postAndClearMessage() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.postMessage("hello")
        assertEquals("hello", vm.message.value)

        vm.clearMessage()
        assertNull(vm.message.value)
    }

    @Test
    fun directMonthObservationDelegatesToRepository() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.transactions.collect {} }
        backgroundScope.launch { vm.expenseSlices.collect {} }
        val january2020 = YearMonth.of(2020, 1)
        repository.transactionsFlow(january2020).value = listOf(transactionItem())

        val items = vm.observeMonthTransactions(january2020).first()

        assertEquals("Groceries", items.single().description)
    }

    private fun transactionItem() = TransactionListItem(
        id = 1L,
        dateEpochDay = YearMonth.of(2020, 1).atDay(5).toEpochDay(),
        categoryId = 1L,
        categoryName = "Food",
        amountCents = 1_250L,
        type = "EXPENSE",
        description = "Groceries",
        account = "Cash",
        currency = "EUR",
    )

    private class FakeBudgetRepository : BudgetRepository {
        val categoryNamesFlow = MutableStateFlow<List<String>>(emptyList())
        val categoriesFlow = MutableStateFlow<List<CategoryItem>>(emptyList())
        private val transactionsFlows = mutableMapOf<YearMonth, MutableStateFlow<List<TransactionListItem>>>()
        private val slicesFlows = mutableMapOf<YearMonth, MutableStateFlow<List<ExpenseSlice>>>()

        val requestedTransactionMonths = mutableListOf<YearMonth>()
        val savedDrafts = mutableListOf<Pair<Long?, TransactionDraft>>()
        val deletedIds = mutableListOf<Long>()
        val savedCategories = mutableListOf<Triple<String, String, Int>>()
        data class CategoryUpdate(val categoryId: Long, val displayName: String, val iconName: String, val colorArgb: Int)
        val updatedCategories = mutableListOf<CategoryUpdate>()
        val deletedCategoryIds = mutableListOf<Long>()
        val importedContent = mutableListOf<ByteArray>()

        var saveTransactionResult: Boolean? = null
        var saveTransactionError: Throwable? = null
        var saveCategoryResult: Boolean? = null
        var saveCategoryError: Throwable? = null
        var updateCategoryResult: Boolean? = null
        var updateCategoryError: Throwable? = null
        var deleteCategoryResult: Boolean? = null
        var deleteCategoryError: Throwable? = null
        var csvImportResult: CsvImportResult? = null
        var csvImportError: Throwable? = null

        fun transactionsFlow(month: YearMonth) = transactionsFlows.getOrPut(month) { MutableStateFlow(emptyList()) }

        fun slicesFlow(month: YearMonth) = slicesFlows.getOrPut(month) { MutableStateFlow(emptyList()) }

        override fun observeCategoryNames(): Flow<List<String>> = categoryNamesFlow

        override fun observeCategories(): Flow<List<CategoryItem>> = categoriesFlow

        override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> {
            requestedTransactionMonths += month
            return transactionsFlow(month)
        }

        override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> = slicesFlow(month)

        override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean {
            saveCategoryError?.let { throw it }
            savedCategories += Triple(displayName, iconName, colorArgb)
            return saveCategoryResult ?: true
        }

        override suspend fun updateCategory(categoryId: Long, displayName: String, iconName: String, colorArgb: Int): Boolean {
            updateCategoryError?.let { throw it }
            updatedCategories += CategoryUpdate(categoryId, displayName, iconName, colorArgb)
            return updateCategoryResult ?: true
        }

        override suspend fun deleteCategory(categoryId: Long): Boolean {
            deleteCategoryError?.let { throw it }
            deletedCategoryIds += categoryId
            return deleteCategoryResult ?: true
        }

        override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft): Boolean {
            saveTransactionError?.let { throw it }
            savedDrafts += transactionId to draft
            return saveTransactionResult ?: true
        }

        override suspend fun deleteTransaction(transactionId: Long) {
            deletedIds += transactionId
        }

        override suspend fun importCsv(inputStream: InputStream): CsvImportResult {
            inputStream.use { stream ->
                val buffer = ByteArrayOutputStream()
                stream.copyTo(buffer)
                importedContent += buffer.toByteArray()
            }
            csvImportError?.let { throw it }
            return csvImportResult ?: CsvImportResult(0, 0, 0, 0)
        }
    }
}
