package com.lorenzo.monefyme.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.lorenzo.monefyme.ui.category.DefaultCategories
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pie-chart category highlight: keeping a finger/mouse pressed on a category icon must
 * highlight that icon slot with a colored circle and replace the centre income/expense totals with
 * the pressed category's icon, name and total amount.
 *
 * The decision logic lives in the internal pure helpers of ExpensePieChart and is asserted
 * directly. The Compose rendering itself needs an emulator, so the wiring inside the composable is
 * asserted by reading the source file (same pattern as HomeRestoreWiringTest and
 * TransactionHistoryScreenTest); the end-to-end resting state is covered by the instrumentation
 * test PieChartHighlightUiTest in app/src/androidTest.
 */
class ExpensePieChartHighlightTest {

    private val pieChartSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt")
    }

    // --- press-and-hold state machine ---

    @Test
    fun nothingIsHighlightedByDefault() {
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = null)
        assertNull("No press means no highlighted category.", pressed)
        assertFalse(isCategoryHighlighted(pressedCategoryKey = null, categoryKey = "food"))
    }

    @Test
    fun pressingACategoryHighlightsIt() {
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = "Food")
        assertEquals("food", pressed)
        assertTrue(isCategoryHighlighted(pressedCategoryKey = pressed, categoryKey = "food"))
    }

    @Test
    fun pressingACategoryOnlyHighlightsThatCategory() {
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = "Food")
        assertTrue(isCategoryHighlighted(pressed, "Food"))
        assertFalse(
            "Only the pressed slot may be highlighted.",
            isCategoryHighlighted(pressed, "Transport"),
        )
    }

    @Test
    fun pressingIsCaseInsensitiveAndIgnoresSurroundingWhitespace() {
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = " Food ")
        assertEquals("food", pressed)
        assertTrue(isCategoryHighlighted(pressedCategoryKey = "FOOD", categoryKey = "food"))
    }

    @Test
    fun releasingThePressClearsTheHighlight() {
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = "Food")
        val afterRelease = pressedCategoryAfterRelease(pressed)
        assertNull("After release the centre must show the totals again.", afterRelease)
        assertFalse(isCategoryHighlighted(afterRelease, "food"))
    }

    @Test
    fun inactivePlaceholderSlotsCannotBePressed() {
        // Extra (non-slice-backed) icon slots have a null connector key and must not highlight.
        val pressed = pressedCategoryAfterPress(currentCategoryKey = null, categoryKey = null)
        assertNull(pressed)
        assertFalse(isCategoryHighlighted(pressedCategoryKey = "food", categoryKey = null))
    }

    // --- centre content for the pressed category ---

    @Test
    fun centreUsesTheVisualOfThePressedCategory() {
        val visuals = mapOf(
            "food" to CategoryVisual(
                categoryKey = "food",
                icon = DefaultCategories.findByName("Food")!!.icon,
                color = Color.Red,
            ),
        )
        assertNull(pressedCategoryVisual(null, visuals))
        assertNull(pressedCategoryVisual("Transport", visuals))
        // Lookup tolerates case and padding differences in the pressed key.
        assertEquals("food", pressedCategoryVisual(" FOOD ", visuals)?.categoryKey)
    }

    @Test
    fun centreShowsTotalOfPressedCategoryAcrossItsSlices() {
        val totals = mapOf(
            "food" to 4000L + 1000L,
            "transport" to 250L,
        )
        val label = highlightedCategoryLabel("Food", totals, baseFontSize = 16.sp)
        assertEquals("€50.00", label)
        assertEquals("€2.50", highlightedCategoryLabel("transport", totals, baseFontSize = 16.sp))
    }

    @Test
    fun centreFallsBackToZeroWhenPressedKeyHasNoSlice() {
        assertEquals(
            "€0.00",
            highlightedCategoryLabel("unknown", mapOf("food" to 5000L), baseFontSize = 16.sp),
        )
        assertEquals(
            "€0.00",
            highlightedCategoryLabel(null, mapOf("food" to 5000L), baseFontSize = 16.sp),
        )
    }

    // --- wiring inside the composable (rendering needs an emulator) ---

    @Test
    fun composableTracksThePressedCategory() {
        val body = expensePieChartBody()
        assertTrue(
            "ExpensePieChart must keep the currently pressed category in state.",
            body.contains("var pressedCategoryKey by remember { mutableStateOf<String?>(null) }"),
        )
    }

    @Test
    fun pressingAnIconSlotSetsAndReleasingClearsThePressedCategory() {
        val body = expensePieChartBody()
        val gestureBlock = body.substringAfter("awaitEachGesture {")
        val downIndex = gestureBlock.indexOf("awaitFirstDown")
        val pressIndex = gestureBlock.indexOf("pressedCategoryAfterPress")
        val upIndex = gestureBlock.indexOf("waitForUpOrCancellation")
        val releaseIndex = gestureBlock.indexOf("pressedCategoryAfterRelease")
        assertTrue(
            "The icon slot must listen for a press gesture:\n$gestureBlock",
            downIndex >= 0 && pressIndex >= 0 && upIndex >= 0 && releaseIndex >= 0,
        )
        assertTrue(
            "Press down must highlight the slot, release must clear it:\n$gestureBlock",
            downIndex < pressIndex && pressIndex < upIndex && upIndex < releaseIndex,
        )
    }

    @Test
    fun highlightedIconSlotDrawsAColorCircleAroundItsIcon() {
        val body = expensePieChartBody()
        assertTrue(
            "Each slot must compute whether it is the pressed one via isCategoryHighlighted.",
            body.contains(
                "val isHighlighted = isCategoryHighlighted(pressedCategoryKey, placement.connectorCategoryKey)",
            ),
        )
        val colorIndex = body.indexOf("val ringColor = highlightedVisual?.color")
        val guardIndex = body.indexOf("if (ringColor != null)")
        val ringIndex = body.indexOf("radius = with(density) { (iconSize + highlightRingGap).toPx() }")
        assertTrue(
            "The highlight ring color must come from the highlighted category's color.",
            colorIndex >= 0,
        )
        assertTrue(
            "The colored circle must be drawn only while the category is highlighted.",
            guardIndex >= 0 && guardIndex < ringIndex,
        )
    }

    @Test
    fun centreReplacesTotalsWithPressedCategoryInfo() {
        val body = expensePieChartBody()
        val highlightBranch = body.indexOf("if (highlightedVisual != null)")
        val incomeText = body.indexOf("text = \"€\${centsToDisplay(displayedIncomeCents)}\"")
        assertTrue(
            "The centre must switch on the pressed category before drawing the default totals.",
            highlightBranch >= 0 && highlightBranch < incomeText,
        )
        assertTrue(
            "While pressed, the centre shows the category name.",
            body.contains("categoryDisplayNames[highlightedVisual.categoryKey]"),
        )
        assertTrue(
            "While pressed, the centre shows the category's total amount.",
            body.contains("highlightedCategoryLabel("),
        )
    }

    @Test
    fun centreAndDefaultTotalsAreTaggedForUiTests() {
        val body = expensePieChartBody()
        for (tag in listOf(
            "pie_chart_centre_category",
            "pie_chart_centre_income",
            "pie_chart_centre_expense",
        )) {
            assertTrue("The centre nodes must keep stable test tags ($tag).", body.contains("\"$tag\""))
        }
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
        // Normalise line endings so the assertions are independent of the checkout setting.
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
