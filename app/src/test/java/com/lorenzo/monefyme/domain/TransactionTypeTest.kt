package com.lorenzo.monefyme.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionTypeTest {
    @Test
    fun fromStorageParsesStoredNames() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromStorage("EXPENSE"))
        assertEquals(TransactionType.INCOME, TransactionType.fromStorage("INCOME"))
    }

    @Test
    fun fromStorageFallsBackToExpenseForUnknownValues() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromStorage("expense"))
        assertEquals(TransactionType.EXPENSE, TransactionType.fromStorage(""))
        assertEquals(TransactionType.EXPENSE, TransactionType.fromStorage("TRANSFER"))
    }
}
