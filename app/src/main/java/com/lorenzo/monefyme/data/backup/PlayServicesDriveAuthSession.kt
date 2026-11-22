package com.lorenzo.monefyme.data.backup

import androidx.activity.result.IntentSenderRequest
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [DriveAuthSession] backed by Google Play Services (Credential Manager authorization via
 * `Identity.getAuthorizationClient`): requests the Drive file scope with offline access for the
 * configured OAuth client. Once the user granted access, subsequent calls usually return a fresh
 * access token without any UI, which keeps the sign-in state usable across process restarts;
 * the first call publishes a consent intent that the UI must launch.
 */
class PlayServicesDriveAuthSession(
    context: Context,
    private val clientId: String,
) : DriveAuthSession {
    private val appContext: Context = context.applicationContext
    private val _pendingConsent = MutableStateFlow<IntentSenderRequest?>(null)
    override val pendingConsent: StateFlow<IntentSenderRequest?> = _pendingConsent.asStateFlow()

    private var consentRequester: CompletableDeferred<Intent?>? = null

    override suspend fun authorize(): DriveAuthResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .requestOfflineAccess(clientId)
            .build()

        val result = try {
            authorizationClient().awaitAuthorizationResult(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return DriveAuthResult.Failed(failure.message ?: "Google sign-in failed.")
        }

        if (result.hasResolution()) {
            return requestUserConsent(result)
        }
        return toAuthResult(result)
    }

    private suspend fun requestUserConsent(resolution: AuthorizationResult): DriveAuthResult {
        val pendingIntent = resolution.pendingIntent
            ?: return DriveAuthResult.Failed("Google did not provide a sign-in prompt.")
        val deferred = CompletableDeferred<Intent?>()
        consentRequester = deferred
        _pendingConsent.value = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        val resultData = try {
            deferred.await()
        } finally {
            consentRequester = null
            _pendingConsent.value = null
        }

        if (resultData == null) {
            return DriveAuthResult.Cancelled
        }

        val resolved = try {
            authorizationClient().getAuthorizationResultFromIntent(resultData)
        } catch (apiException: ApiException) {
            return DriveAuthResult.Failed(apiException.message ?: "Google sign-in was denied.")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return DriveAuthResult.Failed(failure.message ?: "Google sign-in was denied.")
        }
        return toAuthResult(resolved)
    }

    override fun resolveConsent(data: Intent?) {
        consentRequester?.complete(data)
    }

    private fun authorizationClient(): AuthorizationClient = Identity.getAuthorizationClient(appContext)

    private fun toAuthResult(result: AuthorizationResult): DriveAuthResult {
        val accessToken = result.accessToken
        if (accessToken.isNullOrEmpty()) {
            return DriveAuthResult.Failed("Google did not return an access token.")
        }
        val accountName = result.toGoogleSignInAccount()?.email ?: SIGNED_IN_WITHOUT_EMAIL
        return DriveAuthResult.Authorized(accessToken = accessToken, accountName = accountName)
    }

    /** Awaits the Play Services [com.google.android.gms.tasks.Task] without blocking a UI thread. */
    private suspend fun AuthorizationClient.awaitAuthorizationResult(
        request: AuthorizationRequest,
    ): AuthorizationResult = suspendCancellableCoroutine { continuation ->
        val executor = Executors.newSingleThreadExecutor()
        val task = authorize(request)
        task.addOnSuccessListener(executor) { authorizationResult ->
            executor.shutdown()
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(authorizationResult))
            }
        }
        task.addOnFailureListener(executor) { failure ->
            executor.shutdown()
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(failure))
            }
        }
        continuation.invokeOnCancellation {
            executor.shutdown()
        }
    }

    companion object {
        /** Drive scope allowing the app to create/manage only the files it created, enough for the backup. */
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val SIGNED_IN_WITHOUT_EMAIL = "Google account"
    }
}
