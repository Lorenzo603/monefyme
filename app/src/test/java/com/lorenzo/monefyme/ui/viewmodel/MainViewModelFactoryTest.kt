package com.lorenzo.monefyme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import java.io.InputStream
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelFactoryTest {
    private val repository = object : BudgetRepository {
        override fun observeCategoryNames(): Flow<List<String>> = emptyFlow()
        override fun observeCategories(): Flow<List<CategoryItem>> = emptyFlow()
        override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> = emptyFlow()
        override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> = emptyFlow()
        override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int) = true
        override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft) = true
        override suspend fun deleteTransaction(transactionId: Long) {}
        override suspend fun importCsv(inputStream: InputStream): CsvImportResult = CsvImportResult(0, 0, 0, 0)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createsMainViewModelForItsClass() {
        val viewModel = MainViewModelFactory(repository).create(MainViewModel::class.java)

        assertTrue(viewModel is MainViewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownViewModelClasses() {
        MainViewModelFactory(repository).create(OtherViewModel::class.java)
    }

    private class OtherViewModel : ViewModel()
}
