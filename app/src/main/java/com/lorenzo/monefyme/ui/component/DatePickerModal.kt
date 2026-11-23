package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorenzo.monefyme.ui.util.YEAR_MENU_PAST_YEARS
import com.lorenzo.monefyme.ui.util.absoluteMonthIndexOf
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.util.parseDateOrNull
import com.lorenzo.monefyme.ui.util.shiftMonth
import com.lorenzo.monefyme.ui.util.yearMonthOfAbsoluteMonthIndex
import com.lorenzo.monefyme.ui.util.yearOptions
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private const val MONTH_PAGER_HALF_SPAN = 1200 // +/- 100 years of swipeable months around the shown date

private val DayCellHeight = 44.dp
private val DayCellVerticalSpacing = 3.dp
private val CalendarPagerHeight = 330.dp

// Year grid replacing the calendar while the month/year dropdown is open: three years per
// row and as tall as the calendar it replaces, so the modal keeps its size while scrolling.
private val YearCellHeight = 44.dp
private val YearCellVerticalSpacing = 3.dp
private val YearGridHeight = CalendarPagerHeight

private val dialogGreen = Color(0xFF74C792)
private val dialogBackground = Color(0xFFEAF8F2)

// Tighter dialog corners than the old 16.dp; the full-bleed header inherits them from the clip.
private val dialogShape = RoundedCornerShape(8.dp)
private val dayCellBackground = Color(0xFFD9F2E6)
private val onDialogText = Color(0xFF2D3336)

// No weekday and a 3-letter month abbreviation, e.g. "21 Nov 2026".
private val pickerHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

