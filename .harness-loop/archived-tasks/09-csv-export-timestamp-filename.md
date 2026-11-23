---
status: done
attempts: 2
title: "Add timestamp to CSV export filename"
source: backlog.md
depends_on: nothing
---

## Task

Change the default filename used when the user exports transactions to CSV in the Restore
screen (`defaultExportFileName()` in `app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt`).

Current behavior: the file name is `Monefy.Export.yyyy-MM-dd.csv` (date only, e.g.
`Monefy.Export.2026-11-23.csv`), so two exports on the same day collide.

Required behavior:

- The default file name gains a time component (hours, minutes, seconds) to avoid filename
  conflicts, using this exact pattern: `Monefy.Export.yyyy-MM-dd.HH-mm-ss.csv`
  (e.g. `Monefy.Export.2026-11-23.20-41-24.csv`).
- The file is still exported via the existing `CreateDocument("text/csv")` launcher, so only
  the string produced by `defaultExportFileName()` changes — do not change the export flow,
  content, or the Google Drive backup file name (`Monefy.Backup.yyyy-MM-dd.csv`), which is
  intentionally left unchanged.
- Format with a `DateTimeFormatter`/`LocalDateTime.now()` (or equivalent); ISO hyphen-style
  separators are fine, but do not create characters that are invalid in filenames.

Tests (a test change is REQUIRED with any main change):
- Update the existing export wiring test `app/src/test/java/com/lorenzo/monefyme/ui/screen/RestoreScreenExportWiringTest.kt`
  (or add a small test in an existing test file) to assert the new name pattern, e.g. by
  matching `Monefy.Export.<yyyy-MM-dd>.<HH-mm-ss>.csv`.

## Definition of done

- [ ] `defaultExportFileName()` returns `Monefy.Export.yyyy-MM-dd.HH-mm-ss.csv`
- [ ] Existing export flow (CreateDocument launcher, CSV content, Drive backup) unchanged
- [ ] Tests updated/added to cover the new pattern; `./gradlew testDebugUnitTest` is green

## Run log

- 2026-11-23 21:18:30 attempt 2: status=done exit=0 log=09-csv-export-timestamp-filename-run2.log changed `defaultExportFileName()` in `app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt` to emit `Monefy.Export.<yyyy-MM-dd>.<HH-mm-ss>.csv` (LocalDateTime.now() + `yyyy-MM-dd.HH-mm-ss` pattern); update the export wiring test `RestoreScreenExportWiringTest.kt` to assert the new time-component pattern. Export flow, CSV content, and the Drive backup name are untouched. Verified with `./gradlew testDebugUnitTest` (BUILD SUCCESSFUL).
-   STATUS: DONE changed `defaultExportFileName()` in `app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt` to emit `Monefy.Export.<yyyy-MM-dd>.<HH-mm-ss>.csv` (LocalDateTime.now() + `yyyy-MM-dd.HH-mm-ss` pattern); update the export wiring test `RestoreScreenExportWiringTest.kt` to assert the new time-component pattern. Export flow, CSV content, and the Drive backup name are untouched. Verified with `./gradlew testDebugUnitTest` (BUILD SUCCESSFUL).
-   STATUS: DONE changed `defaultExportFileName()` in `app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt` to emit `Monefy.Export.<yyyy-MM-dd>.<HH-mm-ss>.csv` (LocalDateTime.now() + `yyyy-MM-dd.HH-mm-ss` pattern); update the export wiring test `RestoreScreenExportWiringTest.kt` to assert the new time-component pattern. Export flow, CSV content, and the Drive backup name are untouched. Verified with `./gradlew testDebugUnitTest` (BUILD SUCCESSFUL).
