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

private val groupHeaderDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
        
private val groupDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.getDefault())

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun formatDateForDateGroupHeader(date: LocalDate): String = date.format(groupHeaderDateFormatter)

fun formatDateForGroup(date: LocalDate): String = date.format(groupDateFormatter)

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

fun getAmount(cents: Long): BigDecimal {
    return BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
}

fun centsToDisplay(cents: Long, type: TransactionType? = null): String {
    val amount = getAmount(cents)
    val signPrefix = when (type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        null -> ""
    }
    return "$signPrefix${amount.toPlainString()}"
}

fun centsToDisplayForGroup(cents: Long): String {
    val amount = getAmount(cents)
    return "€${amount.toPlainString()}"
}
