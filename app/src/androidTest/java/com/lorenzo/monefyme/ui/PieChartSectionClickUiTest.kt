package com.lorenzo.monefyme.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
 * into a swipe (the month pager drag) reports nothing.
 *
 * The chart is rendered on its own so the section angles are known: Food (60% of the spending) sweeps
 * 0..216 degrees and Transport the remaining 216..360 degrees, both measured from 3 o'clock clockwise
 * - the convention drawArc uses. The tappable band is the ring stroke itself (75.dp..110.dp around the
 * centre of the 220.dp chart with its 35.dp stroke). The band, the angles and the wiring are also
 * covered by the JVM unit tests in ExpensePieChartClickTest.
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

    private fun tapSection(angleDegrees: Double, radiusDp: Int = 92) {
        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput {
            val radiusPx = with(density) { radiusDp.dp.toPx() }
            click(ringPoint(center, radiusPx, angleDegrees))
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun wholeChartIsTheTappableRingArea() {
        showChart()

        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).assertIsDisplayed()
    }

    @Test
    fun tappingASectionReportsThatCategory() {
        showChart()

        // 100 degrees lies inside Food's 0..216 sweep.
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

        // The band runs from 75.dp to 110.dp around the centre: tap close to both of its edges.
        tapSection(angleDegrees = 90.0, radiusDp = 80)
        tapSection(angleDegrees = 90.0, radiusDp = 105)

        assertEquals(listOf("Food", "Food"), clickedCategories)
    }

    @Test
    fun tappingTheChartHoleOrOutsideTheRingReportsNothing() {
        showChart()

        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput { click(center) }
        tapSection(angleDegrees = 90.0, radiusDp = 40)
        tapSection(angleDegrees = 90.0, radiusDp = 150)
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
    fun swipingAcrossTheChartReportsNoClick() {
        showChart()

        // The month pager swipe: a press that travels must not be read as a click on a section.
        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).performTouchInput {
            val radiusPx = with(density) { 92.dp.toPx() }
            swipe(
                ringPoint(center, radiusPx, angleDegrees = 60.0),
                ringPoint(center, radiusPx, angleDegrees = 300.0),
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "A press that turns into a swipe is cancelled and opens nothing: " +
                "clickedCategories=$clickedCategories",
            clickedCategories.isEmpty(),
        )
    }

    /**
     * Point on the ring at [angleDegrees] (0 = 3 o'clock, growing clockwise, the convention drawArc
     * and the slice angles use), [radiusPx] away from the touched node's [center].
     */
    private fun ringPoint(center: Offset, radiusPx: Float, angleDegrees: Double): Offset {
        val radians = Math.toRadians(angleDegrees)
        return Offset(
            x = center.x + cos(radians).toFloat() * radiusPx,
            y = center.y + sin(radians).toFloat() * radiusPx,
        )
    }
}
