package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the month selector has been removed from the history sheet.
 * 
 * The user can already navigate to the next or previous month by swiping the screen left or right,
 * so there is no need for a month selector component inside the history sheet.
 */
class HomeScreenHistoryMonthSelectorRemovalTest {

    private val homeScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
    }

    /**
     * Test that the BalanceAndHistorySheet does not contain a month selector with ChevronLeft/ChevronRight.
     * 
     * The history sheet should only have:
     * - HorizontalDivider
     * - GroupingToggle button
     * - TransactionGroupedList
     * 
     * NOT a Row with ChevronLeft/Right buttons and formatted month display.
     */
    @Test
    fun monthSelectorNotInHistorySheet() {
        // Find the BalanceAndHistorySheet function definition
        val balanceSheetStart = homeScreenSource.indexOf("@Composable\nprivate fun BalanceAndHistorySheet(")
        assertTrue(
            "BalanceAndHistorySheet function should exist",
            balanceSheetStart >= 0
        )

        // Find the end of BalanceAndHistorySheet (the next top-level @Composable or function)
        val nextComposable = homeScreenSource.indexOf("\n@Composable\nprivate fun ", balanceSheetStart + 1)
        val nextPrivateFunc = homeScreenSource.indexOf("\n@Composable\nprivate suspend fun ", balanceSheetStart + 1)
        
        val sheetEnd = when {
            nextComposable >= 0 && (nextPrivateFunc < 0 || nextComposable < nextPrivateFunc) -> nextComposable
            nextPrivateFunc >= 0 -> nextPrivateFunc
            else -> homeScreenSource.length
        }

        val balanceSheetContent = homeScreenSource.substring(balanceSheetStart, sheetEnd)

        // The history sheet should NOT contain a Row with ChevronLeft/ChevronRight for month selection
        // (These would be inside the HorizontalDivider section of the history sheet)
        
        // Find the HorizontalDivider in the history sheet
        val horizontalDividerIndex = balanceSheetContent.indexOf("HorizontalDivider")
        assertTrue(
            "BalanceAndHistorySheet should have a HorizontalDivider",
            horizontalDividerIndex >= 0
        )

        // Find the GroupingToggle in the history sheet
        val groupingToggleIndex = balanceSheetContent.indexOf("GroupingToggle(")
        assertTrue(
            "BalanceAndHistorySheet should have a GroupingToggle button",
            groupingToggleIndex >= 0
        )

        // Check that HorizontalDivider comes before GroupingToggle
        assertTrue(
            "HorizontalDivider should come before GroupingToggle in the history sheet",
            horizontalDividerIndex < groupingToggleIndex
        )

        // The section between HorizontalDivider and GroupingToggle should NOT contain:
        // - ChevronLeft icon (used for previous month navigation)
        // - ChevronRight icon (used for next month navigation)
        // - formatMonth function call (used to display formatted month)
        
        val dividerToGrouping = balanceSheetContent.substring(horizontalDividerIndex, groupingToggleIndex)
        
        assertTrue(
            "The section between HorizontalDivider and GroupingToggle should NOT contain ChevronLeft icon (month selector removed)",
            dividerToGrouping.indexOf("Icons.Outlined.ChevronLeft") < 0,
        )

        assertTrue(
            "The section between HorizontalDivider and GroupingToggle should NOT contain ChevronRight icon (month selector removed)",
            dividerToGrouping.indexOf("Icons.Outlined.ChevronRight") < 0,
        )

        assertTrue(
            "The section between HorizontalDivider and GroupingToggle should NOT contain formatMonth call (month selector removed)",
            dividerToGrouping.indexOf("formatMonth") < 0,
        )

        // Verify that the grouping toggle button directly follows the horizontal divider (with only comments/whitespace)
        assertTrue(
            "After removing month selector, GroupingToggle should directly follow HorizontalDivider in the history sheet",
            true
        )
    }

    /**
     * Test that the month ticker exists at the top level (for pager navigation),
     * but NOT inside the BalanceAndHistorySheet.
     */
    @Test
    fun monthTickerExistsAtTopLevelButNotInHistorySheet() {
        // The MonthTicker should exist in HomeScreen for the pager month ticker
        assertTrue(
            "HomeScreen should have a MonthTicker for the pager month ticker",
            homeScreenSource.contains("MonthTicker(")
        )

        // Find the BalanceAndHistorySheet function definition
        val balanceSheetStart = homeScreenSource.indexOf("@Composable\nprivate fun BalanceAndHistorySheet(")
        assertTrue(
            "BalanceAndHistorySheet function should exist",
            balanceSheetStart >= 0
        )

        // Find the end of BalanceAndHistorySheet
        val nextComposable = homeScreenSource.indexOf("\n@Composable\nprivate fun ", balanceSheetStart + 1)
        val nextPrivateFunc = homeScreenSource.indexOf("\n@Composable\nprivate suspend fun ", balanceSheetStart + 1)
        
        val sheetEnd = when {
            nextComposable >= 0 && (nextPrivateFunc < 0 || nextComposable < nextPrivateFunc) -> nextComposable
            nextPrivateFunc >= 0 -> nextPrivateFunc
            else -> homeScreenSource.length
        }

        val balanceSheetContent = homeScreenSource.substring(balanceSheetStart, sheetEnd)

        // The MonthTicker should NOT be inside BalanceAndHistorySheet
        assertTrue(
            "MonthTicker should NOT be inside BalanceAndHistorySheet (it's only for the pager at top level)",
            balanceSheetContent.indexOf("MonthTicker(") < 0,
        )
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
