package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Train
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.util.centsToDisplay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ExpensePieChart(
    slices: List<ExpenseSlice>,
    incomeCents: Long,
    expenseCents: Long,
    modifier: Modifier = Modifier,
) {
    val totalExpense = slices.sumOf { it.totalCents }
    val backgroundColor = Color(0xFFEAF8F2)
    if (totalExpense <= 0L) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(360.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No expense data for this month.")
        }
        return
    }

    val sliceColors = listOf(
        Color(0xFFE970A8),
        Color(0xFF9AD6B4),
        Color(0xFFF6B45F),
        Color(0xFFA567C4),
        Color(0xFFC9943A),
        Color(0xFF8DB8EA),
        Color(0xFF7BC0B8),
        Color(0xFFF07F7F),
        Color(0xFFB3B3B3),
    )
    val slicesWithMeta = remember(slices) {
        var startAngle = -90f
        slices.mapIndexed { index, slice ->
            val sweep = (slice.totalCents.toFloat() / totalExpense.toFloat()) * 360f
            val mid = startAngle + (sweep / 2f)
            SliceMeta(
                categoryName = slice.categoryName,
                amountCents = slice.totalCents,
                color = sliceColors[index % sliceColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                midAngle = mid,
            ).also {
                startAngle += sweep
            }
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
        val density = LocalDensity.current
        val chartRadiusPx = with(density) { chartSize.toPx() / 2f }
        val ringCenterRadiusPx = with(density) { chartRadiusPx - strokeWidth.toPx() / 2f }
        val lineLengthPx = with(density) { 52.dp.toPx() }
        val labelDistancePx = with(density) { 110.dp.toPx() }
        val lineWidthPx = with(density) { 2.dp.toPx() }

        Canvas(
            modifier = Modifier
                .size(chartSize)
                .align(Alignment.Center),
        ) {
            drawCircle(
                color = Color(0xFFD7ECE1),
                radius = ringCenterRadiusPx,
                style = Stroke(width = strokeWidth.toPx()),
            )

            slicesWithMeta.forEach { slice ->
                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle,
                    sweepAngle = slice.sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                )

                val angle = Math.toRadians(slice.midAngle.toDouble())
                val startX = center.x + cos(angle).toFloat() * ringCenterRadiusPx
                val startY = center.y + sin(angle).toFloat() * ringCenterRadiusPx
                val endX = center.x + cos(angle).toFloat() * (ringCenterRadiusPx + lineLengthPx)
                val endY = center.y + sin(angle).toFloat() * (ringCenterRadiusPx + lineLengthPx)
                drawLine(
                    color = slice.color.copy(alpha = 0.9f),
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = lineWidthPx,
                )
            }
        }

        Text(
            text = "€${centsToDisplay(incomeCents)}",
            color = Color(0xFF39B87A),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-8).dp),
        )
        Text(
            text = "-€${centsToDisplay(expenseCents)}",
            color = Color(0xFFDE6A75),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 24.dp),
        )

        slicesWithMeta.forEach { slice ->
            val angle = Math.toRadians(slice.midAngle.toDouble())
            val labelX = (cos(angle) * labelDistancePx).toFloat()
            val labelY = (sin(angle) * labelDistancePx).toFloat()
            val percentage = ((slice.amountCents.toDouble() / totalExpense.toDouble()) * 100).roundToInt()
            val icon = iconForCategory(slice.categoryName)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = with(density) { labelX.toDp() },
                        y = with(density) { labelY.toDp() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = slice.categoryName,
                        tint = slice.color,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "$percentage%",
                        color = Color(0xFF5D5D5D),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.offset(y = 26.dp),
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

private fun iconForCategory(categoryName: String): ImageVector {
    return when (categoryName.trim().lowercase()) {
        "food" -> Icons.Outlined.Restaurant
        "eating out" -> Icons.Outlined.LocalBar
        "car" -> Icons.Outlined.DirectionsCar
        "transport" -> Icons.Outlined.Train
        "clothes" -> Icons.Outlined.Checkroom
        "house", "housing", "home" -> Icons.Outlined.Home
        "sports" -> Icons.Outlined.SportsSoccer
        "health" -> Icons.Outlined.FavoriteBorder
        "entertainment" -> Icons.Outlined.SportsEsports
        "communications" -> Icons.Outlined.Call
        "toiletry" -> Icons.Outlined.SelfImprovement
        else -> Icons.Outlined.AccountBalanceWallet
    }
}
