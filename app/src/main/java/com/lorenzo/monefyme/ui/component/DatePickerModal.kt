package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.lorenzo.monefyme.ui.util.absoluteMonthIndexOf
import com.lorenzo.monefyme.ui.util.parseManualDateOrNull
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

private val dialogGreen = Color(0xFF74C792)
private val dialogBackground = Color(0xFFEAF8F2)
private val dayCellBackground = Color(0xFFD9F2E6)
private val onDialogText = Color(0xFF2D3336)

private val pickerHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

// Weekday column headers, matching the Monday-first grid the calendar pages render.
private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Custom date picker dialog used for transaction dates.
 *
 * Compared to the plain Material picker it shows a smaller "Select date" title, a green header
 * with the selected date centered and enlarged plus an edit button that opens a manual
 * day/month/year entry dialog (validated), a month/year dropdown to jump (or cycle) to another
 * year, prev/next month buttons next to that dropdown with the calendar itself also scrollable
 * month by month, and larger day cells with more spacing.
 *
 * Selecting a day, or confirming a valid manual date, reports the chosen [LocalDate] through
 * [onDateSelected] exactly like the previous dialog did.
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // Smaller title than a regular dialog heading.
            Text(
                text = "Select date",
                modifier = Modifier.testTag("date_picker_title"),
                style = MaterialTheme.typography.titleMedium,
                color = onDialogText,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Green header: selected date big and centered, with the manual-entry edit button.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_picker_header"),
                    shape = RoundedCornerShape(12.dp),
                    color = dialogGreen,
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = selectedDate.format(pickerHeaderFormatter),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 52.dp, vertical = 12.dp)
                                .testTag("date_picker_selected_date"),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = { showManualEntry = true },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .testTag("date_picker_edit_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Enter date manually",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Month/year dropdown with the prev/next month buttons moved to its sides.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = { showYearMenu = true },
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
                        DropdownMenu(
                            expanded = showYearMenu,
                            onDismissRequest = { showYearMenu = false },
                            modifier = Modifier
                                .testTag("date_picker_year_menu")
                                .background(dialogBackground),
                        ) {
                            YearPickerMenu(
                                currentYear = visibleMonth.year,
                                onYearSelected = { year ->
                                    showYearMenu = false
                                    jumpToMonth(YearMonth.of(year, visibleMonth.monthValue))
                                },
                            )
                        }
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

                // Scrolling the calendar itself pages to the previous/next month.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CalendarPagerHeight)
                        .testTag("date_picker_calendar_pager"),
                ) { page ->
                    CalendarMonthPage(
                        month = yearMonthOfAbsoluteMonthIndex(absoluteMonthOfPage(page)),
                        selectedDate = selectedDate,
                        today = today,
                        onDaySelected = { day ->
                            val date = yearMonthOfAbsoluteMonthIndex(absoluteMonthOfPage(page)).atDay(day)
                            selectedDate = date
                            onDateSelected(date)
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = dialogGreen),
            ) {
                Text("Cancel")
            }
        },
        containerColor = dialogBackground,
        shape = RoundedCornerShape(16.dp),
    )

    if (showManualEntry) {
        ManualDateEntryDialog(
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
 * Year chooser shown inside the month/year dropdown: a horizontally scrollable list of years
 * around [currentYear] with cycle buttons that slide the window through the years.
 */
@Composable
private fun YearPickerMenu(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
) {
    var centerYear by remember { mutableIntStateOf(currentYear) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { centerYear -= 1 },
            modifier = Modifier.testTag("date_picker_year_cycle_previous"),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Earlier years",
                tint = onDialogText,
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            yearOptions(centerYear).forEach { year ->
                val isSelected = year == currentYear
                TextButton(
                    onClick = { onYearSelected(year) },
                    modifier = Modifier.testTag("date_picker_year_$year"),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) dialogGreen else Color.Transparent,
                        contentColor = if (isSelected) Color.White else onDialogText,
                    ),
                ) {
                    Text(year.toString())
                }
            }
        }
        IconButton(
            onClick = { centerYear += 1 },
            modifier = Modifier.testTag("date_picker_year_cycle_next"),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Later years",
                tint = onDialogText,
            )
        }
    }
}

/** One calendar page: weekday headers plus a month grid of roomy, evenly spaced day buttons. */
@Composable
private fun CalendarMonthPage(
    month: YearMonth,
    selectedDate: LocalDate,
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
                            val isToday = date == today
                            Button(
                                onClick = { onDaySelected(day) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(DayCellHeight)
                                    .testTag("date_picker_day_${month.year}_${month.monthValue}_$day"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) dialogGreen else dayCellBackground,
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
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth().height(DayCellHeight))
                        }
                    }
                }
            }
        }
    }
}

/** Modal to type the exact transaction date into separate day/month/year fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualDateEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var dayText by rememberSaveable { mutableStateOf("") }
    var monthText by rememberSaveable { mutableStateOf("") }
    var yearText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter date",
                style = MaterialTheme.typography.titleMedium,
                color = onDialogText,
            )
        },
        text = {
            Column {
                Text(
                    text = "Type the exact day, month and year.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F6768),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ManualDateField(
                        label = "Day",
                        placeholder = "dd",
                        value = dayText,
                        maxLength = 2,
                        testTag = "manual_date_day",
                        onValueChange = { dayText = it },
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.titleMedium,
                        color = onDialogText,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    ManualDateField(
                        label = "Month",
                        placeholder = "mm",
                        value = monthText,
                        maxLength = 2,
                        testTag = "manual_date_month",
                        onValueChange = { monthText = it },
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.titleMedium,
                        color = onDialogText,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    ManualDateField(
                        label = "Year",
                        placeholder = "yyyy",
                        value = yearText,
                        maxLength = 4,
                        testTag = "manual_date_year",
                        onValueChange = { yearText = it },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        modifier = Modifier.testTag("manual_date_error"),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = parseManualDateOrNull(dayText, monthText, yearText)
                    if (parsed != null) {
                        onConfirm(parsed)
                    } else {
                        errorMessage = "Not a valid date. Use day 1-31, month 1-12 and a real year."
                    }
                },
                modifier = Modifier.testTag("manual_date_confirm"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = dialogBackground,
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun ManualDateField(
    label: String,
    placeholder: String,
    value: String,
    maxLength: Int,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = onDialogText,
        )
        TextField(
            value = value,
            onValueChange = { raw -> onValueChange(raw.filter(Char::isDigit).take(maxLength)) },
            modifier = Modifier
                .width(if (maxLength == 4) 88.dp else 72.dp)
                .testTag(testTag),
            singleLine = true,
            placeholder = { Text(placeholder, color = Color(0xFF95A2A3)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = dayCellBackground,
                unfocusedContainerColor = dayCellBackground,
                focusedTextColor = onDialogText,
                unfocusedTextColor = onDialogText,
            ),
        )
    }
}
