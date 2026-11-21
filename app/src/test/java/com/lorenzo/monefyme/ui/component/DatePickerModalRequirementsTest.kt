package com.lorenzo.monefyme.ui.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the rebuilt DatePickerModal used by AddTransactionScreen.
 *
 * The dialog is Compose UI, so like the other screen-structure tests in this repository this
 * test asserts on the source structure: the smaller title, the green centered header with the
 * enlarged selected date and its manual-entry edit button, the manual modal's own green header
 * with its single dd/MM/yyyy input, the month/year dropdown with prev/next month buttons moved
 * to the top, the scrollable (paged) calendar with larger day cells, and that the callers still
 * receive their LocalDate through the unchanged DatePickerModal call.
 *
 * The pure navigation behaviour (month math) is covered behaviourally by DatePickerLogicTest and
 * the dd/MM/yyyy parsing by FormattersTest; the whole flow end-to-end by DatePickerModalUiTest
 * (androidTest).
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

        val headerSection = pickerSource.substring(headerStart, headerStart + 1600)
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
        val manualModal = manualModalSource()
        assertTrue(
            "The manual modal must use one single date input (manual_date_input) instead of the " +
                "old day/month/year fields",
            manualModal.contains("testTag(\"manual_date_input\")") &&
                !pickerSource.contains("manual_date_day") &&
                !pickerSource.contains("manual_date_month") &&
                !pickerSource.contains("manual_date_year"),
        )
        assertTrue(
            "The manual modal must validate the entered text with the app's dd/MM/yyyy parser " +
                "(parseDateOrNull) before confirming",
            manualModal.contains("parseDateOrNull(") &&
                !pickerSource.contains("parseManualDateOrNull"),
        )
        assertTrue(
            "Invalid manual input must show a validation error to the user",
            manualModal.contains("testTag(\"manual_date_error\")"),
        )
        assertTrue(
            "The manual modal must keep its Save/Cancel buttons with the manual_date_confirm tag",
            manualModal.contains("testTag(\"manual_date_confirm\")") &&
                manualModal.contains("Text(\"Save\")") &&
                manualModal.contains("Text(\"Cancel\")"),
        )
    }

    @Test
    fun manualModalShowsAGreenHeaderWithTheDateBeingEdited() {
        // The parent picker hands the date currently selected in it down to the manual modal.
        val callStart = pickerSource.indexOf("ManualDateEntryDialog(")
        assertTrue(
            "The manual modal must receive the date currently selected in the parent picker",
            pickerSource.substring(callStart, callStart + 120).contains("currentDate = selectedDate"),
        )

        val manualModal = manualModalSource()
        val headerStart = manualModal.indexOf("testTag(\"manual_date_header\")")
        assertTrue("The manual modal must have a green header like the main picker", headerStart >= 0)
        val inputStart = manualModal.indexOf("testTag(\"manual_date_input\")")
        assertTrue(
            "The green header must sit at the top of the modal, above the single date input",
            inputStart > headerStart,
        )
        val headerSection = manualModal.substring(headerStart, headerStart + 1200)
        assertTrue(
            "The manual modal header must use the same app green (0xFF74C792) as the main header",
            headerSection.contains("color = dialogGreen"),
        )

        val dateTag = manualModal.indexOf("testTag(\"manual_date_header_date\")")
        assertTrue("The manual modal header must show the date being edited", dateTag >= 0)
        val dateBlock = manualModal.substring((dateTag - 260).coerceAtLeast(0), dateTag + 260)
        assertTrue(
            "The header date must be the date passed down for editing, in the same compact " +
                "weekday-less format (d MMM yyyy) as the main picker header",
            dateBlock.contains("currentDate.format(pickerHeaderFormatter)"),
        )
        val fontSize = Regex("fontSize = (\\d+)\\.sp").find(dateBlock)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue(
            "The header date must be centered and enlarged like the main picker header",
            dateBlock.contains("textAlign = TextAlign.Center") && (fontSize ?: 0) >= 22,
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
            "The year picker must sit on the LEFT with both month arrows to its right on one row: " +
                "[year picker] [previous month] [next month]",
            dropdownIndex < previousIndex && previousIndex < nextIndex,
        )
    }

    // --- Full-bleed green header, tighter corners, compact date format ---

    @Test
    fun headerIsFullBleedWithTightCornersAndCompactDateFormat() {
        // The dialog lays its own content so the green header can span the whole modal window;
        // AlertDialog would inset it with its fixed 24.dp content padding.
        assertTrue(
            "The picker must be a BasicAlertDialog so the header can be full-bleed",
            pickerSource.contains("BasicAlertDialog("),
        )

        val headerTag = pickerSource.indexOf("testTag(\"date_picker_header\")")
        assertTrue("The dialog must have a testable green header", headerTag >= 0)
        val beforeHeader = pickerSource.substring((headerTag - 400).coerceAtLeast(0), headerTag)
        assertTrue(
            "The header Surface must fill the dialog width with no horizontal padding around it " +
                "(full-bleed across the whole modal window)",
            beforeHeader.contains(".fillMaxWidth()") && !beforeHeader.contains("padding(horizontal"),
        )
        assertTrue(
            "Negative padding must not be used to fake the full-bleed header (Compose rejects it at runtime)",
            !Regex("padding\\([^)]*\\(\\s*-").containsMatchIn(pickerSource),
        )

        val headerShape = Regex(
            """testTag\("date_picker_header"\),\s*shape = RoundedCornerShape\((\d+)\.dp\)""",
        ).find(pickerSource)
        assertTrue("The header Surface must keep an explicit tight shape", headerShape != null)
        assertTrue(
            "The header corners must be clearly tighter than the old 12.dp (0-8.dp), " +
                "found ${headerShape?.groupValues?.get(1)}.dp",
            headerShape!!.groupValues[1].toInt() <= 8,
        )

        val dialogShape = Regex("private val dialogShape = RoundedCornerShape\\((\\d+)\\.dp\\)").find(pickerSource)
        assertTrue("The dialog corner radius must be a shared constant", dialogShape != null)
        assertTrue(
            "The dialog corners must be clearly tighter than the old 16.dp (e.g. 8.dp), " +
                "found ${dialogShape?.groupValues?.get(1)}.dp",
            dialogShape!!.groupValues[1].toInt() in 1..8,
        )
        assertTrue(
            "No dialog may keep the old wide 16.dp corners",
            !pickerSource.contains("RoundedCornerShape(16.dp)"),
        )

        // Compact header date: no weekday, standard 3-letter month abbreviation (e.g. 21 Nov 2026).
        assertTrue(
            "The header date format must drop the weekday and abbreviate the month (d MMM yyyy)",
            pickerSource.contains("DateTimeFormatter.ofPattern(\"d MMM yyyy\""),
        )
        assertTrue(
            "The old format with the day of the week and full month name must be gone",
            !pickerSource.contains("EEEE") && !pickerSource.contains("MMMM"),
        )
    }

    // --- Round selected day, no background on the other days ---

    @Test
    fun selectedDayIsAFilledCircleAndUnselectedDaysHaveNoBackground() {
        val dayTagStart = pickerSource.indexOf(".testTag(\"date_picker_day_")
        assertTrue("Each day must be a testable day button", dayTagStart >= 0)
        val buttonStart = pickerSource.lastIndexOf("Button(", dayTagStart)
        val dayButtonBlock = pickerSource.substring(buttonStart, dayTagStart + 700)
        assertTrue(
            "The day highlight must be a full circle, not an elongated rounded rectangle",
            dayButtonBlock.contains("shape = RoundedCornerShape(percent = 50)"),
        )
        assertTrue(
            "The day button must be square so the circle highlight stays round",
            dayButtonBlock.contains(".size(DayCellHeight)"),
        )
        assertTrue(
            "Non-selected days must render with no filled background",
            dayButtonBlock.contains("if (isSelected) dialogGreen else Color.Transparent"),
        )
        assertTrue(
            "The old filled background for non-selected days must be gone",
            !pickerSource.contains("else dayCellBackground"),
        )
        assertTrue(
            "The old 10.dp rounded-rectangle day cells must be gone",
            !pickerSource.contains("RoundedCornerShape(10.dp)"),
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
            "Day buttons must be square (shared day cell height on both axes) so the round " +
                "selected highlight is a perfect circle",
            dayButtonBlock.contains(".size(DayCellHeight)"),
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

    /** The body of the ManualDateEntryDialog composable, from its signature to the end of the file. */
    private fun manualModalSource(): String {
        val start = pickerSource.indexOf("private fun ManualDateEntryDialog(")
        assertTrue("The picker must keep a ManualDateEntryDialog composable", start >= 0)
        return pickerSource.substring(start)
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
