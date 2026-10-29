package com.lorenzo.monefyme.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lorenzo.monefyme.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryImportTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository

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

    @Test
    fun importCsv_isIdempotentOnDuplicateRows() = runBlocking {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            01/01/2026,Cash,Food,-10,EUR,-10,EUR,Coffee
            01/01/2026,Cash,Food,-10,EUR,-10,EUR,Coffee
            01/01/2026,Cash,Food,0,EUR,0,EUR,invalid amount
        """.trimIndent()

        val result = repository.importCsv(csv.byteInputStream())

        assertEquals(3, result.processedRows)
        assertEquals(1, result.insertedRows)
        assertEquals(1, result.duplicateRows)
        assertEquals(1, result.invalidRows)
        assertEquals(1, database.transactionDao().count())
    }
}
