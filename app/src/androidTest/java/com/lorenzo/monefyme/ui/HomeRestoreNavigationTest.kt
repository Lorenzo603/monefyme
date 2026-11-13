package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
        // Index 0 is the off-screen adjacent page; index 1 is the current (displayed) page
        composeTestRule.onAllNodesWithTag("balance_history_left", useUnmergedTree = false)
            .get(1)
            .performClick()

        // Index 1 is the current page; index 0 is the off-screen adjacent page.
        composeTestRule.onAllNodesWithText("Transactions")
            .get(1)
            .assertIsDisplayed()
    }

    @Test
    fun balanceBoxRightButtonOpensTransactionHistory() {
        composeTestRule.waitForIdle()
        // Index 0 is the off-screen adjacent page; index 1 is the current (displayed) page
        composeTestRule.onAllNodesWithTag("balance_history_right", useUnmergedTree = false)
            .get(1)
            .performClick()

        // Index 1 is the current page; index 0 is the off-screen adjacent page.
        composeTestRule.onAllNodesWithText("Transactions")
            .get(1)
            .assertIsDisplayed()
    }

    @Test
    fun balanceBoxSideButtonsUseHamburgerIconsAndNotRestoreIcons() {
        // With beyondViewportPageCount = 1 the pager pre-renders one adjacent page, so we
        // get 2 buttons from the current page and 2 from the adjacent (off-screen) page = 4.
        composeTestRule.onAllNodesWithContentDescription("Open transaction history")
            .assertCountEquals(4)
        // The Restore entry point now lives only in the sidebar, so the closed Home screen must
        // not expose a Restore button any more.
        composeTestRule.onNodeWithContentDescription("Open restore").assertDoesNotExist()
    }
}
