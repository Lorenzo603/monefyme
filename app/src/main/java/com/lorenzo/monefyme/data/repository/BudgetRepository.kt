package com.lorenzo.monefyme.data.repository

import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import java.io.InputStream
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeCategoryNames(): Flow<List<String>>
    fun observeCategories(): Flow<List<CategoryItem>>
    fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>>
    fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>>
    suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean
    suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft): Boolean
    suspend fun deleteTransaction(transactionId: Long)
    suspend fun importCsv(inputStream: InputStream): CsvImportResult
}
