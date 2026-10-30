package com.lorenzo.monefyme.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    var amountText by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var firstOperand by rememberSaveable { mutableStateOf<BigDecimal?>(null) }
    var pendingOperator by rememberSaveable { mutableStateOf<Char?>(null) }
    var startNewNumber by rememberSaveable { mutableStateOf(true) }
    val screenTitle = if (type == TransactionType.EXPENSE) "New expense" else "New income"

    val persistTransaction: (String) -> Unit = { category ->
        scope.launch {
            val normalizedAmount = amountText.removeSuffix(".").removePrefix("-")
            val success = viewModel.saveTransaction(
                transactionId = null,
                dateText = formatDate(today),
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
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF5F6768),
            )
            Text(
                text = today.format(headerDateFormatter),
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
                Divider(
                    modifier = Modifier
                        .height(74.dp)
                        .padding(end = 14.dp),
                    color = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    text = amountText,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Normal,
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
                onCategorySelected = persistTransaction,
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
                onChooseCategory = { showCategoryPicker = true },
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
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = remember {
        listOf(
            PredefinedCategory("Bills", Icons.Outlined.LocalActivity, Color(0xFFC49B00)),
            PredefinedCategory("Breakfast out", Icons.Outlined.LocalBar, Color(0xFF99631E)),
            PredefinedCategory("Caffe offerti", Icons.Outlined.BakeryDining, Color(0xFFC49B00)),
            PredefinedCategory("Car", Icons.Outlined.DirectionsCar, Color(0xFF7788A3)),
            PredefinedCategory("Clothes", Icons.Outlined.Checkroom, Color(0xFFA665CA)),
            PredefinedCategory("Communications", Icons.Outlined.Call, Color(0xFFAFA0BE)),
            PredefinedCategory("Eating out", Icons.Outlined.LocalDining, Color(0xFF7EB7A4)),
            PredefinedCategory("Entertainment", Icons.Outlined.LocalBar, Color(0xFFC28B47)),
            PredefinedCategory("Food", Icons.Outlined.Category, Color(0xFFDE6CA5)),
            PredefinedCategory("Gifts", Icons.Outlined.Redeem, Color(0xFFB5A8B8)),
            PredefinedCategory("Health", Icons.Outlined.FavoriteBorder, Color(0xFFD95A5A)),
            PredefinedCategory("House", Icons.Outlined.Home, Color(0xFF6EA9E6)),
            PredefinedCategory("Pets", Icons.Outlined.Pets, Color(0xFF67B68C)),
            PredefinedCategory("Sports", Icons.Outlined.SportsSoccer, Color(0xFF6AB8B1)),
            PredefinedCategory("Taxi", Icons.Outlined.LocalTaxi, Color(0xFFC78A00)),
            PredefinedCategory("Toiletry", Icons.Outlined.CleanHands, Color(0xFF5E7EA5)),
            PredefinedCategory("Transport", Icons.Outlined.Train, Color(0xFFDF6E75)),
            PredefinedCategory("ADD", Icons.Outlined.Diversity3, Color(0xFF8F9A9D), selectable = false),
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { category ->
            OutlinedButton(
                onClick = {
                    if (category.selectable) {
                        onCategorySelected(category.name)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp),
                enabled = category.selectable,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFEAF8F2),
                    contentColor = category.color,
                    disabledContainerColor = Color(0xFFEAF8F2),
                    disabledContentColor = category.color,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(category.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                    Text(
                        text = category.name,
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = category.color,
                    )
                }
            }
        }
    }
}

private data class PredefinedCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val selectable: Boolean = true,
)

private fun applyOperator(left: BigDecimal, right: BigDecimal, op: Char): BigDecimal {
    return when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right.compareTo(BigDecimal.ZERO) == 0) left else left.divide(right, 2, java.math.RoundingMode.HALF_UP)
        else -> right
    }
}
