package com.lorenzo.monefyme.data.backup

import androidx.activity.result.IntentSenderRequest
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The "Back up to Google Drive" use case: exports the same CSV as the CSV export task and
 * uploads it to the root of the user's My Drive. Null/unavailable when the OAuth client ID is
 * not configured in the build, in which case the UI reports "not configured" instead of signing in.
 */
interface DriveBackup {
    /** The remembered signed-in Google account name across restarts, or null when never signed in. */
    val signedInAccount: Flow<String?>

    /** Non-null while the Google consent UI must be launched by the screen. */
    val pendingConsent: StateFlow<IntentSenderRequest?>

    /** Delivers the consent activity result back to a suspended [backup] call. */
    fun resolveConsent(data: Intent?)

    /** Runs export + sign-in + upload; never throws, every failure comes back as a [DriveBackupResult]. */
    suspend fun backup(): DriveBackupResult
}
