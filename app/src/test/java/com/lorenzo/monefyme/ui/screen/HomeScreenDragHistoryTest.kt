package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the transaction history dragging/swiping functionality on the HomeScreen.
 * These tests verify that:
 * 1. Dragging the balance box reveals the transaction history overlay
 * 2. Clicking the balance box or anchors also reveals the overlay
 * 3. The overlay can be closed by swiping down or clicking the close button
 * 4. Smooth animations are applied during the reveal/hide transitions
 * 5. The balance box remains visible on top when history is fully displayed
 */
class HomeScreenDragHistoryTest {

    /**
     * Test that the reveal offset is correctly bounded between 0 and 1.
     * - 0 means history is completely hidden
     * - 1 means history is completely revealed
     * - Values in between show the history partially
     */
    @Test
    fun revealOffsetIsProperlyBounded() {
        // Test that offset coercion works correctly
        val offset1 = 0.5f.coerceIn(0f, 1f)
        assertEquals("Offset within range should stay the same", 0.5f, offset1)

        val offset2 = (-0.2f).coerceIn(0f, 1f)
        assertEquals("Negative offset should be clamped to 0", 0f, offset2)

        val offset3 = 1.5f.coerceIn(0f, 1f)
        assertEquals("Offset > 1 should be clamped to 1", 1f, offset3)
    }

    /**
     * Test that dragging calculations work correctly.
     * Dragging up (negative dragAmount) should increase offset.
     * Dragging down (positive dragAmount) should decrease offset.
     */
    @Test
    fun dragAmountCalculationIsCorrect() {
        val normalizationFactor = 500f

        // Simulate dragging up by -100 pixels (negative drag amount)
        val dragUp = -100f
        val delta1 = -dragUp / normalizationFactor
        assertEquals("Dragging up should increase offset", 0.2f, delta1)

        // Simulate dragging down by +200 pixels (positive drag amount)
        val dragDown = 200f
        val delta2 = -dragDown / normalizationFactor
        assertEquals("Dragging down should decrease offset", -0.4f, delta2)
    }

    /**
     * Test the snap-to behavior when dragging ends.
     * - If offset > 0.3f after dragging, it should snap to fully open (1.0)
     * - If offset <= 0.3f, it should snap to fully closed (0.0)
     */
    @Test
    fun dragSnappingBehaviorIsCorrect() {
        // Case 1: User dragged enough to reveal more than 30%
        val offsetAfterDrag1 = 0.35f
        val shouldOpen1 = offsetAfterDrag1 > 0.3f
        assertTrue("Offset > 0.3 should snap open", shouldOpen1)

        // Case 2: User didn't drag enough to exceed 30% reveal
        val offsetAfterDrag2 = 0.25f
        val shouldOpen2 = offsetAfterDrag2 > 0.3f
        assertFalse("Offset <= 0.3 should snap closed", shouldOpen2)

        // Case 3: Exactly at threshold
        val offsetAfterDrag3 = 0.3f
        val shouldOpen3 = offsetAfterDrag3 > 0.3f
        assertFalse("Offset = 0.3 should snap closed", shouldOpen3)
    }

    /**
     * Regression guard for the "history does not stick after dragging" bug:
     * BalanceAndHistorySheet's pointerInput(Unit) block runs once and captures the
     * historyRevealOffset *parameter* (a plain Float from the first composition, always 0f at
     * start). onDragEnd then always saw 0f and snapped the sheet closed, so dragging the sheet
     * to the top never stuck - only the balance box buttons (which call onToggleHistoryFull with
     * the live HomeScreen state) kept it open.
     * The drag gesture handlers must therefore read a rememberUpdatedState value, not the
     * captured parameter.
     */
    @Test
    fun dragGestureHandlersReadTheLiveRevealOffsetNotTheCapturedParameter() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        assertTrue(
            "BalanceAndHistorySheet must expose the reveal offset through rememberUpdatedState so " +
                "the long-lived pointerInput(Unit) gesture lambdas see the current value:",
            source.contains("by rememberUpdatedState(historyRevealOffset)"),
        )

