package com.lorenzo.monefyme.ui.util

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure logic behind the improved DatePickerModal: absolute month indexing and
 * month/year navigation math (used by the scrollable calendar pager, the prev/next month buttons
 * and the month/year dropdown) and the year options it offers. The manual date entry modal's
 * single dd/MM/yyyy input is validated by parseDateOrNull, covered in FormattersTest.
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
    fun yearOptionsSpanABroadScrollableRangeAroundTheAppliedYear() {
        val options = yearOptions(2025)
        // Many more years than the old +/-5 window: the dropdown grid scrolls through them.
        assertEquals(YEAR_MENU_PAST_YEARS + 1 + YEAR_MENU_FUTURE_YEARS, options.size)
        assertEquals((1995..2075).toList(), options)
        assertTrue(options.contains(2025))
        assertTrue(
            "The window must reach at least 20 years into both directions",
            options.first() <= 2025 - 20 && options.last() >= 2025 + 20,
        )
        // The applied year sits exactly YEAR_MENU_PAST_YEARS items in, which is where the grid
        // scrolls on open; a multiple of the three grid columns keeps it at the start of a row.
        assertEquals(2025, options[YEAR_MENU_PAST_YEARS])
        assertEquals(0, YEAR_MENU_PAST_YEARS % 3)
    }

    @Test
    fun yearOptionsAdaptToCustomSpans() {
        assertEquals(listOf(2024, 2025, 2026), yearOptions(2025, pastYears = 1, futureYears = 1))
        assertEquals((1995..2000).toList(), yearOptions(2000, pastYears = 5, futureYears = 0))
        assertTrue(yearOptions(2025).contains(2025))
    }
}
