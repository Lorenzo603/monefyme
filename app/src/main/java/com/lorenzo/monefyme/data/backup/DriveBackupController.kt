package com.lorenzo.monefyme.data.backup

import androidx.activity.result.IntentSenderRequest
import android.content.Intent
import android.database.SQLException
import com.lorenzo.monefyme.data.repository.BudgetRepository
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Orchestrates the "Back up to Google Drive" flow:
 * 1. reuses the CSV export ([BudgetRepository.exportCsv], all months, same 8-column layout),
 * 2. asks for Google sign-in with the Drive scope only when there is something to upload,
 * 3. uploads the bytes unchanged as `Monefy.Backup.yyyy-MM-dd.csv` to the root of My Drive.
 *
 * Every failure path (empty dataset, sign-in cancellation/denial, upload failure) returns a
 * [DriveBackupResult] instead of throwing, so the ViewModel can report it as a message.
 */
class DriveBackupController(
    private val repository: BudgetRepository,
    private val authSession: DriveAuthSession,
    private val uploader: DriveUploader,
    private val sessionStore: DriveSessionStore,
    private val today: () -> LocalDate = LocalDate::now,
) : DriveBackup {
    override val signedInAccount: Flow<String?> = sessionStore.accountFlow
    override val pendingConsent: StateFlow<IntentSenderRequest?> = authSession.pendingConsent

    override fun resolveConsent(data: Intent?) = authSession.resolveConsent(data)

    override suspend fun backup(): DriveBackupResult {
        val content = try {
            repository.exportCsv()
        } catch (sqlException: SQLException) {
            return DriveBackupResult.ExportFailed("Database error while exporting CSV.")
        } catch (ioException: IOException) {
            return DriveBackupResult.ExportFailed("Could not export CSV: ${ioException.message ?: "unknown error"}.")
        }

        val rowCount = countCsvDataRows(content)
        if (rowCount == 0) {
            return DriveBackupResult.EmptyDataset
        }

        return when (val auth = authSession.authorize()) {
            is DriveAuthResult.Cancelled -> DriveBackupResult.SignInCancelled
            is DriveAuthResult.Failed -> DriveBackupResult.SignInFailed(auth.reason)
            is DriveAuthResult.Authorized -> {
                val request = DriveUploadRequest(driveBackupFileName(today()), CSV_MIME_TYPE, content)
                try {
                    uploader.upload(auth.accessToken, request)
                    sessionStore.rememberAccount(auth.accountName)
                    DriveBackupResult.Success(
                        fileName = request.fileName,
                        rowCount = rowCount,
                        byteCount = request.content.size,
                    )
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (uploadFailure: Exception) {
                    DriveBackupResult.UploadFailed(uploadFailure.message ?: "unknown error")
                }
            }
        }
    }

    /** Counts CSV data rows (the file always starts with the header line). */
    private fun countCsvDataRows(content: ByteArray): Int {
        return content.toString(Charsets.UTF_8).count { it == '\n' } - 1
    }

    companion object {
        /** MIME type of the exported CSV, matching the export's CreateDocument contract. */
        const val CSV_MIME_TYPE = "text/csv"

        /** Backup file name for [date], e.g. `Monefy.Backup.2026-11-22.csv`. */
        fun driveBackupFileName(date: LocalDate): String {
            return "Monefy.Backup." + DateTimeFormatter.ISO_LOCAL_DATE.format(date) + ".csv"
        }
    }
}
