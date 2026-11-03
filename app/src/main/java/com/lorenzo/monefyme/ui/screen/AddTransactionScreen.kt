package com.lorenzo.monefyme.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.category.DefaultCategories
import com.lorenzo.monefyme.ui.category.resolveColor
import com.lorenzo.monefyme.ui.category.resolveIcon
import com.lorenzo.monefyme.ui.component.AddCategoryDialog
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val headerDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    type: TransactionType,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val pickerCategories by viewModel.pickerCategories.collectAsStateWithLifecycle()
    var amountText by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var firstOperand by rememberSaveable { mutableStateOf<BigDecimal?>(null) }
    var pendingOperator by rememberSaveable { mutableStateOf<Char?>(null) }
    var startNewNumber by rememberSaveable { mutableStateOf(true) }
    var amountFlashError by remember { mutableStateOf(false) }
    val amountDisplayColor by animateColorAsState(
        targetValue = if (amountFlashError) Color(0xFFE53935) else Color.White,
        animationSpec = tween(durationMillis = 100),
        label = "amountFlash",
    )
    LaunchedEffect(amountFlashError) {
        if (amountFlashError) {
            delay(300L)
            amountFlashError = false
        }
    }
    val screenTitle = if (type == TransactionType.EXPENSE) "New expense" else "New income"
    var todayToUse by rememberSaveable { mutableStateOf(today) }

    val persistTransaction: (String) -> Unit = { category ->
        scope.launch {
            val normalizedAmount = amountText.removeSuffix(".").removePrefix("-")
            val success = viewModel.saveTransaction(
                transactionId = null,
                dateText = formatDate(todayToUse),
                amountText = normalizedAmount,
                type = type,
                categoryName = category,
                description = note.trim(),
                account = "Cash",
            )
            if (success) {
                onBack()
            }
        }
    }

    val persistCategory: suspend (String, String, Int) -> Boolean = { displayName, iconName, colorArgb ->
        val success = viewModel.saveCategory(displayName, iconName, colorArgb)
        if (success) showAddCategoryDialog = false
        success
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF8F2)),
    ) {
        TopAppBar(
            title = { Text(screenTitle) },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (showCategoryPicker) {
                            showCategoryPicker = false
                        } else {
                            onBack()
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showCategoryPicker = !showCategoryPicker }) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "Toggle category")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF74C792),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    showDatePicker = true
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = "Select date",
                tint = Color(0xFF5F6768),
            )
            Text(
                text = todayToUse.format(headerDateFormatter),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF2D3336),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 26.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF79C896),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.padding(end = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalAtm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp),
                    )
                    Text(
                        text = "EUR",
                        color = Color(0xFF2F5E4B),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .height(74.dp)
                        .padding(end = 14.dp),
                    color = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    text = currencyAnnotatedString(text = amountText, baseFontSize = 58.sp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("amount_input"),
                    color = amountDisplayColor,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = {
                        amountText = when {
                            amountText.length <= 1 -> "0"
                            else -> amountText.dropLast(1)
                        }
                        startNewNumber = amountText == "0"
                    },
                ) {
                    Text(
                        text = "⌫",
                        color = Color.White,
                        fontSize = 28.sp,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = Color(0xFF7DC8A5))
            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Note", color = Color(0xFF95A2A3)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFA4B0B1),
                    unfocusedIndicatorColor = Color(0xFFA4B0B1),
                    focusedTextColor = Color(0xFF2D3336),
                    unfocusedTextColor = Color(0xFF2D3336),
                ),
            )
        }

        if (showCategoryPicker) {
            CategoryPicker(
                categories = pickerCategories,
                onCategorySelected = persistTransaction,
                onAddCategory = { showAddCategoryDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
            )
        } else {
            CalculatorPad(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp),
                onKeyPress = { key ->
                    val current = amountText
                    when (key) {
                        in '0'..'9' -> {
                            amountText = if (startNewNumber || current == "0") {
                                key.toString()
                            } else {
                                current + key
                            }
                            startNewNumber = false
                        }

                        '.' -> {
                            if (startNewNumber) {
                                amountText = "0."
                                startNewNumber = false
                            } else if (!current.contains('.')) {
                                amountText = "$current."
                            }
                        }

                        '+', '-', '*', '/' -> {
                            val operand = current.toBigDecimalOrNull()
                            if (operand != null) {
                                firstOperand = if (firstOperand != null && pendingOperator != null && !startNewNumber) {
                                    applyOperator(firstOperand!!, operand, pendingOperator!!)
                                } else {
                                    operand
                                }
                                pendingOperator = key
                                startNewNumber = true
                                amountText = firstOperand?.stripTrailingZeros()?.toPlainString() ?: amountText
                            }
                        }

                        '=' -> {
                            val operand = current.toBigDecimalOrNull()
                            if (operand != null && firstOperand != null && pendingOperator != null) {
                                val result = applyOperator(firstOperand!!, operand, pendingOperator!!)
                                amountText = result.stripTrailingZeros().toPlainString()
                                firstOperand = null
                                pendingOperator = null
                                startNewNumber = true
                            }
                        }
                    }
                },
                onChooseCategory = {
                    val normalizedAmount = amountText.removeSuffix(".").removePrefix("-")
                    val amountValue = normalizedAmount.toBigDecimalOrNull()
                    if (amountValue == null || amountValue.compareTo(BigDecimal.ZERO) == 0) {
                        amountFlashError = true
                    } else {
                        showCategoryPicker = true
                    }
                },
            )
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onSave = persistCategory,
            )
        }

        if (showDatePicker) {
            DatePickerModal(
                currentDate = todayToUse,
                onDateSelected = { newDate ->
                    todayToUse = newDate
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
            )
        }
    }
}

