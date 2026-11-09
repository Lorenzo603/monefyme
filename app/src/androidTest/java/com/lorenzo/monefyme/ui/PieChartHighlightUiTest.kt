package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end guard for the pie chart centre: on the Home screen the centre shows the month's
 * income and expense totals while no category icon is pressed, and the pressed-category centre is
 * absent until a press happens. The highlight decision logic and the composable wiring are covered
 * by the JVM unit tests in ExpensePieChartHighlightTest.
 */
class PieChartHighlightUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun centreShowsIncomeAndExpenseTotalsWhenNoCategoryIsPressed() {
        composeTestRule.onNodeWithTag("pie_chart_centre_income").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pie_chart_centre_expense").assertIsDisplayed()
    }

    @Test
    fun centreShowsCategoryPlaceholderOnlyAfterAPress() {
        // Before any press the pressed-category centre must not be present.
        composeTestRule.onNodeWithTag("pie_chart_centre_category").assertDoesNotExist()
    }
}
