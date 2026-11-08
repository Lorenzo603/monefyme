package com.lorenzo.monefyme.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.lorenzo.monefyme.MainActivity
import com.lorenzo.monefyme.ui.util.formatMonth
import java.time.YearMonth
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end guard for the month pager the balance bar now lies on top of.
 *
 * The month's transaction history is dragged out of the balance bar on the Home screen itself, so the
 * bar and its anchors sit over the month pager: a sideways drag of the bar must still be handed to that
 * pager - the month swipe users had before - while opening the transaction history from that month
 * leaves the month exactly where it is.
 *
 * Which month HomeScreen shows is read off the month ticker: the label sitting at the ticker's
 * horizontal centre is the selected month.
 *
 * Requires a device/emulator: ./gradlew connectedDebugAndroidTest
 */
class BalanceBarMonthSwipeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun draggingTheBalanceBarSidewaysSwipesTheMonthAndOpeningTheHistoryLeavesItAlone() {
        composeTestRule.waitForIdle()

        val monthAtStart = centredMonthLabel()
        val neighbourMonth = leftMonthLabel()
        assertEquals(
            "Home opens on the current month, so its ticker must centre that one.",
            monthLabel(YearMonth.now()),
            monthAtStart,
        )
        assertEquals(
            "The month drawn to the left of the centred one is the previous month.",
            monthLabel(YearMonth.now().minusMonths(1)),
            neighbourMonth,
        )

        // A sideways drag of the balance bar belongs to the month pager underneath it.
        composeTestRule.onNodeWithTag("balance_bar").performTouchInput { swipeRight() }
        settle()

        val monthAfterTheSwipe = centredMonthLabel()
        assertNotEquals(
            "Dragging the balance bar sideways must move the month pager it lies on top of " +
                "($monthAtStart -> $monthAfterTheSwipe).",
            monthAtStart,
            monthAfterTheSwipe,
        )
        assertTrue(
            "A month swipe may not reveal the transaction history (revealed " +
                "${revealedHistoryHeightPx()}px).",
            revealedHistoryHeightPx() < 1f,
        )

        // Opening the transaction history from that month leaves the month where it is.
        composeTestRule.onNodeWithTag("balance_box").performClick()
        settle()

        assertEquals(
            "Opening the month's transaction history must not change the month " +
                "($monthAfterTheSwipe -> ${centredMonthLabel()}).",
            monthAfterTheSwipe,
            centredMonthLabel(),
        )
        assertTrue(
            "The transaction history is on screen under the balance bar (revealed " +
                "${revealedHistoryHeightPx()}px).",
            revealedHistoryHeightPx() > 1f,
        )
    }

    /** Lets the pager fling and the open motion of the balance bar finish. */
    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(1_500L)
        composeTestRule.waitForIdle()
    }

    private fun monthLabel(month: YearMonth): String = formatMonth(month).substringBefore(' ')

    /** The label at the horizontal centre of the month ticker: the month HomeScreen selected. */
    private fun centredMonthLabel(): String {
        val centreX = (tickerBounds().left + tickerBounds().right) / 2f
        val labels = tickerLabels()
        assertTrue("The month ticker must draw its month labels.", labels.isNotEmpty())
        return labels.minByOrNull { abs(it.centerX - centreX) }!!.text
    }

    /** The label drawn just to the left of the centred one, which is the neighbouring month. */
    private fun leftMonthLabel(): String {
        val centred = centredMonthLabel()
        val centredX = tickerLabels().first { it.text == centred }.centerX
        return tickerLabels()
            .filter { it.centerX < centredX - 1f }
            .maxByOrNull { it.centerX }
            ?.text
            ?: throw AssertionError("The month ticker draws no label to the left of '$centred'.")
    }

    /** Every month label of the ticker, ordered by how far right it is drawn. */
    private fun tickerLabels(): List<MonthLabel> {
        val ticker = tickerBounds()
        return composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val text = node.textOrNull() ?: return@mapNotNull null
                val bounds = node.boundsInRoot
                // Only the ticker's own labels: every other text of the Home screen is drawn elsewhere.
                if (bounds.top < ticker.top - 1f || bounds.bottom > ticker.bottom + 1f) return@mapNotNull null
                // A label the ticker clipped away has no width left to centre.
                if (bounds.right <= bounds.left) return@mapNotNull null
                MonthLabel(text = text, centerX = (bounds.left + bounds.right) / 2f)
            }
            .sortedBy { it.centerX }
    }

    private fun tickerBounds() =
        composeTestRule.onNodeWithTag("month_ticker").fetchSemanticsNode().boundsInRoot

    /** How much of the transaction history is on screen, under the balance bar, in pixels. */
    private fun revealedHistoryHeightPx(): Float {
        val sheet = composeTestRule.onNodeWithTag("transaction_history_sheet").fetchSemanticsNode()
        return sheet.boundsInRoot.bottom - sheet.boundsInRoot.top
    }

    /** The text a node draws, which the semantics tree hands over as a list of annotated strings. */
    private fun SemanticsNode.textOrNull(): String? =
        (config[SemanticsProperties.Text] as? List<*>)?.firstOrNull()?.toString()

    private class MonthLabel(val text: String, val centerX: Float)
}
