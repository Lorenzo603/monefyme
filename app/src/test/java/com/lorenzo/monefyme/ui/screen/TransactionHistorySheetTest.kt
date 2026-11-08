package com.lorenzo.monefyme.ui.screen

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers dragging the balance box (and its two anchors) to bring the month's transaction history on
 * screen: dragging up reveals it little by little, a release rests it on the side it is closer to (a
 * flick finishes the swipe in its direction), clicking the box or an anchor runs the same motion in one
 * go - and again in reverse - and the bar with its anchors always stays visible on top of the history.
 *
 * The drag/click geometry and the open/close decisions are pure and asserted directly below. The
 * gesture itself needs an emulator and is exercised end-to-end by TransactionHistoryDragUiTest in
 * app/src/androidTest, while the wiring of the bar to those decisions is asserted by reading the
 * source file (the pattern used by HomeRestoreWiringTest and TransactionHistoryScreenTest).
 */
class TransactionHistorySheetTest {

    private val sheetSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionHistorySheet.kt")
    }

    // The area the bar travels through and the bar itself, as the composable measures them.
    private val areaHeightPx = 600f
    private val barHeightPx = 100f
    private val travelPx = 500f

    // --- the distance the bar travels is the distance the history is revealed in ---

    @Test
    fun travelIsTheHeightOfTheAreaMinusTheBar() {
        assertEquals(travelPx, balanceBarTravelPx(areaHeightPx, barHeightPx), 0.001f)
    }

    @Test
    fun travelIsNeverNegative() {
        assertEquals(0f, balanceBarTravelPx(areaHeightPx = 80f, barHeightPx = 120f), 0.001f)
        assertEquals(0f, balanceBarTravelPx(areaHeightPx = 0f, barHeightPx = 0f), 0.001f)
    }

    // --- dragging the bar up reveals the transaction history little by little ---

    @Test
    fun draggingTheBarUpRevealsTheHistoryLittleByLittle() {
        var progress = TRANSACTION_HISTORY_CLOSED
        // Four equal upward drags of a quarter of the travel each: 25%, 50%, 75%, 100%.
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { expectedProgress ->
            progress = historyProgressAfterDrag(progress, dragAmountYPx = -travelPx * 0.25f, travelPx = travelPx)
            assertEquals(expectedProgress, progress, 0.001f)
        }
    }

    @Test
    fun draggingTheBarDownHidesItLittleByLittle() {
        var progress = TRANSACTION_HISTORY_OPEN
        listOf(0.75f, 0.5f, 0.25f, 0.0f).forEach { fraction ->
            progress = historyProgressAfterDrag(progress, dragAmountYPx = travelPx * 0.25f, travelPx = travelPx)
            assertEquals(fraction, progress, 0.001f)
        }
    }

    @Test
    fun aDragNeverRevealsMoreThanTheWholeHistory() {
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterDrag(0.9f, -travelPx * 4f, travelPx), 0.001f)
        assertEquals(TRANSACTION_HISTORY_CLOSED, historyProgressAfterDrag(0.1f, travelPx * 4f, travelPx), 0.001f)
    }

    @Test
    fun aDragOfAnUnknownProgressStartsFromTheClosedToEndRange() {
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterDrag(-3f, -travelPx, travelPx), 0.001f)
        assertEquals(TRANSACTION_HISTORY_CLOSED, historyProgressAfterDrag(5f, travelPx, travelPx), 0.001f)
    }

    @Test
    fun aSwipeUpWithoutAnyRoomToRevealStillOpens() {
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterDrag(0f, -40f, travelPx = 0f), 0.001f)
        assertEquals(TRANSACTION_HISTORY_CLOSED, historyProgressAfterDrag(1f, 40f, travelPx = 0f), 0.001f)
        assertEquals(0.4f, historyProgressAfterDrag(0.4f, 0f, travelPx = 0f), 0.001f)
    }

    // --- releasing the bar: rest on the nearer side, keep going on a flick ---

    @Test
    fun aReleaseAtOrPastHalfOpensTheHistoryCompletely() {
        assertEquals(
            TRANSACTION_HISTORY_OPEN,
            historyProgressOnDragEnd(progress = 0.5f, velocityYPxPerSecond = 0f, minFlingVelocityPxPerSecond = 400f),
            0.001f,
        )
        assertEquals(
            TRANSACTION_HISTORY_OPEN,
            historyProgressOnDragEnd(progress = 0.62f, velocityYPxPerSecond = 0f, minFlingVelocityPxPerSecond = 400f),
            0.001f,
        )
    }

    @Test
    fun aReleaseBelowHalfClosesTheHistoryAgain() {
        assertEquals(
            TRANSACTION_HISTORY_CLOSED,
            historyProgressOnDragEnd(progress = 0.45f, velocityYPxPerSecond = 0f, minFlingVelocityPxPerSecond = 400f),
            0.001f,
        )
    }

    @Test
    fun aFlickUpOpensTheHistoryEvenFromBelowHalf() {
        assertEquals(
            TRANSACTION_HISTORY_OPEN,
            historyProgressOnDragEnd(progress = 0.2f, velocityYPxPerSecond = -900f, minFlingVelocityPxPerSecond = 400f),
            0.001f,
        )
    }

    @Test
    fun aFlickDownClosesTheHistoryEvenFromAboveHalf() {
        assertEquals(
            TRANSACTION_HISTORY_CLOSED,
            historyProgressOnDragEnd(progress = 0.8f, velocityYPxPerSecond = 900f, minFlingVelocityPxPerSecond = 400f),
            0.001f,
        )
    }

    @Test
    fun theFlingSpeedScalesWithHowFarTheBarTravels() {
        // A flick is a swipe fast enough to cover most of the travel distance in one second.
        assertEquals(travelPx * FLING_TRAVELS_PER_SECOND, flingVelocityThresholdPxPerSecond(travelPx), 0.001f)
        assertEquals(0f, flingVelocityThresholdPxPerSecond(0f), 0.001f)
        assertTrue(flingVelocityThresholdPxPerSecond(900f) > flingVelocityThresholdPxPerSecond(travelPx))
    }

    // --- clicking the balance box or one of its anchors does the same in one motion ---

    @Test
    fun clickingTheBarOrItsAnchorsOpensTheHistory() {
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterClick(TRANSACTION_HISTORY_CLOSED), 0.001f)
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterClick(0.3f), 0.001f)
        assertEquals(TRANSACTION_HISTORY_OPEN, historyProgressAfterClick(0.99f), 0.001f)
    }

    @Test
    fun clickingAgainClosesACompletelyShownHistory() {
        assertEquals(TRANSACTION_HISTORY_CLOSED, historyProgressAfterClick(TRANSACTION_HISTORY_OPEN), 0.001f)
        assertEquals(TRANSACTION_HISTORY_CLOSED, historyProgressAfterClick(2f), 0.001f)
    }

    // --- the bar and its anchors stay visible on top of the history ---

    @Test
    fun theBarStartsAtTheBottomOfTheAreaAndRidesUpToItsTop() {
        assertEquals(0f, balanceBarTranslationPx(TRANSACTION_HISTORY_CLOSED, travelPx), 0.001f)
        assertEquals(-travelPx, balanceBarTranslationPx(TRANSACTION_HISTORY_OPEN, travelPx), 0.001f)
    }

    @Test
    fun theHistoryIsRevealedByTheSameFractionTheBarMovesUp() {
        listOf(0f, 0.2f, 0.5f, 0.8f, 1f).forEach { progress ->
            assertEquals(progress * travelPx, historyRevealedHeightPx(progress, travelPx), 0.001f)
        }
        assertEquals(0f, historyRevealedHeightPx(-1f, travelPx), 0.001f)
        assertEquals(travelPx, historyRevealedHeightPx(4f, travelPx), 0.001f)
    }

    @Test
    fun theBarNeverLeavesTheAreaAndAlwaysSitsRightOnTopOfTheHistory() {
        var progress = 0f
        while (progress <= 1.0001f) {
            val translation = balanceBarTranslationPx(progress, travelPx)
            val revealed = historyRevealedHeightPx(progress, travelPx)

            val barTopY = areaHeightPx + translation - barHeightPx
            val barBottomY = areaHeightPx + translation
            val historyTopY = areaHeightPx - revealed

            assertTrue("Bar left the top of the area at progress $progress.", barTopY >= -0.001f)
            assertTrue("Bar left the area at progress $progress.", barBottomY <= areaHeightPx + 0.001f)
            assertTrue(
                "Whatever of the history is shown starts at the bottom edge of the bar at progress " +
                    "$progress.",
                historyTopY <= barBottomY + 0.001f,
            )
            assertEquals(
                "The history grows exactly with the space the bar gives up at progress $progress.",
                areaHeightPx - barBottomY,
                revealed,
                0.001f,
            )

            progress += 0.05f
        }
    }

    // --- the motion: what each interaction asks for, and how it gets there ---

    @Test
    fun theHistoryStartsHidden() {
        val motion = TransactionHistoryMotion()

        assertTrue(motion.isHidden)
        assertEquals(TRANSACTION_HISTORY_CLOSED, motion.targetProgress, 0.001f)
    }

    @Test
    fun draggingFollowsTheFingerThenRestsOnTheNearerSide() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = -travelPx * 0.25f, travelPx = travelPx)
        assertEquals("Half a short drag opens a quarter of the history.", 0.25f, motion.targetProgress, 0.001f)

        motion.onDrag(dragAmountYPx = -travelPx * 0.15f, travelPx = travelPx)
        motion.onDragEnd(velocityYPxPerSecond = 0f, travelPx = travelPx)
        assertEquals("Released below half, the history closes again.", TRANSACTION_HISTORY_CLOSED, motion.targetProgress, 0.001f)

        motion.onDrag(dragAmountYPx = -travelPx * 0.7f, travelPx = travelPx)
        motion.onDragEnd(velocityYPxPerSecond = 0f, travelPx = travelPx)
        assertEquals("Released past half, the history opens completely.", TRANSACTION_HISTORY_OPEN, motion.targetProgress, 0.001f)
    }

    @Test
    fun aSwipeUpKeepsOpeningTheHistoryAfterTheFingerLetsGo() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = -travelPx * 0.2f, travelPx = travelPx)
        motion.onDragEnd(velocityYPxPerSecond = -travelPx, travelPx = travelPx)

        assertEquals(TRANSACTION_HISTORY_OPEN, motion.targetProgress, 0.001f)
    }

    @Test
    fun aSwipeDownKeepsClosingTheHistoryAfterTheFingerLetsGo() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = travelPx * 0.6f, travelPx = travelPx)
        motion.onDragEnd(velocityYPxPerSecond = travelPx, travelPx = travelPx)

        assertEquals(TRANSACTION_HISTORY_CLOSED, motion.targetProgress, 0.001f)
        assertTrue(motion.isHidden)
    }

    @Test
    fun clickingOpensTheWholeHistoryAndClickingAgainClosesIt() {
        val motion = TransactionHistoryMotion()

        motion.onClick()
        assertEquals(TRANSACTION_HISTORY_OPEN, motion.targetProgress, 0.001f)

        motion.onClick()
        assertEquals(TRANSACTION_HISTORY_CLOSED, motion.targetProgress, 0.001f)
    }

    @Test
    fun hidingClosesTheHistoryWithTheSameMotionInReverse() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = -travelPx, travelPx = travelPx)
        motion.hide()

        assertEquals(TRANSACTION_HISTORY_CLOSED, motion.targetProgress, 0.001f)
        assertEquals(HistoryMotionDriver.SETTLE, motion.driver)
    }

    @Test
    fun whileDraggingTheHistoryTracksTheFingerExactly() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = -travelPx * 0.4f, travelPx = travelPx)

        assertTrue(
            "A drag must follow the finger without lagging behind it.",
            motionAnimationSpec(motion.driver) is SnapSpec<*>,
        )
    }

    @Test
    fun aReleaseSettlesSmoothlyWithASpring() {
        val motion = TransactionHistoryMotion()

        motion.onDrag(dragAmountYPx = -travelPx * 0.4f, travelPx = travelPx)
        motion.onDragEnd(velocityYPxPerSecond = 0f, travelPx = travelPx)

        assertTrue(motionAnimationSpec(motion.driver) is SpringSpec<*>)
    }

    @Test
    fun aClickRunsTheWholeDistanceInOneMotionOfTheSameDurationInBothDirections() {
        val motion = TransactionHistoryMotion()

        motion.onClick()
        val opening = motionAnimationSpec(motion.driver) as TweenSpec<*>

        motion.onClick()
        val closing = motionAnimationSpec(motion.driver) as TweenSpec<*>

        assertEquals(TRANSACTION_HISTORY_CLICK_DURATION_MS, opening.durationMillis)
        assertEquals("Closing must take exactly as long as opening.", opening.durationMillis, closing.durationMillis)
    }

    @Test
    fun theReleaseSpeedIsHandedToTheMotionAsAFractionOfTheTravelDistance() {
        assertEquals(2f, dragVelocityFraction(velocityYPxPerSecond = travelPx * 2f, travelPx = travelPx), 0.001f)
        assertEquals(-0.5f, dragVelocityFraction(velocityYPxPerSecond = -travelPx * 0.5f, travelPx = travelPx), 0.001f)
        assertEquals(-MAX_VELOCITY_FRACTION_PER_SECOND, dragVelocityFraction(-travelPx * 100f, travelPx), 0.001f)
        assertEquals(MAX_VELOCITY_FRACTION_PER_SECOND, dragVelocityFraction(travelPx * 100f, travelPx), 0.001f)
        assertEquals(0f, dragVelocityFraction(-900f, travelPx = 0f), 0.001f)
    }

    @Test
    fun aPressInsideTheTouchSlopIsAClickAndBeyondItIsADrag() {
        assertFalse("A press that never left the slop is a click.", isHistoryDrag(totalDragXPx = 0f, totalDragYPx = 8f, touchSlopPx = 16f))
        assertFalse("The same holds for a press that moved up.", isHistoryDrag(totalDragXPx = 0f, totalDragYPx = -8f, touchSlopPx = 16f))
        assertTrue("Past the slop the press drags the history.", isHistoryDrag(totalDragXPx = 0f, totalDragYPx = -17f, touchSlopPx = 16f))
        assertTrue("And the same downwards.", isHistoryDrag(totalDragXPx = 0f, totalDragYPx = 17f, touchSlopPx = 16f))
    }

    @Test
    fun aSidewaysPressOfTheBarBelongsToTheMonthPagerNotTheHistory() {
        // Dragging the balance box or its anchors left or right changes the month; it must not be
        // taken as a drag of the transaction history.
        assertFalse(isHistoryDrag(totalDragXPx = 60f, totalDragYPx = 5f, touchSlopPx = 16f))
        assertFalse(isHistoryDrag(totalDragXPx = -60f, totalDragYPx = -5f, touchSlopPx = 16f))
        assertTrue("A press that moved as much sideways as up still drags the history.", isHistoryDrag(totalDragXPx = 30f, totalDragYPx = -30f, touchSlopPx = 16f))
        assertTrue("A sideways press that moved even further up is a history drag.", isHistoryDrag(totalDragXPx = 30f, totalDragYPx = -40f, touchSlopPx = 16f))
    }

    // --- wiring of the bar, its anchors and the history they drag open ---

    @Test
    fun theBarRowDragsTheHistoryOpenAndClosed() {
        val barRow = balanceBarRowSource()

        assertTrue("The bar row must listen for a drag of the bar and its anchors.", barRow.contains("awaitEachGesture"))
        assertTrue("Past the touch slop the press drives the history.", barRow.contains("onHistoryDrag(dragAmount.y)"))
        assertTrue("Releasing it decides where the history rests.", barRow.contains("onHistoryDragEnd(velocityYPxPerSecond)"))
        assertTrue("A press inside the touch slop stays a click.", barRow.contains("isHistoryDrag(totalDragXPx, totalDragYPx, touchSlopPx)"))
    }

    @Test
    fun draggingTheBarCancelsItsClickSoOneGestureIsNotBoth() {
        val barRow = balanceBarRowSource()

        assertTrue(
            "The drag must consume the pointer changes, otherwise the same gesture would also fire " +
                "the click of the bar or of an anchor.",
            barRow.contains("change.consume()"),
        )
    }

    @Test
    fun theBarAndItsAnchorsToggleTheHistoryOnClick() {
        val barRow = balanceBarRowSource()

        for (tag in listOf("balance_history_left", "balance_history_right")) {
            assertTrue("The anchor '$tag' must exist.", barRow.contains(".testTag(\"$tag\")"))
        }
        assertEquals("Both anchors must toggle the history.", 2, occurrences(barRow, "onClick = onToggleHistory"))
        assertTrue("Clicking the balance box itself must toggle the history too.", occurrences(barRow, "onToggleHistory()") >= 1)
        assertTrue(
            "The balance box must be addressable as the click target that toggles the history.",
            barRow.contains(".testTag(\"balance_box\")"),
        )
        assertFalse("The bar and its anchors must not open the Restore screen.", barRow.contains("onOpenRestore"))
    }

    @Test
    fun theBarRidesUpThroughTheAreaOnItsOwnTranslation() {
        val barRow = balanceBarRowSource()

        assertTrue(
            "The bar must really move by the translation the progress computes for it: a graphicsLayer " +
                "translation only transforms the drawing and leaves the layout - and with it the bar's " +
                "own bounds and touch target - where it was.",
            barRow.contains("offset { IntOffset(x = 0, y = translationYPx.roundToInt()) }"),
        )
        assertTrue(
            "The bar must sit flush at the bottom of the area so its bottom edge is exactly where the " +
                "history starts.",
            !barRow.contains(".padding(top ="),
        )
    }

    @Test
    fun theHistoryIsRevealedUnderTheBarWhichIsDrawnOnTopOfIt() {
        val barBody = balanceBarComposableBody()

        val historyIndex = barBody.indexOf(".testTag(\"transaction_history_sheet\")")
        val barIndex = barBody.indexOf("BalanceBarRow(")
        assertTrue("The month's transaction history must be rendered by the bar.", historyIndex >= 0)
        assertTrue(
            "The bar must be declared after the history so it is drawn on top of it and stays visible " +
                "when the history is completely shown.",
            historyIndex in 0 until barIndex,
        )
        assertTrue("The history fills the area from its bottom edge up.", barBody.contains(".align(Alignment.BottomCenter)"))
        assertTrue("Its height follows the progress of the drag.", barBody.contains("historyRevealedHeightPx(progress, travelPx)"))
        assertTrue("The bar rides up by the same fraction.", barBody.contains("balanceBarTranslationPx(progress, travelPx)"))
    }

    @Test
    fun theBarIsWiredToTheMotionThatBothDragAndClickDrive() {
        val barBody = balanceBarComposableBody()

        assertTrue(barBody.contains("motion.animatedProgress()"))
        assertTrue(barBody.contains("motion.onDrag(dragAmountYPx, travelPx)"))
        assertTrue(barBody.contains("motion.onDragEnd(velocityYPxPerSecond, travelPx)"))
        assertTrue(barBody.contains("onToggleHistory = motion::onClick"))
    }

    private fun balanceBarRowSource(): String {
        val start = sheetSource.indexOf("private fun BalanceBarRow(")
        assertTrue("TransactionHistorySheet must have a BalanceBarRow.", start >= 0)
        return sheetSource.substring(start)
    }

    private fun balanceBarComposableBody(): String {
        val start = sheetSource.indexOf("fun TransactionHistoryBalanceBar(")
        val end = sheetSource.indexOf("private fun BalanceBarRow(", start)
        assertTrue("TransactionHistorySheet must have a TransactionHistoryBalanceBar.", start >= 0 && end > start)
        return sheetSource.substring(start, end)
    }

    private fun occurrences(text: String, needle: String): Int {
        var index = text.indexOf(needle)
        var count = 0
        while (index >= 0) {
            count++
            index = text.indexOf(needle, index + needle.length)
        }
        return count
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