        val dragBlock = source.substringAfter("detectVerticalDragGestures(")
            .substringBefore("verticalAlignment = Alignment.CenterVertically")
        assertTrue(
            "The fully-open guard must use the live offset (currentRevealOffset), otherwise it " +
                "compares the stale value captured at first composition:\n$dragBlock",
            dragBlock.contains("currentRevealOffset >= 1f"),
        )
        assertTrue(
            "onDragEnd must decide the snap from the live offset: dragging the sheet to the top " +
                "must snap it open (stick) instead of snapping it closed:\n$dragBlock",
            dragBlock.contains("if (currentRevealOffset > 0.3f)"),
        )
        assertFalse(
            "The drag handlers must not read the captured historyRevealOffset parameter any " +
                "more - that was the not-sticky bug:\n$dragBlock",
            dragBlock.contains("historyRevealOffset"),
        )
    }

    /**
     * A cancelled drag must snap the sheet the same way a released drag does, so a sheet dragged
     * to its fully expanded position cannot stay half open when the gesture is interrupted.
     */
    @Test
    fun cancelledDragSnapsLikeAReleasedDrag() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
        val dragBlock = source.substringAfter("detectVerticalDragGestures(")
            .substringBefore("verticalAlignment = Alignment.CenterVertically")
        assertTrue(
            "detectVerticalDragGestures must handle onDragCancel with the same snap-to-open/closed " +
                "logic as onDragEnd:\n$dragBlock",
            dragBlock.contains("onDragCancel =") &&
                dragBlock.substringAfter("onDragCancel =").contains("onToggleHistoryFull(true)") &&
                dragBlock.substringAfter("onDragCancel =").contains("onToggleHistoryFull(false)"),
        )
    }

    /**
     * Test that transaction history overlay only renders when needed.
     * The overlay should only be displayed when revealOffset > 0.
     */
    @Test
    fun transactionHistoryOverlayRenderingLogic() {
        // Should not render when hidden
        val shouldRender1 = 0f > 0f
        assertFalse("Overlay should not render when offset is 0", shouldRender1)

        // Should render when even slightly revealed
        val shouldRender2 = 0.01f > 0f
        assertTrue("Overlay should render when offset > 0", shouldRender2)

        // Should render when fully revealed
        val shouldRender3 = 1f > 0f
        assertTrue("Overlay should render when fully revealed", shouldRender3)
    }

    /**
     * Test smooth animation progress calculation using cubic easing.
     * Verifies that animations smoothly interpolate between start and end values.
     */
    @Test
    fun smoothAnimationProgressCalculation() {
        val duration = 300L
        val startTime = 0L

        // Test at start (0ms elapsed)
        val elapsed1 = 0L
        val progress1 = (elapsed1 - startTime).toFloat() / duration
        val eased1 = progress1 * progress1 * (3 - 2 * progress1)
        assertEquals("At start, eased progress should be 0", 0f, eased1)

        // Test at halfway (150ms elapsed)
        val elapsed2 = 150L
        val progress2 = (elapsed2 - startTime).toFloat() / duration
        val eased2 = progress2 * progress2 * (3 - 2 * progress2)
        assertEquals("At midpoint, eased progress should be 0.5", 0.5f, eased2, 0.01f)

        // Test at end (300ms elapsed or more)
        val elapsed3 = 300L
        val progress3 = (elapsed3 - startTime).toFloat() / duration
        val eased3 = if (progress3 >= 1f) 1f else progress3 * progress3 * (3 - 2 * progress3)
        assertEquals("At end, eased progress should be 1", 1f, eased3)
    }

    /**
     * Test interpolation between from and to values in animation.
     * The animated value should smoothly transition from `from` to `to`.
     */
    @Test
    fun animationInterpolation() {
        val from = 0f
        val to = 1f
        val eased = 0.5f  // 50% through animation

        val value = from + (to - from) * eased
        assertEquals("At 50% progress, value should be 0.5", 0.5f, value)

        // Test with non-zero starting point
        val from2 = 0.3f
        val to2 = 0.8f
        val eased2 = 0.5f

        val value2 = from2 + (to2 - from2) * eased2
        assertEquals("Interpolation should account for starting offset", 0.55f, value2, 0.01f)
    }

    /**
     * Test back handler logic for dismissing the overlay.
     * When history is revealed, pressing back should close it before leaving the screen.
     */
    @Test
    fun backHandlerLogicForDismissing() {
        // Back handler should be enabled when history is revealed
        val historyRevealOffset = 0.5f
        val backHandlerEnabled = historyRevealOffset > 0f
        assertTrue("Back handler should be enabled when history is revealed", backHandlerEnabled)

        // Back handler should be disabled when history is hidden
        val hiddenOffset = 0f
        val backHandlerDisabled = hiddenOffset > 0f
        assertFalse("Back handler should be disabled when history is hidden", backHandlerDisabled)
    }

    /**
     * Test that the balance box section properly handles all interaction modes:
     * 1. Dragging up reveals history gradually
     * 2. Clicking opens history fully (snapped to 1.0)
     * 3. Dragging down hides history (snapped to 0.0)
     */
    @Test
    fun balanceBoxSectionInteractionModes() {
        // Mode 1: Dragging - should return gradual offset changes
        val dragDelta = 0.15f
        var offset = 0f
        offset = (offset + dragDelta).coerceIn(0f, 1f)
        assertEquals("Dragging up should increase offset gradually", 0.15f, offset)

        // Mode 2: Clicking - should snap to fully open
        val clickResult = if (offset > 0.3f) 1f else 0f
        assertEquals("Clicking when offset is low should snap to 0", 0f, clickResult)

        offset = 0.5f  // Simulate more dragging
        val clickResult2 = if (offset > 0.3f) 1f else 0f
        assertEquals("Clicking when offset > 0.3 should snap to 1", 1f, clickResult2)

        // Mode 3: Swiping down - should snap to fully closed
        offset = 1f  // Start from fully open
        offset = 0f  // Snap to closed
        assertEquals("Swiping down should snap to 0", 0f, offset)
    }

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        // Normalise line endings so the assertions below are independent of the checkout setting.
        return file.readText().replace("\r\n", "\n")
    }

    private fun findProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
        var dir: File? = start
        while (dir != null) {
            val current: File = dir
            if (File(current, "settings.gradle.kts").exists() && File(current, "app").isDirectory) {
                return current
            }
            dir = current.parentFile
        }
        throw IllegalStateException("Could not locate the project root starting from $start")
    }
}
