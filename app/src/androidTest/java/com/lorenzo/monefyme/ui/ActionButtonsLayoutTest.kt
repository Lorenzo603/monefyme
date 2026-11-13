package com.lorenzo.monefyme.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the action buttons (Add Expense and Add Income) remain visible and usable
 * even when the transaction history is fully revealed/open. This is a regression test for
 * the issue where buttons were being squished (becoming oval instead of circular) when
 * the transaction history sheet was expanded.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class ActionButtonsLayoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val density by lazy {
        composeTestRule.activity.resources.displayMetrics.density
    }

    @Test
    fun actionButtonsRemainVisibleWhenTransactionHistoryIsOpen() {
        // Verify buttons are initially displayed
        composeTestRule.onNodeWithTag("add_expense_button", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_income_button", useUnmergedTree = true)
            .assertIsDisplayed()
        
        // Drag the balance box upwards to reveal the full transaction history
        // Get the first (displayed) node, as there may be multiple due to HorizontalPager's beyondViewportPageCount
        composeTestRule.onAllNodesWithTag("balance_history_left", useUnmergedTree = false)
            .get(0)
            .performTouchInput {
                swipe(
                    start = center,
                    end = Offset(center.x, center.y - 400f * density),
                    durationMillis = 400,
                )
            }
        composeTestRule.waitForIdle()
        // Wait for the snap animation to complete
        Thread.sleep(600)
        composeTestRule.waitForIdle()

        // Verify the history sheet is now fully revealed
        composeTestRule.onNodeWithTag("history_sheet", useUnmergedTree = true)
            .assertIsDisplayed()

        // Most importantly: verify the buttons are still displayed after history is opened
        // If the buttons were squished, they might fail layout constraints or not be displayed
        composeTestRule.onNodeWithTag("add_expense_button", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_income_button", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
