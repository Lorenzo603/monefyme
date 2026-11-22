package com.lorenzo.monefyme.data.backup

import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.StateFlow

/** Outcome of the interactive Google sign-in for the Drive backup. */
sealed interface DriveAuthResult {
    /** Sign-in succeeded; [accessToken] authorizes one Drive upload and [accountName] identifies the user. */
    data class Authorized(val accessToken: String, val accountName: String) : DriveAuthResult

    /** The user dismissed the sign-in prompt; nothing was uploaded. */
    data object Cancelled : DriveAuthResult

    /** Sign-in failed (denied, no Google account on the device, network problem, ...). */
    data class Failed(val reason: String) : DriveAuthResult
}

/**
 * Performs the Google sign-in needed by the Drive backup. When Google needs user interaction it
 * publishes an [IntentSenderRequest] on [pendingConsent]; the UI launches it through an
 * ActivityResult launcher and hands the result data back via [resolveConsent] (null = cancelled),
 * which lets [authorize] resume.
 */
interface DriveAuthSession {
    /** Non-null while the sign-in consent UI must be launched by the UI layer. */
    val pendingConsent: StateFlow<IntentSenderRequest?>

    /** Suspends until the user is signed in with the Drive scope, cancelled, or failed. */
    suspend fun authorize(): DriveAuthResult

    /** Delivers the consent activity result data back to a suspended [authorize] call. */
    fun resolveConsent(data: Intent?)
}
