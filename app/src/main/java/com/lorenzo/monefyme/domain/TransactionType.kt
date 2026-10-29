package com.lorenzo.monefyme.domain

enum class TransactionType {
    EXPENSE,
    INCOME,
    ;

    companion object {
        fun fromStorage(value: String): TransactionType {
            return entries.firstOrNull { it.name == value } ?: EXPENSE
        }
    }
}
