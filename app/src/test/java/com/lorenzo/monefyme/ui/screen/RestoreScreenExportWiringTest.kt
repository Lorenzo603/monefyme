package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wiring of the "Export to CSV" button on the Restore screen (Compose click behaviour
 * itself needs an emulator; these JVM tests assert the wiring in the source, like the other
 * Restore screen wiring tests do).
 */
class RestoreScreenExportWiringTest {
    private val restoreScreenSource by lazy {
        val relativePath = "app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt"
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        file.readText().replace("\r\n", "\n")
    }

    @Test
    fun restoreScreenOffersBothImportAndExportButtons() {
        assertTrue(
            "The Restore screen must keep its import button.",
            restoreScreenSource.contains("Text(\"Select CSV file\")"),
        )
        assertTrue(
            "The Restore screen must offer the new export button.",
            restoreScreenSource.contains("Text(\"Export to CSV\")"),
        )
    }

    @Test
    fun exportUsesCreateDocumentAndWritesThroughTheContentResolver() {
        assertTrue(
            "The export destination must be picked with ActivityResultContracts.CreateDocument " +
                "using a CSV MIME type.",
            restoreScreenSource.contains("ActivityResultContracts.CreateDocument(\"text/csv\")"),
        )
        assertTrue(
            "The export launcher must write the exported bytes to the chosen Uri via openOutputStream.",
            restoreScreenSource.contains("contentResolver.openOutputStream(uri)"),
        )
        assertTrue(
            "A suggested Monefy.Export.yyyy-MM-dd.HH-mm-ss.csv file name must be offered to the user.",
            restoreScreenSource.contains("Monefy.Export."),
        )
        assertTrue(
            "The export file name pattern must include a time component (yyyy-MM-dd.HH-mm-ss).",
            restoreScreenSource.contains("yyyy-MM-dd.HH-mm-ss"),
        )
    }

    @Test
    fun statusLineReportsTheRowsWrittenOrTheExportError() {
        assertTrue(
            "The status line must show the number of rows written by the last export.",
            restoreScreenSource.contains("viewModel.lastExportRowCount"),
        )
        assertTrue(
            "The status line must keep showing a failed export.",
            restoreScreenSource.contains("viewModel.lastExportError"),
        )
    }

    @Test
    fun writeFailureIsSurfacedThroughTheViewModelMessageAndTheStatusLine() {
        assertTrue(
            "A failing output stream must be reported via viewModel.postMessage instead of crashing.",
            restoreScreenSource.contains("viewModel.postMessage(failure)"),
        )
        assertTrue(
            "A write failure must also be kept for the status line.",
            restoreScreenSource.contains("writeError = failure"),
        )
        assertTrue(
            "The status line must prefer an export/write failure over the success summary.",
            restoreScreenSource.contains("val exportFailure = writeError ?: exportError"),
        )
        assertFalse(
            "The export must not swallow write errors in an empty catch block.",
            restoreScreenSource.contains("catch (ioException: IOException) {\n            }"),
        )
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
