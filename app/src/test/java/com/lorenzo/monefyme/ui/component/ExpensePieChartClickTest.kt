package com.lorenzo.monefyme.ui.component

import java.io.File
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers clicking a section of the pie chart: a tap on one of the colored ring sections (or on that
 * category's icon slot) reports the clicked category through ExpensePieChart's `onCategoryClick`, so
 * HomeScreen can open the transaction history with that category's accordion already expanded.
 *
 * Which section a tap landed on is pure geometry and is asserted directly below. The Compose gesture
 * wiring needs an emulator, so it is asserted by reading the source file (the pattern used by
 * ExpensePieChartHighlightTest, HomeRestoreWiringTest and TransactionHistoryScreenTest); the tap
 * itself is exercised end-to-end by PieChartSectionClickUiTest in app/src/androidTest.
 */
class ExpensePieChartClickTest {

    private val pieChartSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt")
    }

    // Chart geometry as the composable uses it: 186.dp chart, 50.dp ring stroke, drawn centred.
    private val centerX = 200f
    private val centerY = 230f
    private val innerRadius = 43f
    private val outerRadius = 118f

    /** Food covers 0..225 degrees, Transport the remaining 225..360. */
    private val arcs = listOf(
        PieArc(categoryKey = "food", startAngle = 0f, sweepAngle = 225f),
        PieArc(categoryKey = "transport", startAngle = 225f, sweepAngle = 135f),
    )

    private fun pointOnRing(angleDegrees: Float, radius: Float = 80f): Pair<Float, Float> {
        val radians = Math.toRadians(angleDegrees.toDouble())
        return kotlin.math.cos(radians).toFloat() * radius + centerX to
            kotlin.math.sin(radians).toFloat() * radius + centerY
    }

    // --- angle of a tap around the chart ---

    @Test
    fun anglesStartAtThreeOClockAndGrowClockwise() {
        assertEquals(0f, angleDegreesAt(dx = 10f, dy = 0f), 0.001f)
        assertEquals(90f, angleDegreesAt(dx = 0f, dy = 10f), 0.001f)
        assertEquals(180f, angleDegreesAt(dx = -10f, dy = 0f), 0.001f)
        assertEquals(270f, angleDegreesAt(dx = 0f, dy = -10f), 0.001f)
    }

    @Test
    fun anglesAreNormalizedToAFullTurn() {
        assertEquals(315f, angleDegreesAt(dx = 10f, dy = -10f), 0.001f)
        assertEquals(45f, angleDegreesAt(dx = 10f, dy = 10f), 0.001f)
        assertEquals(135f, angleDegreesAt(dx = -10f, dy = 10f), 0.001f)
        assertEquals(225f, angleDegreesAt(dx = -10f, dy = -10f), 0.001f)
    }

    @Test
    fun arcMembershipHandlesTheZeroThreeOclockWrapAround() {
        // A section that crosses 0 degrees must still be hit on both sides of it.
        val crossing = PieArc(categoryKey = "leisure", startAngle = 300f, sweepAngle = 90f)
        assertTrue(isAngleWithinArc(350f, crossing.startAngle, crossing.sweepAngle))
        assertTrue(isAngleWithinArc(10f, crossing.startAngle, crossing.sweepAngle))
        assertTrue(isAngleWithinArc(29f, crossing.startAngle, crossing.sweepAngle))
        assertFalse("The section ends at 30 degrees.", isAngleWithinArc(30f, crossing.startAngle, crossing.sweepAngle))
        assertFalse(isAngleWithinArc(200f, crossing.startAngle, crossing.sweepAngle))
        assertFalse(isAngleWithinArc(190f, crossing.startAngle, crossing.sweepAngle))
    }

    @Test
    fun emptySectionNeverMatches() {
        assertFalse(isAngleWithinArc(0f, startAngle = 0f, sweepAngle = 0f))
        assertFalse(isAngleWithinArc(90f, startAngle = 90f, sweepAngle = -10f))
    }

    // --- which section a tap landed on ---

    @Test
    fun tappingASectionReportsThatCategory() {
        val (x, y) = pointOnRing(angleDegrees = 100f)
        assertEquals(
            "A tap inside Food's ring section must report Food.",
            "food",
            categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
        )

        val (transportX, transportY) = pointOnRing(angleDegrees = 300f)
        assertEquals(
            "A tap inside Transport's ring section must report Transport.",
            "transport",
            categoryKeyAtPoint(arcs, centerX, centerY, transportX, transportY, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingAnywhereInsideASectionReportsIt() {
        // Every angle of Food's 0..225 section, near the inner and the outer edge of the ring band.
        for (angle in listOf(0f, 45f, 90f, 135f, 180f, 224f)) {
            for (radius in listOf(innerRadius + 1f, 80f, outerRadius - 1f)) {
                val (x, y) = pointOnRing(angleDegrees = angle, radius = radius)
                assertEquals(
                    "Angle $angle at radius $radius is inside the Food section.",
                    "food",
                    categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
                )
            }
        }
    }

    @Test
    fun ringBandEdgesAreTappable() {
        assertEquals(
            "A tap on the inner edge of the ring stroke still belongs to the section.",
            "food",
            categoryKeyAtPoint(
                arcs = arcs,
                centerX = centerX,
                centerY = centerY,
                x = centerX + innerRadius,
                y = centerY,
                innerRadiusPx = innerRadius,
                outerRadiusPx = outerRadius,
            ),
        )
        assertEquals(
            "A tap on the outer edge of the ring stroke still belongs to the section.",
            "transport",
            categoryKeyAtPoint(
                arcs = arcs,
                centerX = centerX,
                centerY = centerY,
                x = centerX,
                y = centerY - outerRadius,
                innerRadiusPx = innerRadius,
                outerRadiusPx = outerRadius,
            ),
        )
    }

    @Test
    fun sectionBoundaryIsAttributedToTheSectionThatStartsThere() {
        val (x, y) = pointOnRing(angleDegrees = 225f)
        assertEquals(
            "A tap exactly on the boundary belongs to the section that starts there.",
            "transport",
            categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingTheCentreOrOutsideTheRingReportsNothing() {
        assertNull(
            "The hole in the middle of the chart is not a section.",
            categoryKeyAtPoint(arcs, centerX, centerY, centerX, centerY, innerRadius, outerRadius),
        )
        val (insideHoleX, insideHoleY) = pointOnRing(angleDegrees = 100f, radius = innerRadius - 5f)
        assertNull(
            "A tap inside the ring must not open a category.",
            categoryKeyAtPoint(arcs, centerX, centerY, insideHoleX, insideHoleY, innerRadius, outerRadius),
        )
        val (outsideX, outsideY) = pointOnRing(angleDegrees = 100f, radius = outerRadius + 5f)
        assertNull(
            "A tap outside the ring must not open a category.",
            categoryKeyAtPoint(arcs, centerX, centerY, outsideX, outsideY, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingAnEmptyMonthReportsNothing() {
        val (x, y) = pointOnRing(angleDegrees = 100f)
        assertNull(
            categoryKeyAtPoint(
                arcs = emptyList(),
                centerX = centerX,
                centerY = centerY,
                x = x,
                y = y,
                innerRadiusPx = innerRadius,
                outerRadiusPx = outerRadius,
            ),
        )
    }

    @Test
    fun singleFullTurnSectionIsTappableEverywhere() {
        val single = listOf(PieArc(categoryKey = "food", startAngle = 0f, sweepAngle = 360f))
        for (angle in listOf(0f, 90f, 180f, 270f, 315f)) {
            val (x, y) = pointOnRing(angleDegrees = angle)
            assertEquals("food", categoryKeyAtPoint(single, centerX, centerY, x, y, innerRadius, outerRadius))
        }
    }

    @Test
    fun tapMatchingUsesTheDrawnRingBandOfTheChart() {
        // chart 186.dp with a 50.dp stroke: the band runs from (186 - 50) / 2 to 186 / 2 + 25 dp.
        val chartSize = 186f
        val strokeWidth = 50f
        val ringCenterRadius = chartSize / 2f - strokeWidth / 2f
        assertEquals(68f, ringCenterRadius, 0.001f)
        val (x, y) = pointOnRing(angleDegrees = 100f, radius = ringCenterRadius)
        assertEquals(sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)), ringCenterRadius, 0.001f)
        assertEquals(
            "The middle of the stroke must be tappable.",
            "food",
            categoryKeyAtPoint(
                arcs = arcs,
                centerX = centerX,
                centerY = centerY,
                x = x,
                y = y,
                innerRadiusPx = ringCenterRadius - strokeWidth / 2f,
                outerRadiusPx = ringCenterRadius + strokeWidth / 2f,
            ),
        )
    }

    // --- name reported for the clicked category ---

    @Test
    fun clickedCategoryIsReportedWithItsOriginalSpelling() {
        val displayNames = mapOf("food" to "Food", "transport" to "Transport")
        assertEquals("Food", categoryClickCategory("food", displayNames))
        assertEquals("Food", categoryClickCategory("FOOD", displayNames))
        assertEquals("Transport", categoryClickCategory(" transport ", displayNames))
    }

    @Test
    fun clickedCategoryFallsBackToItsKeyWhenNoDisplayNameIsKnown() {
        assertEquals("leisure", categoryClickCategory("leisure", mapOf("food" to "Food")))
    }

    // --- icon slots are not ring taps (the icon reports its own click) ---

    /** Icon slot circles as the chart builds them, relative to the chart centre. */
    private val iconSlotOffsets = listOf(
        androidx.compose.ui.geometry.Offset(x = 0f, y = -160f),
        androidx.compose.ui.geometry.Offset(x = 140f, y = 30f),
        androidx.compose.ui.geometry.Offset(x = -140f, y = 30f),
    )

    @Test
    fun tapOnAnIconSlotIsSkippedByTheRingHitTest() {
        assertTrue(
            "A tap on the icon itself must not also be counted as a tap on the ring section behind it.",
            isWithinIconSlot(iconSlotOffsets, x = 4f, y = -155f, radiusPx = 20f),
        )
        assertTrue(
            "The exact icon centre is inside its slot.",
            isWithinIconSlot(iconSlotOffsets, x = 140f, y = 30f, radiusPx = 20f),
        )
    }

    @Test
    fun tapAwayFromEveryIconSlotIsNotSkipped() {
        assertFalse(
            "A ring-section tap far from any icon must stay a ring tap.",
            isWithinIconSlot(iconSlotOffsets, x = 60f, y = -30f, radiusPx = 20f),
        )
        assertFalse(
            "Just outside the slot circle is outside the icon.",
            isWithinIconSlot(iconSlotOffsets, x = 0f, y = -160f - 21f, radiusPx = 20f),
        )
        assertFalse(
            "No slots means nothing to skip.",
            isWithinIconSlot(emptyList(), x = 0f, y = 0f, radiusPx = 20f),
        )
    }

    // --- a press that turns into a drag (the month pager swipe) is not a click ---

    @Test
    fun pressThatStaysInsideTheTouchSlopIsAClick() {
        assertFalse(
            "A tap whose up event is within the touch slop is still a click.",
            isDragPastTouchSlop(downX = 100f, downY = 100f, upX = 103f, upY = 103f, touchSlopPx = 8f),
        )
        assertFalse(
            "Exactly on the slop boundary is not a drag.",
            isDragPastTouchSlop(downX = 100f, downY = 100f, upX = 108f, upY = 100f, touchSlopPx = 8f),
        )
    }

    @Test
    fun pressThatTravelsPastTheTouchSlopIsADragNotAClick() {
        assertTrue(
            "The month pager swipe must not be read as a click on the section it started on.",
            isDragPastTouchSlop(downX = 100f, downY = 100f, upX = 240f, upY = 40f, touchSlopPx = 8f),
        )
        assertTrue(
            isDragPastTouchSlop(downX = 100f, downY = 100f, upX = 100f, upY = 109f, touchSlopPx = 8f),
        )
    }

    // --- wiring inside the composable (gestures need an emulator) ---

    @Test
    fun composableAcceptsACategoryClickCallback() {
        val signature = pieChartSource.substringAfter("fun ExpensePieChart(").substringBefore(") {")
        assertTrue(
            "ExpensePieChart must expose an onCategoryClick callback for the caller to navigate:\n$signature",
            signature.contains("onCategoryClick"),
        )
    }

    @Test
    fun ringSectionsAreMatchedToTapsThroughHitTesting() {
        val body = expensePieChartBody()
        assertTrue(
            "The chart must keep its drawn ring sections so a tap can be matched to one.",
            body.contains("PieArc(") && body.contains("val pieArcs = remember(slicesWithMeta)"),
        )
        val ringGesture = body.substringAfter(".testTag(\"pie_chart_arc_area\")").substringBefore("val arcTopLeft =")
        assertTrue("The ring area must listen for gestures:\n$ringGesture", ringGesture.contains("pointerInput"))
        assertTrue("The ring area must await the press:\n$ringGesture", ringGesture.contains("awaitFirstDown"))
        assertTrue(
            "The ring area must resolve the tapped section with categoryKeyAtPoint:\n$ringGesture",
            ringGesture.contains("categoryKeyAtPoint("),
        )
        assertTrue(
            "The hit test must use the ring band, not the whole chart:\n$ringGesture",
            ringGesture.contains("innerRadiusPx = ringCenterRadiusPx - strokeWidthPx / 2f") &&
                ringGesture.contains("outerRadiusPx = ringCenterRadiusPx + strokeWidthPx / 2f"),
        )
    }

    @Test
    fun tappingASectionOrItsIconReportsTheClickedCategory() {
        val body = expensePieChartBody()
        val ringGesture = body.substringAfter(".testTag(\"pie_chart_arc_area\")").substringBefore("val arcTopLeft =")
        val iconGesture = body.substringAfter(".pointerInput(placement.connectorCategoryKey)")
            .substringBefore("\n                        },")
        for (gesture in listOf(ringGesture, iconGesture)) {
            assertTrue(
                "A completed tap must report the clicked category through onCategoryClick:\n$gesture",
                gesture.contains("onCategoryClick(categoryClickCategory("),
            )
            assertTrue(
                "Only a tap that ended in a release counts as a click, so a month-pager swipe " +
                    "does not navigate:\n$gesture",
                gesture.contains("if (up != null"),
            )
            assertTrue(
                "The up event must be awaited before the click is reported:\n$gesture",
                gesture.indexOf("waitForUpOrCancellation") < gesture.indexOf("if (up != null"),
            )
            assertTrue(
                "A press that travels past the touch slop is a drag (the month pager swipe), not " +
                    "a click on the section it started on:\n$gesture",
                gesture.contains("!isDragPastTouchSlop("),
            )
        }
    }

    @Test
    fun ringHitTestSkipsTapsThatLandOnAnIconSlot() {
        val body = expensePieChartBody()
        val ringGesture = body.substringAfter(".testTag(\"pie_chart_arc_area\")").substringBefore("val arcTopLeft =")
        assertTrue(
            "The icon sits on top of the ring area, so a tap on it would also reach the ring " +
                "handler; the ring handler must skip icon-slot taps so the click is not reported " +
                "twice:\n$ringGesture",
            ringGesture.contains("isWithinIconSlot("),
        )
        assertTrue(
            "The icon slot circles must be computed from the same placements that draw the icons.",
            body.contains("val iconSlotOffsets = iconPlacements.map"),
        )
    }

    @Test
    fun ringAreaIsTaggedForUiTests() {
        assertTrue(
            "The tappable ring area needs a stable test tag.",
            expensePieChartBody().contains("\"pie_chart_arc_area\""),
        )
    }

    private fun expensePieChartBody(): String {
        val start = pieChartSource.indexOf("fun ExpensePieChart(")
        assertTrue("ExpensePieChart composable must exist.", start >= 0)
        val end = pieChartSource.indexOf("private data class SliceMeta", start)
        assertTrue(end > start)
        return pieChartSource.substring(start, end)
    }

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        // Normalise line endings so the assertions are independent of the checkout setting.
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
