package com.lorenzo.monefyme.data.model

import com.lorenzo.monefyme.domain.TransactionType
import java.time.LocalDate

data class TransactionListItem(
    val id: Long,
    val dateEpochDay: Long,
    val categoryId: Long,
    val categoryName: String,
    val amountCents: Long,
    val type: String,
    val description: String,
    val account: String,
    val currency: String,
) {
    val transactionType: TransactionType
        get() = TransactionType.fromStorage(type)

    val date: LocalDate
        get() = LocalDate.ofEpochDay(dateEpochDay)
}
