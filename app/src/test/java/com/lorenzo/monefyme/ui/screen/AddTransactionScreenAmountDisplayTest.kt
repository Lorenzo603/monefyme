package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the amount input box displays values visibly when users click number buttons.
 * 
 * This ensures that the amount text color is visible against the green background
 * of the amount display box, so users can see the value as they type it.
 */
class AddTransactionScreenAmountDisplayTest {

    private val addTransactionScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt")
    }

    /**
     * Test that the amount display color is white on the green background.
     */
    @Test
    fun amountDisplayColorIsVisibleOnGreenBackground() {
        // Find the animateColorAsState definition for amountDisplayColor
        val colorDefStart = addTransactionScreenSource.indexOf("val amountDisplayColor by animateColorAsState(")
        assertTrue(
            "amountDisplayColor should be defined using animateColorAsState",
            colorDefStart >= 0
        )

        // Get the next 250 characters to check the targetValue
        val colorDefEnd = minOf(colorDefStart + 250, addTransactionScreenSource.length)
        val colorDef = addTransactionScreenSource.substring(colorDefStart, colorDefEnd)

        // The default value should be Color.White
        assertTrue(
            "The amountDisplayColor default targetValue should use Color.White.\n" +
                "Found: $colorDef",
            colorDef.contains("Color.White")
        )
    }

    /**
     * Test that the amount text uses the amountDisplayColor variable.
     */
    @Test
    fun amountTextUsesAmountDisplayColor() {
        // Find where the amount Text composable is defined - use simpler pattern
        val textStart = addTransactionScreenSource.indexOf("currencyAnnotatedString(text = amountText, baseFontSize = 58.sp)")
        assertTrue(
            "Amount display Text should use currencyAnnotatedString with amountText",
            textStart >= 0
        )

        // Get the next 300 characters to check the color property
        val textEnd = minOf(textStart + 300, addTransactionScreenSource.length)
        val textDef = addTransactionScreenSource.substring(textStart, textEnd)

        assertTrue(
            "The amount Text should use amountDisplayColor for its color, which is now a visible dark color",
            textDef.contains("color = amountDisplayColor"),
        )
    }

    /**
     * Test that the green background color is set on the Surface.
     */
    @Test
    fun surfaceHasGreenBackground() {
        // Find the Surface definition for the amount display area - use simpler pattern
        val surfaceStart = addTransactionScreenSource.indexOf(".padding(top = 34.dp, bottom = 5.dp, start = 18.dp, end = 18.dp),")
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
