package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for the Home screen "Restore" entry points.
 *
 * - The sidebar "Restore" row must open the Restore screen (it used to be a silent no-op because
 *   HomeScreen never passed the sidebar's onOpenRestore callback).
 * - The two buttons next to the balance box carry the hamburger icon, not the Restore icon, and they
 *   open the month's transaction history right there on the Home screen - under the balance bar -
 *   instead of navigating to a screen of its own.
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
        composeTestRule.onNodeWithTag("balance_history_left")
            .assert(hasClickAction())
            .performClick()
        revealTheHistory()

        assertTrue(
            "Clicking the left anchor reveals the month's transaction history under the balance " +
                "bar.",
            revealedHistoryHeight() > 0.dp,
        )
        assertStillOnHome()
    }

    @Test
    fun balanceBoxRightButtonOpensTransactionHistory() {
        composeTestRule.onNodeWithTag("balance_history_right")
            .assert(hasClickAction())
            .performClick()
        revealTheHistory()

        assertTrue(
            "Clicking the right anchor reveals the month's transaction history under the balance " +
                "bar.",
            revealedHistoryHeight() > 0.dp,
        )
        assertStillOnHome()
    }

    /** Lets the open motion of the balance bar finish. */
    private fun revealTheHistory() {
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()
    }

    private fun revealedHistoryHeight(): androidx.compose.ui.unit.Dp {
        val sheet = composeTestRule.onNodeWithTag("transaction_history_sheet").getBoundsInRoot()
        return sheet.bottom - sheet.top
    }

    /** The history opens in place: the Home screen stays on screen, no separate history route. */
    private fun assertStillOnHome() {
        composeTestRule.onNodeWithText("Monefy").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertDoesNotExist()
    }

    @Test
    fun balanceBoxSideButtonsUseHamburgerIconsAndNotRestoreIcons() {
        composeTestRule.onAllNodesWithContentDescription("Open transaction history")
            .assertCountEquals(2)
        // The Restore entry point now lives only in the sidebar, so the closed Home screen must
        // not expose a Restore button any more.
        composeTestRule.onNodeWithContentDescription("Open restore").assertDoesNotExist()
    }
}
