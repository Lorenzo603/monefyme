---
status: done
attempts: 1
title: "Back up transactions CSV to Google Drive"
source: backlog/gdrive-backups.md
depends_on: csv-backup-restore
---

## Task

Add an automatic "Back up to Google Drive" flow to the backup/restore screen: the same CSV
exported by the CSV export task (`csv-backup-restore`) is uploaded to the user's Google Drive.

This task is gated on `csv-backup-restore` (the export must exist first — this task consumes
its output). No Drive integration exists in this repo yet; nothing about a Drive account or
OAuth client may be invented or committed — see "Human configuration" below.

Context to preserve:
- Restore/backup UI in `RestoreScreen` (`app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt`),
  wired through `MainViewModel` (`postMessage(...)`) and `BudgetRepository`.
- The export itself lives in the same feature area added by the export task (all months, same
  8-column CSV layout, written to a destination the user picks). Reuse it — do not reimplement CSV
  generation here — either by calling the export producing in-memory CSV bytes or by reusing its
  output.

Requirements:

1. **Sign-in:** add Google sign-in for the user (e.g. Google Sign-In via Credential Manager /
   Play Services Auth) and request the Drive scope the upload needs. If the user is not signed in,
   sign in before uploading. Cancelled or denied sign-in must not crash — surface it through the
   existing `viewModel.message` channel and stop.
2. **Upload:** upload the CSV as a file named `Monefy.Backup.yyyy-MM-dd.csv` (today's date,
   matching the CSV export's naming) to the user's Drive — pick one and document it: the Drive root
   of My Drive, or an app-created folder. Use the Drive API (REST or the client library, with the
   authenticated HTTP client) rather than the undocumented wire trick. MIME type should match the
   exported CSV's. Do NOT try to upload when sign-in is not possible (e.g. no client configured) —
   report that instead.
3. **UI:** add a "Back up to Google Drive" button to the Restore screen next to the export flow,
   with a progress indicator and success/failure feedback through the existing `viewModel.message`
   mechanism (and/or a small summary line, matching the export task's UI). Restore behavior must
   be unchanged.
4. **Failure handling:** sign-in cancellation/denial, missing configuration, upload failure, and
   empty-dataset uploads are all reported as messages — never an uncaught exception.
5. **Persistence:** remember the user's sign-in state across process restarts (e.g. DataStore, as
   already used in this repo) so the user does not sign in again on every app launch. Do not
   persist anything beyond the account/session state needed to resume.
6. **Human configuration (document, do not commit):** the repo contains no Google Cloud project,
   OAuth client ID, or branding. Obtain the OAuth client ID from `local.properties` (or an
   equivalent gitignored config source) via a `BuildConfig`-injected constant, and make the
   upload disabled/unavailable with a clear "not configured" message when it is absent. Document
   in the PR/summary what a human must do (enable the Drive API in the Google Cloud project,
   register the OAuth client for this app's package/signing, and drop the client ID into
   `local.properties`). No secrets, API keys, or client secrets may be committed anywhere.
7. **Minimal change:** do not refactor unrelated code; follow existing patterns (DI, DataStore,
   Compose wiring) already present in the repo.

Tests (a test change is REQUIRED with any main change; this repo enforces it):
- Unit tests under `app/src/test` for all logic that is testable without network or an account:
  the upload module's request construction (file name, MIME type, size/structure of the CSV
  bytes it receives), the "not configured" path, and error handling (sign-in denial, upload
  failure) — driving it through a fake/in-memory upload client, never the real Drive API.
- Do not attempt to reach the real Drive API from unit tests.
- Existing tests stay green.

## Definition of done

- [ ] "Back up to Google Drive" button on the Restore screen; triggers export + upload
- [ ] Google sign-in with the Drive scope; sign-in state remembered across restarts
- [ ] CSV uploaded as `Monefy.Backup.yyyy-MM-dd.csv`, byte-identical content to the CSV export
- [ ] Sign-in cancel/deny, missing configuration, and upload failure are reported as messages
- [ ] OAuth client configuration is gitignored/documented; no secrets committed; upload degrades
      gracefully when nothing is configured
- [ ] Unit tests cover upload request construction and failure handling via a fake client
- [ ] `./gradlew testDebugUnitTest` is green

## Run log

