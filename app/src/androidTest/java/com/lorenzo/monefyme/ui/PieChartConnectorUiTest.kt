package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.ui.component.ExpensePieChart
import org.junit.Rule
import org.junit.Test

/**
 * Integration smoke-tests for the connector-line feature: the chart must render without crashing
 * when categories are placed at positions where connector lines arrive from any direction (above,
 * below, left, right, diagonal), and all active icon slots must remain visible.
 *
 * The connector endpoint geometry is verified by the JVM unit tests in
 * ExpensePieChartConnectorTest. These tests require a device/emulator; run with
 * ./gradlew connectedDebugAndroidTest.
 */
class PieChartConnectorUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun showChart(slices: List<ExpenseSlice>) {
        val total = slices.sumOf { it.totalCents }
        composeTestRule.setContent {
            ExpensePieChart(
                slices = slices,
                incomeCents = total + 2_000L,
                expenseCents = total,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun chartRendersWithSlicesSpanningAllQuadrants() {
        // Five slices distribute connectors to icons in all four quadrants, exercising the
        // top/bottom asymmetric padding and left/right boundaries.
        showChart(
            listOf(
                ExpenseSlice("Food", 2_000L, colorArgb = 0xFFE57373.toInt()),
                ExpenseSlice("Transport", 1_500L, colorArgb = 0xFF4FC3F7.toInt()),
                ExpenseSlice("Shopping", 1_000L, colorArgb = 0xFFA5D6A7.toInt()),
                ExpenseSlice("Health", 800L, colorArgb = 0xFFFFD54F.toInt()),
                ExpenseSlice("Leisure", 600L, colorArgb = 0xFFCE93D8.toInt()),
            ),
        )

        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).assertIsDisplayed()
    }

    @Test
    fun iconSlotsForAllActiveCategoriesAreVisible() {
        showChart(
            listOf(
                ExpenseSlice("Food", 3_000L, colorArgb = 0xFFE57373.toInt()),
                ExpenseSlice("Transport", 2_000L, colorArgb = 0xFF4FC3F7.toInt()),
                ExpenseSlice("Shopping", 1_000L, colorArgb = 0xFFA5D6A7.toInt()),
            ),
        )

        for (category in listOf("food", "transport", "shopping")) {
            composeTestRule.onNode(hasTestTag("pie_chart_icon_$category")).assertIsDisplayed()
        }
    }

    @Test
    fun singleSliceChartRendersWithoutCrash() {
        // A single 360-degree slice produces a horizontal connector; verifies the degenerate case
        // where there is no meaningful angle to separate from.
        showChart(listOf(ExpenseSlice("Food", 5_000L, colorArgb = 0xFFE57373.toInt())))

        composeTestRule.onNode(hasTestTag("pie_chart_arc_area")).assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("pie_chart_icon_food")).assertIsDisplayed()
    }
}
