package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.lorenzo.monefyme.ui.component.DatePickerModal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for the rebuilt transaction date picker (DatePickerModal).
 *
 * Asserts the picker still returns the same LocalDate contract to its caller when a day is
 * tapped, and covers the new selectable behaviour: prev/next month buttons next to the
 * month/year dropdown, jumping to another year through the dropdown, scrolling the calendar
 * itself to a neighbouring month, and the header edit button opening a validated manual
 * day/month/year entry modal that returns the manually entered date.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class DatePickerModalUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var selected: LocalDate? = null
    private var dismissed = false

    private fun showPicker(initial: LocalDate) {
        selected = null
        dismissed = false
        composeTestRule.setContent {
            DatePickerModal(
                currentDate = initial,
                onDateSelected = { selected = it },
                onDismiss = { dismissed = true },
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun monthYearLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year

    /** Scoped to the dropdown node: the header date text may contain the same month/year words. */
    private fun assertDropdownShows(month: YearMonth) {
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("date_picker_month_year_dropdown")
            .assert(hasText(monthYearLabel(month), substring = true))
            .assertIsDisplayed()
    }

    /** Day tags are month-scoped, so neighbouring pager pages never collide. */
    private fun dayNode(month: YearMonth, day: Int) =
        composeTestRule.onNodeWithTag("date_picker_day_${month.year}_${month.monthValue}_$day")

    @Test
    fun dialogShowsSmallerTitleAndGreenHeaderWithTheSelectedDate() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select date").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_header").assertIsDisplayed()
        // Header shows the selected date and the manual-entry edit button.
        composeTestRule.onNodeWithTag("date_picker_selected_date").assertIsDisplayed()
        // Compact format: 3-letter month abbreviation, no weekday (was "Wednesday, 1 October 2025").
        composeTestRule.onNodeWithTag("date_picker_selected_date")
            .assertTextEquals("1 Oct 2025")
        composeTestRule.onNodeWithText("1 October 2025").assertDoesNotExist()
        composeTestRule.onNodeWithTag("date_picker_edit_button").assertIsDisplayed()
        assertDropdownShows(YearMonth.of(2025, 10))
    }

    @Test
    fun yearPickerSitsOnTheLeftWithBothMonthArrowsToItsRight() {
        showPicker(LocalDate.of(2025, 10, 1))

        val dropdownLeft = composeTestRule.onNodeWithTag("date_picker_month_year_dropdown")
            .fetchSemanticsNode().boundsInRoot.left
        val previousLeft = composeTestRule.onNodeWithTag("date_picker_previous_month")
            .fetchSemanticsNode().boundsInRoot.left
        val nextLeft = composeTestRule.onNodeWithTag("date_picker_next_month")
            .fetchSemanticsNode().boundsInRoot.left

        assertTrue(
            "The year picker must sit left of the previous-month arrow (found " +
                "dropdown=$dropdownLeft previous=$previousLeft)",
            dropdownLeft < previousLeft,
        )
        assertTrue(
            "The previous-month arrow must sit left of the next-month arrow (found " +
                "previous=$previousLeft next=$nextLeft)",
            previousLeft < nextLeft,
        )
    }

    @Test
    fun fullBleedHeaderSpansTheWholeDialogWidth() {
        showPicker(LocalDate.of(2025, 10, 1))

        val header = composeTestRule.onNodeWithTag("date_picker_header").fetchSemanticsNode()
        val dialog = composeTestRule.onNodeWithTag("date_picker_calendar_pager").fetchSemanticsNode()
        // The header may only differ from the dialog content width by the calendar's own insets:
        // it spans the full modal window instead of being inset like padded dialog content.
        val headerWidth = header.boundsInRoot.width
        val contentWidth = dialog.boundsInRoot.width
        assertTrue(
            "The green header must span the whole modal window (header=$headerWidth " +
                "content=$contentWidth)",
            headerWidth > contentWidth,
        )
    }

    @Test
    fun tappingADayReturnsTheSameLocalDateToTheCaller() {
        showPicker(LocalDate.of(2025, 10, 1))

        dayNode(YearMonth.of(2025, 10), 15).performClick()
        composeTestRule.waitForIdle()

        assertEquals(LocalDate.of(2025, 10, 15), selected)
    }

    @Test
    fun nextAndPreviousMonthButtonsMoveTheMonthShownByTheDropdown() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_next_month").performClick()
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2025, 11))

        composeTestRule.onNodeWithTag("date_picker_previous_month").performClick()
        composeTestRule.onNodeWithTag("date_picker_previous_month").performClick()
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2025, 9))
    }

    @Test
    fun nextMonthButtonWrapsFromDecemberIntoTheNextYear() {
        showPicker(LocalDate.of(2024, 11, 20))

        composeTestRule.onNodeWithTag("date_picker_next_month").performClick()
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2024, 12))

        // The December -> January step must roll the year over.
        composeTestRule.onNodeWithTag("date_picker_next_month").performClick()
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2025, 1))
    }

    @Test
    fun dropdownLetsTheUserPickAYearToJumpTo() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_month_year_dropdown").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("date_picker_year_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_year_2022").performClick()
        composeTestRule.waitForIdle()

        assertDropdownShows(YearMonth.of(2022, 10))
        // And the day grid followed the jump: day 20 of October 2022 returns that date.
        dayNode(YearMonth.of(2022, 10), 20).performClick()
        assertEquals(LocalDate.of(2022, 10, 20), selected)
    }

    @Test
    fun dropdownCycleButtonsReachYearsOutsideTheInitialWindow() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_month_year_dropdown").performClick()
        composeTestRule.waitForIdle()
        // The initial window is 2020..2030; cycling back twice exposes 2019.
        composeTestRule.onNodeWithTag("date_picker_year_cycle_previous").performClick()
        composeTestRule.onNodeWithTag("date_picker_year_cycle_previous").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("date_picker_year_2019").performClick()
        composeTestRule.waitForIdle()

        assertDropdownShows(YearMonth.of(2019, 10))
    }

    @Test
    fun scrollingTheCalendarPagesToNeighbouringMonths() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_calendar_pager").performGesture { swipeLeft() }
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2025, 11))

        composeTestRule.onNodeWithTag("date_picker_calendar_pager").performGesture { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()
        assertDropdownShows(YearMonth.of(2025, 10))
    }

    @Test
    fun editButtonOpensManualEntryAndAValidManualDateIsReturned() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_edit_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("manual_date_day").performTextInput("29")
        composeTestRule.onNodeWithTag("manual_date_month").performTextInput("02")
        composeTestRule.onNodeWithTag("manual_date_year").performTextInput("2024")
        composeTestRule.onNodeWithTag("manual_date_confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(LocalDate.of(2024, 2, 29), selected)
    }

    @Test
    fun manualEntryRejectsAnImpossibleDateWithoutReturningIt() {
        showPicker(LocalDate.of(2025, 10, 1))

        composeTestRule.onNodeWithTag("date_picker_edit_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("manual_date_day").performTextInput("31")
        composeTestRule.onNodeWithTag("manual_date_month").performTextInput("02")
        composeTestRule.onNodeWithTag("manual_date_year").performTextInput("2025")
        composeTestRule.onNodeWithTag("manual_date_confirm").performClick()
        composeTestRule.waitForIdle()

        // 31 February must be refused with a visible validation error and no date reported.
        assertNull(selected)
        composeTestRule.onNodeWithTag("manual_date_error").assertIsDisplayed()
    }

}
