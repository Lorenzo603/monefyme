package com.lorenzo.monefyme.ui.screen

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.ui.util.currencyAnnotatedString
import com.lorenzo.monefyme.ui.util.centsToDisplay
import kotlin.math.abs
import kotlin.math.roundToInt

// --- how much of the month's transaction history is shown: 0 = hidden, 1 = completely shown ---

/** The transaction history is hidden: only the balance bar and its anchors are on screen. */
internal const val TRANSACTION_HISTORY_CLOSED = 0f

/** The transaction history is completely shown, under the balance bar. */
internal const val TRANSACTION_HISTORY_OPEN = 1f

/**
 * A drag released without a flick rests on the side it is closer to: at or above this fraction the
 * history opens completely, below it it closes again.
 */
internal const val TRANSACTION_HISTORY_SNAP_FRACTION = 0.5f

/** Duration of the smooth open/close motion a click on the bar or its anchors drives. */
internal const val TRANSACTION_HISTORY_CLICK_DURATION_MS = 320

/**
 * A released drag keeps going on its own (a flick) when the finger was moving fast enough to cover
 * this many times the whole travel distance in one second.
 */
internal const val FLING_TRAVELS_PER_SECOND = 0.75f

/** How fast the history settles after a drag: an underdamped spring, so it glides to its resting side. */
internal const val HISTORY_SPRING_DAMPING_RATIO = 0.55f
internal const val HISTORY_SPRING_STIFFNESS = 900f

/** Cap of the finger speed handed to the settle motion, in fractions of the travel distance per second. */
internal const val MAX_VELOCITY_FRACTION_PER_SECOND = 4f

/**
 * What last moved the transaction history, which decides how the next motion runs: while the finger
 * drags the bar the history follows it exactly; after a release it settles with a spring; after a click
 * it travels the whole distance in one fixed-time motion.
 */
internal enum class HistoryMotionDriver { DRAG, SETTLE, CLICK }

/**
 * The height the balance bar travels up through, which is exactly the height the transaction history
 * is revealed in: the history fills the space the bar vacates, from just under the bar down to the
 * bottom of the [areaHeightPx] tall area it lives in. The bar never leaves that area.
 */
internal fun balanceBarTravelPx(areaHeightPx: Float, barHeightPx: Float): Float =
    (areaHeightPx - barHeightPx).coerceAtLeast(0f)

/**
 * Progress of the transaction history after a drag of the balance bar.
 *
 * [dragAmountYPx] is the vertical movement Compose reports for the finger (positive = the finger moved
 * down), so dragging the bar up (negative) opens the history and dragging it down closes it.
 * [travelPx] is the height the bar travels - and therefore the drag distance that maps onto the whole
 * closed..open range - and the result never leaves that range.
 */
internal fun historyProgressAfterDrag(
    currentProgress: Float,
    dragAmountYPx: Float,
    travelPx: Float,
): Float {
    val progress = currentProgress.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN)
    if (travelPx <= 0f) {
        // No room to reveal the history in: the direction of the drag alone decides.
        return when {
            dragAmountYPx < 0f -> TRANSACTION_HISTORY_OPEN
            dragAmountYPx > 0f -> TRANSACTION_HISTORY_CLOSED
            else -> progress
        }
    }
    return (progress - dragAmountYPx / travelPx)
        .coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN)
}

/** Fling speed (px/s) at which a released drag keeps going instead of resting where it was left. */
internal fun flingVelocityThresholdPxPerSecond(travelPx: Float): Float =
    abs(travelPx) * FLING_TRAVELS_PER_SECOND

/**
 * Where the history rests when the finger is released mid-drag: a flick in one direction finishes the
 * motion in that direction, otherwise the history snaps to the side it is closer to.
 * [velocityYPxPerSecond] is the finger speed (negative = moving up = opening).
 */
