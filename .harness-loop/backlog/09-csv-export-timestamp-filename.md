---
status: todo
attempts: 1
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

-   STATUS: todo
