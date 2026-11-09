package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end guard for the pie chart centre: on the Home screen the centre shows the month's
 * income and expense totals while no category icon is pressed, and the pressed-category centre is
 * absent until a press happens. The highlight decision logic and the composable wiring are covered
 * by the JVM unit tests in ExpensePieChartHighlightTest.
 *
 * The Home screen month pager composes the neighbouring months too (beyondViewportPageCount = 1), so
 * several charts - and several centre labels, some of them clipped off screen - exist at once: the
 * assertions therefore speak about the nodes rather than expecting exactly one.
 */
class PieChartHighlightUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun centreShowsIncomeAndExpenseTotalsWhenNoCategoryIsPressed() {
        composeTestRule.onAllNodesWithTag("pie_chart_centre_income")
            .assertAny(atLeastPartiallyOnScreen())
        composeTestRule.onAllNodesWithTag("pie_chart_centre_expense")
            .assertAny(atLeastPartiallyOnScreen())
    }

    @Test
    fun centreShowsCategoryPlaceholderOnlyAfterAPress() {
        // Before any press the pressed-category centre must not be present.
        composeTestRule.onNodeWithTag("pie_chart_centre_category").assertDoesNotExist()
    }

    /**
     * Matches nodes whose bounds reach into the screen: the pager lays out the neighbouring months
     * too, and their labels are offset a full viewport width away, so their bounds lie entirely
     * outside the screen while the selected month's labels are on it.
     */
    private fun atLeastPartiallyOnScreen(): SemanticsMatcher {
        val metrics = composeTestRule.activity.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels.toFloat()
        val screenHeightPx = metrics.heightPixels.toFloat()
        return SemanticsMatcher(
            "has bounds inside the screen (a visible month's centre label)",
        ) { node ->
            val bounds = node.boundsInRoot
            bounds.left < screenWidthPx && bounds.right > 0f &&
                bounds.top < screenHeightPx && bounds.bottom > 0f
        }
    }
}
