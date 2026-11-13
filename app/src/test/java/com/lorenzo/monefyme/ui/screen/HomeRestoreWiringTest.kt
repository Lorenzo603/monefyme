package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wiring of the "Restore" entry points on the Home screen.
 *
 * HomeScreen and CategoriesSidebar are Compose composables, so their click behaviour is asserted
 * by the instrumentation test HomeRestoreNavigationTest (which needs an emulator). These JVM tests
 * assert the wiring itself by reading the two source files, which is exactly what the original bug
 * was about: CategoriesSidebar declared `onOpenRestore` with a default no-op lambda, so HomeScreen
 * never passed it and the sidebar "Restore" row silently did nothing, while the two buttons next to
 * the balance box were wired to Restore instead of the month's transaction history.
 *
 * Reverting any of those wirings makes the matching assertion below fail.
 */
class HomeRestoreWiringTest {

    private val homeScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
    }

    private val categoriesSidebarSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt")
    }

    @Test
    fun homeScreenPassesOnOpenRestoreToTheSidebar() {
        val sidebarCall = homeScreenSource.substringAfter("CategoriesSidebar(")
            .substringBefore("\n            )")
        assertTrue(
            "HomeScreen must wire the sidebar Restore entry to the Restore route, otherwise " +
                "clicking 'Restore' in the sidebar has no effect. Call:\n$sidebarCall",
            sidebarCall.contains("onOpenRestore ="),
        )
    }

    @Test
    fun sidebarRestoreCallbackHasNoSilentNoOpDefault() {
        assertFalse(
            "CategoriesSidebar must require onOpenRestore (no default no-op lambda) so a caller " +
                "that forgets the Restore wiring fails to compile instead of silently doing " +
                "nothing.",
            categoriesSidebarSource.contains("onOpenRestore: () -> Unit ="),
        )
        assertTrue(
            "CategoriesSidebar must declare an onOpenRestore parameter for the sidebar Restore row.",
            categoriesSidebarSource.contains("onOpenRestore: () -> Unit,"),
        )
    }

    @Test
    fun balanceSideButtonsOpenTransactionHistoryNotRestore() {
        // The balance and history sheet now combines the balance box with the transaction history
        assertTrue(
            "HomeScreen must include the BalanceAndHistorySheet composable for handling drag gestures:\n",
            homeScreenSource.contains("BalanceAndHistorySheet("),
        )
        
        for (tag in listOf("balance_history_left", "balance_history_right")) {
            val buttonBlock = buttonBlockWithTag(homeScreenSource, tag)
            assertTrue(
                "The button next to the balance box ($tag) must trigger toggling the transaction " +
                    "history overlay by calling onToggleHistoryFull with toggle logic:\n$buttonBlock",
                buttonBlock.contains("onToggleHistoryFull(currentRevealOffset <= 0.5f)"),
            )
            assertFalse(
                "The button next to the balance box ($tag) must not open the Restore screen:\n" +
                    buttonBlock,
                buttonBlock.contains("onOpenRestore"),
            )
        }
        
        // Pie chart click still uses onOpenTransactionHistory for navigation
        assertTrue(
            "The pie chart section click must still open the transaction history for category " +
                "expansion support.",
            homeScreenSource.contains("onCategoryClick = { categoryName ->") &&
                homeScreenSource.contains("viewModel.requestCategoryExpansion(categoryName)") &&
                homeScreenSource.contains("onOpenTransactionHistory()"),
        )
    }

    /**
     * Clicking a section of the pie chart opens the month's transaction history with that category's
     * accordion already expanded: HomeScreen must hand the chart a click handler that records the
     * clicked category in the ViewModel (TransactionHistoryScreen expands it) and then navigates.
     */
    @Test
    fun pieChartSectionClickOpensTransactionHistoryWithThatCategoryExpanded() {
        val pieChartCall = homeScreenSource.substringAfter("ExpensePieChart(")
            .substringBefore("\n            )")
        assertTrue(
            "HomeScreen must handle pie chart section clicks:\n$pieChartCall",
            pieChartCall.contains("onCategoryClick ="),
        )
        assertTrue(
            "The clicked category must be handed to the ViewModel so the history opens its accordion " +
                "(MainViewModel.requestCategoryExpansion):\n$pieChartCall",
            pieChartCall.contains("requestCategoryExpansion(categoryName)"),
        )
        assertTrue(
            "The pie chart click must open the month's transaction history:\n$pieChartCall",
            pieChartCall.contains("onOpenTransactionHistory()"),
        )
    }

    @Test
    fun balanceSideButtonsUseTheHamburgerIcon() {
        for (tag in listOf("balance_history_left", "balance_history_right")) {
            val buttonBlock = buttonBlockWithTag(homeScreenSource, tag)
            assertTrue(
                "The button next to the balance box ($tag) must use the 3 horizontal lines " +
                    "(hamburger) icon instead of the Restore icon:\n$buttonBlock",
                buttonBlock.contains("imageVector = Icons.Outlined.Menu"),
            )
            assertFalse(
                "The button next to the balance box ($tag) must not show the Restore icon:\n" +
                    buttonBlock,
                buttonBlock.contains("Icons.Outlined.Restore"),
            )
            assertTrue(
                "The button next to the balance box ($tag) must describe itself as toggling the " +
                    "transaction history:\n$buttonBlock",
                buttonBlock.contains("contentDescription = \"Toggle transaction history\""),
            )
        }
    }

    @Test
    fun homeScreenHasNoRestoreButtonsAnymore() {
        assertFalse(
            "The Home screen must not show the Restore icon any more: the buttons next to the " +
                "balance box open the transaction history instead.",
            homeScreenSource.contains("Icons.Outlined.Restore"),
        )
        assertFalse(
            "The Home screen must not expose an 'Open restore' button any more.",
            homeScreenSource.contains("Open restore"),
        )
        assertTrue(
            "The sidebar must keep its Restore entry (with the Restore icon) in the menu body.",
            categoriesSidebarSource.contains("Icons.Outlined.Restore"),
        )
    }

    @Test
    fun balanceBoxSectionSupportsDraggingToRevealHistory() {
        assertTrue(
            "BalanceAndHistorySheet must support vertical drag gestures for revealing the history:",
            homeScreenSource.contains("detectVerticalDragGestures"),
        )
        assertTrue(
            "HomeScreen must render the balance and history sheet when history is revealed:",
            homeScreenSource.contains("BalanceAndHistorySheet("),
        )
        assertTrue(
            "Dragging and clicking the balance box should trigger smooth animations:",
            homeScreenSource.contains("animateRevealOffset"),
        )
    }

    /** Returns the source of the `IconButton { ... }` block that carries the given test tag. */
    private fun buttonBlockWithTag(source: String, tag: String): String {
        val tagIndex = source.indexOf(".testTag(\"$tag\")")
        assertTrue("HomeScreen has no button tagged '$tag'.", tagIndex >= 0)
        val start = source.lastIndexOf("IconButton(", tagIndex)
        assertTrue("Tag '$tag' is not used inside an IconButton.", start >= 0)
        val end = source.indexOf("\n            }", tagIndex)
        assertTrue("Could not find the end of the IconButton block tagged '$tag'.", end > tagIndex)
        return source.substring(start, end)
    }

    private fun countOccurrences(text: String, needle: String): Int {
        var index = text.indexOf(needle)
        var count = 0
        while (index >= 0) {
            count++
            index = text.indexOf(needle, index + needle.length)
        }
        return count
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
