package com.lorenzo.monefyme.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import java.util.Collections
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for clicking a section of the pie chart.
 *
 * A tap on one of the chart's colored ring sections - or on that category's icon slot - reports the
 * clicked category through ExpensePieChart.onCategoryClick, which is what HomeScreen uses to open the
 * transaction history with that category already expanded (the accordion opening itself is covered by
 * TransactionHistoryAccordionTest). A tap in the chart's hole, outside the ring, or a press that turns
 * into a drag (the month pager swipe) reports nothing.
 *
 * The chart is rendered on its own so the section angles are known: Food sweeps 0..216 degrees (its
 * 6000 of the 10000 expense cents) and Transport the remaining 216..360 degrees, both measured from
 * 3 o'clock clockwise - the same convention drawArc uses. Taps are taken in the middle of the
 * tappable ring band; the band and the section angles are also covered by the JVM unit tests in
 * ExpensePieChartClickTest.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class PieChartSectionClickUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val clickedCategories = Collections.synchronizedList(mutableListOf<String>())

    private fun showChart() {
        clickedCategories.clear()
        composeTestRule.setContent {
            ExpensePieChart(
                slices = listOf(
                    ExpenseSlice(categoryName = "Food", totalCents = 6_000L, colorArgb = 0xFFFF0000.toInt()),
                    ExpenseSlice(categoryName = "Transport", totalCents = 4_000L, colorArgb = 0xFF0000FF.toInt()),
                ),
                incomeCents = 20_000L,
                expenseCents = 10_000L,
                modifier = Modifier.padding(12.dp),
                onCategoryClick = { clickedCategories += it },
            )
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Point on the ring at [angleDegrees] (0 = 3 o'clock, growing clockwise) and [radiusDp] from the
     * chart's centre, in the injection scope's local pixel coordinates. The canvas fills the chart's
     * 450.dp area, so its centre is the chart's centre and dp convert to px through the scope.
     */
    private fun InjectionScope.ringPoint(angleDegrees: Double, radiusDp: Float): Offset {
        val radiusPx = radiusDp * density
        val radians = Math.toRadians(angleDegrees)
        return Offset(
            x = centerX + cos(radians).toFloat() * radiusPx,
            y = centerY + sin(radians).toFloat() * radiusPx,
        )
    }

    private fun tapSection(angleDegrees: Double, radiusDp: Float = 80f) {
        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput {
            val point = ringPoint(angleDegrees = angleDegrees, radiusDp = radiusDp)
            down(point)
            up()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun chartIsRenderedWithTheFullRingAreaTappable() {
        showChart()

        val bounds = composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).getBoundsInRoot()
        assertTrue(
            "The whole chart area must be the tappable ring area, otherwise a section cannot be hit.",
            bounds.right > bounds.left && bounds.bottom > bounds.top,
        )
    }

    @Test
    fun tappingASectionReportsThatCategory() {
        showChart()

        // 100 degrees lies inside Food's 0..225 sweep.
        tapSection(angleDegrees = 100.0)

        assertEquals(listOf("Food"), clickedCategories)
    }

    @Test
    fun tappingAnotherSectionReportsThatCategory() {
        showChart()

        // 300 degrees lies inside Transport's 216..360 sweep.
        tapSection(angleDegrees = 300.0)

        assertEquals(listOf("Transport"), clickedCategories)
    }

    @Test
    fun everyTappedSectionReportsItsOwnCategory() {
        showChart()

        for (angle in listOf(5.0, 45.0, 90.0, 135.0, 180.0, 210.0)) {
            tapSection(angleDegrees = angle)
        }
        for (angle in listOf(220.0, 270.0, 310.0, 355.0)) {
            tapSection(angleDegrees = angle)
        }

        assertEquals(
            "Each section reports its own category, in the order they were clicked.",
            listOf(
                "Food", "Food", "Food", "Food", "Food", "Food",
                "Transport", "Transport", "Transport", "Transport",
            ),
            clickedCategories,
        )
    }

    @Test
    fun tappingAnywhereInsideTheRingBandReportsACategory() {
        showChart()

        // The band runs from 43.dp to 93.dp around the centre: tap near both of its edges.
        tapSection(angleDegrees = 90.0, radiusDp = 50f)
        tapSection(angleDegrees = 90.0, radiusDp = 88f)

        assertEquals(listOf("Food", "Food"), clickedCategories)
    }

    @Test
    fun tappingTheChartCentreOrOutsideTheRingReportsNothing() {
        showChart()

        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput {
            down(center)
            up()
        }
        tapSection(angleDegrees = 90.0, radiusDp = 20f)
        tapSection(angleDegrees = 90.0, radiusDp = 160f)
        composeTestRule.waitForIdle()

        assertTrue(
            "The hole in the middle and the area outside the ring are not chart sections: " +
                "clickedCategories=$clickedCategories",
            clickedCategories.isEmpty(),
        )
    }

    @Test
    fun tappingACategoryIconReportsItsCategoryToo() {
        showChart()

        composeTestRule.onNodeWithContentDescription("food").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("Food"), clickedCategories)
    }

    @Test
    fun draggingAcrossTheChartReportsNoClick() {
        showChart()

        // The month pager swipe: a press that travels must not be read as a click on a section.
        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput {
            down(ringPoint(angleDegrees = 60.0, radiusDp = 80f))
            moveTo(ringPoint(angleDegrees = 200.0, radiusDp = 80f))
            moveTo(ringPoint(angleDegrees = 300.0, radiusDp = 80f))
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "A press that turns into a drag is cancelled and opens nothing: " +
                "clickedCategories=$clickedCategories",
            clickedCategories.isEmpty(),
        )
    }
}
