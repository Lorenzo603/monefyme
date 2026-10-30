package com.lorenzo.monefyme.ui.viewmodel

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.util.amountTextToCentsOrNull
import com.lorenzo.monefyme.ui.util.parseDateOrNull
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

class MainViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
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

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _lastImportResult = MutableStateFlow<CsvImportResult?>(null)
    val lastImportResult: StateFlow<CsvImportResult?> = _lastImportResult.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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

    fun postMessage(value: String) {
        _message.value = value
    }

    fun clearMessage() {
        _message.value = null
    }
}
