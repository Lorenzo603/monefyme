package com.lorenzo.monefyme.data.importer

import com.lorenzo.monefyme.data.model.TransactionListItem
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MonefyCsvExporter]: the exported bytes must be readable by the real
 * [MonefyCsvParser] (round trip), so the two directions of the CSV format stay in sync.
 */
class MonefyCsvExporterTest {
    private val exporter = MonefyCsvExporter()
    private val parser = MonefyCsvParser()

    private fun item(
        id: Long = 1L,
        date: LocalDate = LocalDate.of(2026, 11, 3),
        categoryName: String = "Food",
        amountCents: Long = 5_000L,
        type: String = "EXPENSE",
        description: String = "",
        account: String = "Cash",
        currency: String = "EUR",
    ) = TransactionListItem(
        id = id,
        dateEpochDay = date.toEpochDay(),
        categoryId = 1L,
        categoryName = categoryName,
        amountCents = amountCents,
        type = type,
        description = description,
        account = account,
        currency = currency,
    )

    private fun exportText(transactions: List<TransactionListItem>): String =
        exporter.export(transactions).toString(Charsets.UTF_8)

    @Test
    fun exportStartsWithTheExactImporterHeader() {
        assertEquals(
            "date,account,category,amount,currency,converted amount,currency,description\n",
            exportText(emptyList()),
        )
    }

    @Test
    fun rowUsesStrictDateAccountCategorySignedAmountCurrencyMirroredAndDescription() {
        val csv = exportText(
            listOf(
                item(
                    date = LocalDate.of(2026, 11, 3),
                    account = "Cash",
                    categoryName = "Eating out",
                    amountCents = 5_000L,
                    type = "EXPENSE",
                    description = "Lunch",
                ),
            ),
        )

        assertEquals(
            "date,account,category,amount,currency,converted amount,currency,description\n" +
                "03/11/2026,Cash,Eating out,-50,EUR,-50,EUR,Lunch\n",
            csv,
        )
    }

    @Test
    fun expenseIsNegativeAndIncomeIsPositive() {
        val csv = exportText(
            listOf(
                item(amountCents = 5_000L, type = "EXPENSE"),
                item(id = 2L, amountCents = 100_000L, type = "INCOME", categoryName = "Salary"),
            ),
        )

        val rows = csv.trim().lines().drop(1)
        assertTrue(rows[0].startsWith("03/11/2026,Cash,Food,-50,EUR,-50,EUR"))
        assertTrue(rows[1].startsWith("03/11/2026,Cash,Salary,1000,EUR,1000,EUR"))
    }

    @Test
    fun amountsHaveNoTrailingZerosAndUseTheDotSeparator() {
        val csv = exportText(
            listOf(
                item(id = 1L, amountCents = 5_000L), // 50.00 -> -50
                item(id = 2L, amountCents = 3_750L), // 37.50 -> -37.5
                item(id = 3L, amountCents = 100_000L), // 1000.00 -> -1000
                item(id = 4L, amountCents = 999L), // 9.99 -> -9.99
            ),
        )

        val amounts = csv.trim().lines().drop(1).map { it.split(",")[3] }
        assertEquals(listOf("-50", "-37.5", "-1000", "-9.99"), amounts)
    }

    @Test
    fun fieldsWithCommasQuotesAndNewlinesAreQuotedAndEscaped() {
        val csv = exportText(
            listOf(
                item(description = "Lunch, coffee"),
                item(id = 2L, description = "He said \"hi\""),
                item(id = 3L, description = "line1\nline2"),
                item(id = 4L, categoryName = "Groceries, fresh", account = "My \"Bank\""),
            ),
        )

        val rows = csv.trim().lines().drop(1)
        assertTrue(rows[0].endsWith(",\"Lunch, coffee\""))
        assertTrue(rows[1].endsWith(",\"He said \"\"hi\"\"\""))
        // Multi-line field is RFC 4180 quoted, so it spans two physical lines: the row
        // after it sits at index 4 rather than index 3.
        assertTrue(csv.contains("\"line1\nline2\""))
        assertEquals(5, rows.size)
        assertTrue(rows[4].startsWith("03/11/2026,\"My \"\"Bank\"\"\",\"Groceries, fresh\","))
    }

    @Test
    fun blankCurrencyDefaultsToEur() {
        val csv = exportText(listOf(item(currency = "  ")))

        val fields = csv.trim().lines().drop(1).single().split(",")
        assertEquals("EUR", fields[4])
        assertEquals("EUR", fields[6])
    }

    @Test
    fun emptyDatasetExportsAHeaderOnlyFile() {
        val csv = exportText(emptyList())

        assertEquals("date,account,category,amount,currency,converted amount,currency,description\n", csv)
        val parseResult = parser.parse(csv.byteInputStream())
        assertEquals(0, parseResult.rows.size)
        assertEquals(0, parseResult.invalidRows)
    }

    @Test
    fun roundTripThroughTheRealParserPreservesEveryField() {
        val transactions = listOf(
            item(
                id = 1L,
                date = LocalDate.of(2016, 2, 1),
                account = "Cash",
                categoryName = "Eating out",
                amountCents = 5_000L,
                type = "EXPENSE",
                description = "Cumulative",
            ),
            item(
                id = 2L,
                date = LocalDate.of(2026, 9, 30),
                account = "Bank",
                categoryName = "Salary",
                amountCents = 2_500_00L,
                type = "INCOME",
                description = "monthly, \"net\"",
                currency = "USD",
            ),
            item(
                id = 3L,
                date = LocalDate.of(2026, 11, 3),
                categoryName = "Food",
                amountCents = 3_750L,
                currency = "", // exported as EUR
            ),
        )

        val csv = exportText(transactions)
        val parseResult = parser.parse(csv.byteInputStream())

        assertEquals(0, parseResult.invalidRows)
        assertEquals(3, parseResult.rows.size)

        transactions.forEachIndexed { index, source ->
            val row = parseResult.rows[index]
            assertEquals(source.date, row.date)
            assertEquals(source.account, row.account)
            assertEquals(source.categoryName, row.category)
            assertEquals(
                source.description,
                row.description,
            )
            assertEquals(
                source.currency.trim().ifEmpty { "EUR" },
                row.currency,
            )

            val expectedCents = when (source.type) {
                "EXPENSE" -> -source.amountCents
                else -> source.amountCents
            }
            val expectedAmount = BigDecimal.valueOf(expectedCents, 2).stripTrailingZeros()
            assertEquals(0, row.amount.compareTo(expectedAmount))
        }
    }
}