// Weekday column headers, matching the Monday-first grid the calendar pages render.
private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Custom date picker dialog used for transaction dates.
 *
 * Compared to the plain Material picker it shows a smaller "SELECT DATE" title, a green header
 * with the selected date centered and enlarged plus an edit button that opens a manual date entry
 * dialog (one dd/MM/yyyy input, validated), a month/year dropdown to jump (or cycle) to another
 * year, prev/next month buttons next to that dropdown with the calendar itself also scrollable
 * month by month, and larger day cells with more spacing. While the dropdown is open its
 * full-width, scrollable three-column year grid replaces the calendar (see [YearPickerMenu]).
 *
 * Selecting a day only moves the pending selection shown in the header; it is applied
 * (reported through [onDateSelected]) when the OK button is clicked. The current/applied
 * date is kept visible as a black circle outline on top of its day cell. Confirming a valid
 * manual date reports the chosen [LocalDate] through [onDateSelected] immediately.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(currentDate) }
    var showYearMenu by rememberSaveable { mutableStateOf(false) }
    var showManualEntry by rememberSaveable { mutableStateOf(false) }

    // The calendar pager is centered on the initially shown month; page N is the month
    // initialAbsoluteMonth + (N - MONTH_PAGER_HALF_SPAN), so swiping pages navigates months.
    val initialAbsoluteMonth = remember { absoluteMonthIndexOf(currentDate) }
    val pagerState = rememberPagerState(
        initialPage = MONTH_PAGER_HALF_SPAN,
        pageCount = { MONTH_PAGER_HALF_SPAN * 2 + 1 },
    )
    fun absoluteMonthOfPage(page: Int): Int = initialAbsoluteMonth + (page - MONTH_PAGER_HALF_SPAN)
    fun pageOfAbsoluteMonth(absoluteMonth: Int): Int =
        (MONTH_PAGER_HALF_SPAN + (absoluteMonth - initialAbsoluteMonth))
            .coerceIn(0, MONTH_PAGER_HALF_SPAN * 2)

    val visibleMonth = yearMonthOfAbsoluteMonthIndex(absoluteMonthOfPage(pagerState.currentPage))

    fun jumpToMonth(month: YearMonth) {
        scope.launch {
            pagerState.animateScrollToPage(pageOfAbsoluteMonth(absoluteMonthIndexOf(month)))
        }
    }

    // BasicAlertDialog instead of AlertDialog: the Material dialog hard-codes 24.dp of inner
    // content padding, which would inset the green header from the dialog edges. Here the
    // content lays itself out so the header can span the whole modal window.
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = dialogShape,
            color = dialogBackground,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Green header spanning the whole modal window: no side insets around it, the
                // dialog's rounded corners clip its top edge. Selected date big and centered,
                // with the manual-entry edit button on the right.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_picker_header"),
                    shape = RoundedCornerShape(0.dp),
                    color = dialogGreen,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 6.dp, top = 14.dp, bottom = 6.dp),
                    ) {
                        // Smaller title than a regular dialog heading, at the header's top-left.
                        Text(
                            text = "SELECT DATE",
                            modifier = Modifier.testTag("date_picker_title"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedDate.format(pickerHeaderFormatter),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("date_picker_selected_date"),
                                color = Color.White,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Start,
                            )
                            IconButton(
                                onClick = { showManualEntry = true },
                                modifier = Modifier
                                    .testTag("date_picker_edit_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Enter date manually",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }

                // Year picker on the left with both month arrows to its right on one row:
                // [year picker] [previous month] [next month].
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 20.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = { showYearMenu = !showYearMenu },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("date_picker_month_year_dropdown"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = dayCellBackground,
                                contentColor = onDialogText,
                            ),
                        ) {
                            Text(
                                text = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                                    " " + visibleMonth.year,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = null)
                        }
                    }

                    IconButton(
                        onClick = { jumpToMonth(shiftMonth(visibleMonth, -1)) },
                        modifier = Modifier.testTag("date_picker_previous_month"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = onDialogText,
                        )
                    }

                    IconButton(
                        onClick = { jumpToMonth(shiftMonth(visibleMonth, 1)) },
                        modifier = Modifier.testTag("date_picker_next_month"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Next month",
                            tint = onDialogText,
                        )
                    }
                }

                // While the month/year dropdown is open its full-modal-width year grid takes the
                // calendar's place; otherwise the paged calendar is shown and scrolling it pages
                // to the previous/next month.
                if (showYearMenu) {
                    YearPickerMenu(
                        currentYear = visibleMonth.year,
                        onYearSelected = { year ->
                            showYearMenu = false
                            jumpToMonth(YearMonth.of(year, visibleMonth.monthValue))
                        },
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CalendarPagerHeight)
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                            .testTag("date_picker_calendar_pager"),
                    ) { page ->
                        CalendarMonthPage(
                            month = yearMonthOfAbsoluteMonthIndex(absoluteMonthOfPage(page)),
                            selectedDate = selectedDate,
                            currentDate = currentDate,
                            today = today,
                            onDaySelected = { day ->
                                selectedDate = yearMonthOfAbsoluteMonthIndex(absoluteMonthOfPage(page)).atDay(day)
                            },
                        )
                    }
                }

                // Cancel + OK at the bottom right: Cancel dismisses without applying, OK confirms.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogGreen),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onDateSelected(selectedDate) },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("date_picker_ok"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = dialogGreen),
                    ) {
                        Text(
                            text = "OK",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (showManualEntry) {
        ManualDateEntryDialog(
            currentDate = selectedDate,
            onDismiss = { showManualEntry = false },
            onConfirm = { date ->
                showManualEntry = false
                selectedDate = date
                scope.launch {
                    pagerState.scrollToPage(pageOfAbsoluteMonth(absoluteMonthIndexOf(date)))
                }
                onDateSelected(date)
            },
        )
    }
}

/**
 * Year chooser shown while the month/year dropdown is open: a vertically scrollable grid of
 * many years around [currentYear] that spans the whole modal width (it replaces the calendar).
 * The applied year is highlighted in the app green and scrolled into view; tapping a year
 * reports it through [onYearSelected], which jumps the calendar to that year.
 */
@Composable
private fun YearPickerMenu(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
) {
    val years = remember(currentYear) { yearOptions(currentYear) }
    // Open the grid with the applied year at its top row: the applied year sits exactly
    // YEAR_MENU_PAST_YEARS items into the (three-column) ascending list.
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = YEAR_MENU_PAST_YEARS)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .height(YearGridHeight)
            .background(dialogBackground)
            .testTag("date_picker_year_menu"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(YearCellVerticalSpacing),
    ) {
        items(years) { year ->
            val isSelected = year == currentYear
            TextButton(
                onClick = { onYearSelected(year) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(YearCellHeight)
                    .padding(horizontal = 6.dp)
                    .testTag("date_picker_year_$year"),
                // Applied year: a filled green pill. All others: plain legible text.
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) dialogGreen else Color.Transparent,
                    contentColor = if (isSelected) Color.White else onDialogText,
                ),
            ) {
                Text(year.toString(), fontSize = 16.sp)
            }
        }
    }
}

