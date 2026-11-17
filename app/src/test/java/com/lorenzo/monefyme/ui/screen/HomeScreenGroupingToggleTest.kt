package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the HomeScreen's transaction history grouping toggle is correctly positioned
 * and only visible when the transaction history sheet is open.
 *
 * Bug being tested:
 * - Expected: The grouping toggle button should be displayed to the right of the balance box
 *   (in place of the right hamburger icon anchor) when the transaction history is open.
 *   When the history is closed, the right hamburger icon should be shown again.
 *   - Actual: The toggle was displayed inside the transaction history sheet instead of in the
 *     balance box anchor position.
 */

class HomeScreenGroupingToggleTest {

    @Test
    fun groupingToggleButtonIsInRightAnchorPosition() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        // The grouping toggle should be inside the BalanceAndHistorySheet composable,
        // positioned in the right anchor of the balance box (not inside the history sheet)
        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        // Verify the toggle button is in the right anchor position with testTag "grouping_toggle_button"
        assertTrue(
            "The grouping toggle button should be positioned in the right anchor of the balance box " +
            "with testTag 'grouping_toggle_button'.",
            balanceAndHistorySheetBody.contains("testTag(\"grouping_toggle_button\")"),
        )
    }

    @Test
    fun groupingToggleIsConditionalOnHistoryRevealOffset() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        // The grouping toggle should only be shown when historyRevealOffset > 0f
        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        // Verify there's a conditional check for historyRevealOffset > 0f before showing the toggle
        assertTrue(
            "The grouping toggle should only be displayed when historyRevealOffset > 0f (history is open).",
            balanceAndHistorySheetBody.contains("if (historyRevealOffset > 0f)"),
        )

        // Verify there's an else branch showing the hamburger icon when history is closed
        assertTrue(
            "When history is closed, the right hamburger icon should be shown instead of the toggle.",
            balanceAndHistorySheetBody.contains("else {"),
        )
    }

    @Test
    fun groupingToggleCallsViewModelToggleMethod() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        // The toggle button in the right anchor should call viewModel.toggleGroupByCategory()
        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        // Verify the IconButton onClick calls viewModel.toggleGroupByCategory()
        assertTrue(
            "The grouping toggle IconButton's onClick should call viewModel.toggleGroupByCategory().",
            balanceAndHistorySheetBody.contains("onClick = { viewModel.toggleGroupByCategory() }"),
        )
    }

    @Test
    fun transactionGroupedListReceivesViewModelGroupByCategoryState() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        // Verify TransactionGroupedList is called with the isGroupedByCategory parameter
        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        // Verify TransactionGroupedList is called with isGroupedByCategory parameter
        assertTrue(
            "TransactionGroupedList must receive isGroupedByCategory to ensure consistent state.",
            balanceAndHistorySheetBody.contains("TransactionGroupedList(") &&
            balanceAndHistorySheetBody.contains("isGroupedByCategory = groupByCategory"),
        )
    }

    @Test
    fun rightAnchorShowsHamburgerIconWhenHistoryIsClosed() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        // Verify that when history is closed, the right anchor shows the hamburger icon
        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        // Verify the else branch contains the hamburger icon for opening history
        assertTrue(
            "When history is closed, the right anchor should show a hamburger icon to open it.",
            balanceAndHistorySheetBody.contains("Icons.Outlined.Menu") &&
            balanceAndHistorySheetBody.contains("contentDescription = \"Open transaction history\""),
        )
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
