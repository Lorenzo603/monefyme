package com.lorenzo.monefyme.data.backup

/** Outcome of one "Back up to Google Drive" run; every case maps to a user-visible message. */
sealed interface DriveBackupResult {
    /** The CSV was uploaded as [fileName] with [rowCount] data rows and [byteCount] bytes. */
    data class Success(val fileName: String, val rowCount: Int, val byteCount: Int) : DriveBackupResult

    /** The dataset only has the header, so nothing was uploaded and no sign-in was requested. */
    data object EmptyDataset : DriveBackupResult

    /** Exporting the CSV from the database failed. */
    data class ExportFailed(val reason: String) : DriveBackupResult

    /** The user dismissed the Google sign-in prompt. */
    data object SignInCancelled : DriveBackupResult

    /** Google sign-in failed or was denied. */
    data class SignInFailed(val reason: String) : DriveBackupResult

    /** Drive rejected or could not receive the upload. */
    data class UploadFailed(val reason: String) : DriveBackupResult
}
