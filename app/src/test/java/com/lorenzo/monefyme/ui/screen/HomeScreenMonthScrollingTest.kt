package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the balance box is now part of the scrollable month.
 * 
 * This ensures that when scrolling to a previous or next month in the pager,
 * the balance box for that month is displayed along with its pie chart,
 * rather than remaining fixed at the bottom and only updating after the
 * month is settled.
 */
class HomeScreenMonthScrollingTest {

    private val homeScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
    }

    /**
     * Test that BalanceAndHistorySheet is called inside the HorizontalPager,
     * making it part of each scrollable month's content.
     */
    @Test
    fun balanceBoxIsPartOfEachPagerPage() {
        // The HorizontalPager lambda should contain BalanceAndHistorySheet call
        val pagerStart = homeScreenSource.indexOf("HorizontalPager(")
        assertTrue("HorizontalPager should be in HomeScreen", pagerStart >= 0)
        
        val actionButtonsStart = homeScreenSource.indexOf("// Action buttons", pagerStart)
        assertTrue("Action buttons section should exist after pager", actionButtonsStart >= 0)
        
        val pagerSection = homeScreenSource.substring(pagerStart, actionButtonsStart)
        
        assertTrue(
            "BalanceAndHistorySheet must be called inside each HorizontalPager page so it " +
                "scrolls with the month, not remain fixed at the bottom.\n" +
                "The balance box should be part of each month's scrollable content.",
            pagerSection.contains("BalanceAndHistorySheet("),
        )
    }

    /**
     * Test that each pager page calculates its own balance from its month's transactions.
     */
    @Test
    fun eachPageCalculatesItsOwnBalance() {
        val pagerStart = homeScreenSource.indexOf("HorizontalPager(")
        val actionButtonsStart = homeScreenSource.indexOf("// Action buttons", pagerStart)
        val pagerSection = homeScreenSource.substring(pagerStart, actionButtonsStart)
        
        assertTrue(
            "Each pager page must calculate pageBalance from its own transactions:\n" +
                "val pageBalance = pageTotalIncome - pageTotalExpense",
            pagerSection.contains("val pageBalance = pageTotalIncome - pageTotalExpense"),
        )
        
        assertTrue(
            "BalanceAndHistorySheet must be called with the page's balance, not a global balance:\n" +
                "balance = pageBalance",
            pagerSection.contains("balance = pageBalance"),
        )
    }

    /**
     * Test that each pager page passes its own transactions to the balance sheet.
     */
    @Test
    fun eachPagePassesItsOwnTransactionsToBalanceSheet() {
        val pagerStart = homeScreenSource.indexOf("HorizontalPager(")
        val actionButtonsStart = homeScreenSource.indexOf("// Action buttons", pagerStart)
        val pagerSection = homeScreenSource.substring(pagerStart, actionButtonsStart)
        
        assertTrue(
            "Each pager page must have its own pageTransactions from viewModel.observeMonthTransactions",
            pagerSection.contains("val pageTransactions by viewModel.observeMonthTransactions(pageMonth)"),
        )
        
        assertTrue(
            "BalanceAndHistorySheet must receive the page's transactions:\n" +
                "transactions = pageTransactions",
            pagerSection.contains("transactions = pageTransactions"),
        )
    }

    /**
     * Test that the balance box is inside a Column along with the pie chart,
     * so both scroll together.
     */
    @Test
    fun balanceAndPieChartAreInTheSameColumn() {
        val pagerStart = homeScreenSource.indexOf("HorizontalPager(")
        val actionButtonsStart = homeScreenSource.indexOf("// Action buttons", pagerStart)
        val pagerSection = homeScreenSource.substring(pagerStart, actionButtonsStart)
        
        assertTrue(
            "The pager page must contain ExpensePieChart",
            pagerSection.contains("ExpensePieChart("),
        )
        
        assertTrue(
            "The pager page must contain BalanceAndHistorySheet",
            pagerSection.contains("BalanceAndHistorySheet("),
        )
        
        // Verify order: pie chart should come before balance box
        val pieChartIndex = pagerSection.indexOf("ExpensePieChart(")
        val balanceSheetIndex = pagerSection.indexOf("BalanceAndHistorySheet(")
        assertTrue(
            "The ExpensePieChart should appear before BalanceAndHistorySheet",
            pieChartIndex >= 0 && balanceSheetIndex >= 0 && pieChartIndex < balanceSheetIndex,
        )
    }

    /**
     * Test that the balance box scrolls with the month, not by checking for absence
     * of the old structure (fixed balance box below the pager).
     */
    @Test
    fun balanceBoxIsNotFixedBelowPager() {
        // The key test: BalanceAndHistorySheet should be called inside the HorizontalPager
        val pagerStart = homeScreenSource.indexOf("HorizontalPager(")
        val actionButtonsStart = homeScreenSource.indexOf("// Action buttons", pagerStart)
        
        assertTrue(
            "BalanceAndHistorySheet should be called inside the HorizontalPager, not after it",
            pagerStart >= 0 && actionButtonsStart >= 0,
        )
        
        val pagerSection = homeScreenSource.substring(pagerStart, actionButtonsStart)
        
        // Should have BalanceAndHistorySheet inside pager
        assertTrue(
            "BalanceAndHistorySheet must be inside the HorizontalPager pager lambda",
            pagerSection.contains("BalanceAndHistorySheet("),
        )
        
        // The section after pager (action buttons) should NOT have BalanceAndHistorySheet
        val afterPager = homeScreenSource.substring(actionButtonsStart)
        assertTrue(
            "After the pager, there should not be another BalanceAndHistorySheet (it should only be inside pager)",
            !afterPager.substringBefore("// Sidebar").contains("BalanceAndHistorySheet("),
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
