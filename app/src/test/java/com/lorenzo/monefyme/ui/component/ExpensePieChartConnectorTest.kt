package com.lorenzo.monefyme.ui.component

import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the connector line that links each pie-chart section to its category icon-and-label group.
 *
 * The endpoint calculation lives in the pure [connectorLineEnd] helper and is verified below with
 * concrete geometry. The layout convention (group-centre Y offset) and the Canvas wiring are
 * checked by reading the source file; the visual result is exercised end-to-end by
 * PieChartConnectorUiTest in app/src/androidTest.
 */
class ExpensePieChartConnectorTest {

    private val pieChartSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt")
    }

    // Shared bounding-box dimensions used across the pure-math tests.
    private val cx = 300f            // group centre X
    private val cy = 400f            // group centre Y
    private val hw = 30f             // groupHalfWidthPx
    private val ht = 45f             // groupHalfHeightTopPx
    private val hb = 55f             // groupHalfHeightBottomPx  (> ht by design)

    // --- endpoint lands on the correct face of the bounding rectangle ---

    @Test
    fun lineFromLeftEndsOnLeftBoundary() {
        val end = connectorLineEnd(cx - 200f, cy, cx, cy, hw, ht, hb)
        assertOffset(cx - hw, cy, end)
    }

    @Test
    fun lineFromRightEndsOnRightBoundary() {
        val end = connectorLineEnd(cx + 200f, cy, cx, cy, hw, ht, hb)
        assertOffset(cx + hw, cy, end)
    }

    @Test
    fun lineFromAboveEndsOnTopBoundary() {
        val end = connectorLineEnd(cx, cy - 200f, cx, cy, hw, ht, hb)
        assertOffset(cx, cy - ht, end)
    }

    @Test
    fun lineFromBelowEndsOnBottomBoundary() {
        val end = connectorLineEnd(cx, cy + 200f, cx, cy, hw, ht, hb)
        assertOffset(cx, cy + hb, end)
    }

    // --- asymmetric bottom padding ---

    @Test
    fun bottomClearanceIsLargerThanTopClearance() {
        val endFromAbove = connectorLineEnd(cx, cy - 200f, cx, cy, hw, ht, hb)
        val endFromBelow = connectorLineEnd(cx, cy + 200f, cx, cy, hw, ht, hb)
        val distTop = cy - endFromAbove.y
        val distBottom = endFromBelow.y - cy
        assertTrue(
            "Bottom clearance ($distBottom) must exceed top clearance ($distTop).",
            distBottom > distTop,
        )
    }

    @Test
    fun bottomBoundaryIsGroupHalfHeightBottomBelowGroupCentre() {
        val end = connectorLineEnd(cx, cy + 200f, cx, cy, hw, ht, hb)
        assertEquals("Bottom boundary Y", cy + hb, end.y, 0.5f)
    }

    @Test
    fun topBoundaryIsGroupHalfHeightTopAboveGroupCentre() {
        val end = connectorLineEnd(cx, cy - 200f, cx, cy, hw, ht, hb)
        assertEquals("Top boundary Y", cy - ht, end.y, 0.5f)
    }

    // --- diagonal lines hit the nearest face ---

    @Test
    fun diagonalFromBelowLeftHitsLeftBoundaryWhenMoreHorizontal() {
        // 45-degree from below-left: tX = hw/cos45 ≈ 42, tY = hb/sin45 ≈ 78 → left face wins.
        val end = connectorLineEnd(cx - 200f, cy + 200f, cx, cy, hw, ht, hb)
        assertEquals("Left boundary X", cx - hw, end.x, 0.5f)
    }

    @Test
    fun diagonalFromBelowHitsBottomBoundaryWhenMostlyVertical() {
        // Small horizontal offset, large vertical: tY wins and the endpoint is on the bottom face.
        val end = connectorLineEnd(cx - 10f, cy + 200f, cx, cy, hw, ht, hb)
        assertEquals("Bottom boundary Y", cy + hb, end.y, 0.5f)
    }

    @Test
    fun diagonalFromAboveHitsTopBoundaryWhenMostlyVertical() {
        val end = connectorLineEnd(cx + 10f, cy - 200f, cx, cy, hw, ht, hb)
        assertEquals("Top boundary Y", cy - ht, end.y, 0.5f)
    }

    // --- endpoint always lies on the bounding-box boundary (never inside) ---

    @Test
    fun endpointLiesOnGroupBoundaryForAllApproachDirections() {
        for (step in 0 until 36) {
            val angleDeg = step * 10.0
            val rad = Math.toRadians(angleDeg)
            val start = Pair(cx + cos(rad).toFloat() * 300f, cy + sin(rad).toFloat() * 300f)
            val end = connectorLineEnd(start.first, start.second, cx, cy, hw, ht, hb)

            val halfH = if (end.y < cy) ht else hb
            val onLeftOrRight = abs(abs(end.x - cx) - hw) < 0.5f && abs(end.y - cy) <= halfH + 0.5f
            val onTopOrBottom = abs(abs(end.y - cy) - halfH) < 0.5f && abs(end.x - cx) <= hw + 0.5f
            assertTrue(
                "Endpoint from angle $angleDeg must lie on the box boundary " +
                    "(end=(${end.x}, ${end.y}), group=($cx,$cy), hw=$hw, ht=$ht, hb=$hb).",
                onLeftOrRight || onTopOrBottom,
            )
        }
    }

    // --- source-code wiring ---

    @Test
    fun connectorTargetIsTheGroupCentreNotTheIconCentre() {
        // The connector slot Y must be shifted by half the icon-label slot height to target the
        // group centre (icon + percentage label), not just the icon centre.
        val connectorBlock = pieChartSource
            .substringAfter("val connectorSlotByCategory")
            .substringBefore("drawCircle(")
        assertTrue(
            "connectorSlotByCategory must shift Y to the group centre:\n$connectorBlock",
            connectorBlock.contains("+ iconLabelSlotHeightPx / 2f"),
        )
    }

    @Test
    fun canvasCallsConnectorLineEndWithAsymmetricHeights() {
        val body = expensePieChartBody()
        assertTrue(
            "The Canvas must delegate endpoint calculation to connectorLineEnd.",
            body.contains("connectorLineEnd("),
        )
        assertTrue(
            "connectorLineEnd must receive the separate top and bottom half-heights.",
            body.contains("groupHalfHeightTopPx = groupHalfHeightTopPx") &&
                body.contains("groupHalfHeightBottomPx = groupHalfHeightBottomPx"),
        )
    }

    @Test
    fun bottomHalfHeightDefinitionAddsMorePaddingThanTop() {
        val body = expensePieChartBody()
        val topDef = body.substringAfter("val groupHalfHeightTopPx").substringBefore("\n")
        val bottomDef = body.substringAfter("val groupHalfHeightBottomPx").substringBefore("\n")
        assertTrue(
            "groupHalfHeightTopPx must reference the symmetric base.",
            topDef.contains("groupHalfHeightSymmetricPx"),
        )
        assertTrue(
            "groupHalfHeightBottomPx must add an extra positive term beyond groupHalfHeightTopPx " +
                "(bottom boundary needs extra clearance for the percentage label).\n" +
                "top:    $topDef\nbottom: $bottomDef",
            bottomDef.contains("groupHalfHeightSymmetricPx") &&
                bottomDef.count { it == '+' } > topDef.count { it == '+' },
        )
    }

    // --- helpers ---

    private fun assertOffset(expectedX: Float, expectedY: Float, actual: androidx.compose.ui.geometry.Offset, delta: Float = 0.5f) {
        assertEquals("x", expectedX, actual.x, delta)
        assertEquals("y", expectedY, actual.y, delta)
    }

    private fun expensePieChartBody(): String {
        val start = pieChartSource.indexOf("fun ExpensePieChart(")
        assertTrue("ExpensePieChart composable must exist.", start >= 0)
        val end = pieChartSource.indexOf("private data class SliceMeta", start)
        assertTrue(end > start)
        return pieChartSource.substring(start, end)
    }

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        return file.readText().replace("\r\n", "\n")
    }

    private fun findProjectRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val current: File = dir
            if (File(current, "settings.gradle.kts").exists() && File(current, "app").isDirectory) {
                return current
            }
            dir = current.parentFile
        }
        throw IllegalStateException("Could not locate the project root starting from $dir")
    }
}
