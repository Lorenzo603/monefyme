package com.lorenzo.monefyme.data.backup

import kotlinx.coroutines.flow.Flow

/**
 * Remembers the Google account used for Drive backups across process restarts, so the UI can
 * show the signed-in state without any interactive sign-in. Only the account name is kept —
 * nothing else about the session is persisted here.
 */
interface DriveSessionStore {
    /** The last backed-up-with account name, or null while the user never signed in. */
    val accountFlow: Flow<String?>

    /** Persists [accountName] as the remembered signed-in account. */
    suspend fun rememberAccount(accountName: String)
}
