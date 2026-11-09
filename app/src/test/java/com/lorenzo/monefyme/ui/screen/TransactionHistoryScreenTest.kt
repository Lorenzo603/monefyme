package com.lorenzo.monefyme.ui.screen

import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import com.lorenzo.monefyme.ui.util.formatDate
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import com.lorenzo.monefyme.ui.viewmodel.MainViewModelTest
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHistoryScreenTest {

    @Test
    fun transactionHistoryScreenGroupsByCategoryByDefault() {
        val categories = listOf(
            CategoryItem(id = 1L, displayName = "Food", iconName = "Restaurant", colorArgb = null),
            CategoryItem(id = 2L, displayName = "Transport", iconName = "LocalTaxi", colorArgb = null),
        )
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 1000L,
                type = "EXPENSE",
                description = "Lunch",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 3L,
                dateEpochDay = LocalDate.of(2024, 1, 17).toEpochDay(),
                categoryId = 2L,
                categoryName = "Transport",
                amountCents = 500L,
                type = "EXPENSE",
                description = "Taxi",
                account = "Card",
                currency = "EUR",
            ),
        )

        // Group by category
        val groupedByCategory = transactions.groupBy { it.categoryName }
        assertEquals(2, groupedByCategory.size)
        assertTrue(groupedByCategory.containsKey("Food"))
        assertTrue(groupedByCategory.containsKey("Transport"))
        assertEquals(2, groupedByCategory["Food"]?.size)
        assertEquals(1, groupedByCategory["Transport"]?.size)
    }

    @Test
    fun transactionHistoryScreenGroupsByDateWhenToggled() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 1000L,
                type = "EXPENSE",
                description = "Lunch",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 3L,
                dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
                categoryId = 2L,
                categoryName = "Transport",
                amountCents = 500L,
                type = "EXPENSE",
                description = "Taxi",
                account = "Card",
                currency = "EUR",
            ),
        )

        // Group by date (formatted as string)
        val groupedByDate = transactions.groupBy { formatDate(it.date) }
        assertEquals(2, groupedByDate.size)
        assertTrue(groupedByDate.containsKey("15/01/2024"))
        assertTrue(groupedByDate.containsKey("16/01/2024"))
        assertEquals(2, groupedByDate["15/01/2024"]?.size)
        assertEquals(1, groupedByDate["16/01/2024"]?.size)
    }

    @Test
    fun transactionHistoryScreenCalculatesGroupTotals() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 1000L,
                type = "EXPENSE",
                description = "Lunch",
                account = "Cash",
                currency = "EUR",
            ),
            TransactionListItem(
                id = 2L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 2000L,
                type = "EXPENSE",
                description = "Dinner",
                account = "Cash",
                currency = "EUR",
            ),
        )

        val groupedByCategory = transactions.groupBy { it.categoryName }
        val foodGroup = groupedByCategory["Food"]!!
        val totalAmount = foodGroup.sumOf { it.amountCents }
        val transactionCount = foodGroup.size

        assertEquals(3000L, totalAmount)
        assertEquals(2, transactionCount)
    }

    @Test
    fun transactionHistoryScreenEmptyListShowsNoGroups() {
        val transactions = emptyList<TransactionListItem>()
        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(0, groupedByCategory.size)
        assertEquals(0, groupedByDate.size)
    }

    @Test
    fun transactionHistoryScreenSingleTransactionGroupsCorrectly() {
        val transactions = listOf(
            TransactionListItem(
                id = 1L,
                dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
                categoryId = 1L,
                categoryName = "Food",
                amountCents = 1000L,
                type = "EXPENSE",
                description = "Lunch",
                account = "Cash",
                currency = "EUR",
            ),
        )

        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(1, groupedByCategory.size)
        assertEquals(1, groupedByDate.size)
        assertEquals(1, groupedByCategory["Food"]?.size)
        assertEquals("15/01/2024", groupedByDate.keys.first())
    }

    @Test
    fun transactionHistoryScreenEmptyListGroupsCorrectly() {
        val transactions = emptyList<TransactionListItem>()
        val groupedByCategory = transactions.groupBy { it.categoryName }
        val groupedByDate = transactions.groupBy { formatDate(it.date) }

        assertEquals(0, groupedByCategory.size)
        assertEquals(0, groupedByDate.size)
    }

    // --- Accordions: every group starts collapsed and clicking a header expands that group ---

    @Test
    fun groupAccordionsStartCollapsed() {
        val grouped = sampleTransactions().groupBy { it.categoryName }
        // Nothing was clicked yet, so the expansion state is empty.
        val openedGroups = emptyMap<String, Boolean>()

        grouped.keys.forEach { groupName ->
            assertFalse("Group '$groupName' must start collapsed.", isGroupExpanded(openedGroups, groupName))
        }
    }

    @Test
    fun clickingGroupHeaderExpandsThatGroup() {
        val openedGroups = mutableMapOf<String, Boolean>()

        openedGroups["Food"] = expandedGroupsAfterToggle(openedGroups, "Food")

        assertTrue("Clicking a collapsed group must expand its accordion.", isGroupExpanded(openedGroups, "Food"))
    }

    @Test
    fun clickingExpandedGroupHeaderCollapsesItAgain() {
        val openedGroups = mutableMapOf<String, Boolean>()
        openedGroups["Food"] = expandedGroupsAfterToggle(openedGroups, "Food")

        openedGroups["Food"] = expandedGroupsAfterToggle(openedGroups, "Food")

        assertFalse("Clicking an expanded group must collapse its accordion.", isGroupExpanded(openedGroups, "Food"))
    }

    @Test
    fun clickingOneGroupKeepsTheOtherGroupsCollapsed() {
        val openedGroups = mutableMapOf<String, Boolean>()
        openedGroups["Food"] = expandedGroupsAfterToggle(openedGroups, "Food")
        openedGroups["Transport"] = expandedGroupsAfterToggle(openedGroups, "Transport")
        openedGroups["Food"] = expandedGroupsAfterToggle(openedGroups, "Food")

        assertFalse("Food must be collapsed again.", isGroupExpanded(openedGroups, "Food"))
        assertTrue("Transport must stay expanded.", isGroupExpanded(openedGroups, "Transport"))
    }

    @Test
    fun groupsThatAppearLaterStartCollapsed() {
        val openedGroups = mutableMapOf<String, Boolean>()
        openedGroups["Food"] = true

        // Switching month or grouping (category <-> date) introduces group names that were never
        // clicked; they must not inherit an expanded state.
        assertFalse(isGroupExpanded(openedGroups, "Utilities"))
        assertFalse(isGroupExpanded(openedGroups, "15/02/2024"))
    }

    @Test
    fun collapsedGroupsRenderTheirHeaderOnly() {
        val listBody = transactionGroupedListSource()

        val headerIndex = listBody.indexOf("GroupHeader(")
        val expandedGuardIndex = listBody.indexOf("if (isExpanded)")
        val transactionsIndex = listBody.indexOf("key = { it.id }")

        assertTrue("TransactionGroupedList must render a GroupHeader per group.", headerIndex >= 0)
        assertTrue(
            "TransactionGroupedList must ask isGroupExpanded() whether a group is open.",
            listBody.contains("isGroupExpanded(expandedGroups, groupName)"),
        )
        assertTrue(
            "TransactionGroupedList must guard the transactions of a group by its expanded state, " +
                "otherwise the accordions are always open:\n$listBody",
            expandedGuardIndex >= 0 && expandedGuardIndex < transactionsIndex,
        )
        assertTrue(
            "The group header must always be rendered, before the guarded transactions:\n$listBody",
            headerIndex >= 0 && headerIndex < expandedGuardIndex,
        )
    }

    @Test
    fun clickingGroupHeaderTogglesOnlyThatGroup() {
        val listBody = transactionGroupedListSource()

        assertTrue(
            "Clicking a group header must flip that group's own state " +
                "(expandedGroupsAfterToggle), not expand every group:\n$listBody",
            listBody.contains("expandedGroups[groupName] = expandedGroupsAfterToggle(expandedGroups, groupName)"),
        )
        assertTrue(
            "The GroupHeader must receive the expanded state so its arrow shows Expand/Collapse:",
            listBody.contains("isExpanded = isExpanded"),
        )
    }

    /** Body of TransactionGroupedList, read from the source: the rendering itself needs an emulator. */
    private fun transactionGroupedListSource(): String {
        val source = readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreen.kt")
        val marker = "internal fun TransactionGroupedList("
        val start = source.indexOf(marker)
        assertTrue("TransactionHistoryScreen must keep TransactionGroupedList visible to the UI tests.", start >= 0)
        val end = source.indexOf("@Composable", start + marker.length)
        assertTrue(end > start)
        return source.substring(start, end)
    }

    private fun sampleTransactions(): List<TransactionListItem> = listOf(
        TransactionListItem(
            id = 11L,
            dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
            categoryId = 1L,
            categoryName = "Food",
            amountCents = 1000L,
            type = "EXPENSE",
            description = "Lunch",
            account = "Cash",
            currency = "EUR",
        ),
        TransactionListItem(
            id = 12L,
            dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
            categoryId = 2L,
            categoryName = "Transport",
            amountCents = 500L,
            type = "EXPENSE",
            description = "Taxi",
            account = "Card",
            currency = "EUR",
        ),
    )

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
