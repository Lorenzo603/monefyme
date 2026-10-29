package com.lorenzo.monefyme.ui.util

import com.lorenzo.monefyme.domain.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/uuuu", Locale.US)
    .withResolverStyle(ResolverStyle.STRICT)

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.getDefault())

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun parseDateOrNull(raw: String): LocalDate? {
    return runCatching {
        LocalDate.parse(raw.trim(), dateFormatter)
    }.getOrNull()
}

fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

fun amountTextToCentsOrNull(rawAmount: String): Long? {
    val normalized = rawAmount.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    return runCatching {
        BigDecimal(normalized)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()?.takeIf { it > 0 }
}

fun centsToEditableAmountText(cents: Long): String {
    return BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
}

fun centsToDisplay(cents: Long, type: TransactionType? = null): String {
    val amount = BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
    val signPrefix = when (type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        null -> ""
    }
    return "$signPrefix${amount.toPlainString()}"
}
