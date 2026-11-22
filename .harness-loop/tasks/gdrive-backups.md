---
status: todo
attempts: 0
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
