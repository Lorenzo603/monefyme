package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the HomeScreen's BalanceAndHistorySheet properly handles category expansion requests
 * when a user clicks on a pie chart section or category icon.
 *
 * Bug being tested:
 * - Expected: When clicking a pie chart section, the transaction history should open with that
 *   category's accordion already expanded.
 * - Actual: The transaction history opens but all categories are collapsed.
 *
 * Root cause: BalanceAndHistorySheet always passed expandCategory = null to TransactionGroupedList,
 * instead of passing the expansion request from the viewModel.
 */
class HomeScreenExpansionRequestTest {

    @Test
    fun balanceAndHistorySheetConsumesThePieChartExpansionRequest() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        // Find the end of the BalanceAndHistorySheet function (next function or end of file)
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        assertTrue(
            "BalanceAndHistorySheet must observe the requested category from viewModel so a pie chart " +
            "click can expand a category's accordion in the history sheet.",
            balanceAndHistorySheetBody.contains("viewModel.categoryExpansionRequest.collectAsStateWithLifecycle()"),
        )
        
        assertTrue(
            "It must consume the request so the same click cannot force the accordion open again. " +
            "This matches the TransactionHistoryScreen behavior.",
            balanceAndHistorySheetBody.contains("viewModel.consumeCategoryExpansionRequest()"),
        )
        
        assertTrue(
            "It must maintain categoryToExpand state to store the consumed expansion request.",
            balanceAndHistorySheetBody.contains("categoryToExpand"),
        )
        
        assertTrue(
            "It must hand the consumed category to TransactionGroupedList as expandCategory so that " +
            "group's accordion opens without any user interaction.",
            balanceAndHistorySheetBody.contains("expandCategory = categoryToExpand"),
        )
        
        assertFalse(
            "The bug was always passing expandCategory = null, which never expanded any category. " +
            "This has been fixed to pass the consumed expansion request instead.",
            balanceAndHistorySheetBody.contains("expandCategory = null"),
        )
    }

    @Test
    fun homeScreenRequestsCategoryExpansionOnPieChartClick() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        val homeScreenBody = source.substring(source.indexOf("fun HomeScreen("), source.indexOf("@Composable", source.indexOf("fun HomeScreen(") + 1))

        assertTrue(
            "HomeScreen must call viewModel.requestCategoryExpansion when a pie chart section is clicked. " +
            "This tells BalanceAndHistorySheet which category to expand.",
            homeScreenBody.contains("viewModel.requestCategoryExpansion(categoryName)"),
        )
        
        assertTrue(
            "HomeScreen must also open the history sheet (toggle it to full) when a category is clicked " +
            "on the pie chart.",
            homeScreenBody.contains("onToggleHistoryFull(true)"),
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
