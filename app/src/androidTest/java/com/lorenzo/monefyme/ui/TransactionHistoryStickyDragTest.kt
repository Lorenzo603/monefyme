package com.lorenzo.monefyme.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end coverage for the sticky transaction history sheet on the Home screen.
 *
 * Dragging the balance box upwards must reveal the history sheet and, once it reaches its fully
 * expanded position, it must STICK there until the user drags it down again. (The original bug:
 * the drag handlers read a reveal-offset value captured at the first composition, so onDragEnd
 * always snapped the sheet closed - only the balance box buttons kept it open.)
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class TransactionHistoryStickyDragTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val density by lazy {
        composeTestRule.activity.resources.displayMetrics.density
    }

    /** Drags the balance box [distanceDp] in [dy] direction (negative = up) and releases. */
    private fun dragBalanceBox(distanceDp: Float, dy: Float) {
        // Index 0 is the off-screen adjacent page; index 1 is the current (displayed) page
        composeTestRule.onAllNodesWithTag("balance_history_left", useUnmergedTree = false)
            .get(1)
            .performTouchInput {
                swipe(
                    start = center,
                    end = Offset(center.x, center.y + dy * distanceDp * density),
                    durationMillis = 400,
                )
            }
        composeTestRule.waitForIdle()
        // The snap animation runs on wall-clock time; let it finish before asserting.
        Thread.sleep(600)
        composeTestRule.waitForIdle()
    }

    private fun dragUp() = dragBalanceBox(distanceDp = 400f, dy = -1f)

    @Test
    fun draggingHistoryToTheTopKeepsItStuckFullyExpanded() {
        composeTestRule.onNodeWithTag("history_sheet").assertDoesNotExist()

        // Drag up far enough to push the sheet to its fully expanded position, then release.
        dragUp()

        // The sheet stuck at its fully expanded position: it keeps (almost) the full 500.dp height.
        // Index 1 is the current page; index 0 is the off-screen adjacent page.
        composeTestRule.onAllNodesWithTag("history_sheet")
            .get(1)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(450.dp)
    }

    @Test
    fun draggedOpenHistoryStaysOpenUntilDraggedDownAgain() {
        composeTestRule.onNodeWithTag("history_sheet").assertDoesNotExist()

        // Drag up: the sheet sticks fully open.
        dragUp()
        // Index 1 is the current page; index 0 is the off-screen adjacent page.
        composeTestRule.onAllNodesWithTag("history_sheet")
            .get(1)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(450.dp)

        // Drag down again: only then does the sheet close.
        dragBalanceBox(distanceDp = 400f, dy = 1f)

        composeTestRule.onNodeWithTag("history_sheet").assertDoesNotExist()
    }
}