internal fun historyProgressOnDragEnd(
    progress: Float,
    velocityYPxPerSecond: Float,
    minFlingVelocityPxPerSecond: Float,
): Float {
    val velocityThreshold = abs(minFlingVelocityPxPerSecond)
    if (velocityThreshold > 0f) {
        if (velocityYPxPerSecond <= -velocityThreshold) return TRANSACTION_HISTORY_OPEN
        if (velocityYPxPerSecond >= velocityThreshold) return TRANSACTION_HISTORY_CLOSED
    }
    val settled = progress.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN)
    return if (settled >= TRANSACTION_HISTORY_SNAP_FRACTION) {
        TRANSACTION_HISTORY_OPEN
    } else {
        TRANSACTION_HISTORY_CLOSED
    }
}

/**
 * Clicking the balance bar or one of its anchors does the same as dragging it through the whole
 * distance in one motion: a hidden - or only partly dragged - history opens completely, a completely
 * shown one closes again.
 */
internal fun historyProgressAfterClick(currentProgress: Float): Float =
    if (currentProgress.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN) >=
        TRANSACTION_HISTORY_OPEN
    ) {
        TRANSACTION_HISTORY_CLOSED
    } else {
        TRANSACTION_HISTORY_OPEN
    }

/**
 * A press of the bar or one of its anchors drags the transaction history when it left the touch slop
 * moving mostly up or down; up to the slop it is still a click, and a press that moved mostly left or
 * right belongs to the month pager underneath the bar (it changes the month), not to the history.
 */
internal fun isHistoryDrag(totalDragXPx: Float, totalDragYPx: Float, touchSlopPx: Float): Boolean =
    abs(totalDragYPx) > touchSlopPx && abs(totalDragYPx) >= abs(totalDragXPx)

/**
 * Speed of a finger drag expressed for the progress instead of the screen: [velocityYPxPerSecond]
 * (px/s) over [travelPx], capped so a short flick can never launch the history faster than the whole
 * travel distance per second [MAX_VELOCITY_FRACTION_PER_SECOND] times.
 */
internal fun dragVelocityFraction(velocityYPxPerSecond: Float, travelPx: Float): Float =
    if (travelPx <= 0f) {
        0f
    } else {
        (velocityYPxPerSecond / travelPx)
            .coerceIn(-MAX_VELOCITY_FRACTION_PER_SECOND, MAX_VELOCITY_FRACTION_PER_SECOND)
    }

/**
 * Position of the balance bar while the history is at [progress]: the bar rides on top of the history
 * by the same fraction of its travel, so it stays visible above it. Negative, because Compose
 * translates downwards.
 */
internal fun balanceBarTranslationPx(progress: Float, travelPx: Float): Float =
    -(progress.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN) * travelPx)

/**
 * Height of the history for a given [progress]: nothing while closed, the whole [travelPx] - the space
 * the bar vacated - when completely open, and everything in between while it is dragged out "little by
 * little".
 */
internal fun historyRevealedHeightPx(progress: Float, travelPx: Float): Float =
    (progress.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN) * travelPx)
        .coerceAtLeast(0f)

/**
 * How the history moves to where the last interaction wants it: exactly with the finger while it is
 * dragged, gliding with the drag's own momentum after a release, and in one fixed-time motion - the
 * same duration open as closed - after a click.
 */
internal fun motionAnimationSpec(driver: HistoryMotionDriver): FiniteAnimationSpec<Float> =
    when (driver) {
    HistoryMotionDriver.DRAG -> snap()
    HistoryMotionDriver.CLICK -> tween(durationMillis = TRANSACTION_HISTORY_CLICK_DURATION_MS)
    HistoryMotionDriver.SETTLE -> spring(
        dampingRatio = HISTORY_SPRING_DAMPING_RATIO,
        stiffness = HISTORY_SPRING_STIFFNESS,
    )
}

// --- the balance bar, its anchors, and the transaction history they drag open ---

/**
 * Motion state of the month's transaction history: [targetProgress] says how much of it is shown, and
 * both the drag and the click drive it - the drag follows the finger, the click runs the same motion in
 * one go, or in reverse when the history is already shown.
 */
@Composable
fun rememberTransactionHistoryMotion(): TransactionHistoryMotion =
    remember { TransactionHistoryMotion() }

class TransactionHistoryMotion {
    private val targetState = mutableFloatStateOf(TRANSACTION_HISTORY_CLOSED)
    private val driverState = mutableStateOf(HistoryMotionDriver.SETTLE)
    private val velocityState = mutableFloatStateOf(0f)

