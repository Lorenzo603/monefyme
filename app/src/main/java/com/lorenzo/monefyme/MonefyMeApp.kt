package com.lorenzo.monefyme

import android.app.Application
import com.lorenzo.monefyme.data.backup.DataStoreDriveSessionStore
import com.lorenzo.monefyme.data.backup.DriveBackup
import com.lorenzo.monefyme.data.backup.DriveBackupController
import com.lorenzo.monefyme.data.backup.HttpDriveUploader
import com.lorenzo.monefyme.data.backup.PlayServicesDriveAuthSession
import com.lorenzo.monefyme.data.local.AppDatabase
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.data.repository.BudgetRepositoryImpl
import com.lorenzo.monefyme.ui.category.DefaultCategories

class MonefyMeApp : Application() {
    /**
     * The "Back up to Google Drive" use case, or null when the build has no Google OAuth client
     * id (see the `google.oauth.client.id` entry in the gitignored local.properties). When null
     * the Restore screen reports that Drive backup is not configured instead of signing in.
     */
    val driveBackup: DriveBackup? by lazy {
        val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        if (clientId.isBlank()) {
            null
        } else {
            DriveBackupController(
                repository = repository,
                authSession = PlayServicesDriveAuthSession(this, clientId),
                uploader = HttpDriveUploader(),
                sessionStore = DataStoreDriveSessionStore(this),
            )
        }
    }

    val repository: BudgetRepository by lazy {
        val seedCategories = DefaultCategories.all.map { it.normalizedName to it.displayName }
        val database = AppDatabase.getInstance(this, seedCategories)
        BudgetRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
        )
    }
}
