package com.lorenzo.monefyme.ui.util

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure calculation helpers backing the custom [com.lorenzo.monefyme.ui.component.DatePickerModal].
 *
 * They are kept free of any Compose/Android dependency so they can be unit tested on the JVM:
 * month/year navigation across the pager and the month/year dropdown, and parsing/validation of
 * the manually entered day/month/year fields.
 */

/** Half-width of the year window shown by the month/year dropdown (center year +/- this span). */
const val YEAR_MENU_SPAN = 5

/**
 * Absolute month counter (year * 12 + (month - 1)); strictly increasing across year boundaries,
 * so month navigation is plain integer arithmetic.
 */
fun absoluteMonthIndexOf(date: LocalDate): Int = date.year * 12 + (date.monthValue - 1)

/** Absolute month counter for a [YearMonth]; inverse of [yearMonthOfAbsoluteMonthIndex]. */
fun absoluteMonthIndexOf(month: YearMonth): Int = month.year * 12 + (month.monthValue - 1)

/**
 * Inverse of [absoluteMonthIndexOf]; uses floor division/modulo so it also round-trips indices
 * below zero (years before year 1 of the calendar).
 */
fun yearMonthOfAbsoluteMonthIndex(index: Int): YearMonth =
    YearMonth.of(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1)

/** Move [month] by [deltaMonths] months (negative goes back); wraps across year boundaries. */
fun shiftMonth(month: YearMonth, deltaMonths: Int): YearMonth = month.plusMonths(deltaMonths.toLong())

/**
 * Years offered by the month/year dropdown: [span] years below and above [centerYear], in
 * ascending order, always containing [centerYear].
 */
fun yearOptions(centerYear: Int, span: Int = YEAR_MENU_SPAN): List<Int> =
    ((centerYear - span)..(centerYear + span)).toList()

/**
 * Parse the manually entered day/month/year text into a real date, or null when any field is
 * missing/non-numeric or the combination is not a valid calendar date (day 0/32, month 13,
 * 31 February, 29 February in a non-leap year, year outside 1..9999).
 */
fun parseManualDateOrNull(dayText: String, monthText: String, yearText: String): LocalDate? {
    val day = dayText.trim().toIntOrNull() ?: return null
    val month = monthText.trim().toIntOrNull() ?: return null
    val year = yearText.trim().toIntOrNull() ?: return null
    if (year !in 1..9999) return null
    return try {
        YearMonth.of(year, month).atDay(day)
    } catch (_: DateTimeException) {
        null
    }
}
