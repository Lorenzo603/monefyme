package com.lorenzo.monefyme.ui.util

import com.lorenzo.monefyme.domain.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    @Test
    fun formatDateRendersDayMonthYear() {
        assertEquals("29/10/2025", formatDate(LocalDate.of(2025, 10, 29)))
        assertEquals("01/01/2026", formatDate(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun parseDateOrNullParsesSupportedFormat() {
        assertEquals(LocalDate.of(2025, 10, 29), parseDateOrNull("29/10/2025"))
        assertEquals(LocalDate.of(2025, 10, 29), parseDateOrNull("  29/10/2025  "))
    }

    @Test
    fun parseDateOrNullRejectsInvalidDates() {
        assertNull(parseDateOrNull(""))
        assertNull(parseDateOrNull("not a date"))
        assertNull(parseDateOrNull("29-10-2025"))
        assertNull(parseDateOrNull("2025/10/29"))
        assertNull(parseDateOrNull("31/02/2025"))
        assertNull(parseDateOrNull("29/13/2025"))
    }

    @Test
    fun formatMonthRendersMonthNameAndYear() {
        assertEquals("October 2025", formatMonth(YearMonth.of(2025, 10)))
    }

    @Test
    fun amountTextToCentsOrNullConvertsCommonNotations() {
        assertEquals(1250L, amountTextToCentsOrNull("12.50"))
        assertEquals(1250L, amountTextToCentsOrNull("12,50"))
        assertEquals(1000L, amountTextToCentsOrNull(" 10 "))
        assertEquals(1234567L, amountTextToCentsOrNull("12345.67"))
    }

    @Test
    fun amountTextToCentsOrNullRoundsHalfUpToCents() {
        assertEquals(1001L, amountTextToCentsOrNull("10.005"))
        assertEquals(1000L, amountTextToCentsOrNull("10.004"))
    }

    @Test
    fun amountTextToCentsOrNullRejectsNonPositiveOrBrokenAmounts() {
        assertNull(amountTextToCentsOrNull(""))
        assertNull(amountTextToCentsOrNull("   "))
        assertNull(amountTextToCentsOrNull("0"))
        assertNull(amountTextToCentsOrNull("0.001"))
        assertNull(amountTextToCentsOrNull("-12.50"))
        assertNull(amountTextToCentsOrNull("12,5O"))
        assertNull(amountTextToCentsOrNull("1.2.3"))
    }

    @Test
    fun centsToEditableAmountTextDropsTrailingZeros() {
        assertEquals("12.5", centsToEditableAmountText(1250L))
        assertEquals("10", centsToEditableAmountText(1000L))
        assertEquals("12.34", centsToEditableAmountText(1234L))
    }

    @Test
    fun centsToDisplayAlwaysShowsTwoDecimals() {
        assertEquals("10", centsToDisplay(1000L).substringBefore('.'))
        assertEquals("10.00", centsToDisplay(1000L))
        assertEquals("12.50", centsToDisplay(1250L))
    }

    @Test
    fun centsToDisplayAddsSignPerTransactionType() {
        assertEquals("-12.50", centsToDisplay(1250L, TransactionType.EXPENSE))
        assertEquals("+12.50", centsToDisplay(1250L, TransactionType.INCOME))
        assertEquals("12.50", centsToDisplay(1250L, null))
    }
}