@Composable
private fun CalculatorPad(
    modifier: Modifier = Modifier,
    onKeyPress: (Char) -> Unit,
    onChooseCategory: () -> Unit,
) {
    val keyRows = listOf(
        listOf('1', '2', '3', '+'),
        listOf('4', '5', '6', '-'),
        listOf('7', '8', '9', '*'),
        listOf('.', '0', '=', '/'),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onKeyPress(key) },
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF74C792)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFEAF8F2),
                            contentColor = Color(0xFF4B5558),
                        ),
                    ) {
                        Text(
                            text = if (key == '*') "×" else if (key == '/') "÷" else key.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onChooseCategory,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 8.dp)
                .height(74.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, Color(0xFF74C792)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFEAF8F2),
                contentColor = Color(0xFF4B5558),
            ),
        ) {
            Text(
                text = "CHOOSE CATEGORY",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<CategoryItem>,
    onCategorySelected: (String) -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.displayName }) { category ->
            val categoryColor = category.resolveColor()
            OutlinedButton(
                onClick = { onCategorySelected(category.displayName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFEAF8F2),
                    contentColor = categoryColor,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        category.resolveIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = category.displayName,
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = categoryColor,
                    )
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onAddCategory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFEAF8F2),
                    contentColor = DefaultCategories.fallbackColor,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "ADD",
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = DefaultCategories.fallbackColor,
                    )
                }
            }
        }
    }
}

private fun applyOperator(left: BigDecimal, right: BigDecimal, op: Char): BigDecimal {
    return when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right.compareTo(BigDecimal.ZERO) == 0) left else left.divide(right, 2, java.math.RoundingMode.HALF_UP)
        else -> right
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedYear by rememberSaveable { mutableIntStateOf(currentDate.year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(currentDate.monthValue - 1) }
    var selectedDay by rememberSaveable { mutableIntStateOf(currentDate.dayOfMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select date",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF2D3336),
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = YearMonth
                            .of(selectedYear, selectedMonth + 1)
                            .month
                            .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2D3336),
                    )
                    Text(
                        text = "  ",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2D3336),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day of week headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7DC8A5),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar grid
                val daysInMonth = YearMonth.of(selectedYear, selectedMonth + 1).lengthOfMonth()
                val firstDayOfWeek = YearMonth.of(selectedYear, selectedMonth + 1).atDay(1).dayOfWeek.value
                val adjustedFirstDay = (firstDayOfWeek + 6) % 7
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Empty cells for days before the first day of the month
                    items(adjustedFirstDay) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                        )
                    }

                    // Days of the month
                    items((1..daysInMonth).toList()) { day ->
                        val isSelected = day == selectedDay
                        val isToday = day == LocalDate.now().dayOfMonth &&
                            selectedMonth == LocalDate.now().monthValue - 1 &&
                            selectedYear == LocalDate.now().year

                        OutlinedButton(
                            onClick = {
                                selectedDay = day
                                onDateSelected(
                                    LocalDate.of(selectedYear, selectedMonth + 1, day),
                                )
                            },
                            modifier = Modifier
                                .size(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) Color(0xFF74C792) else Color(0xFFEAF8F2),
                                contentColor = if (isSelected) Color.White else Color(0xFF2D3336),
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Gray,
                            ),
                            enabled = true,
                        ) {
                            Text(
                                text = day.toString(),
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedButton(
                        onClick = {
                            val newYearMonth = YearMonth.of(selectedYear, selectedMonth + 1).minusMonths(1)
                            selectedYear = newYearMonth.year
                            selectedMonth = newYearMonth.monthValue - 1
                            selectedDay = selectedDay.coerceAtMost(newYearMonth.lengthOfMonth())
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("<")
                    }

                    OutlinedButton(
                        onClick = {
                            val newYearMonth = YearMonth.of(selectedYear, selectedMonth + 1).plusMonths(1)
                            selectedYear = newYearMonth.year
                            selectedMonth = newYearMonth.monthValue - 1
                            selectedDay = selectedDay.coerceAtMost(newYearMonth.lengthOfMonth())
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(">")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF74C792),
                ),
            ) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFFEAF8F2),
        shape = RoundedCornerShape(16.dp),
    )
}
