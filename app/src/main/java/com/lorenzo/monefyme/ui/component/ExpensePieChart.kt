package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.util.centsToDisplay

@Composable
fun ExpensePieChart(
    slices: List<ExpenseSlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.totalCents }
    if (total <= 0L) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No expense data for this month.")
        }
        return
    }

    val colors = listOf(
        Color(0xFF6750A4),
        Color(0xFF00639A),
        Color(0xFF2E7D32),
        Color(0xFFE65100),
        Color(0xFFAD1457),
        Color(0xFF00897B),
        Color(0xFF5D4037),
        Color(0xFF1565C0),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweepAngle = (slice.totalCents.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height),
                )
                startAngle += sweepAngle
            }

            drawCircle(
                color = MaterialTheme.colorScheme.surface,
                radius = size.minDimension * 0.23f,
                center = center,
            )
        }

        Text(
            text = "Expense split by category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        slices.forEachIndexed { index, slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = colors[index % colors.size],
                        content = {},
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = slice.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                val ratio = (slice.totalCents.toDouble() / total.toDouble()) * 100
                Text(
                    text = "${centsToDisplay(slice.totalCents)} (${String.format("%.1f", ratio)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
