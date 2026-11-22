package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wiring of the "Back up to Google Drive" button on the Restore screen (Compose click
 * behaviour itself needs an emulator; these JVM tests assert the wiring in the source, like the
 * other Restore screen wiring tests do).
 */
class RestoreScreenDriveBackupWiringTest {
    private val restoreScreenSource by lazy {
        val relativePath = "app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt"
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        file.readText().replace("\r\n", "\n")
    }

    @Test
    fun restoreScreenOffersTheGoogleDriveBackupButtonAndKeepsRestore() {
        assertTrue(
            "The Restore screen must offer the new Google Drive backup button.",
            restoreScreenSource.contains("Text(\"Back up to Google Drive\")"),
        )
        assertTrue(
            "The Drive button must trigger the ViewModel backup flow.",
            restoreScreenSource.contains("viewModel.backupToGoogleDrive()"),
        )
        assertTrue(
            "The manual restore import button must stay untouched.",
            restoreScreenSource.contains("Text(\"Select CSV file\")"),
        )
        assertTrue(
            "The manual restore import must still call viewModel.importCsv.",
            restoreScreenSource.contains("viewModel.importCsv(content)"),
        )
    }

    @Test
    fun backupShowsAProgressIndicatorWhileRunning() {
        assertTrue(
            "The screen must observe the backup progress state.",
            restoreScreenSource.contains("viewModel.isBackingUpToDrive"),
        )
        assertTrue(
            "A CircularProgressIndicator must be shown while the backup runs.",
            restoreScreenSource.contains("if (isBackingUpToDrive) {") &&
                restoreScreenSource.contains("CircularProgressIndicator("),
        )
        assertTrue(
            "The backup button must be disabled while a backup is in flight.",
            restoreScreenSource.contains("enabled = !isBackingUpToDrive"),
        )
    }

    @Test
    fun googleConsentIntentIsLaunchedThroughAnActivityResultLauncher() {
        assertTrue(
            "The screen must observe the pending Google consent request.",
            restoreScreenSource.contains("viewModel.drivePendingConsent"),
        )
        assertTrue(
            "The consent IntentSender must be launched via StartIntentSenderForResult.",
            restoreScreenSource.contains("ActivityResultContracts.StartIntentSenderForResult()"),
        )
        assertTrue(
            "The consent result data (null = dismissed) must be handed back to the ViewModel.",
            restoreScreenSource.contains("viewModel.resolveDriveConsent(activityResult.data)"),
        )
    }

    @Test
    fun signedInAccountIsShownAsASummaryLine() {
        assertTrue(
            "The screen must observe the remembered Drive account.",
            restoreScreenSource.contains("viewModel.driveAccount"),
        )
        assertTrue(
            "A summary line must show the remembered account.",
            restoreScreenSource.contains("for Google Drive backups."),
        )
    }

    @Test
    fun noUncaughtFailurePathIsIntroduced() {
        assertFalse(
            "The Drive section must not swallow the backup call in a bare catch block.",
            restoreScreenSource.contains("backupToGoogleDrive() } catch"),
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
