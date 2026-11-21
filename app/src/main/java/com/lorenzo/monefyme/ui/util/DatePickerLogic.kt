package com.lorenzo.monefyme.ui.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure calculation helpers backing the custom [com.lorenzo.monefyme.ui.component.DatePickerModal].
 *
 * They are kept free of any Compose/Android dependency so they can be unit tested on the JVM:
 * month/year navigation across the pager and the month/year dropdown. The manual date entry
 * dialog validates its single dd/MM/yyyy input with [com.lorenzo.monefyme.ui.util.parseDateOrNull].
 */

/**
 * Years the month/year dropdown grid offers before and after the applied year. The grid shows
 * them in three columns, so the past-years count stays a multiple of three: the grid can then
 * scroll to a whole row starting exactly at the applied year.
 */
const val YEAR_MENU_PAST_YEARS = 30
const val YEAR_MENU_FUTURE_YEARS = 50

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
 * Years offered by the month/year dropdown grid: from [pastYears] below to [futureYears] above
 * [currentYear], in ascending order, always containing [currentYear]. The window is deliberately
 * broad (many grid rows) so the panel scrolls vertically instead of paging year by year.
 */
fun yearOptions(
    currentYear: Int,
    pastYears: Int = YEAR_MENU_PAST_YEARS,
    futureYears: Int = YEAR_MENU_FUTURE_YEARS,
): List<Int> = ((currentYear - pastYears)..(currentYear + futureYears)).toList()
