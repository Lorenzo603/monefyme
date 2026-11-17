package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the HomeScreen's transaction history grouping toggle properly uses ViewModel state
 * and persists the preference.
 *
 * Bug being tested:
 * - Expected: When toggling the transaction grouping to be by category, the setting should remain
 *   selected the next time the transaction history is open.
 * - Actual: The setting is lost when closing the transaction history because it uses local
 *   rememberSaveable state instead of ViewModel-persisted state.
 */
class HomeScreenGroupingToggleTest {

    @Test
    fun balanceAndHistorySheetUsesViewModelGroupByCategoryState() {
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
            "BalanceAndHistorySheet must observe the groupByCategory state from viewModel " +
            "so it persists across session boundaries.",
            balanceAndHistorySheetBody.contains("viewModel.groupByCategory.collectAsStateWithLifecycle()"),
        )
        
        assertFalse(
            "The bug was using local rememberSaveable state which doesn't persist across app restarts. " +
            "This has been fixed to use ViewModel-persisted state instead.",
            balanceAndHistorySheetBody.contains("var isGroupedByCategory by rememberSaveable { mutableStateOf(true) }"),
        )
    }

    @Test
    fun groupingToggleCallsViewModelToggleMethod() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        assertTrue(
            "The GroupingToggle's onToggle callback must call viewModel.toggleGroupByCategory() " +
            "to persist the preference change.",
            balanceAndHistorySheetBody.contains("viewModel.toggleGroupByCategory()"),
        )
        
        assertFalse(
            "The bug was using local state assignment { isGroupedByCategory = !isGroupedByCategory } " +
            "which doesn't persist. This has been fixed to call the ViewModel method.",
            balanceAndHistorySheetBody.contains("isGroupedByCategory = !isGroupedByCategory"),
        )
    }

    @Test
    fun transactionGroupedListReceivesViewModelGroupByCategoryState() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        assertTrue(
            "TransactionGroupedList must receive isGroupedByCategory from viewModel.groupByCategory " +
            "to ensure consistent state throughout the UI.",
            balanceAndHistorySheetBody.contains("isGroupedByCategory = groupByCategory"),
        )
    }

    @Test
    fun balanceAndHistorySheetCollectsGroupByCategoryState() {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")

        val balanceAndHistorySheetStart = source.indexOf("private fun BalanceAndHistorySheet(")
        assertTrue("BalanceAndHistorySheet function not found", balanceAndHistorySheetStart >= 0)
        
        val balanceAndHistorySheetEnd = source.indexOf("private fun ", balanceAndHistorySheetStart + 1)
        val balanceAndHistorySheetBody = if (balanceAndHistorySheetEnd > 0) {
            source.substring(balanceAndHistorySheetStart, balanceAndHistorySheetEnd)
        } else {
            source.substring(balanceAndHistorySheetStart)
        }

        assertTrue(
            "BalanceAndHistorySheet must collect the groupByCategory state from viewModel so it persists across session boundaries.",
            balanceAndHistorySheetBody.contains("viewModel.groupByCategory.collectAsStateWithLifecycle()"),
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