-   AGENTS.md read & echoed already (source `app/src/main/*`; tests `app/src/test/*`,`app/src/androidTest/*`; verify `./gradlew testDebugUnitTest`; env `set-env-variables`). Inspecting the failing test sources:
-   Root causes found. Fixing: (a) `toByteArray` bound to only last string literal, (b) suspend call in non-suspend lambda, (c) duplicate imports, (d) missing `override`:
-   AGENTS.md read/echoed (source `app/src/main/*`; tests `app/src/test/*`,`app/src/androidTest/*`; verify `./gradlew testDebugUnitTest`; env `set-env-variables`). Applying fixes:
-   Read/echo of AGENTS.md stands (source `app/src/main/*`; tests `app/src/test/*`,`app/src/androidTest/*`; verify `./gradlew testDebugUnitTest`; env `set-env-variables`). Removing the duplicate imports and fixing the suspend lambda:
-   AGENTS.md followed (read; config echoed above). Re-running tests:
-   Unit tests green. Verifying the full debug APK still builds (the production Play Services code and manifest):
-   Build green. Sanity-checking the generated BuildConfig default (unconfigured) and test counts:
-   All 293 unit tests pass (0 failures/errors), BuildConfig defaults to empty client ID ("not configured"), and the APK assembles. Final scope check:
-   Everything is green and scoped. Final verification run of the harness command was `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (293 tests, 0 failures, 0 errors), plus `./gradlew assembleDebug` succeeds.
-   ## Summary of changes
-   **New "Back up to Google Drive" flow** (reuses the CSV export; uploads byte-identical bytes to the **root of My Drive**, documented choice):
-   - `app/src/main/java/com/lorenzo/monefyme/data/backup/` (new package):
-   - `DriveUploadRequest.kt` / `DriveUploader.kt` — upload request (file name, MIME type, CSV bytes) + fakeable upload client interface
-   - `DriveAuthResult.kt` / `DriveAuthSession.kt` — sign-in outcome (Authorized/Cancelled/Failed) + fakeable auth session
-   - `DriveSessionStore.kt` + `DataStoreDriveSessionStore.kt` — remembers the signed-in account across restarts via DataStore (only the account name)
-   - `DriveBackup.kt` / `DriveBackupResult.kt` / `DriveBackupController.kt` — orchestrator: reuses `BudgetRepository.exportCsv()` (no CSV reimplementation), empty-dataset guard before sign-in, upload as `Monefy.Backup.yyyy-MM-dd.csv` / `text/csv`, never throws
-   - `PlayServicesDriveAuthSession.kt` — Google sign-in via Play Services Credential Manager authorization (`Identity.getAuthorizationClient`), `drive.file` scope with offline access; consent intent published for the UI to launch
-   - `HttpDriveUploader.kt` — Drive API v3 multipart REST upload with the OAuth bearer token
-   - `app/build.gradle.kts` — `buildConfig = true`; `GOOGLE_OAUTH_CLIENT_ID` BuildConfig field read from the **gitignored** `local.properties` (`google.oauth.client.id=...`) or `-PGOOGLE_OAUTH_CLIENT_ID`; adds `play-services-auth:21.3.0`
-   - `AndroidManifest.xml` — INTERNET permission + Play Services auth `<queries>`
-   - `MainViewModel`/`MainViewModelFactory`/`MonefyMeApp`/`MainActivity` — optional `DriveBackup` injection (null when unconfigured → "not configured" message, no sign-in attempt); `backupToGoogleDrive()`, `isBackingUpToDrive`, `driveAccount`, consent forwarding
-   - `RestoreScreen.kt` — "Back up to Google Drive" button + progress spinner + account summary line + consent `StartIntentSenderForResult` launcher; restore/export flows untouched
-   - `README.md` — human configuration section (enable Drive API, register OAuth client for package/signing, add client ID to `local.properties`; no secrets committed)
-   **Tests added** (`app/src/test`, all via fakes — real Drive API never reached):
-   - `data/backup/DriveBackupControllerTest.kt` (11): request construction (file name, MIME, byte-identical content, token), sign-in cancel/deny, upload failure (+ unexpected exception), empty dataset skips sign-in/upload, export failures, account survives controller restart, consent delegation
-   - `data/backup/HttpDriveUploaderRequestTest.kt` (5): multipart body/metadata construction
-   - `MainViewModelTest.kt` (+10), `MainViewModelFactoryTest.kt` (+2): not-configured, success/cancel/fail/empty messages, no-throw on unexpected errors, overlapping-run guard, account flow
-   - `RestoreScreenDriveBackupWiringTest.kt` (5): button/progress/consent-launcher wiring; restore behavior preserved
-   **Caveats / follow-ups for a human:** the actual OAuth end-to-end (real consent + upload) can't be exercised until the OAuth client ID is provisioned per the README section; unconfigured builds intentionally degrade to a message. Instrumented UI tests were not extended (kept out of harness verify commands per AGENTS.md).
-   STATUS: DONE Added Google Drive backup (button, Credential Manager sign-in with drive.file scope, Drive API v3 multipart upload of the exported CSV as Monefy.Backup.yyyy-MM-dd.csv to My Drive root, DataStore-remembered account, BuildConfig/local.properties OAuth client config, not-configured degradation) with 23 new/updated unit tests; verified with ./gradlew testDebugUnitTest (293 tests, 0 failures) and ./gradlew assembleDebug (green)