    /** How much of the transaction history is asked for: 0 = hidden, 1 = completely shown. */
    val targetProgress: Float
        get() = targetState.floatValue.coerceIn(TRANSACTION_HISTORY_CLOSED, TRANSACTION_HISTORY_OPEN)

    /** What last moved the history: a drag, the release of a drag, or a click. */
    internal val driver: HistoryMotionDriver
        get() = driverState.value

    /** The speed of the last drag release, in fractions of the travel distance per second. */
    val velocityFraction: Float
        get() = velocityState.floatValue

    /** True while none of the history is asked for. */
    val isHidden: Boolean
        get() = targetProgress <= TRANSACTION_HISTORY_CLOSED

    /** Follows the finger while the balance bar (or one of its anchors) is dragged. */
    fun onDrag(dragAmountYPx: Float, travelPx: Float) {
        driverState.value = HistoryMotionDriver.DRAG
        targetState.floatValue = historyProgressAfterDrag(targetProgress, dragAmountYPx, travelPx)
    }

    /**
     * Releasing the bar: a flick keeps going in the direction it was swiped, any other release rests on
     * the side it is closer to - in both cases gliding there with the momentum the drag had.
     */
    fun onDragEnd(velocityYPxPerSecond: Float, travelPx: Float) {
        velocityState.floatValue = dragVelocityFraction(velocityYPxPerSecond, travelPx)
        driverState.value = HistoryMotionDriver.SETTLE
        targetState.floatValue = historyProgressOnDragEnd(
            progress = targetProgress,
            velocityYPxPerSecond = velocityYPxPerSecond,
            minFlingVelocityPxPerSecond = flingVelocityThresholdPxPerSecond(travelPx),
        )
    }

    /** Clicking the bar or one of its anchors: the same motion as dragging, in one go, or in reverse. */
    fun onClick() {
        velocityState.floatValue = 0f
        driverState.value = HistoryMotionDriver.CLICK
        targetState.floatValue = historyProgressAfterClick(targetProgress)
    }

    /** Hides the history again with the same motion in reverse (back button). */
    fun hide() {
        velocityState.floatValue = 0f
        driverState.value = HistoryMotionDriver.SETTLE
        targetState.floatValue = TRANSACTION_HISTORY_CLOSED
    }
}

/**
 * The fraction of the month's transaction history that is currently shown: [TransactionHistoryMotion]'s
 * target, animated with the motion matching how it was last moved, so a drag tracks the finger while a
 * click or a release glides.
 */
@Composable
fun TransactionHistoryMotion.animatedProgress(): Float {
    val animated by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = motionAnimationSpec(driver),
        label = "transactionHistoryProgress",
    )
    return animated
}

/**
 * The balance bar with its two anchors, and the month's transaction history under it, filling the
 * [areaHeightPx] tall area it is placed in.
 *
 * Dragging the bar - or either anchor - up reveals the history little by little, following the finger;
 * releasing it at or past the snap fraction shows it completely, releasing it below closes it again.
 * Clicking the bar or an anchor does the same in one smooth motion, and clicking again closes it in
 * reverse. The bar and its anchors stay visible the whole time and are drawn on top of the history,
 * which is revealed in the space underneath them.
 */
@Composable
fun TransactionHistoryBalanceBar(
    motion: TransactionHistoryMotion,
    areaHeightPx: Float,
    balanceCents: Long,
    categoriesMap: Map<Long, CategoryItem>,
    transactions: List<TransactionListItem>,
    onTransactionClick: (TransactionListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    val travelPx = balanceBarTravelPx(areaHeightPx = areaHeightPx, barHeightPx = barHeightPx)
    val progress = motion.animatedProgress()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Declared before the bar, so the bar and its anchors are drawn on top of the history and stay
        // visible above it, also when it is completely shown.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { historyRevealedHeightPx(progress, travelPx).toDp() })
                .testTag("transaction_history_sheet"),
        ) {
            TransactionGroupedList(
                transactions = transactions,
                categoriesMap = categoriesMap,
                isGroupedByCategory = true,
                onTransactionClick = onTransactionClick,
            )
        }

        BalanceBarRow(
            balanceCents = balanceCents,
            translationYPx = balanceBarTranslationPx(progress, travelPx),
            onBarHeightChanged = { measuredHeightPx -> barHeightPx = measuredHeightPx },
            onToggleHistory = motion::onClick,
            onHistoryDrag = { dragAmountYPx -> motion.onDrag(dragAmountYPx, travelPx) },
            onHistoryDragEnd = { velocityYPxPerSecond -> motion.onDragEnd(velocityYPxPerSecond, travelPx) },
        )
    }
}

