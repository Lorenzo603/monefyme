package com.lorenzo.monefyme.data.model

data class ExpenseSlice(
    val categoryName: String,
    val totalCents: Long,
    val colorArgb: Int? = null,
    val icon: String? = null,
)
