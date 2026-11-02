package com.lorenzo.monefyme.data.repository

import com.lorenzo.monefyme.data.local.dao.CategoryDao
import com.lorenzo.monefyme.data.local.dao.TransactionDao
import com.lorenzo.monefyme.data.local.entity.CategoryEntity
import com.lorenzo.monefyme.data.local.entity.TransactionEntity
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BudgetRepositoryImpl] business logic, backed by in-memory fake DAOs so no
 * Android/Room infrastructure is required. SQL semantics (unique constraints, joins, ordering,
 * date ranges) are mirrored by the fakes; the real queries are covered by the Room-backed
 * integration test in androidTest.
 */
class BudgetRepositoryImplTest {
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var repository: BudgetRepositoryImpl

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        transactionDao = FakeTransactionDao(categoryDao)
        repository = BudgetRepositoryImpl(categoryDao, transactionDao)
    }

    private fun seedCategory(displayName: String, iconName: String? = null, colorArgb: Int? = null): Long {
        return runBlocking { categoryDao.insert(CategoryEntity(normalizedName = displayName.lowercase(), displayName = displayName, iconName = iconName, colorArgb = colorArgb)) }
    }

    // --- queries ---------------------------------------------------------------

    @Test
    fun observeCategoryNamesMapsEntitiesToDisplayNames() = runBlocking {
        seedCategory("Food")
        seedCategory("House")

        assertEquals(listOf("Food", "House"), repository.observeCategoryNames().first())
    }

    @Test
    fun observeCategoriesMapsStoredIconAndColorOntoCategoryItems() = runBlocking {
        val id = seedCategory("Food", iconName = "ShoppingCart", colorArgb = 0xFFDE6CA5.toInt())

        val item = repository.observeCategories().first().single()
        assertEquals(id, item.id)
        assertEquals("Food", item.displayName)
        assertEquals("ShoppingCart", item.iconName)
        assertEquals(0xFFDE6CA5.toInt(), item.colorArgb)
    }

    @Test
    fun observeMonthTransactionsUsesExactMonthDateRange() = runBlocking {
        val February2026 = YearMonth.of(2026, 2)
        val expectedStart = LocalDate.of(2026, 2, 1).toEpochDay()
        val expectedEnd = LocalDate.of(2026, 2, 28).toEpochDay()

        repository.observeMonthTransactions(February2026).first()

        assertEquals(expectedStart, transactionDao.lastRangeStart)
        assertEquals(expectedEnd, transactionDao.lastRangeEnd)
    }

    @Test
    fun observeMonthExpenseSlicesUsesExactMonthDateRange() = runBlocking {
        val December2025 = YearMonth.of(2025, 12)
        val expectedStart = LocalDate.of(2025, 12, 1).toEpochDay()
        val expectedEnd = LocalDate.of(2025, 12, 31).toEpochDay()

        repository.observeMonthExpenseSlices(December2025).first()

        assertEquals(expectedStart, transactionDao.lastSliceRangeStart)
        assertEquals(expectedEnd, transactionDao.lastSliceRangeEnd)
    }

    // --- categories ------------------------------------------------------------

    @Test
    fun saveCategoryNormalizesDisplayAndLookupName() = runBlocking {
        assertTrue(repository.saveCategory("  Fast   food  ", "Fastfood", 0xFF112233.toInt()))

        val stored = categoryDao.findByNormalizedName("fast food")
        assertNotNull(stored)
        assertEquals("Fast food", stored!!.displayName)
        assertEquals("fast food", stored.normalizedName)
        assertEquals("Fastfood", stored.iconName)
        assertEquals(0xFF112233.toInt(), stored.colorArgb)
    }

    @Test
    fun saveCategoryRejectsDuplicatesRegardlessOfCaseAndPadding() = runBlocking {
        seedCategory("Food")

        assertFalse(repository.saveCategory("  FOOD ", "ShoppingCart", 0))
    }

    // --- transactions ----------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun saveTransactionRejectsNonPositiveAmount() {
        runBlocking {
            repository.saveTransaction(
                transactionId = null,
                draft = draft(amountCents = 0L),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveTransactionRejectsBlankCategory() {
        runBlocking {
            repository.saveTransaction(
                transactionId = null,
                draft = draft(categoryName = "   "),
            )
        }
    }

    @Test
    fun saveTransactionNewInsertsEntityAndResolvesCategoryCaseInsensitively() = runBlocking {
        val categoryId = seedCategory("Food")

        assertTrue(
            repository.saveTransaction(
                transactionId = null,
                draft = draft(categoryName = " food ", description = "  Lunch ", account = " Cash ", currency = "  "),
            ),
        )

        val stored = transactionDao.queries.single()
        assertEquals(categoryId, stored.categoryId)
        assertEquals(2025, LocalDate.ofEpochDay(stored.dateEpochDay).year)
        assertEquals(1_250L, stored.amountCents)
        assertEquals("EXPENSE", stored.type)
        assertEquals("Lunch", stored.description)
        assertEquals("Cash", stored.account)
        assertEquals("EUR", stored.currency) // blank currency defaults to EUR
    }

    @Test
    fun saveTransactionCreatesMissingCategoryOnTheFly() = runBlocking {
        assertTrue(repository.saveTransaction(null, draft(categoryName = "Yachts")))

        assertNotNull(categoryDao.findByNormalizedName("yachts"))
        assertEquals(1, categoryDao.stored.size)
    }

    @Test
    fun saveTransactionUpdateAppliesToExistingRowOnly() = runBlocking {
        seedCategory("Food")
        assertTrue(repository.saveTransaction(null, draft(description = "before")))

        val id = transactionDao.queries.single().id
        assertTrue(repository.saveTransaction(id, draft(description = "after", amountCents = 990L)))

        val updated = requireNotNull(transactionDao.findById(id))
        assertEquals("after", updated.description)
        assertEquals(990L, updated.amountCents)
        assertEquals(1, transactionDao.queries.size)
    }

    @Test
    fun saveTransactionUpdateOfUnknownIdReturnsFalse() = runBlocking {
        seedCategory("Food")

        assertFalse(repository.saveTransaction(999L, draft()))
        assertTrue(transactionDao.queries.isEmpty())
    }

    @Test
    fun deleteTransactionRemovesExistingRowAndIgnoresUnknownId() = runBlocking {
        seedCategory("Food")
        repository.saveTransaction(null, draft())
        val id = transactionDao.queries.single().id

        repository.deleteTransaction(id)
        assertEquals(0, transactionDao.count())

        repository.deleteTransaction(404L) // must not throw
        assertEquals(0, transactionDao.count())
    }

    // --- CSV import ------------------------------------------------------------

    @Test
    fun importCsvMapsSignToTransactionTypeAndRoundsAmounts() = runBlocking {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            01/02/2026,Cash,Food,-10.005,EUR,-10.005,EUR,lunch rounds half up
            02/02/2026,Bank,Salary,2500,EUR,2500,EUR,monthly salary
            03/02/2026,Cash,Food,0,EUR,0,EUR,zero amount is invalid
            broken
        """.trimIndent()

        val result = repository.importCsv(csv.byteInputStream())

        assertEquals(4, result.processedRows)
        assertEquals(2, result.insertedRows)
        assertEquals(0, result.duplicateRows)
        assertEquals(2, result.invalidRows)

        val expense = transactionDao.queries.first { it.type == "EXPENSE" }
        assertEquals(1_001L, expense.amountCents) // 10.005 -> 1000.5 cents -> 1001 (HALF_UP)
        val income = transactionDao.queries.first { it.type == "INCOME" }
        assertEquals(250_000L, income.amountCents)
    }

    @Test
    fun importCsvReusesOneCategoryForRepeatedNames() = runBlocking {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            01/02/2026,Cash,Food,-1,EUR,-1,EUR,one
            02/02/2026,Cash, food ,-2,EUR,-2,EUR,two
            03/02/2026,Cash,FOOD,-3,EUR,-3,EUR,three
        """.trimIndent()

        repository.importCsv(csv.byteInputStream())

        assertEquals(1, categoryDao.stored.size)
        assertEquals("Food", categoryDao.stored.single().displayName)
        assertEquals(3, transactionDao.queries.size)
    }

    @Test
    fun importCsvCountsConflictingRowsAsDuplicates() = runBlocking {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            01/02/2026,Cash,Food,-10,EUR,-10,EUR,same row
            01/02/2026,Cash,Food,-10,EUR,-10,EUR,same row
        """.trimIndent()

        val first = repository.importCsv(csv.byteInputStream())
        assertEquals(1, first.insertedRows)
        assertEquals(1, first.duplicateRows)

        val secondRun = repository.importCsv(csv.byteInputStream())
        assertEquals(0, secondRun.insertedRows)
        // Both rows of the second import conflict with what is already stored.
        assertEquals(2, secondRun.duplicateRows)
        assertEquals(1, transactionDao.count())
    }

    private fun draft(
        amountCents: Long = 1_250L,
        categoryName: String = "Food",
        description: String = "note",
        account: String = "Cash",
        currency: String = "EUR",
        type: TransactionType = TransactionType.EXPENSE,
    ) = com.lorenzo.monefyme.domain.TransactionDraft(
        date = LocalDate.of(2025, 10, 29),
        type = type,
        amountCents = amountCents,
        categoryName = categoryName,
        description = description,
        account = account,
        currency = currency,
    )

    // --- fakes -----------------------------------------------------------------

    private class FakeCategoryDao : CategoryDao {
        val stored = mutableListOf<CategoryEntity>()
        private var nextId = 1L

        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(stored.sortedBy { it.displayName.lowercase() })

        override suspend fun findByNormalizedName(normalizedName: String): CategoryEntity? =
            stored.firstOrNull { it.normalizedName == normalizedName }

        override suspend fun insert(category: CategoryEntity): Long {
            if (stored.any { it.normalizedName == category.normalizedName }) return -1L
            val entity = category.copy(id = nextId++)
            stored += entity
            return entity.id
        }
    }

    private class FakeTransactionDao(
        private val categoryDao: FakeCategoryDao,
    ) : TransactionDao {
        val queries = mutableListOf<TransactionEntity>()
        private var nextId = 1L
        var lastRangeStart: Long? = null
        var lastRangeEnd: Long? = null
        var lastSliceRangeStart: Long? = null
        var lastSliceRangeEnd: Long? = null

        override fun observeByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionListItem>> {
            lastRangeStart = startEpochDay
            lastRangeEnd = endEpochDay
            val items = queries
                .filter { it.dateEpochDay in startEpochDay..endEpochDay }
                .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.id })
                .mapNotNull { transaction ->
                    val category = categoryDao.stored.firstOrNull { it.id == transaction.categoryId } ?: return@mapNotNull null
                    TransactionListItem(
                        id = transaction.id,
                        dateEpochDay = transaction.dateEpochDay,
                        categoryId = transaction.categoryId,
                        categoryName = category.displayName,
                        amountCents = transaction.amountCents,
                        type = transaction.type,
                        description = transaction.description,
                        account = transaction.account,
                        currency = transaction.currency,
                    )
                }
            return flowOf(items)
        }

        override fun observeExpenseSlicesByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseSlice>> {
            lastSliceRangeStart = startEpochDay
            lastSliceRangeEnd = endEpochDay
            val slices = queries
                .filter { it.type == "EXPENSE" && it.dateEpochDay in startEpochDay..endEpochDay }
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, rows) ->
                    val category = categoryDao.stored.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                    ExpenseSlice(categoryName = category.displayName, totalCents = rows.sumOf { it.amountCents })
                }
                .sortedByDescending { it.totalCents }
            return flowOf(slices)
        }

        override suspend fun insert(transaction: TransactionEntity): Long {
            val duplicate = queries.any {
                it.dateEpochDay == transaction.dateEpochDay &&
                    it.categoryId == transaction.categoryId &&
                    it.amountCents == transaction.amountCents &&
                    it.type == transaction.type &&
                    it.description == transaction.description &&
                    it.account == transaction.account
            }
            if (duplicate) return -1L
            val entity = transaction.copy(id = nextId++)
            queries += entity
            return entity.id
        }

        override suspend fun update(transaction: TransactionEntity) {
            val index = queries.indexOfFirst { it.id == transaction.id }
            if (index >= 0) queries[index] = transaction
        }

        override suspend fun delete(transaction: TransactionEntity) {
            queries.removeAll { it.id == transaction.id }
        }

        override suspend fun findById(id: Long): TransactionEntity? = queries.firstOrNull { it.id == id }

        override suspend fun count(): Int = queries.size
    }
}