/** One calendar page: weekday headers plus a month grid of roomy, evenly spaced day buttons. */
@Composable
private fun CalendarMonthPage(
    month: YearMonth,
    selectedDate: LocalDate,
    currentDate: LocalDate,
    today: LocalDate,
    onDaySelected: (Int) -> Unit,
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1 // Monday-first grid
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).toList()
    val rows = cells.chunked(7).map { row -> row + List(7 - row.size) { null } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5FA37F),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = DayCellVerticalSpacing),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val date = month.atDay(day)
                            val isSelected = date == selectedDate
                            val isCurrent = date == currentDate
                            val isToday = date == today
                            Box(contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = { onDaySelected(day) },
                                    modifier = Modifier
                                        .size(DayCellHeight)
                                        .testTag("date_picker_day_${month.year}_${month.monthValue}_$day"),
                                    // Selected day: a filled green circle. All others: no fill at all.
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) dialogGreen else Color.Transparent,
                                        contentColor = onDialogText,
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isSelected) Color.White else onDialogText,
                                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp,
                                    )
                                }
                                // The applied date stays marked with a black circle outline on top of its cell.
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(DayCellHeight)
                                            .border(2.dp, Color.Black, RoundedCornerShape(percent = 50))
                                            .testTag("date_picker_current_date_outline"),
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth().height(DayCellHeight))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal to type the exact transaction date into one dd/MM/yyyy field.
 *
 * It mirrors the main picker: a green header showing the date currently selected in the parent
 * picker (same compact "21 Nov 2026" format, centered and enlarged), a single date input
 * pre-filled with that date, and Save/Cancel buttons. A valid entry is parsed with the app's
 * [parseDateOrNull] and reported through [onConfirm]; an invalid one shows a validation error
 * and keeps the modal open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualDateEntryDialog(
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var dateText by rememberSaveable { mutableStateOf(formatDate(currentDate)) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // BasicAlertDialog like the main picker so the green header can span the whole modal window.
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = dialogShape,
            color = dialogBackground,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Green header in the main picker's style, showing the date being edited.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_date_header"),
                    shape = RoundedCornerShape(0.dp),
                    color = dialogGreen,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
                    ) {
                        Text(
                            text = "SELECT DATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = currentDate.format(pickerHeaderFormatter),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_date_header_date"),
                                color = Color.White,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Start,
                            )
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Ok button",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // One single dd/MM/yyyy input, pre-filled with the date being edited.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5F6768),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = dateText,
                        onValueChange = { raw ->
                            dateText = raw.filter { it.isDigit() || it == '/' }.take(10)
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_date_input"),
                        singleLine = true,
                        placeholder = { Text("dd/MM/yyyy", color = Color(0xFF95A2A3)) },
                        // Phone-style keypad: like a number pad but it also offers the "/" separator.
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = dayCellBackground,
                            unfocusedContainerColor = dayCellBackground,
                            focusedTextColor = onDialogText,
                            unfocusedTextColor = onDialogText,
                        ),
                    )
                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                            modifier = Modifier.testTag("manual_date_error"),
                        )
                    }
                }

                // Save/Cancel at the bottom right, green like the main picker's Cancel.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("manual_date_cancel"),
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogGreen),
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val parsed = parseDateOrNull(dateText)
                            if (parsed != null) {
                                onConfirm(parsed)
                            } else {
                                errorMessage = "Not a valid date. Use the dd/MM/yyyy format, e.g. 21/11/2026."
                            }
                        },
                        modifier = Modifier.testTag("manual_date_confirm"),
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogGreen),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
