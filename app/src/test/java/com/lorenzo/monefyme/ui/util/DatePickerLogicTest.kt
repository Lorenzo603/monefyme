package com.lorenzo.monefyme.ui.util

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure logic behind the improved DatePickerModal: absolute month indexing and
 * month/year navigation math (used by the scrollable calendar pager, the prev/next month buttons
 * and the month/year dropdown), the year options it offers, and parsing/validation of the
 * manually entered day/month/year fields.
 */
class DatePickerLogicTest {

    // --- Absolute month index <-> YearMonth (calendar pager pages) ---

    @Test
    fun absoluteMonthIndexOfRoundTrips() {
        val dates = listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 10, 15),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
        )
        dates.forEach { date ->
            val index = absoluteMonthIndexOf(date)
            assertEquals(
                "index of $date must invert back to ${date.withDayOfMonth(1)}",
                YearMonth.of(date.year, date.monthValue),
                yearMonthOfAbsoluteMonthIndex(index),
            )
        }
    }

    @Test
    fun absoluteMonthIndexOfIncrementsAcrossYearBoundaries() {
        val december = absoluteMonthIndexOf(YearMonth.of(2025, 12))
        val january = absoluteMonthIndexOf(YearMonth.of(2026, 1))
        assertEquals(december + 1, january)
        assertEquals(YearMonth.of(2026, 1), yearMonthOfAbsoluteMonthIndex(january))

        val januaryBc = absoluteMonthIndexOf(YearMonth.of(2025, 1))
        val decemberPrev = absoluteMonthIndexOf(YearMonth.of(2024, 12))
        assertEquals(decemberPrev + 1, januaryBc)
        assertEquals(YearMonth.of(2024, 12), yearMonthOfAbsoluteMonthIndex(januaryBc - 1))
    }

    @Test
    fun yearMonthOfAbsoluteMonthIndexHandlesNegativeIndices() {
        // Floor division must keep the pair inverse even below zero (years before year 1).
        assertEquals(YearMonth.of(0, 12), yearMonthOfAbsoluteMonthIndex(absoluteMonthIndexOf(YearMonth.of(0, 12))))
        assertEquals(YearMonth.of(0, 1), yearMonthOfAbsoluteMonthIndex(absoluteMonthIndexOf(YearMonth.of(0, 1))))
        // December is the highest index in its absolute year, January the lowest.
        assertEquals(11, Math.floorMod(absoluteMonthIndexOf(YearMonth.of(0, 12)), 12))
        assertEquals(0, Math.floorMod(absoluteMonthIndexOf(YearMonth.of(0, 1)), 12))
    }

    // --- Prev/next month navigation math ---

    @Test
    fun shiftMonthWrapsAcrossYearsInBothDirections() {
        assertEquals(YearMonth.of(2026, 1), shiftMonth(YearMonth.of(2025, 12), 1))
        assertEquals(YearMonth.of(2025, 12), shiftMonth(YearMonth.of(2026, 1), -1))
        assertEquals(YearMonth.of(2024, 12), shiftMonth(YearMonth.of(2025, 1), -1))
        assertEquals(YearMonth.of(2027, 1), shiftMonth(YearMonth.of(2025, 12), 13))
        assertEquals(YearMonth.of(2024, 12), shiftMonth(YearMonth.of(2025, 12), -12))
        assertEquals(YearMonth.of(2023, 12), shiftMonth(YearMonth.of(2024, 12), -12))
        assertEquals(YearMonth.of(2025, 10), shiftMonth(YearMonth.of(2025, 10), 0))
    }

    @Test
    fun shiftMonthStaysConsistentWithAbsoluteIndex() {
        val month = YearMonth.of(2025, 10)
        assertEquals(absoluteMonthIndexOf(month) + 3, absoluteMonthIndexOf(shiftMonth(month, 3)))
        assertEquals(absoluteMonthIndexOf(month) - 15, absoluteMonthIndexOf(shiftMonth(month, -15)))
    }

    // --- Year dropdown options ---

    @Test
    fun yearOptionsCenterOnCurrentYearAscending() {
        val options = yearOptions(2025)
        assertEquals(2 * YEAR_MENU_SPAN + 1, options.size)
        assertEquals((2020..2030).toList(), options)
        assertTrue(options.contains(2025))
    }

    @Test
    fun yearOptionsSupportCyclingToAdjacentWindows() {
        // Tapping the cycle buttons must always expose the newly centered year.
        val before = yearOptions(2024, span = 1)
        val after = yearOptions(2026, span = 1)
        assertEquals(listOf(2023, 2024, 2025), before)
        assertEquals(listOf(2025, 2026, 2027), after)
        assertTrue(before.contains(2025) && after.contains(2025))
        assertFalse(before.contains(2026))
        assertFalse(after.contains(2024))
    }

    // --- Manual day/month/year entry parsing and validation ---

    @Test
    fun parseManualDateAcceptsValidDates() {
        assertEquals(LocalDate.of(2025, 2, 1), parseManualDateOrNull("1", "2", "2025")) // day=1, month=2
        assertEquals(LocalDate.of(2025, 1, 2), parseManualDateOrNull("2", "1", "2025")) // day=2, month=1
        assertEquals(LocalDate.of(2025, 10, 29), parseManualDateOrNull("29", "10", "2025"))
        assertEquals(LocalDate.of(2025, 10, 29), parseManualDateOrNull(" 29 ", "10 ", " 2025"))
        assertEquals(LocalDate.of(2024, 2, 29), parseManualDateOrNull("29", "02", "2024")) // leap year
        assertEquals(LocalDate.of(2100, 12, 31), parseManualDateOrNull("31", "12", "2100"))
    }

    @Test
    fun parseManualDateRejectsInvalidInput() {
        assertNull(parseManualDateOrNull("", "10", "2025"))
        assertNull(parseManualDateOrNull("29", "", "2025"))
        assertNull(parseManualDateOrNull("29", "10", ""))
        assertNull(parseManualDateOrNull("29a", "10", "2025"))
        assertNull(parseManualDateOrNull("2.9", "10", "2025"))
        assertNull(parseManualDateOrNull("0", "10", "2025")) // day below 1
        assertNull(parseManualDateOrNull("32", "10", "2025")) // day above 31
        assertNull(parseManualDateOrNull("29", "13", "2025")) // month above 12
        assertNull(parseManualDateOrNull("29", "0", "2025")) // month below 1
        assertNull(parseManualDateOrNull("31", "2", "2025")) // 31 February
        assertNull(parseManualDateOrNull("29", "2", "2025")) // 29 February in a non-leap year
        assertNull(parseManualDateOrNull("31", "4", "2025")) // 31 April
        assertNull(parseManualDateOrNull("29", "10", "0")) // year below 1
        assertNull(parseManualDateOrNull("29", "10", "10000")) // year above 9999
        assertNull(parseManualDateOrNull("-1", "10", "2025")) // negative day
        assertNull(parseManualDateOrNull("29", "-1", "2025")) // negative month
    }

    @Test
    fun parseManualDateUsesLeapYearRules() {
        assertEquals(LocalDate.of(2024, 2, 29), parseManualDateOrNull("29", "2", "2024"))
        assertNull(parseManualDateOrNull("29", "2", "2023"))
        // Century rule: 2000 is a leap year, 1900 is not.
        assertEquals(LocalDate.of(2000, 2, 29), parseManualDateOrNull("29", "2", "2000"))
        assertNull(parseManualDateOrNull("29", "2", "1900"))
    }
}
