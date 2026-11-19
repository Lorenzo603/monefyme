package com.lorenzo.monefyme.ui.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the rebuilt DatePickerModal used by AddTransactionScreen.
 *
 * The dialog is Compose UI, so like the other screen-structure tests in this repository this
 * test asserts on the source structure: the smaller title, the green centered header with the
 * enlarged selected date and its manual-entry edit button, the month/year dropdown with prev/next
 * month buttons moved to the top, the scrollable (paged) calendar with larger day cells, and that
 * the callers still receive their LocalDate through the unchanged DatePickerModal call.
 *
 * The pure navigation/parsing behaviour (month math, manual entry validation) is covered
 * behaviourally by DatePickerLogicTest and end-to-end by DatePickerModalUiTest (androidTest).
 */
class DatePickerModalRequirementsTest {

    private val pickerSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt")
    }

    private val screenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt")
    }

    // --- Smaller "Select date" title ---

    @Test
    fun selectDateTitleIsSmallerThanALargeDialogTitle() {
        val titleStart = pickerSource.indexOf("text = \"Select date\"")
        assertTrue("The dialog must keep the \"Select date\" title", titleStart >= 0)
        val titleBlock = pickerSource.substring(titleStart, titleStart + 220)
        assertTrue(
            "The \"Select date\" title should use the smaller titleMedium style",
            titleBlock.contains("titleMedium"),
        )
        assertTrue(
            "The picker must not use the previous big titleLarge style anywhere",
            !pickerSource.contains("titleLarge"),
        )
    }

    // --- Green header with the enlarged, centered selected date and edit button ---

    @Test
    fun headerIsGreenAndShowsTheSelectedDateEnlargedAndCentered() {
        val headerStart = pickerSource.indexOf("testTag(\"date_picker_header\")")
        assertTrue("The dialog must have a testable green header", headerStart >= 0)

        val headerSection = pickerSource.substring(headerStart, headerStart + 1200)
        assertTrue(
            "The header Surface must be filled with the app green (0xFF74C792)",
            pickerSource.contains("color = dialogGreen") && pickerSource.contains("dialogGreen = Color(0xFF74C792)"),
        )
        assertTrue(
            "The selected date (date_picker_selected_date) must be centered in the header",
            headerSection.contains("testTag(\"date_picker_selected_date\")") &&
                headerSection.contains("textAlign = TextAlign.Center"),
        )

        // "Bigger" than the title/body text: the header font size must be at least 22.sp.
        val dateBlock = headerSection.substring(
            headerSection.indexOf("testTag(\"date_picker_selected_date\")"),
        ).lineSequence().take(8).joinToString("\n")
        val fontSizeMatch = Regex("fontSize = (\\d+)\\.sp").find(dateBlock)
        assertTrue("The selected date Text must set an enlarged fontSize", fontSizeMatch != null)
        assertTrue(
            "The selected date must be displayed bigger (>= 22.sp), found ${fontSizeMatch?.groupValues?.get(1)}.sp",
            fontSizeMatch!!.groupValues[1].toInt() >= 22,
        )
    }

    @Test
    fun headerEditButtonOpensTheValidatedManualDateModal() {
        assertTrue(
            "The header must expose an edit button (date_picker_edit_button)",
            pickerSource.contains("testTag(\"date_picker_edit_button\")"),
        )
        assertTrue(
            "The edit button must open the manual date entry modal",
            pickerSource.contains("showManualEntry = true") && pickerSource.contains("ManualDateEntryDialog("),
        )
        assertTrue(
            "The manual modal must have day/month/year input fields",
            pickerSource.contains("manual_date_day") &&
                pickerSource.contains("manual_date_month") &&
                pickerSource.contains("manual_date_year"),
        )
        assertTrue(
            "The manual modal must validate the entered date with parseManualDateOrNull before confirming",
            pickerSource.contains("parseManualDateOrNull(dayText, monthText, yearText)"),
        )
        assertTrue(
            "Invalid manual input must show a validation error to the user",
            pickerSource.contains("manual_date_error"),
        )
    }

    // --- Month/year dropdown, prev/next buttons on top, scrollable calendar ---

    @Test
    fun monthYearDropdownLetsYouPickOrCycleYears() {
        assertTrue(
            "There must be a month/year dropdown box (date_picker_month_year_dropdown)",
            pickerSource.contains("testTag(\"date_picker_month_year_dropdown\")"),
        )
        assertTrue(
            "The dropdown must open a year menu with selectable and cycleable years",
            pickerSource.contains("DropdownMenu(") &&
                pickerSource.contains("YearPickerMenu(") &&
                pickerSource.contains("yearOptions(") &&
                pickerSource.contains("date_picker_year_cycle_previous") &&
                pickerSource.contains("date_picker_year_cycle_next"),
        )
        assertTrue(
            "Picking a year must jump the calendar to that year",
            pickerSource.contains("jumpToMonth(YearMonth.of(year, visibleMonth.monthValue))"),
        )
    }

    @Test
    fun monthNavigationUsesTheTestedLogicHelpers() {
        assertTrue(
            "Prev/next month buttons must use the tested shiftMonth math",
            pickerSource.contains("shiftMonth(visibleMonth, -1)") &&
                pickerSource.contains("shiftMonth(visibleMonth, 1)"),
        )
        assertTrue(
            "Calendar pages must be derived with absoluteMonthIndexOf/yearMonthOfAbsoluteMonthIndex",
            pickerSource.contains("absoluteMonthIndexOf(") &&
                pickerSource.contains("yearMonthOfAbsoluteMonthIndex("),
        )
    }

    @Test
    fun calendarScrollsMonthsAndPrevNextButtonsAreOnTopNextToTheDropdown() {
        val pagerIndex = pickerSource.indexOf("testTag(\"date_picker_calendar_pager\")")
        assertTrue(
            "The calendar must be a swipeable HorizontalPager of months",
            pickerSource.contains("HorizontalPager(") &&
                pickerSource.contains("rememberPagerState(") &&
                pagerIndex >= 0,
        )

        val previousIndex = pickerSource.indexOf("testTag(\"date_picker_previous_month\")")
        val nextIndex = pickerSource.indexOf("testTag(\"date_picker_next_month\")")
        val dropdownIndex = pickerSource.indexOf("testTag(\"date_picker_month_year_dropdown\")")
        assertTrue(
            "The previous/next month buttons and the month/year dropdown must appear before the " +
                "calendar pager, i.e. moved to the top next to the dropdown",
            previousIndex in 0 until pagerIndex &&
                nextIndex in 0 until pagerIndex &&
                dropdownIndex in 0 until pagerIndex,
        )
        assertTrue(
            "The previous button must sit left of the dropdown and the next button right of it",
            previousIndex < dropdownIndex && nextIndex > dropdownIndex,
        )
    }

    @Test
    fun dayCellsAreLargerAndMoreSpacedThanTheOld40dpGrid() {
        val heightMatch = Regex("private val DayCellHeight = (\\d+)\\.dp").find(pickerSource)
        assertTrue("Day cells must have a shared height constant", heightMatch != null)
        assertTrue(
            "Day buttons need a larger hit target (>= 44.dp tall, was 40.dp), found ${heightMatch?.groupValues?.get(1)}.dp",
            heightMatch!!.groupValues[1].toInt() >= 44,
        )

        val spacingMatch = Regex("private val DayCellVerticalSpacing = (\\d+)\\.dp").find(pickerSource)
        assertTrue("Day cells must be spaced vertically", spacingMatch != null)
        assertTrue(
            "Day rows need more vertical spacing (>= 3.dp padding on each side), found ${spacingMatch?.groupValues?.get(1)}.dp",
            spacingMatch!!.groupValues[1].toInt() >= 3,
        )
        val dayButtonStart = pickerSource.indexOf(".testTag(\"date_picker_day_")
        assertTrue("Each day must be a testable day button", dayButtonStart >= 0)
        val dayButtonBlock = pickerSource.substring(
            pickerSource.lastIndexOf("Button(", dayButtonStart),
            dayButtonStart,
        )
        assertTrue(
            "Day buttons must fill the full grid column width and the shared day cell height",
            dayButtonBlock.contains(".fillMaxWidth()") && dayButtonBlock.contains(".height(DayCellHeight)"),
        )
    }

    // --- Callers keep working with the same LocalDate result ---

    @Test
    fun addTransactionScreenStillUsesTheSameDatePickerModalContract() {
        assertTrue(
            "AddTransactionScreen must import the shared DatePickerModal component",
            screenSource.contains("import com.lorenzo.monefyme.ui.component.DatePickerModal"),
        )
        val callStart = screenSource.indexOf("DatePickerModal(")
        assertTrue("AddTransactionScreen must keep showing the date picker", callStart >= 0)
        val call = screenSource.substring(callStart, callStart + 400)
        assertTrue(
            "The picker must keep receiving the current date and returning the selected LocalDate",
            call.contains("currentDate = todayToUse") &&
                call.contains("onDateSelected = { newDate ->") &&
                call.contains("todayToUse = newDate") &&
                call.contains("onDismiss = { showDatePicker = false }"),
        )
        assertTrue(
            "The old private dialog copy must be gone from the screen file",
            !screenSource.contains("private fun DatePickerModal"),
        )
    }

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        return file.readText().replace("\r\n", "\n")
    }

    private fun findProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
        var dir: File? = start
        while (dir != null) {
            val current: File = dir
            if (File(current, "settings.gradle.kts").exists() && File(current, "app").isDirectory) {
                return current
            }
            dir = current.parentFile
        }
        throw IllegalStateException("Could not locate the project root starting from $start")
    }
}
