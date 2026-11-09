package com.lorenzo.monefyme.ui.component

import androidx.compose.ui.geometry.Offset
import java.io.File
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
 * ExpensePieChartHighlightTest and HomeRestoreWiringTest); the tap itself is exercised end-to-end by
 * PieChartSectionClickUiTest in app/src/androidTest.
 */
class ExpensePieChartClickTest {

    private val pieChartSource by lazy { readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt") }

    // Chart geometry as the composable draws it: 220.dp chart, 35.dp ring stroke, drawn centred, so
    // the colored band runs from 75.dp to 110.dp around the centre.
    private val centerX = 300f
    private val centerY = 400f
    private val innerRadius = 75f
    private val outerRadius = 110f

    /** Food covers 0..225 degrees, Transport the remaining 225..360. */
    private val arcs = listOf(
        PieArc(categoryKey = "food", startAngle = 0f, sweepAngle = 225f),
        PieArc(categoryKey = "transport", startAngle = 225f, sweepAngle = 135f),
    )

    private fun pointOnRing(angleDegrees: Double, radius: Float): Pair<Float, Float> {
        val radians = Math.toRadians(angleDegrees)
        return kotlin.math.cos(radians).toFloat() * radius + centerX to kotlin.math.sin(radians).toFloat() * radius + centerY
    }

    // --- angle of a point around the chart ---

    @Test
    fun anglesStartAtThreeOClockAndGrowClockwise() {
        assertEquals(0f, angleDegreesAt(dx = 10f, dy = 0f), 0.001f)
        assertEquals(90f, angleDegreesAt(dx = 0f, dy = 10f), 0.001f)
        assertEquals(180f, angleDegreesAt(dx = -10f, dy = 0f), 0.001f)
        assertEquals(270f, angleDegreesAt(dx = 0f, dy = -10f), 0.001f)
    }

    @Test
    fun anglesAreNormalizedToAFullTurn() {
        assertEquals(45f, angleDegreesAt(dx = 10f, dy = 10f), 0.001f)
        assertEquals(135f, angleDegreesAt(dx = -10f, dy = 10f), 0.001f)
        assertEquals(225f, angleDegreesAt(dx = -10f, dy = -10f), 0.001f)
        assertEquals(315f, angleDegreesAt(dx = 10f, dy = -10f), 0.001f)
    }

    @Test
    fun sectionMembershipHandlesTheThreeOClockWrapAround() {
        // A section that crosses 0 degrees must be hit on both sides of it.
        val crossing = PieArc(categoryKey = "leisure", startAngle = 300f, sweepAngle = 90f)
        assertTrue(isAngleWithinArc(angle = 350f, startAngle = crossing.startAngle, sweepAngle = crossing.sweepAngle))
        assertTrue(isAngleWithinArc(angle = 10f, startAngle = crossing.startAngle, sweepAngle = crossing.sweepAngle))
        assertTrue(isAngleWithinArc(angle = 29f, startAngle = crossing.startAngle, sweepAngle = crossing.sweepAngle))
        assertFalse(isAngleWithinArc(angle = 30f, startAngle = crossing.startAngle, sweepAngle = crossing.sweepAngle))
        assertFalse(isAngleWithinArc(angle = 200f, startAngle = crossing.startAngle, sweepAngle = crossing.sweepAngle))
    }

    @Test
    fun emptySectionNeverMatches() {
        assertFalse(isAngleWithinArc(angle = 0f, startAngle = 0f, sweepAngle = 0f))
        assertFalse(isAngleWithinArc(angle = 90f, startAngle = 90f, sweepAngle = -10f))
    }

    // --- which section a tap landed on ---

    @Test
    fun tappingASectionReportsThatCategory() {
        val (foodX, foodY) = pointOnRing(angleDegrees = 100.0, radius = 92f)
        assertEquals(
            "A tap inside Food's ring section must report Food.",
            "food",
            categoryKeyAtPoint(arcs, centerX, centerY, foodX, foodY, innerRadius, outerRadius),
        )

        val (transportX, transportY) = pointOnRing(angleDegrees = 300.0, radius = 92f)
        assertEquals(
            "A tap inside Transport's ring section must report Transport.",
            "transport",
            categoryKeyAtPoint(arcs, centerX, centerY, transportX, transportY, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingAnywhereInsideTheRingBandReportsTheSectionThere() {
        // Every angle of Food's 0..225 sweep, near both edges of the band.
        for (angle in listOf(5.0, 45.0, 90.0, 135.0, 180.0, 220.0)) {
            for (radius in listOf(innerRadius + 1f, 92f, outerRadius - 1f)) {
                val (x, y) = pointOnRing(angleDegrees = angle, radius = radius)
                assertEquals(
                    "Angle $angle at radius $radius lies inside the Food section.",
                    "food",
                    categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
                )
            }
        }
        for (angle in listOf(230.0, 270.0, 310.0, 355.0)) {
            val (x, y) = pointOnRing(angleDegrees = angle, radius = 92f)
            assertEquals(
                "Angle $angle lies inside the Transport section.",
                "transport",
                categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
            )
        }
    }

    @Test
    fun sectionBoundaryBelongsToTheSectionThatStartsThere() {
        val (x, y) = pointOnRing(angleDegrees = 225.0, radius = 92f)
        assertEquals(
            "A tap exactly on the boundary belongs to the section that starts there.",
            "transport",
            categoryKeyAtPoint(arcs, centerX, centerY, x, y, innerRadius, outerRadius),
        )
    }

    @Test
    fun ringBandEdgesAreTappable() {
        assertEquals(
            "A tap on the inner edge of the ring stroke belongs to its section.",
            "food",
            categoryKeyAtPoint(arcs, centerX, centerY, centerX + innerRadius, centerY, innerRadius, outerRadius),
        )
        assertEquals(
            "A tap on the outer edge of the ring stroke belongs to its section.",
            "transport",
            categoryKeyAtPoint(arcs, centerX, centerY, centerX, centerY - outerRadius, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingTheChartHoleOrOutsideTheRingReportsNothing() {
        assertNull(
            "The hole in the middle of the chart is not a section.",
            categoryKeyAtPoint(arcs, centerX, centerY, centerX, centerY, innerRadius, outerRadius),
        )
        val (holeX, holeY) = pointOnRing(angleDegrees = 100.0, radius = innerRadius - 5f)
        assertNull(
            "A tap inside the ring must not report a category.",
            categoryKeyAtPoint(arcs, centerX, centerY, holeX, holeY, innerRadius, outerRadius),
        )
        val (outsideX, outsideY) = pointOnRing(angleDegrees = 100.0, radius = outerRadius + 5f)
        assertNull(
            "A tap outside the ring must not report a category.",
            categoryKeyAtPoint(arcs, centerX, centerY, outsideX, outsideY, innerRadius, outerRadius),
        )
    }

    @Test
    fun tappingAnEmptyMonthReportsNothing() {
        val (x, y) = pointOnRing(angleDegrees = 100.0, radius = 92f)
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
        for (angle in listOf(0.0, 90.0, 180.0, 270.0, 315.0)) {
            val (x, y) = pointOnRing(angleDegrees = angle, radius = 92f)
            assertEquals("food", categoryKeyAtPoint(single, centerX, centerY, x, y, innerRadius, outerRadius))
        }
    }

    // --- name reported for the clicked category ---

    @Test
    fun aPressThatStayedWhereItStartedIsAClickAndATravelingPressIsNot() {
        assertTrue(isPressWithoutDrag(Offset(50f, 50f), Offset(50f, 50f), 20f))
        assertTrue(isPressWithoutDrag(Offset(50f, 50f), Offset(60f, 55f), 20f))
        assertTrue(isPressWithoutDrag(Offset(50f, 50f), Offset(50f, 70f), 20f))

        assertFalse(isPressWithoutDrag(Offset(50f, 50f), Offset(50f, 71f), 20f))
        assertFalse(isPressWithoutDrag(Offset(50f, 50f), Offset(300f, 90f), 20f))
        assertFalse(isPressWithoutDrag(Offset(50f, 50f), Offset(50.5f, 50.5f), 0f))
        assertTrue(isPressWithoutDrag(Offset(50f, 50f), Offset(50.5f, 50.5f), 1f))
    }

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

    // --- wiring inside the composable (gestures need an emulator) ---

    @Test
    fun composableAcceptsACategoryClickCallback() {
        val signature = pieChartSource.substringAfter("fun ExpensePieChart(").substringBefore("\n) {")
        assertTrue(
            "ExpensePieChart must expose an onCategoryClick callback so the caller can navigate:\n$signature",
            signature.contains("onCategoryClick: ((String) -> Unit)?"),
        )
    }

    @Test
    fun ringSectionsAreKeptSoATapCanBeMatchedToTheDrawnSection() {
        val body = expensePieChartBody()
        assertTrue(
            "The chart must remember its drawn ring sections (category, start angle, sweep) for hit " +
                "testing.",
            body.contains("val pieArcs = remember(slicesWithMeta)") && body.contains("PieArc("),
        )
        val ringGesture = body.substringAfter(".testTag(\"pie_chart_arc_area\")").substringBefore("val arcTopLeft =")
        assertTrue("The ring area must listen for gestures:\n$ringGesture", ringGesture.contains("pointerInput"))
        assertTrue("The ring area must await the press:\n$ringGesture", ringGesture.contains("awaitFirstDown"))
        assertTrue(
            "The ring area must resolve the tapped section with categoryKeyAtPoint:\n$ringGesture",
            ringGesture.contains("categoryKeyAtPoint("),
        )
        assertTrue(
            "The hit test must use the drawn ring band, not the whole chart:\n$ringGesture",
            ringGesture.contains("innerRadiusPx = ringCenterRadiusPx - strokeWidthPx / 2f") &&
                ringGesture.contains("outerRadiusPx = ringCenterRadiusPx + strokeWidthPx / 2f"),
        )
    }

    @Test
    fun tappingASectionOrItsIconReportsTheClickedCategory() {
        val body = expensePieChartBody()
        val ringGesture = body.substringAfter(".testTag(\"pie_chart_arc_area\")").substringBefore("val arcTopLeft =")
        val iconGesture = body.substringAfter(".testTag(\"pie_chart_icon_")
            .substringAfter("awaitEachGesture {")
            .substringBefore("\n                            }")
        for (gesture in listOf(ringGesture, iconGesture)) {
            assertTrue(
                "A completed tap must report the clicked category through onCategoryClick:\n$gesture",
                gesture.contains("onCategoryClick(categoryClickCategory("),
            )
            assertTrue(
                "Only a tap that ended in a release counts as a click, so a month-pager swipe does " +
                    "not navigate:\n$gesture",
                gesture.contains("if (up != null"),
            )
            assertTrue(
                "A press that travelled over the chart (the month pager swipe) must not be reported " +
                    "as a click on a section:\n$gesture",
                gesture.contains("isPressWithoutDrag("),
            )
            assertTrue(
                "The release must be awaited before the click is reported:\n$gesture",
                gesture.indexOf("waitForUpOrCancellation") < gesture.indexOf("if (up != null"),
            )
        }
    }

    @Test
    fun chartAreasAreTaggedForUiTests() {
        val body = expensePieChartBody()
        assertTrue("The tappable ring area needs a stable test tag.", body.contains("\"pie_chart_arc_area\""))
        assertTrue(
            "Every category icon slot needs a stable test tag so a UI test can click that category.",
            body.contains("\"pie_chart_icon_"),
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
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val current: File = dir
            if (File(current, "settings.gradle.kts").exists() && File(current, "app").isDirectory) {
                return current
            }
            dir = current.parentFile
        }
        throw IllegalStateException("Could not locate the project root starting from $dir")
    }
}
