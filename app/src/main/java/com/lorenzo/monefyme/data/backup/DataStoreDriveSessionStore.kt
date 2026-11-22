package com.lorenzo.monefyme.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [DriveSessionStore]: remembers only the Google account name used for Drive
 * backups so the signed-in state survives process restarts. No token is stored here; Play
 * Services manages the OAuth grant itself.
 */
class DataStoreDriveSessionStore(context: Context) : DriveSessionStore {
    private val dataStore: DataStore<Preferences> = context.applicationContext.driveSessionDataStore

    override val accountFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACCOUNT_NAME_KEY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun rememberAccount(accountName: String) {
        dataStore.edit { preferences ->
            preferences[ACCOUNT_NAME_KEY] = accountName
        }
    }

    companion object {
        private val ACCOUNT_NAME_KEY = stringPreferencesKey("drive_backup_account_name")
        private val Context.driveSessionDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "drive_backup_session",
        )
    }
}
