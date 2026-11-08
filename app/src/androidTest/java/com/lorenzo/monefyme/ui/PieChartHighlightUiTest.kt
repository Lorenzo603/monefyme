package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasTestTag
import com.lorenzo.monefyme.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end guard for the pie chart centre: on the Home screen the centre shows the month's
 * income and expense totals until a category icon is pressed. Press-and-hold highlight behaviour
 * itself is exercised here only in its resting state (no press); the decision logic is covered by
 * the JVM unit tests in ExpensePieChartHighlightTest.
 *
 * The month pager keeps the neighbouring month's chart composed next to the visible one, so several
 * chart centres exist in the semantics tree at once. The assertions therefore single out the chart
 * that actually sits inside the window, which is the month the ticker shows.
 */
class PieChartHighlightUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Matches a node that is really on screen: its bounds lie wholly inside the app window and have
     * an area of their own. A pager page that is scrolled off to the side is composed and reported as
     * well, but it is clipped away by the pager viewport, which leaves its semantics bounds either
     * outside the window or collapsed to nothing at its edge - a zero area node is not the chart the
     * ticker shows.
     */
    private fun insideTheWindow(): SemanticsMatcher {
        val window = composeTestRule.activity.window.decorView
        val width = window.width.toFloat()
        val height = window.height.toFloat()
        assertTrue(
            "The app window must be laid out before its centres can be checked: ${width}x${height}",
            width > 0f && height > 0f,
        )
        return SemanticsMatcher("wholly inside the $width x $height window") { node ->
            val bounds = node.boundsInRoot
            val hasAnAreaOfItsOwn = bounds.right > bounds.left && bounds.bottom > bounds.top
            hasAnAreaOfItsOwn &&
                bounds.left >= 0f && bounds.top >= 0f && bounds.right <= width && bounds.bottom <= height
        }
    }

    @Test
    fun centreShowsIncomeAndExpenseTotalsWhenNoCategoryIsPressed() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasTestTag("pie_chart_centre_income").and(insideTheWindow()))
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTestTag("pie_chart_centre_expense").and(insideTheWindow()))
            .assertIsDisplayed()
    }

    @Test
    fun theMonthPagerComposesTheNeighbouringMonthToo() {
        composeTestRule.waitForIdle()

        val centres = composeTestRule
            .onAllNodesWithTag("pie_chart_centre_income")
            .fetchSemanticsNodes()
        assertTrue(
            "The pager renders the neighbouring month alongside the visible one, so its centre must " +
                "exist too: ${centres.size} centres found.",
            centres.size >= 2,
        )
    }

    @Test
    fun centreShowsCategoryPlaceholderOnlyAfterAPress() {
        composeTestRule.waitForIdle()

        // Before any press the pressed-category centre must not be present.
        val placeholders = composeTestRule
            .onAllNodesWithTag("pie_chart_centre_category")
            .fetchSemanticsNodes()
        assertTrue(
            "No category may be highlighted before a category is pressed: " +
                "${placeholders.size} placeholder nodes found.",
            placeholders.isEmpty(),
        )
    }
}
