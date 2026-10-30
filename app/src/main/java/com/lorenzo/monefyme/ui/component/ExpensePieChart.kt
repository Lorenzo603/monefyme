package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Train
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.util.centsToDisplay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ExpensePieChart(
    slices: List<ExpenseSlice>,
    incomeCents: Long,
    expenseCents: Long,
    modifier: Modifier = Modifier,
) {
    val totalExpense = slices.sumOf { it.totalCents }
    val backgroundColor = Color(0xFFEAF8F2)
    val isMonthEmpty = slices.isEmpty() && incomeCents == 0L && expenseCents == 0L
    val hasExpenseSlices = totalExpense > 0L
    val displayedIncomeCents = if (isMonthEmpty) 0L else incomeCents
    val displayedExpenseCents = if (isMonthEmpty) 0L else expenseCents
    val incomeLabelColor = if (isMonthEmpty) Color(0xFF8F9A9D) else Color(0xFF39B87A)
    val expenseLabelColor = if (isMonthEmpty) Color(0xFF8F9A9D) else Color(0xFFDE6A75)
    val ringBaseColor = if (hasExpenseSlices) Color(0xFFD7ECE1) else Color(0xFFBFC8C3)
    val slotFactors = remember {
        listOf(
            Pair(-0.88f, -0.82f),
            Pair(-0.46f, -0.92f),
            Pair(-0.15f, -0.95f),
            Pair(0.15f, -0.95f),
            Pair(0.46f, -0.92f),
            Pair(0.88f, -0.82f),
            Pair(0.95f, -0.25f),
            Pair(0.95f, 0.25f),
            Pair(0.88f, 0.82f),
            Pair(0.46f, 0.92f),
            Pair(0.15f, 0.95f),
            Pair(-0.15f, 0.95f),
            Pair(-0.46f, 0.92f),
            Pair(-0.88f, 0.82f),
            Pair(-0.95f, 0.25f),
            Pair(-0.95f, -0.25f),
        )
    }

    val slicesWithMeta = remember(slices, hasExpenseSlices, totalExpense) {
        if (!hasExpenseSlices) {
            return@remember emptyList()
        }
        var startAngle = -90f
        slices.map { slice ->
            val sweep = (slice.totalCents.toFloat() / totalExpense.toFloat()) * 360f
            val mid = startAngle + (sweep / 2f)
            SliceMeta(
                categoryName = slice.categoryName,
                amountCents = slice.totalCents,
                color = colorForCategory(slice.categoryName),
                startAngle = startAngle,
                sweepAngle = sweep,
                midAngle = mid,
            ).also {
                startAngle += sweep
            }
        }
    }
    val iconCategories = remember(slicesWithMeta, slotFactors) {
        val active = slicesWithMeta
            .map { normalizedCategory(it.categoryName) }
            .distinct()
        val extras = knownCategoryOrder.filterNot { it in active }
        (active + extras)
            .take(slotFactors.size)
            .map(::categoryVisualFor)
    }
    val iconPlacements = remember(iconCategories, slotFactors) {
        iconCategories.mapIndexed { index, visual ->
            IconPlacement(
                visual = visual,
                xFactor = slotFactors[index].first,
                yFactor = slotFactors[index].second,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(390.dp)
            .drawBehind {
                drawRect(backgroundColor)
            },
    ) {
        val chartSize: Dp = 226.dp
        val strokeWidth: Dp = 50.dp
        val iconSize = 34.dp
        val density = LocalDensity.current
        val chartRadiusPx = with(density) { chartSize.toPx() / 2f }
        val strokeWidthPx = with(density) { strokeWidth.toPx() }
        val ringCenterRadiusPx = chartRadiusPx - strokeWidthPx / 2f
        val ringOuterRadiusPx = ringCenterRadiusPx + strokeWidthPx / 2f
        val iconSizePx = with(density) { iconSize.toPx() }
        val xLimitPx = with(density) { (maxWidth / 2f - (iconSize / 2f) - 4.dp).toPx() }
        val yLimitPx = with(density) { (390.dp / 2f - (iconSize / 2f) - 4.dp).toPx() }
        val connectorStartRadiusPx = ringOuterRadiusPx + with(density) { 4.dp.toPx() }
        val lineWidthPx = with(density) { 1.2.dp.toPx() }
        val iconBufferPx = (iconSizePx / 2f) + with(density) { 3.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            val arcTopLeft = androidx.compose.ui.geometry.Offset(
                x = center.x - chartRadiusPx,
                y = center.y - chartRadiusPx,
            )
            val arcSize = androidx.compose.ui.geometry.Size(
                width = chartRadiusPx * 2f,
                height = chartRadiusPx * 2f,
            )
            val slotByCategory = iconPlacements.associate { placement ->
                val x = center.x + placement.xFactor * xLimitPx
                val y = center.y + placement.yFactor * yLimitPx
                placement.visual.categoryKey to androidx.compose.ui.geometry.Offset(x, y)
            }

            drawCircle(
                color = ringBaseColor,
                center = center,
                radius = ringCenterRadiusPx,
                style = Stroke(width = strokeWidthPx),
            )

            if (hasExpenseSlices) {
                slicesWithMeta.forEach { slice ->
                    drawArc(
                        color = slice.color,
                        startAngle = slice.startAngle,
                        sweepAngle = slice.sweepAngle,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                    )

                    val angle = Math.toRadians(slice.midAngle.toDouble())
                    val startX = center.x + cos(angle).toFloat() * connectorStartRadiusPx
                    val startY = center.y + sin(angle).toFloat() * connectorStartRadiusPx
                    val target = slotByCategory[normalizedCategory(slice.categoryName)] ?: return@forEach
                    val dx = target.x - startX
                    val dy = target.y - startY
                    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val endX = target.x - (dx / length) * iconBufferPx
                    val endY = target.y - (dy / length) * iconBufferPx
                    drawLine(
                        color = slice.color.copy(alpha = 0.9f),
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = lineWidthPx,
                    )
                }
            }
        }

        Text(
            text = "€${centsToDisplay(displayedIncomeCents)}",
            color = incomeLabelColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-7).dp),
        )
        Text(
            text = if (isMonthEmpty) {
                "€${centsToDisplay(displayedExpenseCents)}"
            } else {
                "-€${centsToDisplay(displayedExpenseCents)}"
            },
            color = expenseLabelColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 21.dp),
        )

        iconPlacements.forEach { placement ->
            val labelX = placement.xFactor * xLimitPx
            val labelY = placement.yFactor * yLimitPx
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = with(density) { labelX.toDp() },
                        y = with(density) { labelY.toDp() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .background(color = backgroundColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = placement.visual.icon,
                        contentDescription = placement.visual.categoryKey,
                        tint = placement.visual.color,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

private data class SliceMeta(
    val categoryName: String,
    val amountCents: Long,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
)

private data class CategoryVisual(
    val categoryKey: String,
    val icon: ImageVector,
    val color: Color,
)

private data class IconPlacement(
    val visual: CategoryVisual,
    val xFactor: Float,
    val yFactor: Float,
)

private val knownCategoryOrder = listOf(
    "bills",
    "breakfast out",
    "caffe offerti",
    "food",
    "eating out",
    "car",
    "taxi",
    "transport",
    "clothes",
    "house",
    "sports",
    "health",
    "entertainment",
    "communications",
    "toiletry",
    "gifts",
    "pets",
)

private fun categoryVisualFor(categoryName: String): CategoryVisual {
    val categoryKey = normalizedCategory(categoryName)
    return CategoryVisual(
        categoryKey = categoryKey,
        icon = iconForCategory(categoryKey),
        color = colorForCategory(categoryKey),
    )
}

private fun iconForCategory(categoryName: String): ImageVector {
    return when (normalizedCategory(categoryName)) {
        "bills" -> Icons.Outlined.LocalActivity
        "breakfast out", "caffe offerti" -> Icons.Outlined.BakeryDining
        "food" -> Icons.Outlined.Restaurant
        "eating out" -> Icons.Outlined.LocalBar
        "car" -> Icons.Outlined.DirectionsCar
        "taxi" -> Icons.Outlined.LocalTaxi
        "transport" -> Icons.Outlined.Train
        "clothes" -> Icons.Outlined.Checkroom
        "house", "housing", "home" -> Icons.Outlined.Home
        "sports" -> Icons.Outlined.SportsSoccer
        "health" -> Icons.Outlined.FavoriteBorder
        "entertainment" -> Icons.Outlined.SportsEsports
        "communications" -> Icons.Outlined.Call
        "toiletry" -> Icons.Outlined.SelfImprovement
        "gifts" -> Icons.Outlined.Redeem
        "pets" -> Icons.Outlined.Pets
        else -> Icons.Outlined.AccountBalanceWallet
    }
}

private fun colorForCategory(categoryName: String): Color {
    return when (normalizedCategory(categoryName)) {
        "bills" -> Color(0xFFC49B00)
        "breakfast out", "caffe offerti" -> Color(0xFF99631E)
        "car" -> Color(0xFF7788A3)
        "clothes" -> Color(0xFFA665CA)
        "communications" -> Color(0xFFAFA0BE)
        "eating out" -> Color(0xFF7EB7A4)
        "entertainment" -> Color(0xFFC28B47)
        "food" -> Color(0xFFDE6CA5)
        "gifts" -> Color(0xFFB5A8B8)
        "health" -> Color(0xFFD95A5A)
        "house", "housing", "home" -> Color(0xFF6EA9E6)
        "pets" -> Color(0xFF67B68C)
        "sports" -> Color(0xFF6AB8B1)
        "taxi" -> Color(0xFFC78A00)
        "toiletry" -> Color(0xFF5E7EA5)
        "transport" -> Color(0xFFDF6E75)
        else -> Color(0xFF8F9A9D)
    }
}

private fun normalizedCategory(categoryName: String): String {
    return categoryName.trim().lowercase()
}
