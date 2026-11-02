package com.lorenzo.monefyme.data.importer

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonefyCsvParserTest {
    private val parser = MonefyCsvParser()

    @Test
    fun `parses valid monefy rows with quoted fields`() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            29/10/2026,Cash,Eating out,-50.75,EUR,-50.75,EUR,"Lunch, coffee"
            30/10/2026,Bank,Salary,1000,EUR,1000,EUR,October salary
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(0, result.invalidRows)
        assertEquals(2, result.rows.size)
        assertEquals("Lunch, coffee", result.rows[0].description)
        assertEquals("Salary", result.rows[1].category)
        assertEquals("1000", result.rows[1].amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun `counts invalid rows without stopping`() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            bad,Cash,Food,-12,EUR,-12,EUR,broken date
            29/10/2026,Cash,Food,-12,EUR,-12,EUR,valid row
            29/10/2026,Cash,,10,EUR,10,EUR,missing category
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(2, result.invalidRows)
        assertEquals(1, result.rows.size)
        assertTrue(result.rows.first().amount.signum() < 0)
    }

    @Test
    fun handlesEscapedQuotesInsideFields() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            29/10/2026,Cash,Food,-3.20,EUR,-3.20,EUR,"He said ""hi${"\"\"\""}
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(0, result.invalidRows)
        assertEquals("He said \"hi\"", result.rows.single().description)
    }

    @Test
    fun acceptsQuotedCommaDecimalAmountsAndDefaultsCurrencyToEur() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            29/10/2026,Cash,Food,"-12,50","","","",note
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(0, result.invalidRows)
        val row = result.rows.single()
        assertEquals("-12.50", row.amount.toPlainString())
        assertEquals("EUR", row.currency)
    }

    @Test
    fun skipsBlankLinesAndShortRows() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description

            29/10/2026,Cash,Food,-1,EUR,-1,EUR,short row below
            29/10/2026,Cash,Food
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, result.invalidRows)
        assertEquals(1, result.rows.size)
    }

    @Test
    fun parsesStrictDayMonthYearDatesOnly() {
        val csv = """
            date,account,category,amount,currency,converted amount,currency,description
            31/02/2026,Cash,Food,-1,EUR,-1,EUR,impossible date
            3/11/2026,Cash,Food,-1,EUR,-1,EUR,unpadded day is rejected
            03/11/2026,Cash,Food,-1,EUR,-1,EUR,padded day is accepted
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(2, result.invalidRows)
        assertEquals(java.time.LocalDate.of(2026, 11, 3), result.rows.single().date)
    }
}
