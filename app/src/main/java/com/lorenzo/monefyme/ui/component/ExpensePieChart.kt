package com.lorenzo.monefyme.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.category.CategoryIconRegistry
import com.lorenzo.monefyme.ui.category.DefaultCategories
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.centsToDisplay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
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
            // top row, left → right
            Pair(-1.0f, -1.0f),
            Pair(-0.333f, -1.0f),
            Pair(0.333f, -1.0f),
            Pair(1.0f, -1.0f),
            // right column, top → bottom (middle 3)
            Pair(1.0f, -0.5f),
            Pair(1.0f, 0.0f),
            Pair(1.0f, 0.5f),
            // bottom row, right → left
            Pair(1.0f, 1.0f),
            Pair(0.333f, 1.0f),
            Pair(-0.333f, 1.0f),
            Pair(-1.0f, 1.0f),
            // left column, bottom → top (middle 3)
            Pair(-1.0f, 0.5f),
            Pair(-1.0f, 0.0f),
            Pair(-1.0f, -0.5f),
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
                color = slice.colorArgb?.let { Color(it.toLong() and 0xFFFFFFFFL) } 
                    ?: DefaultCategories.findByName(slice.categoryName)?.color 
                    ?: DefaultCategories.fallbackColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                midAngle = mid,
            ).also {
                startAngle += sweep
            }
        }
    }

    val expenseCentsByCategory = remember(slicesWithMeta, hasExpenseSlices, totalExpense) {
        if (!hasExpenseSlices) {
            return@remember emptyMap<String, Long>()
        }
        slicesWithMeta
            .groupBy { normalizedCategory(it.categoryName) }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(450.dp)
            .drawBehind { drawRect(backgroundColor) },
    ) {
        val activeCount = slicesWithMeta.size.coerceAtMost(slotFactors.size)
        val iconCount = desiredIconCount(activeCount)
            .coerceAtLeast(activeCount)
            .coerceAtMost(slotFactors.size)
        val visibleSlotFactors = remember(slotFactors, iconCount) {
            selectVisibleSlotFactors(slotFactors, iconCount)
        }
        val iconSize = iconSizeForActiveCount(activeCount)

        val chartSize: Dp = 186.dp
        val strokeWidth: Dp = 50.dp
        val percentageLabelGap: Dp = 2.dp
        val percentageLabelHeight: Dp = 13.dp
        val density = LocalDensity.current
        val chartRadiusPx = with(density) { chartSize.toPx() / 2f }
        val strokeWidthPx = with(density) { strokeWidth.toPx() }
        val ringCenterRadiusPx = chartRadiusPx - strokeWidthPx / 2f
        val ringOuterRadiusPx = ringCenterRadiusPx + strokeWidthPx
        val iconSizePx = with(density) { iconSize.toPx() }
        val xLimitPx = with(density) { (maxWidth / 2f - (iconSize / 2f) - 4.dp).toPx() }
        val yLimitPx = with(density) {
            (450.dp / 2f - (iconSize / 2f) - percentageLabelGap - percentageLabelHeight - 4.dp).toPx()
        }
        val connectorStartRadiusPx = ringOuterRadiusPx
        val lineWidthPx = with(density) { 1.2.dp.toPx() }
        val iconBufferPx = (iconSizePx / 2f) + with(density) { 3.dp.toPx() }

        val activeConnectors = slicesWithMeta.map { slice ->
            val radians = Math.toRadians(slice.midAngle.toDouble())
            CategoryConnector(
                categoryKey = normalizedCategory(slice.categoryName),
                amountCents = slice.amountCents,
                anchorX = cos(radians).toFloat() * connectorStartRadiusPx,
                anchorY = sin(radians).toFloat() * connectorStartRadiusPx,
            )
        }.distinctBy { it.categoryKey }

        val slots = visibleSlotFactors.mapIndexed { index, factor ->
            SlotCandidate(
                index = index,
                xFactor = factor.first,
                yFactor = factor.second,
                x = factor.first * xLimitPx,
                y = factor.second * yLimitPx,
            )
        }

        val assignedSlots = assignSlotsToActiveCategories(activeConnectors, slots)
        val activeKeys = activeConnectors.map { it.categoryKey }.toSet()
        
        // Build a map of category keys to their visuals from the original slices
        val sliceVisualsByCategory = slices.associateBy(
            { slice -> normalizedCategory(slice.categoryName) },
            { slice ->
                val iconFromSlice = slice.icon?.let { iconStr -> CategoryIconRegistry.findByName(iconStr) }
                val colorFromSlice = slice.colorArgb?.let { colorInt -> Color(colorInt.toLong() and 0xFFFFFFFFL) }
                CategoryVisual(
                    categoryKey = normalizedCategory(slice.categoryName),
                    icon = iconFromSlice ?: DefaultCategories.findByName(slice.categoryName)?.icon ?: DefaultCategories.fallbackIcon,
                    color = colorFromSlice ?: DefaultCategories.findByName(slice.categoryName)?.color ?: DefaultCategories.fallbackColor,
                )
            }
        )
        
        val activePlacements = assignedSlots.map { assigned ->
            IconPlacement(
                visual = sliceVisualsByCategory[assigned.categoryKey] ?: categoryVisualFor(assigned.categoryKey),
                slotIndex = assigned.slot.index,
                xFactor = assigned.slot.xFactor,
                yFactor = assigned.slot.yFactor,
                connectorCategoryKey = assigned.categoryKey,
                percentageLabel = expenseCentsByCategory[assigned.categoryKey]
                    ?.let { percentageLabelFor(it, totalExpense) },
            )
        }

        val freeSlots = slots.filter { slot ->
            assignedSlots.none { it.slot.index == slot.index }
        }
        val extrasNeeded = (iconCount - activePlacements.size).coerceAtLeast(0)
        val extraCategories = DefaultCategories.all
            .map { it.normalizedName }
            .filterNot { it in activeKeys }
            .take(extrasNeeded)
        val extraPlacements = freeSlots
            .take(extrasNeeded)
            .mapIndexed { index, slot ->
                val categoryKey = extraCategories.getOrElse(index) { "other" }
                IconPlacement(
                    visual = categoryVisualFor(categoryKey),
                    slotIndex = slot.index,
                    xFactor = slot.xFactor,
                    yFactor = slot.yFactor,
                    connectorCategoryKey = null,
                    percentageLabel = null,
                )
            }

        val iconPlacements = (activePlacements + extraPlacements)
            .sortedBy { it.slotIndex }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcTopLeft = androidx.compose.ui.geometry.Offset(
                x = center.x - chartRadiusPx,
                y = center.y - chartRadiusPx,
            )
            val arcSize = androidx.compose.ui.geometry.Size(
                width = chartRadiusPx * 2f,
                height = chartRadiusPx * 2f,
            )
            val connectorSlotByCategory = iconPlacements
                .filter { it.connectorCategoryKey != null }
                .associate { placement ->
                    placement.connectorCategoryKey!! to androidx.compose.ui.geometry.Offset(
                        x = center.x + placement.xFactor * xLimitPx,
                        y = center.y + placement.yFactor * yLimitPx,
                    )
                }

            drawCircle(
                color = backgroundColor,
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
                    val target = connectorSlotByCategory[normalizedCategory(slice.categoryName)] ?: return@forEach
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

        val centreAmountFontSize = MaterialTheme.typography.titleMedium.fontSize
        Text(
            text = currencyAnnotatedString(
                text = "€${centsToDisplay(displayedIncomeCents)}",
                baseFontSize = centreAmountFontSize,
            ),
            color = incomeLabelColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp),
        )
        Text(
            text = currencyAnnotatedString(
                text = if (isMonthEmpty) {
                    "€${centsToDisplay(displayedExpenseCents)}"
                } else {
                    "-€${centsToDisplay(displayedExpenseCents)}"
                },
                baseFontSize = centreAmountFontSize,
            ),
            color = expenseLabelColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 12.dp),
        )

        // Each icon is rendered together with its percentage label in a single container, so the
        // container offset alone controls the placement. The label slot is reserved for every icon
        // (label or not) to keep all icons on the ring.
        val iconLabelSlotHeight: Dp = percentageLabelGap + percentageLabelHeight
        iconPlacements.forEach { placement ->
            val labelX = placement.xFactor * xLimitPx
            val labelY = placement.yFactor * yLimitPx
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = with(density) { labelX.toDp() },
                        y = with(density) { labelY.toDp() } + iconLabelSlotHeight / 2f,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                        modifier = Modifier.size(iconSize - 6.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = percentageLabelGap)
                        .height(percentageLabelHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val percentageLabel = placement.percentageLabel
                    if (percentageLabel != null) {
                        Text(
                            text = percentageLabel,
                            color = placement.visual.color,
                            fontSize = 14.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
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
    val slotIndex: Int,
    val xFactor: Float,
    val yFactor: Float,
    val connectorCategoryKey: String?,
    val percentageLabel: String?,
)

private data class CategoryConnector(
    val categoryKey: String,
    val amountCents: Long,
    val anchorX: Float,
    val anchorY: Float,
)

private data class SlotCandidate(
    val index: Int,
    val xFactor: Float,
    val yFactor: Float,
    val x: Float,
    val y: Float,
)

private data class AssignedSlot(
    val categoryKey: String,
    val slot: SlotCandidate,
)

private data class LineSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

private fun desiredIconCount(activeCount: Int): Int {
    return 14
}

private fun iconSizeForActiveCount(activeCount: Int): Dp {
    return 40.dp
}

private fun selectVisibleSlotFactors(
    slotFactors: List<Pair<Float, Float>>,
    count: Int,
): List<Pair<Float, Float>> {
    if (count >= slotFactors.size) return slotFactors
    val step = slotFactors.size.toFloat() / count.toFloat()
    val selected = linkedSetOf<Int>()
    repeat(count) { index ->
        selected.add(floor(index * step).toInt().coerceIn(0, slotFactors.lastIndex))
    }
    var cursor = 0
    while (selected.size < count && cursor <= slotFactors.lastIndex) {
        selected.add(cursor)
        cursor += 1
    }
    return selected
        .toList()
        .sorted()
        .map { slotFactors[it] }
}

private fun assignSlotsToActiveCategories(
    connectors: List<CategoryConnector>,
    slots: List<SlotCandidate>,
): List<AssignedSlot> {
    if (connectors.isEmpty() || slots.isEmpty()) return emptyList()
    val sortedConnectors = connectors.sortedByDescending { it.amountCents }
    val assigned = mutableMapOf<String, SlotCandidate>()
    val available = slots.toMutableList()

    sortedConnectors.forEach { connector ->
        val bestSlot = available.minByOrNull { slot ->
            val trial = assigned.toMutableMap()
            trial[connector.categoryKey] = slot
            assignmentScore(connectors, trial)
        } ?: return@forEach
        assigned[connector.categoryKey] = bestSlot
        available.remove(bestSlot)
    }

    var improved = true
    while (improved) {
        improved = false
        val keys = assigned.keys.toList()
        val currentScore = assignmentScore(connectors, assigned)
        for (i in 0 until keys.size) {
            for (j in i + 1 until keys.size) {
                val a = keys[i]
                val b = keys[j]
                val slotA = assigned[a] ?: continue
                val slotB = assigned[b] ?: continue
                val swapped = assigned.toMutableMap()
                swapped[a] = slotB
                swapped[b] = slotA
                val swappedScore = assignmentScore(connectors, swapped)
                if (swappedScore + 0.01f < currentScore) {
                    assigned.clear()
                    assigned.putAll(swapped)
                    improved = true
                }
            }
        }
    }

    return assigned.map { (categoryKey, slot) ->
        AssignedSlot(categoryKey = categoryKey, slot = slot)
    }
}

private fun assignmentScore(
    connectors: List<CategoryConnector>,
    assignment: Map<String, SlotCandidate>,
): Float {
    var totalDistance = 0f
    val segments = mutableListOf<LineSegment>()
    connectors.forEach { connector ->
        val slot = assignment[connector.categoryKey] ?: return@forEach
        val segment = LineSegment(
            x1 = connector.anchorX,
            y1 = connector.anchorY,
            x2 = slot.x,
            y2 = slot.y,
        )
        segments += segment
        val dx = segment.x2 - segment.x1
        val dy = segment.y2 - segment.y1
        totalDistance += sqrt(dx * dx + dy * dy)
    }
    var crossings = 0
    for (i in 0 until segments.size) {
        for (j in i + 1 until segments.size) {
            if (segmentsIntersect(segments[i], segments[j])) {
                crossings += 1
            }
        }
    }
    return totalDistance + (crossings * 100_000f)
}

private fun segmentsIntersect(a: LineSegment, b: LineSegment): Boolean {
    val d1 = direction(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1)
    val d2 = direction(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2)
    val d3 = direction(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1)
    val d4 = direction(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2)

    if (((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
        ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
    ) {
        return true
    }

    if (abs(d1) < 0.001f && onSegment(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1)) return true
    if (abs(d2) < 0.001f && onSegment(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2)) return true
    if (abs(d3) < 0.001f && onSegment(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1)) return true
    if (abs(d4) < 0.001f && onSegment(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2)) return true
    return false
}

private fun direction(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    px: Float,
    py: Float,
): Float {
    return ((px - x1) * (y2 - y1)) - ((py - y1) * (x2 - x1))
}

private fun onSegment(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    px: Float,
    py: Float,
): Boolean {
    return px in minOf(x1, x2)..maxOf(x1, x2) &&
        py in minOf(y1, y2)..maxOf(y1, y2)
}

private fun categoryVisualFor(categoryName: String): CategoryVisual {
    val categoryKey = normalizedCategory(categoryName)
    val def = DefaultCategories.findByName(categoryKey)
    return CategoryVisual(
        categoryKey = categoryKey,
        icon = def?.icon ?: DefaultCategories.fallbackIcon,
        color = def?.color ?: DefaultCategories.fallbackColor,
    )
}

private fun normalizedCategory(categoryName: String): String {
    return categoryName.trim().lowercase()
}

private fun percentageLabelFor(partCents: Long, totalCents: Long): String? {
    if (partCents <= 0L || totalCents <= 0L) return null
    val percent = partCents.toFloat() / totalCents.toFloat() * 100f
    val rounded = percent.roundToInt()
    return if (rounded <= 0) "<1%" else "$rounded%"
}
