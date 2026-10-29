package com.lorenzo.monefyme.domain

import java.time.LocalDate

data class TransactionDraft(
    val date: LocalDate,
    val type: TransactionType,
    val amountCents: Long,
    val categoryName: String,
    val description: String,
    val account: String,
    val currency: String = "EUR",
)