@Composable
private fun BalanceBarRow(
    balanceCents: Long,
    /** How far the bar has moved up through the area; named apart from the layer property it sets. */
    translationYPx: Float,
    onBarHeightChanged: (Float) -> Unit,
    onToggleHistory: () -> Unit,
    onHistoryDrag: (dragAmountYPx: Float) -> Unit,
    onHistoryDragEnd: (velocityYPxPerSecond: Float) -> Unit,
) {
    val viewConfiguration: ViewConfiguration = LocalViewConfiguration.current
    // Compose already reports the touch slop in pixels.
    val touchSlopPx = viewConfiguration.touchSlop

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // A layout offset, not a graphicsLayer translation: the bar has to really move, so its touch
            // target and its bounds move with it (a graphicsLayer offset only transforms the drawing).
            .offset { IntOffset(x = 0, y = translationYPx.roundToInt()) }
            .padding(start = 12.dp, end = 12.dp)
            .testTag("balance_bar")
            .onSizeChanged { size -> onBarHeightChanged(size.height.toFloat()) }
            .pointerInput(touchSlopPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragXPx = 0f
                    var totalDragYPx = 0f
                    var velocityYPxPerSecond = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.positionChanged()) {
                            val dragAmount = change.positionChangeIgnoreConsumed()
                            totalDragXPx += dragAmount.x
                            totalDragYPx += dragAmount.y
                            velocityYPxPerSecond = fingerVelocityPxPerSecond(change, dragAmount.y)
                            if (!dragging && isHistoryDrag(totalDragXPx, totalDragYPx, touchSlopPx)) {
                                dragging = true
                            }
                            if (dragging) {
                                // Consuming the drag is what cancels the click of the bar and of the
                                // anchors, so a drag never ends up being a click as well. Only vertical
                                // drags are consumed: a sideways one goes through to the month pager.
                                change.consume()
                                onHistoryDrag(dragAmount.y)
                            }
                        }
                        if (!change.pressed) break
                    }
                    // A press that never left the touch slop is a click: the bar and the anchors
                    // handle it themselves.
                    if (dragging) onHistoryDragEnd(velocityYPxPerSecond)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onToggleHistory,
            modifier = Modifier
                .size(60.dp)
                .testTag("balance_history_left"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Open transaction history",
                tint = Color(0xFF7CC8A4),
                modifier = Modifier.size(40.dp),
            )
        }
        Surface(
            color = if (balanceCents < 0) Color(0xFFF27982) else Color(0xFF70C889),
            shadowElevation = 2.dp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(45.dp)
                .testTag("balance_box")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onToggleHistory()
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = currencyAnnotatedString(
                        text = "Balance ${if (balanceCents < 0) "-" else ""}€${centsToDisplay(abs(balanceCents))}",
                        baseFontSize = 18.sp,
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                )
            }
        }
        IconButton(
            onClick = onToggleHistory,
            modifier = Modifier
                .size(60.dp)
                .testTag("balance_history_right"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Open transaction history",
                tint = Color(0xFF7CC8A4),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/**
 * Vertical speed of [change] in px/s, from how far it moved and how long that took: negative when the
 * finger moved up (which is what opens the history).
 */
private fun fingerVelocityPxPerSecond(
    change: PointerInputChange,
    dragAmountYPx: Float,
): Float {
    val durationMillis = change.uptimeMillis - change.previousUptimeMillis
    if (durationMillis <= 0L) return 0f
    return dragAmountYPx * 1000f / durationMillis
}
