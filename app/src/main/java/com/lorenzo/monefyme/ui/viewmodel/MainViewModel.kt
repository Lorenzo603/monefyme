package com.lorenzo.monefyme.ui.viewmodel

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.monefyme.data.local.getGroupByCategoryFlow
import com.lorenzo.monefyme.data.local.saveGroupByCategory
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.category.CategoryOrder
import com.lorenzo.monefyme.ui.util.amountTextToCentsOrNull
import com.lorenzo.monefyme.ui.util.parseDateOrNull
import java.io.IOException
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    /**
     * Whether transactions should be grouped by category (true) or by date (false).
     * Loaded from in-memory storage and updated when the user toggles it.
     */
    val groupByCategory: StateFlow<Boolean> = getGroupByCategoryFlow()
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val transactions: StateFlow<List<TransactionListItem>> = _selectedMonth
        .flatMapLatest(repository::observeMonthTransactions)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseSlices: StateFlow<List<ExpenseSlice>> = _selectedMonth
        .flatMapLatest(repository::observeMonthExpenseSlices)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<String>> = repository.observeCategoryNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<CategoryItem>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Every category the picker can offer: the seeded ones in their declared order, the ones
     * added later at the end, and always all the seeded ones even before they are stored.
     */
    val pickerCategories: StateFlow<List<CategoryItem>> = repository.observeCategories()
        .map(CategoryOrder::seededFirst)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryOrder.seededFirst(emptyList()))

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    /** Number of transactions written by the last successful export, null until one runs or after a failure. */
    private val _lastExportRowCount = MutableStateFlow<Int?>(null)
    val lastExportRowCount: StateFlow<Int?> = _lastExportRowCount.asStateFlow()

    /** Error of the last export read, kept (unlike [message]) so a screen can show it persistently. */
    private val _lastExportError = MutableStateFlow<String?>(null)
    val lastExportError: StateFlow<String?> = _lastExportError.asStateFlow()

    private val _lastImportResult = MutableStateFlow<CsvImportResult?>(null)
    val lastImportResult: StateFlow<CsvImportResult?> = _lastImportResult.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Category whose transaction-history accordion must open: set when a pie chart section is clicked
     * and cleared by [consumeCategoryExpansionRequest], so one click expands exactly once.
     */
    private val _categoryExpansionRequest = MutableStateFlow<String?>(null)
    val categoryExpansionRequest: StateFlow<String?> = _categoryExpansionRequest.asStateFlow()

    /**
     * Toggle the transaction grouping preference and persist it to in-memory storage.
     * When true, transactions are grouped by category; when false, they're grouped by date.
     */
    fun toggleGroupByCategory() {
        viewModelScope.launch {
            val newValue = !groupByCategory.value
            saveGroupByCategory(newValue)
        }
    }

    fun previousMonth() {
        selectMonth(_selectedMonth.value.minusMonths(1))
    }

    fun nextMonth() {
        selectMonth(_selectedMonth.value.plusMonths(1))
    }

    fun selectMonth(month: YearMonth) {
        val currentMonth = YearMonth.now()
        if (!month.isAfter(currentMonth)) {
            _selectedMonth.value = month
        }
    }

    fun observeMonthTransactions(month: YearMonth): kotlinx.coroutines.flow.Flow<List<TransactionListItem>> {
        return repository.observeMonthTransactions(month)
    }

    fun observeMonthExpenseSlices(month: YearMonth): kotlinx.coroutines.flow.Flow<List<ExpenseSlice>> {
        return repository.observeMonthExpenseSlices(month)
    }

    /** Ask the transaction history to open with [categoryName]'s accordion expanded. */
    fun requestCategoryExpansion(categoryName: String) {
        _categoryExpansionRequest.value = categoryName.trim().takeIf { it.isNotEmpty() }
    }

    /**
     * The pending category expansion request, or null when there is none. Reading it consumes it, so
     * the same click never forces the accordion open a second time.
     */
    fun consumeCategoryExpansionRequest(): String? {
        val pending = _categoryExpansionRequest.value
        if (pending != null) {
            _categoryExpansionRequest.value = null
        }
        return pending
    }

    /**
     * Category to preselect on the add-expense screen: set when a pie chart category icon slot is
     * clicked (which navigates there) and cleared by [consumeAddExpenseCategoryRequest], so one
     * click preselects exactly once.
     */
    private val _addExpenseCategoryRequest = MutableStateFlow<String?>(null)
    val addExpenseCategoryRequest: StateFlow<String?> = _addExpenseCategoryRequest.asStateFlow()

    /** Ask the add-expense screen to open with [categoryName] already preselected; blanks are ignored. */
    fun requestAddExpense(categoryName: String) {
        _addExpenseCategoryRequest.value = categoryName.trim().takeIf { it.isNotEmpty() }
    }

    /**
     * The pending add-expense preselection, or null when there is none. Reading it consumes it, so
     * the next time the add-expense screen opens without a click it starts with no category.
     */
    fun consumeAddExpenseCategoryRequest(): String? {
        val pending = _addExpenseCategoryRequest.value
        if (pending != null) {
            _addExpenseCategoryRequest.value = null
        }
        return pending
    }

    suspend fun saveTransaction(
        transactionId: Long?,
        dateText: String,
        amountText: String,
        type: TransactionType,
        categoryName: String,
        description: String,
        account: String,
    ): Boolean {
        val date = parseDateOrNull(dateText)
        if (date == null) {
            _message.value = "Invalid date. Expected format dd/MM/yyyy."
            return false
        }

        val amountCents = amountTextToCentsOrNull(amountText)
        if (amountCents == null) {
            _message.value = "Invalid amount. Enter a positive numeric value."
            return false
        }

        if (categoryName.isBlank()) {
            _message.value = "Category is required."
            return false
        }

        return try {
            val persisted = repository.saveTransaction(
                transactionId = transactionId,
                draft = TransactionDraft(
                    date = date,
                    type = type,
                    amountCents = amountCents,
                    categoryName = categoryName,
                    description = description,
                    account = account,
                ),
            )
            if (persisted) {
                _message.value = if (transactionId == null) {
                    "Transaction added."
                } else {
                    "Transaction updated."
                }
            } else {
                _message.value = "Could not save transaction."
            }
            persisted
        } catch (illegalArgumentException: IllegalArgumentException) {
            _message.value = illegalArgumentException.message ?: "Invalid transaction."
            false
        } catch (sqlException: SQLException) {
            _message.value = "Database error while saving transaction."
            false
        }
    }

    suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean {
        if (displayName.isBlank()) {
            _message.value = "Category name cannot be blank."
            return false
        }
        return try {
            val saved = repository.saveCategory(displayName, iconName, colorArgb)
            _message.value = if (saved) "Category \"$displayName\" saved." else "Category \"$displayName\" already exists."
            saved
        } catch (e: Exception) {
            _message.value = "Error saving category."
            false
        }
    }

    suspend fun updateCategory(categoryId: Long, displayName: String, iconName: String, colorArgb: Int): Boolean {
        if (displayName.isBlank()) {
            _message.value = "Category name cannot be blank."
            return false
        }
        return try {
            val updated = repository.updateCategory(categoryId, displayName, iconName, colorArgb)
            _message.value = if (updated) "Category updated." else "Could not update category."
            updated
        } catch (e: Exception) {
            _message.value = "Error updating category."
            false
        }
    }

    suspend fun deleteCategory(categoryId: Long): Boolean {
        return try {
            val deleted = repository.deleteCategory(categoryId)
            _message.value = if (deleted) "Category deleted." else "Could not delete category."
            deleted
        } catch (e: Exception) {
            _message.value = "Error deleting category."
            false
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        repository.deleteTransaction(transactionId)
        _message.value = "Transaction deleted."
    }

    suspend fun importCsv(content: ByteArray) {
        if (_isImporting.value) return

        _isImporting.value = true
        try {
            val result = repository.importCsv(content.inputStream())
            _lastImportResult.value = result
            _message.value = "Import complete: ${result.insertedRows} inserted, ${result.duplicateRows} duplicates, ${result.invalidRows} invalid."
        } catch (sqlException: SQLException) {
            _message.value = "Database error while importing CSV."
        } finally {
            _isImporting.value = false
        }
    }

    /**
     * Exports every stored transaction (all months) as CSV bytes in the layout
     * [com.lorenzo.monefyme.data.importer.MonefyCsvParser] reads. Failures are surfaced through
     * [message] instead of being thrown, so the caller never sees an uncaught exception.
     */
    suspend fun exportCsv(): ByteArray? {
        return try {
            val content = repository.exportCsv()
            val rowCount = countCsvDataRows(content)
            _lastExportRowCount.value = rowCount
            _lastExportError.value = null
            _message.value = "Export complete: $rowCount rows."
            content
        } catch (sqlException: SQLException) {
            reportExportFailure("Database error while exporting CSV.")
        } catch (ioException: IOException) {
            reportExportFailure("Could not export CSV: ${ioException.message ?: "unknown error"}.")
        }
    }

    private fun reportExportFailure(text: String): ByteArray? {
        _lastExportRowCount.value = null
        _lastExportError.value = text
        _message.value = text
        return null
    }

    private fun countCsvDataRows(content: ByteArray): Int {
        return content.toString(Charsets.UTF_8).count { it == '\n' } - 1
    }

    fun postMessage(value: String) {
        _message.value = value
    }

    fun clearMessage() {
        _message.value = null
    }
}
