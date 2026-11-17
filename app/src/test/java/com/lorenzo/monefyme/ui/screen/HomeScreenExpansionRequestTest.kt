package com.lorenzo.monefyme.ui.screen

import com.lorenzo.monefyme.data.model.TransactionListItem
import java.io.File
import java.time.LocalDate
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

    @Test
    fun groupsToExpandExpandsDateGroupForLatestTransactionWhenGroupedByDate() {
        // Create test transactions with different dates and categories
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 10).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 10000L,
                type = "EXPENSE",
                description = "",
                account = "",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "food", // different case
                amountCents = 15000L,
                type = "EXPENSE",
                description = "",
                account = "",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 3L,
                dateEpochDay = LocalDate.of(2024, 1, 5).toEpochDay(),
                categoryId = 2L,
                categoryName = "Transport",
                amountCents = 5000L,
                type = "EXPENSE",
                description = "",
                account = "",
                currency = "EUR",
            ),
        )

        // The date format is dd/MM/uuuu (e.g., "15/01/2024")
        val groupNames = listOf(
            "10/01/2024",
            "15/01/2024",
            "05/01/2024",
        )

        // When grouping by date and clicking on "Food" category,
        // should expand the date group containing the latest Food transaction (Jan 15)
        val result = groupsToExpand(
            groupNames = groupNames,
            requestedCategory = "Food",
            isGroupedByCategory = false, // grouping by date
            transactions = transactions,
        )

        assertTrue(
            "When grouped by date, clicking a category should expand the date group of its latest transaction.",
            result.contains("15/01/2024"),
        )
        assertEquals(
            "Should only expand one date group (the one with the latest transaction).",
            1,
            result.size,
        )
    }

    @Test
    fun groupsToExpandExpandsCategoryWhenGroupedByCategory() {
        val groupNames = listOf(
            "Food",
            "Transport",
            "Entertainment",
        )

        val result = groupsToExpand(
            groupNames = groupNames,
            requestedCategory = "food", // lowercase, should match case-insensitively
            isGroupedByCategory = true,
            transactions = emptyList(),
        )

        assertTrue(
            "When grouped by category, clicking a category should expand its accordion.",
            result.contains("Food"),
        )
        assertEquals(
            "Should only expand the matching category group.",
            1,
            result.size,
        )
    }

    @Test
    fun groupsToExpandReturnsEmptyWhenNoTransactionsMatchRequestedCategory() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 10).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 10000L,
                type = "EXPENSE",
                description = "",
                account = "",
                currency = "EUR",
            ),
        )

        val groupNames = listOf("10/01/2024")

        // Requesting "Shopping" which has no transactions
        val result = groupsToExpand(
            groupNames = groupNames,
            requestedCategory = "Shopping",
            isGroupedByCategory = false,
            transactions = transactions,
        )

        assertTrue(
            "Should return empty list when no transactions match the requested category.",
            result.isEmpty(),
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
