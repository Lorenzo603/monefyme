package com.lorenzo.monefyme.data.importer

import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * Writes stored transactions back out in the exact CSV layout [MonefyCsvParser] reads
 * (see the tracked Monefy.Data.29-10-2026.csv sample): the 8-column header, `dd/MM/uuuu`
 * dates, the amount repeated in the "converted amount" columns, and RFC 4180 quoting.
 *
 * The stored model keeps `amountCents > 0` plus an INCOME/EXPENSE type, so the signed
 * amount is derived here: negative for EXPENSE, positive for INCOME, plain decimals with
 * a dot separator and no trailing zeros (e.g. -50, -37.5, 1000).
 */
class MonefyCsvExporter {
    fun export(transactions: List<TransactionListItem>): ByteArray {
        val builder = StringBuilder()
        builder.append(HEADER).append("\n")
        transactions.forEach { transaction ->
            builder.append(buildRow(transaction)).append("\n")
        }
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    private fun buildRow(transaction: TransactionListItem): String {
        val amount = formatAmount(transaction)
        val currency = transaction.currency.trim().ifEmpty { DEFAULT_CURRENCY }
        val date = TRANSACTION_DATE_FORMATTER.format(LocalDate.ofEpochDay(transaction.dateEpochDay))
        return listOf(
            date,
            transaction.account,
            transaction.categoryName,
            amount,
            currency,
            amount,
            currency,
            transaction.description,
        ).joinToString(",") { escapeField(it) }
    }

    private fun formatAmount(transaction: TransactionListItem): String {
        val signedCents = when (transaction.transactionType) {
            TransactionType.EXPENSE -> -transaction.amountCents
            TransactionType.INCOME -> transaction.amountCents
        }
        return BigDecimal.valueOf(signedCents, 2).stripTrailingZeros().toPlainString()
    }

    /** Quotes a field when it contains a comma, quote, CR, or LF, doubling inner quotes (RFC 4180). */
    private fun escapeField(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    companion object {
        const val HEADER = "date,account,category,amount,currency,converted amount,currency,description"
        const val DEFAULT_CURRENCY = "EUR"

        private val TRANSACTION_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd/MM/uuuu", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
