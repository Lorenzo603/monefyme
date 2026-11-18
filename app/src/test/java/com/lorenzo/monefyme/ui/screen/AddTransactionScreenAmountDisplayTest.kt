package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the amount input box displays values with white text and outline on green background.
 * 
 * This ensures that the amount text is visible and has proper contrast against the green background
 * of the amount display box, so users can see the value as they type it.
 */
class AddTransactionScreenAmountDisplayTest {

    private val addTransactionScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt")
    }

    /**
     * Test that the amount text uses white color for visibility on the green background.
     * 
     * The Surface has a green background (0xFF79C896), so the amount text should use
     * white color with an outline to ensure visibility and contrast.
     */
    @Test
    fun amountTextUsesWhiteColor() {
        // Find where the currencyAnnotatedString is used
        val textStart = addTransactionScreenSource.indexOf("currencyAnnotatedString(text = amountText, baseFontSize = 58.sp)")
        assertTrue(
            "Amount display Text should use currencyAnnotatedString with amountText",
            textStart >= 0
        )

        // Get the next 1300 characters to check for white color and outline
        val textEnd = minOf(textStart + 1300, addTransactionScreenSource.length)
        val textDef = addTransactionScreenSource.substring(textStart, textEnd)

        // Verify white color is used
        assertTrue(
            "The amount Text should use Color.White for visibility on the green background.\n" +
                "Found: $textDef",
            textDef.contains("color = Color.White")
        )
    }

    /**
     * Test that the amount text has an outline (stroke) effect for better contrast.
     */
    @Test
    fun amountTextHasOutlineEffect() {
        // Find where the currencyAnnotatedString is used
        val textStart = addTransactionScreenSource.indexOf("currencyAnnotatedString(text = amountText, baseFontSize = 58.sp)")
        assertTrue(
            "Amount display Text should use currencyAnnotatedString with amountText",
            textStart >= 0
        )

        // Get the next 1300 characters to check for outline drawing
        val textEnd = minOf(textStart + 1300, addTransactionScreenSource.length)
        val textDef = addTransactionScreenSource.substring(textStart, textEnd)

        assertTrue(
            "The amount Text should have an outline (stroke) effect using drawWithContent for contrast.\n" +
                "Found: $textDef",
            textDef.contains("drawWithContent") && textDef.contains("drawRect")
        )
    }

    /**
     * Test that the green background color is set on the Surface.
     */
    @Test
    fun surfaceHasGreenBackground() {
        // Find the Surface definition for the amount display area - use simpler pattern
        val surfaceStart = addTransactionScreenSource.indexOf("padding(horizontal = 18.dp, vertical = 26.dp),")
        assertTrue(
            "Amount display Surface should exist with proper modifiers",
            surfaceStart >= 0
        )

        // Get the next 200 characters to check the color property
        val surfaceEnd = minOf(surfaceStart + 200, addTransactionScreenSource.length)
        val surfaceDef = addTransactionScreenSource.substring(surfaceStart, surfaceEnd)

        assertTrue(
            "The amount display Surface should have a green background color (0xFF79C896)",
            surfaceDef.contains("Color(0xFF79C896)")
        )
    }

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
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
