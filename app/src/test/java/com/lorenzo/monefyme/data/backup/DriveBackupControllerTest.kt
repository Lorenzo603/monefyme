package com.lorenzo.monefyme.data.backup

import androidx.activity.result.IntentSenderRequest
import android.content.Intent
import android.database.SQLException
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the "Back up to Google Drive" orchestration through fakes only: the request it builds
 * (file name, MIME type, byte-identical CSV content), the empty-dataset guard, sign-in
 * cancellation/denial, upload failure, and the sign-in state persisted across a restart.
 * The real Drive API is never touched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DriveBackupControllerTest {
    private val fixedToday = LocalDate.of(2026, 2, 3)
    private val csvBytes = (
        "date,account,category,amount,currency,converted amount,currency,description\n" +
            "01/02/2026,Cash,Food,-12.5,EUR,-12.5,EUR,Groceries\n" +
            "02/02/2026,Bank,Salary,1000,EUR,1000,EUR,February\n"
        ).toByteArray(Charsets.UTF_8)

    private val repository = FakeRepository()
    private val authSession = FakeAuthSession()
    private val uploader = RecordingUploader()
    private val sessionStore = InMemoryDriveSessionStore()

    private fun controller(): DriveBackupController = DriveBackupController(
        repository = repository,
        authSession = authSession,
        uploader = uploader,
        sessionStore = sessionStore,
        today = { fixedToday },
    )

    @Test
    fun successUploadsExportedBytesAsDatedCsvToTheFakeClient() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Authorized("access-token", "user@example.com")

        val result = controller().backup()

        assertEquals(DriveBackupResult.Success("Monefy.Backup.2026-02-03.csv", 2, csvBytes.size), result)

        val request = uploader.requests.single()
        assertEquals("Monefy.Backup.2026-02-03.csv", request.fileName)
        assertEquals("text/csv", request.mimeType)
        assertTrue(
            "The uploaded bytes must be byte-identical to the CSV export output.",
            request.content.contentEquals(csvBytes),
        )
        assertEquals(csvBytes.size, request.content.size)
        assertEquals("access-token", uploader.accessTokens.single())
        assertEquals(listOf("user@example.com"), sessionStore.remembered)
    }

    @Test
    fun signInCancellationStopsBeforeUploading() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Cancelled

        val result = controller().backup()

        assertEquals(DriveBackupResult.SignInCancelled, result)
        assertTrue(uploader.requests.isEmpty())
        assertTrue(sessionStore.remembered.isEmpty())
    }

    @Test
    fun signInFailureReportsTheReasonWithoutUploading() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Failed("no Google account on this device")

        val result = controller().backup()

        assertEquals(DriveBackupResult.SignInFailed("no Google account on this device"), result)
        assertTrue(uploader.requests.isEmpty())
    }

    @Test
    fun uploadFailureIsReportedAndAccountIsNotRemembered() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Authorized("access-token", "user@example.com")
        uploader.error = IOException("Google Drive rejected the upload with HTTP 403.")

        val result = controller().backup()

        assertTrue(result is DriveBackupResult.UploadFailed)
        assertTrue(
            (result as DriveBackupResult.UploadFailed).reason.contains("HTTP 403"),
        )
        assertTrue(sessionStore.remembered.isEmpty())
    }

    @Test
    fun uploadFailureFromUnexpectedExceptionTypeIsStillReportedNotThrown() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Authorized("access-token", "user@example.com")
        uploader.error = IllegalStateException("client exploded")

        val result = controller().backup()

        assertEquals(DriveBackupResult.UploadFailed("client exploded"), result)
    }

    @Test
    fun emptyDatasetSkipsSignInAndUploadAndIsReported() = runTest {
        repository.exportedCsv = "date,account,category,amount,currency,converted amount,currency,description\n".toByteArray()

        val result = controller().backup()

        assertEquals(DriveBackupResult.EmptyDataset, result)
        assertEquals(0, authSession.authorizeCalls)
        assertTrue(uploader.requests.isEmpty())
    }

    @Test
    fun exportFailureIsReportedBeforeAnySignIn() = runTest {
        repository.exportCsvError = SQLException("db boom")

        val result = controller().backup()

        assertTrue(result is DriveBackupResult.ExportFailed)
        assertEquals(0, authSession.authorizeCalls)
        assertTrue(uploader.requests.isEmpty())
    }

    @Test
    fun exportIoFailureKeepsTheUnderlyingReason() = runTest {
        repository.exportCsvError = IOException("disk unreadable")

        val result = controller().backup()

        assertEquals(
            DriveBackupResult.ExportFailed("Could not export CSV: disk unreadable."),
            result,
        )
    }

    @Test
    fun signedInAccountSurvivesAControllerRestart() = runTest {
        repository.exportedCsv = csvBytes
        authSession.nextResult = DriveAuthResult.Authorized("access-token", "user@example.com")

        controller().backup()

        // A fresh controller over the same store (a new ViewModel after a process restart)
        // still sees the remembered account without another sign-in.
        val restarted = controller()
        assertEquals("user@example.com", restarted.signedInAccount.first())
    }

    @Test
    fun pendingConsentAndResolveConsentAreDelegatedToTheAuthSession() = runTest {
        val backup = controller()

        assertSame(authSession.pendingConsent, backup.pendingConsent)
        assertNull(backup.pendingConsent.value)

        backup.resolveConsent(null)
        assertTrue(authSession.resolvedWithNull)
    }

    @Test
    fun driveBackupFileNameUsesIsoDateOfTheGivenDay() {
        assertEquals("Monefy.Backup.2026-02-03.csv", DriveBackupController.driveBackupFileName(fixedToday))
        assertEquals(
            "Monefy.Backup.2026-12-09.csv",
            DriveBackupController.driveBackupFileName(LocalDate.of(2026, 12, 9)),
        )
    }

    private class FakeAuthSession : DriveAuthSession {
        var nextResult: DriveAuthResult = DriveAuthResult.Cancelled
        var authorizeCalls = 0
        var resolvedWithNull = false
        override val pendingConsent = MutableStateFlow<IntentSenderRequest?>(null)

        override suspend fun authorize(): DriveAuthResult {
            authorizeCalls += 1
            return nextResult
        }

        override fun resolveConsent(data: Intent?) {
            resolvedWithNull = data == null
        }
    }

    private class RecordingUploader : DriveUploader {
        val requests = mutableListOf<DriveUploadRequest>()
        val accessTokens = mutableListOf<String>()
        var error: Throwable? = null

        override suspend fun upload(accessToken: String, request: DriveUploadRequest) {
            accessTokens += accessToken
            requests += request
            error?.let { throw it }
        }
    }

    private class InMemoryDriveSessionStore : DriveSessionStore {
        val remembered = mutableListOf<String>()
        override val accountFlow = MutableStateFlow<String?>(null)

        override suspend fun rememberAccount(accountName: String) {
            remembered += accountName
            accountFlow.value = accountName
        }
    }

    private class FakeRepository : BudgetRepository {
        var exportedCsv: ByteArray? = null
        var exportCsvError: Throwable? = null

        override fun observeCategoryNames(): Flow<List<String>> = MutableStateFlow(emptyList())
        override fun observeCategories(): Flow<List<CategoryItem>> = MutableStateFlow(emptyList())
        override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> =
            MutableStateFlow(emptyList())
        override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> =
            MutableStateFlow(emptyList())
        override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int) = true
        override suspend fun updateCategory(categoryId: Long, displayName: String, iconName: String, colorArgb: Int) = true
        override suspend fun deleteCategory(categoryId: Long) = true
        override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft) = true
        override suspend fun deleteTransaction(transactionId: Long) {}
        override suspend fun importCsv(inputStream: InputStream): CsvImportResult {
            inputStream.use { }
            return CsvImportResult(0, 0, 0, 0)
        }

        override suspend fun exportCsv(): ByteArray {
            exportCsvError?.let { throw it }
            return exportedCsv ?: ByteArray(0)
        }
    }
}
