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
        for (tag in listOf("balance_history_left", "balance_history_right")) {
            val buttonBlock = buttonBlockWithTag(homeScreenSource, tag)
            assertTrue(
                "The button next to the balance box ($tag) must open the month's transaction " +
                    "history, same as clicking the balance box itself:\n$buttonBlock",
                buttonBlock.contains("onClick = onOpenTransactionHistory"),
            )
            assertFalse(
                "The button next to the balance box ($tag) must not open the Restore screen:\n" +
                    buttonBlock,
                buttonBlock.contains("onOpenRestore"),
            )
        }
        assertTrue(
            "Clicking the balance box itself must keep opening the transaction history.",
            countOccurrences(homeScreenSource, "onOpenTransactionHistory()") == 1,
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
