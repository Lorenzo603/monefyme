---
status: todo
attempts: 0
title: "Export transactions to CSV"
source: backlog/csv-backup-restore.md
depends_on: nothing
---

## Task

Add a "Export to CSV" feature to the restore flow. Restore (import) already exists end to end;
this task adds the matching export direction, where the user picks a file in the filesystem and
the app writes every stored transaction to it in the same CSV structure the importer reads.

Context to preserve (do not restructure):
- Import path: `RestoreScreen` (`app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt`)
  → `MainViewModel.importCsv(content: ByteArray)` → `BudgetRepository.importCsv(inputStream: InputStream)`
  → `MonefyCsvParser` / `Monefy.Data.29-10-2026.csv` (tracked sample export, same layout).
- The restore screen currently offers a single "Select CSV file" button (`ActivityResultContracts.OpenDocument`).
  Add the export next to it; keep the restore flow unchanged.

Requirements:

1. **Source data:** ALL transactions across all months (not only the month currently displayed),
   with their date, account, category display name, signed amount, currency, description, and type.
   Note the stored model only keeps `amountCents > 0` with a `type` of `INCOME`/`EXPENSE`, so the
   signed amount is derived, not taken from a stored value.
2. **CSV layout (must match the importer, see `Monefy.Data.29-10-2026.csv`):**
   - Header row: `date,account,category,amount,currency,converted amount,currency,description`
   - One row per transaction: `dd/MM/uuuu` date (strict, e.g. `03/11/2026`), then `account`,
     `category`, `amount`, `currency`, then the same `amount`/`currency` again (the `converted amount`
     columns, mirroring the sample), then `description`.
   - `amount`: signed and plain decimals with `.` separator (dot, not comma) and no thousands
     separators. Negative for `EXPENSE`, positive for `INCOME`. No trailing zeros
     (e.g. `-50`, `-37.5`, `1000`).
   - `currency`: `EUR` when blank, as the importer defaults it.
   - Fields containing commas, quotes, or newlines are quoted and double-quoted (RFC 4180 style,
     e.g. `"Lunch, coffee"`, `"He said ""hi"""`), so the file re-parses with `MonefyCsvParser`.
   - Rows must be ordered stably (e.g. by date, ascending) — no requirement of random order.
3. **Save flow:** the user picks the destination with `ActivityResultContracts.CreateDocument`
   (MIME type suitable for CSV, allowing `.csv`, suggested file name like
   `Monefy.Export.yyyy-MM-dd.csv`). The export is written via the Uri's `ContentResolver` stream.
   Empty dataset still writes a valid header-only CSV.
4. **Failure handling:** an unreadable/unwritable destination, or any write error, is surfaced to
   the user through the existing `viewModel.message` mechanism and must not crash or leak an
   uncaught exception.
5. **Wiring:** add the export through `MainViewModel` and `BudgetRepository` following the existing
   import pattern (e.g. `exportCsv()` returning the rows/bytes that get written, DB reads on the
   IO dispatcher). Keep the change minimal — do not refactor unrelated code.
6. **UI:** add an "Export to CSV" button to the Restore screen, alongside the existing import
   button, plus a small status/summary line showing the export result (rows written) or the error.
   Restore behavior must be unchanged.

Tests (a test change is REQUIRED with any main change, this repo enforces it):
- Unit tests (e.g. `app/src/test/java/com/lorenzo/monefyme/...`): exporter/repository coverage at
  minimum for: exact header + row layout, `dd/MM/uuuu` dates, sign handling (negative expense /
  positive income), no trailing zeros, comma/quote/newline quoting and escaping, empty dataset
  (header only), and a round-trip — parse the exported content with the real `MonefyCsvParser`
  and compare date/account/category/amount/currency/description per row.
- If the UI is touched, keep the existing UI/structure tests green; add a small test if the new
  button or status line is asserted by existing tests.

## Definition of done

- [ ] Restore screen offers "Export to CSV"; the chosen destination receives the CSV described above
- [ ] Export contains ALL transactions across all months, with the 8-column layout, sign per type,
      `dd/MM/uuuu` dates, dot decimals, no trailing zeros, RFC 4180 quoting, and a stable row order
- [ ] The exported file re-parses through the real `MonefyCsvParser`, and values match the source
      transactions (date/account/category/amount/currency/description), including an empty dataset
      writing a valid header-only file
- [ ] Write failure surfaces a user-visible message via the existing `viewModel.message` channel; no crash
- [ ] Unit tests added/updated under `app/src/test` covering the exporter (layout, signs, quoting,
      empty dataset, round-trip through the parser)
- [ ] `./gradlew testDebugUnitTest` is green
