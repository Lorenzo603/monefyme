package com.lorenzo.monefyme.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lorenzo.monefyme.data.local.AppDatabase
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests running [BudgetRepositoryImpl] against a real in-memory Room database,
 * exercising the actual SQL queries, joins, unique indexes and ordering.
 */
@RunWith(AndroidJUnit4::class)
class BudgetRepositoryIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = BudgetRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun draft(
        date: LocalDate = LocalDate.of(2026, 2, 10),
        type: TransactionType = TransactionType.EXPENSE,
        amountCents: Long = 1_250L,
        categoryName: String = "Food",
        description: String = "note",
        account: String = "Cash",
        currency: String = "EUR",
    ) = TransactionDraft(
        date = date,
        type = type,
        amountCents = amountCents,
        categoryName = categoryName,
        description = description,
        account = account,
        currency = currency,
    )

    @Test
    fun saveTransactionCreatesMissingCategoryAndReusesExistingOneIgnoringCase() = runBlocking {
        assertTrue(repository.saveTransaction(null, draft(categoryName = "Groceries")))
        assertTrue(repository.saveTransaction(null, draft(categoryName = "  groceries ", description = "second")))

        val names = repository.observeCategoryNames().first()
        assertEquals(listOf("Groceries"), names)
        assertEquals(2, database.transactionDao().count())
    }

    @Test
    fun saveCategoryNormalizesNameAndUniqueIndexRejectsDuplicates() = runBlocking {
        assertTrue(repository.saveCategory("  Eating   out ", "Restaurant", 0xFF7EB7A4.toInt()))

        val stored = database.categoryDao().findByNormalizedName("eating out")
        assertNotNull(stored)
        assertEquals("Eating out", stored!!.displayName)
        assertEquals("Restaurant", stored.iconName)

        assertFalse(repository.saveCategory("EATING OUT", "Restaurant", 0))
        assertEquals(1, repository.observeCategoryNames().first().size)
    }

    @Test
    fun observeCategoriesExposesStoredIconAndColor() = runBlocking {
        repository.saveCategory("House", "Home", 0xFF6EA9E6.toInt())

        val item = repository.observeCategories().first().single()
        assertEquals("House", item.displayName)
        assertEquals("Home", item.iconName)
        assertEquals(0xFF6EA9E6.toInt(), item.colorArgb)
    }

    @Test
    fun observeMonthTransactionsFiltersByMonthAndOrdersNewestFirst() = runBlocking {
        repository.saveTransaction(null, draft(date = LocalDate.of(2026, 1, 31), description = "january row"))
        repository.saveTransaction(null, draft(date = LocalDate.of(2026, 2, 1), description = "february first"))
        repository.saveTransaction(null, draft(date = LocalDate.of(2026, 2, 15), description = "february newer"))
        repository.saveTransaction(null, draft(date = LocalDate.of(2026, 3, 1), description = "march row"))

        val february = repository.observeMonthTransactions(YearMonth.of(2026, 2)).first()

        assertEquals(listOf("february newer", "february first"), february.map { it.description })
        assertEquals("Food", february.single { it.description == "february first" }.categoryName)
        assertEquals(TransactionType.EXPENSE, february.single { it.description == "february newer" }.transactionType)
    }

    @Test
    fun observeMonthExpenseSlicesGroupsExpensesOnlySortedByTotal() = runBlocking {
        repository.saveTransaction(null, draft(categoryName = "Food", amountCents = 1_000L, description = "a", date = LocalDate.of(2026, 2, 1)))
        repository.saveTransaction(null, draft(categoryName = "Food", amountCents = 2_000L, description = "b", date = LocalDate.of(2026, 2, 2)))
        repository.saveTransaction(null, draft(categoryName = "House", amountCents = 2_500L, description = "c", date = LocalDate.of(2026, 2, 3)))
        // Income must not appear in expense slices.
        repository.saveTransaction(null, draft(categoryName = "Food", type = TransactionType.INCOME, amountCents = 9_000L, description = "income", date = LocalDate.of(2026, 2, 4)))
        // Other month must not appear either.
        repository.saveTransaction(null, draft(categoryName = "Food", amountCents = 8_000L, description = "other month", date = LocalDate.of(2026, 1, 4)))

        val slices = repository.observeMonthExpenseSlices(YearMonth.of(2026, 2)).first()

        assertEquals(listOf("Food" to 3_000L, "House" to 2_500L), slices.map { it.categoryName to it.totalCents })
    }

    @Test
    fun updateAndDeleteFlowWorksThroughRealQueries() = runBlocking {
        repository.saveTransaction(null, draft(description = "before"))

        val created = repository.observeMonthTransactions(YearMonth.of(2026, 2)).first().single()
        assertTrue(repository.saveTransaction(created.id, draft(description = "after", amountCents = 990L)))

        val updated = repository.observeMonthTransactions(YearMonth.of(2026, 2)).first().single()
        assertEquals(created.id, updated.id)
        assertEquals("after", updated.description)
        assertEquals(990L, updated.amountCents)

        repository.deleteTransaction(created.id)
        assertTrue(repository.observeMonthTransactions(YearMonth.of(2026, 2)).first().isEmpty())
    }

    @Test
    fun importCsvCreatesCategoriesMapsTypesAndStaysIdempotentOnReruns() = runBlocking {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            01/02/2026,Cash,Food,-10,EUR,-10,EUR,lunch
            02/02/2026,Bank,Salary,2500,EUR,2500,EUR,payday
            01/02/2026,Cash,Food,-10,EUR,-10,EUR,lunch
        """.trimIndent()

        val firstRun = repository.importCsv(csv.byteInputStream())
        assertEquals(2, firstRun.insertedRows)
        assertEquals(1, firstRun.duplicateRows)
        assertEquals(0, firstRun.invalidRows)

        val february = repository.observeMonthTransactions(YearMonth.of(2026, 2)).first()
        assertEquals(2, february.size)
        assertEquals(TransactionType.INCOME, february.first().transactionType) // payday, newest first
        assertEquals(TransactionType.EXPENSE, february.last().transactionType)
        assertEquals(setOf("Food", "Salary"), repository.observeCategoryNames().first().toSet())

        val secondRun = repository.importCsv(csv.byteInputStream())
        assertEquals(0, secondRun.insertedRows)
        assertEquals(3, secondRun.duplicateRows)
        assertEquals(2, database.transactionDao().count())
    }
}
