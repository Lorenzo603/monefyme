package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for the Home screen "Restore" entry points.
 *
 * - The sidebar "Restore" row must open the Restore screen (it used to be a silent no-op because
 *   HomeScreen never passed the sidebar's onOpenRestore callback).
 * - The two buttons next to the balance box must open the month's transaction history (like the
 *   balance box itself) and must carry the hamburger icon, not the Restore icon.
 */
class HomeRestoreNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sidebarRestoreRowOpensRestoreScreen() {
        composeTestRule.onNodeWithText("Monefy").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("More").performClick()
        composeTestRule.onNodeWithTag("sidebar_restore").assertIsDisplayed().assert(hasClickAction())

        composeTestRule.onNodeWithTag("sidebar_restore").performClick()

        composeTestRule.onNodeWithText("Manual CSV restore").assertIsDisplayed()
    }

    @Test
    fun balanceBoxLeftButtonOpensTransactionHistory() {
        composeTestRule.waitForIdle()
        // Get the first (displayed) node, as there may be multiple due to HorizontalPager's beyondViewportPageCount
        composeTestRule.onAllNodesWithTag("balance_history_left", useUnmergedTree = false)
            .get(0)
            .performClick()

        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    fun balanceBoxRightButtonOpensTransactionHistory() {
        composeTestRule.waitForIdle()
        // Get the first (displayed) node, as there may be multiple due to HorizontalPager's beyondViewportPageCount
        composeTestRule.onAllNodesWithTag("balance_history_right", useUnmergedTree = false)
            .get(0)
            .performClick()

        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    fun balanceBoxSideButtonsUseHamburgerIconsAndNotRestoreIcons() {
        // Count all nodes with the content description (including non-displayed ones from adjacent pager pages)
        // This should be at least 2 from the current page
        composeTestRule.onAllNodesWithContentDescription("Open transaction history")
            .assertCountEquals(2)
        // The Restore entry point now lives only in the sidebar, so the closed Home screen must
        // not expose a Restore button any more.
        composeTestRule.onNodeWithContentDescription("Open restore").assertDoesNotExist()
    }
}
