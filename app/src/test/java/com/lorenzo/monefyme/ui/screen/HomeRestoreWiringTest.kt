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
 * assert the wiring itself by reading the source files, which is exactly what the original bug
 * was about: CategoriesSidebar declared `onOpenRestore` with a default no-op lambda, so HomeScreen
 * never passed it and the sidebar "Restore" row silently did nothing, while the two buttons next to
 * the balance box were wired to Restore instead of the month's transaction history.
 *
 * The balance box, its two anchors and the transaction history they drag open live in
 * TransactionHistorySheet.kt, so the assertions about those buttons read that file.
 *
 * Reverting any of those wirings makes the matching assertion below fail.
 */
class HomeRestoreWiringTest {

    private val homeScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
    }

    private val balanceBarSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionHistorySheet.kt")
    }

    private val categoriesSidebarSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt")
    }

    @Test
    fun homeScreenPassesOnOpenRestoreToTheSidebar() {
        val sidebarCall = callSource(homeScreenSource, "CategoriesSidebar(")
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
        for (tag in listOf("balance_history_left", "balance_history_right")) {
            val buttonBlock = buttonBlockWithTag(balanceBarSource, tag)
            assertTrue(
                "The button next to the balance box ($tag) must open the month's transaction " +
                    "history, same as clicking the balance box itself:\n$buttonBlock",
                buttonBlock.contains("onClick = onToggleHistory"),
            )
            assertFalse(
                "The button next to the balance box ($tag) must not open the Restore screen:\n" +
                    buttonBlock,
                buttonBlock.contains("onOpenRestore"),
            )
        }
        val balanceBoxBlock = balanceBarSource.substringAfter("Surface(\n            color = if (balanceCents < 0)")
            .substringBefore("\n        }")
        assertTrue(
            "Clicking the balance box itself must keep opening the transaction history:\n" +
                balanceBoxBlock,
            balanceBoxBlock.contains("onToggleHistory()"),
        )
        assertTrue(
            "HomeScreen must place the balance bar and its anchors in the area the month's " +
                "transaction history is revealed in.",
            homeScreenSource.contains("TransactionHistoryBalanceBar("),
        )
        assertTrue(
            "The balance bar is the one that drags the history open, driven by the motion HomeScreen " +
                "remembers for it.",
            homeScreenSource.contains("motion = historyMotion"),
        )
        assertFalse(
            "The Home screen must not wire the balance bar to the Restore route.",
            homeScreenSource.contains("onOpenRestore = { onOpenTransactionHistory"),
        )
    }

    /**
     * Clicking a pie chart section opens the month's transaction history with that category's
     * accordion already expanded: HomeScreen has to hand the chart a click handler that records the
     * clicked category in the ViewModel and then navigates.
     */
    @Test
    fun pieChartSectionClickOpensTransactionHistoryWithThatCategoryExpanded() {
        val pieChartCall = callSource(homeScreenSource, "ExpensePieChart(")
        assertTrue(
            "HomeScreen must handle pie chart section clicks:\n$pieChartCall",
            pieChartCall.contains("onCategoryClick ="),
        )
        assertTrue(
            "The clicked category must be handed to the ViewModel so the history expands it " +
                "(MainViewModel.requestCategoryExpansion):\n$pieChartCall",
            pieChartCall.contains("requestCategoryExpansion(categoryName)"),
        )
        assertTrue(
            "The pie chart click must open the month's transaction history:\n$pieChartCall",
            pieChartCall.contains("onOpenTransactionHistory()"),
        )
        assertTrue(
            "The pie chart is the only navigation left to the full transaction history screen: the " +
                "balance bar drags its own history out instead of navigating.",
            countOccurrences(homeScreenSource, "onOpenTransactionHistory()") == 1,
        )
    }

    /**
     * The month's pie charts and the balance bar share the one area the transaction history is
     * revealed in, and that area is the whole leftover height of the Home column. That is what gives
     * the bar the distance to travel: dragged to the top of the area it ends up on top of the page,
     * with the history completely shown underneath it.
     */
    @Test
    fun pieChartPagesAndBalanceBarShareTheTransactionHistoryArea() {
        val areaBody = homeScreenSource
            .substringAfter(".testTag(\"transaction_history_area\")")
            .substringBefore("RoundActionButton(")

        val collapsedHome = collapsed(homeScreenSource)
        assertTrue(
            "The area the history is revealed in must be the leftover space of the Home column " +
                "(weight(1f)), not a strip under the pie chart.",
            collapsedHome.contains(".weight(1f) .testTag(\"transaction_history_area\")"),
        )
        assertTrue(
            "The height the bar travels must come out of that area's own layout constraints - a height " +
                "measured once into a plain value leaves the bar with the height from before the area " +
                "was laid out.",
            collapsedHome.contains("val historyAreaHeightPx = with(areaDensity) { maxHeight.toPx() }"),
        )
        assertTrue(
            "The balance bar must be handed that height as its travel distance.",
            collapsedHome.contains("areaHeightPx = historyAreaHeightPx"),
        )
        assertTrue(
            "The month's pie chart pages must sit inside that area, so the bar travels past them:\n" +
                areaBody,
            areaBody.contains("HorizontalPager("),
        )
        assertTrue(
            "The balance bar must sit in that same area, on top of the pie charts:\n$areaBody",
            areaBody.contains("TransactionHistoryBalanceBar("),
        )
        assertTrue(
            "The bar is declared after the pager so it is drawn on top of the pie charts and stays " +
                "visible above the history it drags open.\n$areaBody",
            areaBody.indexOf("HorizontalPager(") < areaBody.indexOf("TransactionHistoryBalanceBar("),
        )
    }

    @Test
    fun balanceSideButtonsUseTheHamburgerIcon() {
        for (tag in listOf("balance_history_left", "balance_history_right")) {
            val buttonBlock = buttonBlockWithTag(balanceBarSource, tag)
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
                "The button next to the balance box ($tag) must describe itself as opening the " +
                    "transaction history:\n$buttonBlock",
                buttonBlock.contains("contentDescription = \"Open transaction history\""),
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

    /** Returns the source of the `IconButton { ... }` block that carries the given test tag. */
    private fun buttonBlockWithTag(source: String, tag: String): String {
        val tagIndex = source.indexOf(".testTag(\"$tag\")")
        assertTrue("No button tagged '$tag' in the balance bar.", tagIndex >= 0)
        val start = source.lastIndexOf("IconButton(", tagIndex)
        assertTrue("Tag '$tag' is not used inside an IconButton.", start >= 0)
        // Walk the braces of the IconButton body: its opening brace is the first one after the tag.
        val bodyOpen = source.indexOf('{', tagIndex)
        assertTrue("The button tagged '$tag' has no body.", bodyOpen > tagIndex)
        var depth = 0
        var index = bodyOpen
        while (index < source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
            index++
        }
        throw IllegalStateException("Could not find the end of the IconButton block tagged '$tag'.")
    }

    /** All runs of whitespace collapsed to one space, so an assertion does not depend on indentation. */
    private fun collapsed(text: String): String = text.replace(Regex("\\s+"), " ")

    /** Source of one call, from its header to the parenthesis that closes it. */
    private fun callSource(source: String, callHeader: String): String {
        val headerIndex = source.indexOf(callHeader)
        assertTrue("No '$callHeader' call in the source.", headerIndex >= 0)
        var depth = 0
        var index = headerIndex + callHeader.length - 1 // the '(' of the call
        while (index < source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return source.substring(headerIndex, index + 1)
                }
            }
            index++
        }
        throw IllegalStateException("Could not find the end of the '$callHeader' call.")
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
