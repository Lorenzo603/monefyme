package com.lorenzo.monefyme.ui.screen

import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import com.lorenzo.monefyme.ui.viewmodel.MainViewModelTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TransactionHistoryScreenTest {

    @Test
    fun transactionHistoryScreenGroupsByCategoryByDefault() {
        val categories = listOf(
            CategoryItem(id = 1L, displayName = "Food", iconName = "Restaurant", colorArgb = null),
            CategoryItem(id = 2L, displayName = "Transport", iconName = "LocalTaxi", colorArgb = null),
        )
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
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
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 3L,
                dateEpochDay = LocalDate.of(2024, 1, 17).toEpochDay(),
                categoryId = 2L,
                categoryName = "Transport",
                amountCents = 500L,
                type = "EXPENSE",
                description = "Taxi",
                account = "Card",
                currency = "EUR",
            ),
        )

        // Group by category
        val groupedByCategory = transactions.groupBy { it.categoryName }
        assertEquals(2, groupedByCategory.size)
        assertTrue(groupedByCategory.containsKey("Food"))
        assertTrue(groupedByCategory.containsKey("Transport"))
        assertEquals(2, groupedByCategory["Food"]?.size)
        assertEquals(1, groupedByCategory["Transport"]?.size)
    }

    @Test
    fun transactionHistoryScreenGroupsByDateWhenToggled() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
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
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 3L,
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

        // Group by date (formatted as string)
        val groupedByDate = transactions.groupBy { formatDate(it.date) }
        assertEquals(2, groupedByDate.size)
        assertTrue(groupedByDate.containsKey("15/01/2024"))
        assertTrue(groupedByDate.containsKey("16/01/2024"))
        assertEquals(2, groupedByDate["15/01/2024"]?.size)
        assertEquals(1, groupedByDate["16/01/2024"]?.size)
    }

    @Test
    fun transactionHistoryScreenCalculatesGroupTotals() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
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
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
        )

        val groupedByCategory = transactions.groupBy { it.categoryName }
        val foodGroup = groupedByCategory["Food"]!!
        val totalAmount = foodGroup.sumOf { it.amountCents }
        val transactionCount = foodGroup.size

        assertEquals(3000L, totalAmount)
        assertEquals(2, transactionCount)
    }

    @Test
    fun transactionHistoryScreenEmptyListShowsNoGroups() {
        val transactions = emptyList<TransactionListItem>()
        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(0, groupedByCategory.size)
        assertEquals(0, groupedByDate.size)
    }

    @Test
    fun transactionHistoryScreenSingleTransactionGroupsCorrectly() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 1000L,
                type = "EXPENSE",
                description = "Lunch",
                account = "Cash",
                currency = "EUR",
            ),
        )

        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(1, groupedByCategory.size)
        assertEquals(1, groupedByDate.size)
        assertEquals(1, groupedByCategory["Food"]?.size)
        assertEquals("15/01/2024", groupedByDate.keys.first())
    }

    @Test
    fun transactionHistoryScreenEmptyListGroupsCorrectly() {
        val transactions = emptyList<TransactionListItem>()
        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(0, groupedByCategory.size)
        assertEquals(0, groupedByDate.size)
    }
}
