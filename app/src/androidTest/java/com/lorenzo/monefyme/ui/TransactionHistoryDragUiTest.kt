package com.lorenzo.monefyme.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.ui.screen.TransactionHistoryBalanceBar
import com.lorenzo.monefyme.ui.screen.rememberTransactionHistoryMotion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * End-to-end coverage for dragging the balance bar to bring the month's transaction history on
 * screen, in the real Compose runtime (the pure open/close decisions are covered by
 * TransactionHistorySheetTest, which needs no device).
 *
 * - Dragging the bar - or either of its anchors - up reveals the history, and a drag that ends past
 *   the middle of its travel shows it completely.
 * - The bar and its anchors stay on screen while that happens and end up on top of the list, which
 *   fills the space underneath them.
 * - Dragging it back down closes the history again.
 * - Clicking the balance box runs the same motion in one go - seen here in the small steps it takes
 *   to get fully shown - and clicking it again runs it in reverse.
 * - Dragging the bar or an anchor sideways is not a history gesture at all: it belongs to the month
 *   pager underneath the bar (BalanceBarMonthSwipeTest shows the Home screen month really changing
 *   that way).
 *
 * Requires a device/emulator: ./gradlew connectedDebugAndroidTest
 */
class TransactionHistoryDragUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The area the bar travels through; the bar measures it exactly like HomeScreen does.
    private val areaWidthDp = 380.dp
    private val areaHeightDp = 520.dp

    private val density = floatArrayOf(0f)
    /** Height of the area minus height of the bar: how far the bar travels, and how far the history
     * can be revealed at most. Measured once, while the bar still rests at the bottom of the area. */
    private val travelPx = floatArrayOf(0f)

    /** Top edge of the bar while it rests at the bottom of the area. */
    private var barTopAtRestPx = 0f

    private val categories = listOf(
        CategoryItem(id = 1L, displayName = "Food", iconName = "Restaurant", colorArgb = null),
        CategoryItem(id = 2L, displayName = "Transport", iconName = "LocalTaxi", colorArgb = null),
    )

    private val transactions = listOf(
        transaction(id = 11L, categoryId = 1L, categoryName = "Food", cents = 1000L, day = 15),
        transaction(id = 12L, categoryId = 1L, categoryName = "Food", cents = 2000L, day = 16),
        transaction(id = 13L, categoryId = 2L, categoryName = "Transport", cents = 500L, day = 17),
        transaction(id = 14L, categoryId = 2L, categoryName = "Transport", cents = 700L, day = 18),
    )

    private fun transaction(id: Long, categoryId: Long, categoryName: String, cents: Long, day: Int) =
        TransactionListItem(
            id = id,
            dateEpochDay = LocalDate.of(2024, 1, day).toEpochDay(),
            categoryId = categoryId,
            categoryName = categoryName,
            amountCents = cents,
            type = "EXPENSE",
            description = "item $id",
            account = "Cash",
            currency = "EUR",
        )

    private fun showBarInItsArea() {
        composeTestRule.setContent {
            density[0] = LocalDensity.current.density
            Box(
                modifier = Modifier
                    .size(width = areaWidthDp, height = areaHeightDp)
                    .testTag("transaction_history_area"),
            ) {
                // The bar is handed the area height exactly like HomeScreen does: read out of the
                // area's own layout constraints, so it is the height the area really has.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val areaHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                    val motion = rememberTransactionHistoryMotion()
                    TransactionHistoryBalanceBar(
                        motion = motion,
                        areaHeightPx = areaHeightPx,
                        balanceCents = -12_345L,
                        categoriesMap = categories.associateBy { it.id },
                        transactions = transactions,
                        onTransactionClick = {},
                    )
                }
            }
        }
        settle()

        val area = boundsOf("transaction_history_area")
        val bar = boundsOf("balance_bar")
        travelPx[0] = area.height - bar.height
        barTopAtRestPx = bar.top
        assertTrue(
            "The bar must have room to travel through the area (area height ${area.height}, bar " +
                "height ${bar.height}).",
            travelPx[0] > 100f,
        )
    }

    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(1_500L)
        composeTestRule.waitForIdle()
    }

    /**
     * Drags the balance bar (or one of its anchors) by [fraction] of its travel distance in one
     * press-move-release gesture: a long [holdDurationMillis] is a slow drag with no flick, a short
     * one is a swipe that keeps travelling after the finger lets go. Coordinates are the pixels the
     * injection works in, the same unit the travel distance is measured in.
     */
    private fun drag(tag: String, fraction: Float, upwards: Boolean, holdDurationMillis: Long) {
        val distance = travelPx[0] * fraction
        composeTestRule.onNodeWithTag(tag).performTouchInput {
            val startY = center.y
            val endY = if (upwards) startY - distance else startY + distance
            if (upwards) {
                swipeUp(startY = startY, endY = endY, durationMillis = holdDurationMillis)
            } else {
                swipeDown(startY = startY, endY = endY, durationMillis = holdDurationMillis)
            }
        }
        settle()
    }

    /** Drags the balance bar (or one of its anchors) sideways, which is not a history gesture. */
    private fun dragSideways(tag: String, distancePx: Float, rightwards: Boolean, holdDurationMillis: Long) {
        composeTestRule.onNodeWithTag(tag).performTouchInput {
            val startX = center.x
            val endX = if (rightwards) startX + distancePx else startX - distancePx
            if (rightwards) {
                swipeRight(startX = startX, endX = endX, durationMillis = holdDurationMillis)
            } else {
                swipeLeft(startX = startX, endX = endX, durationMillis = holdDurationMillis)
            }
        }
        settle()
    }

    private fun boundsOf(tag: String): PxRect {
        val bounds = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
        return PxRect(
            left = bounds.left.toPxOf(),
            top = bounds.top.toPxOf(),
            right = bounds.right.toPxOf(),
            bottom = bounds.bottom.toPxOf(),
        )
    }

    private fun areaBounds() = boundsOf("transaction_history_area")
    private fun barBounds() = boundsOf("balance_bar")
    private fun sheetHeightPx() = boundsOf("transaction_history_sheet").height

    // --- dragging the bar up shows the transaction history, with the bar on top of it ---

    @Test
    fun draggingTheBarUpShowsTheTransactionHistoryWithTheBarOnTopOfIt() {
        showBarInItsArea()

        assertTrue("Nothing of the history is shown before dragging.", sheetHeightPx() < 1f)
        composeTestRule.onNodeWithTag("group_header_Food").assertIsNotDisplayed()

        drag(tag = "balance_bar", fraction = 1.05f, upwards = true, holdDurationMillis = 60L)

        val area = areaBounds()
        val bar = barBounds()
        val revealed = sheetHeightPx()

        assertTrue(
            "Dragging the bar all the way up shows the whole history (revealed $revealed of " +
                "${travelPx[0]}).",
            revealed >= travelPx[0] * 0.9f,
        )
        assertTrue(
            "The bar rode up through the area (from $barTopAtRestPx to ${bar.top}).",
            bar.top < barTopAtRestPx - travelPx[0] * 0.5f,
        )
        assertTrue("The bar stays in the area: top ${bar.top}, area top ${area.top}.", bar.top >= area.top - 5f)
        assertTrue("The bar stays in the area: bottom ${bar.bottom}, area bottom ${area.bottom}.", bar.bottom <= area.bottom + 5f)
        assertTrue(
            "The history is revealed under the bar: bar bottom ${bar.bottom}, history top ${boundsOf("transaction_history_sheet").top}.",
            boundsOf("transaction_history_sheet").top <= bar.bottom + 5f,
        )
        assertTrue(
            "The history fills the area under the bar: history bottom " +
                "${boundsOf("transaction_history_sheet").bottom}, area bottom ${area.bottom}.",
            area.bottom - boundsOf("transaction_history_sheet").bottom < 5f,
        )

        composeTestRule.onNodeWithTag("balance_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_history_left").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_history_right").assertIsDisplayed()
        composeTestRule.onNodeWithTag("group_header_Food").assertIsDisplayed()
        composeTestRule.onNodeWithTag("group_header_Transport").assertIsDisplayed()
    }

    @Test
    fun draggingTheBarBackDownClosesTheTransactionHistoryAgain() {
        showBarInItsArea()

        drag(tag = "balance_bar", fraction = 1.05f, upwards = true, holdDurationMillis = 60L)
        assertTrue(sheetHeightPx() >= travelPx[0] * 0.9f)

        drag(tag = "balance_bar", fraction = 1.05f, upwards = false, holdDurationMillis = 60L)

        val area = areaBounds()
        val bar = barBounds()
        assertTrue("The history is closed again (revealed ${sheetHeightPx()}).", sheetHeightPx() < 1f)
        composeTestRule.onNodeWithTag("group_header_Food").assertIsNotDisplayed()
        assertTrue(
            "The bar rests back at the bottom of the area: bar bottom ${bar.bottom}, area bottom " +
                "${area.bottom}.",
            area.bottom - bar.bottom < 5f,
        )
        composeTestRule.onNodeWithTag("balance_history_left").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_history_right").assertIsDisplayed()
    }

    @Test
    fun aDragPastHalfItsTravelShowsTheWholeHistory() {
        showBarInItsArea()

        drag(tag = "balance_bar", fraction = 0.7f, upwards = true, holdDurationMillis = 1_200L)

        assertTrue(
            "Released past half the travel, the history opens completely (revealed " +
                "${sheetHeightPx()} of ${travelPx[0]}).",
            sheetHeightPx() >= travelPx[0] * 0.9f,
        )
    }

    @Test
    fun aDragBelowHalfItsTravelClosesTheHistoryAgain() {
        showBarInItsArea()

        drag(tag = "balance_bar", fraction = 0.3f, upwards = true, holdDurationMillis = 1_200L)

        assertTrue(
            "Released below half the travel, the history closes again (revealed ${sheetHeightPx()}).",
            sheetHeightPx() < 1f,
        )
    }

    @Test
    fun draggingAnAnchorUpRevealsTheHistoryJustLikeTheBar() {
        showBarInItsArea()

        drag(tag = "balance_history_left", fraction = 1.05f, upwards = true, holdDurationMillis = 60L)

        val bar = barBounds()
        assertTrue(
            "Dragging the left anchor reveals the history (revealed ${sheetHeightPx()} of " +
                "${travelPx[0]}).",
            sheetHeightPx() >= travelPx[0] * 0.9f,
        )
        assertTrue(
            "The bar and its anchors ride up with the history (bar top ${bar.top}, at rest " +
                "$barTopAtRestPx).",
            bar.top < barTopAtRestPx - travelPx[0] * 0.5f,
        )
    }

    // --- clicking the balance box runs the same motion in one go, and in reverse ---

    @Test
    fun clickingTheBalanceBoxRevealsTheHistoryLittleByLittleAndClickingAgainClosesItInReverse() {
        showBarInItsArea()

        // Frames are stepped by hand so the opening motion can be watched as it travels.
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag("balance_box").performClick()

            // 180ms of samples out of a motion that takes 320ms: the history must be on its way.
            val opening = sampleSheetHeightsAfter(stepCount = 6, stepMillis = 30L)
            assertOpeningLittleByLittle(opening)
            assertTrue(
                "The click motion is still on its way ($opening) - it must not jump to the end.",
                opening.last() < travelPx[0] * 0.9f,
            )

            composeTestRule.mainClock.advanceTimeBy(1_000L)
            val openHeight = sheetHeightPx()
            assertTrue(
                "One click shows the whole history (revealed $openHeight of ${travelPx[0]}).",
                openHeight >= travelPx[0] * 0.9f,
            )

            composeTestRule.onNodeWithTag("balance_box").performClick()

            val closing = sampleSheetHeightsAfter(stepCount = 6, stepMillis = 30L)
            assertClosingLittleByLittle(closing)

            composeTestRule.mainClock.advanceTimeBy(1_000L)
            val closedHeight = sheetHeightPx()
            assertTrue("Clicking again closes the history again (revealed $closedHeight).", closedHeight < 1f)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun draggingTheBarOrItsAnchorsSidewaysDoesNotOpenTheTransactionHistory() {
        showBarInItsArea()

        dragSideways(tag = "balance_box", distancePx = areaWidthDp.value * density[0] * 0.4f, rightwards = true, holdDurationMillis = 250L)
        assertTrue(
            "Dragging the balance box left or right belongs to the month pager, not to the history " +
                "(revealed ${sheetHeightPx()}).",
            sheetHeightPx() < 1f,
        )

        dragSideways(tag = "balance_history_left", distancePx = areaWidthDp.value * density[0] * 0.4f, rightwards = true, holdDurationMillis = 250L)
        assertTrue(
            "The same holds for an anchor (revealed ${sheetHeightPx()}).",
            sheetHeightPx() < 1f,
        )
        composeTestRule.onNodeWithTag("group_header_Food").assertIsNotDisplayed()
    }

    @Test
    fun clickingEitherAnchorOpensTheHistoryAndClickingTheOtherClosesIt() {
        showBarInItsArea()

        composeTestRule.onNodeWithTag("balance_history_left").performClick()
        settle()
        assertTrue(
            "Clicking the left anchor shows the whole history (revealed ${sheetHeightPx()} of " +
                "${travelPx[0]}).",
            sheetHeightPx() >= travelPx[0] * 0.9f,
        )

        composeTestRule.onNodeWithTag("balance_history_right").performClick()
        settle()
        assertTrue(
            "Clicking the right anchor again closes it (revealed ${sheetHeightPx()}).",
            sheetHeightPx() < 1f,
        )
    }

    private fun sampleSheetHeightsAfter(stepCount: Int, stepMillis: Long): List<Float> =
        List(stepCount) {
            composeTestRule.mainClock.advanceTimeBy(stepMillis)
            sheetHeightPx()
        }

    /**
     * The samples of an opening motion: never shrinking, and far enough along to prove the history is
     * being revealed step by step instead of appearing at once.
     */
    private fun assertOpeningLittleByLittle(heights: List<Float>) {
        heights.forEachIndexed { index, height ->
            if (index > 0) {
                assertTrue(
                    "The history must be revealed little by little, but step $index ($height) shrank " +
                        "on step ${index - 1} (${heights[index - 1]}).",
                    height >= heights[index - 1] - 1f,
                )
            }
        }
        assertTrue(
            "The history must be on its way up during the motion, but it barely moved ($heights).",
            heights.last() > heights.first() + travelPx[0] * 0.2f,
        )
    }

    /** The samples of a closing motion: the same motion in the other direction. */
    private fun assertClosingLittleByLittle(heights: List<Float>) {
        heights.forEachIndexed { index, height ->
            if (index > 0) {
                assertTrue(
                    "The history must close little by little, but step $index ($height) grew on step " +
                        "${index - 1} (${heights[index - 1]}).",
                    height <= heights[index - 1] + 1f,
                )
            }
        }
        assertTrue(
            "Closing must run the same motion in reverse, but it barely moved ($heights).",
            heights.last() < heights.first() - travelPx[0] * 0.2f,
        )
    }

    private fun androidx.compose.ui.unit.Dp.toPxOf(): Float = this.value * density[0]

    /** Node bounds in pixels, which is what the drag distances and travel are measured in. */
    private class PxRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val height: Float get() = bottom - top
        val width: Float get() = right - left
    }
}
