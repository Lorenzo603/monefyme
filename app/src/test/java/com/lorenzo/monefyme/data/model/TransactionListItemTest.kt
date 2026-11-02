package com.lorenzo.monefyme.data.model

import com.lorenzo.monefyme.domain.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionListItemTest {
    private fun item(type: String, epochDay: Long = 20_390L) = TransactionListItem(
        id = 1L,
        dateEpochDay = epochDay,
        categoryId = 2L,
        categoryName = "Food",
        amountCents = 1_250L,
        type = type,
        description = "Groceries",
        account = "Cash",
        currency = "EUR",
    )

    @Test
    fun transactionTypeParsesStoredType() {
        assertEquals(TransactionType.EXPENSE, item("EXPENSE").transactionType)
        assertEquals(TransactionType.INCOME, item("INCOME").transactionType)
    }

    @Test
    fun dateIsDerivedFromEpochDay() {
        assertEquals(LocalDate.of(2025, 10, 29), item("EXPENSE").date)
    }
}
