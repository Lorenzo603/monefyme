---
status: done
attempts: 1
title: "Improve manual date picker UI modal"
source: backlog/next.md
depends_on: nothing
---

## Task

Improve the manual date entry modal opened from the calendar date picker by its header edit
button (`ManualDateEntryDialog`, file `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`),
using the reference screenshot @.harness-loop/task-assets/manual-date-picker-modal.png.

Current behavior: three separate Day/Month/Year numeric inputs (test tags
`manual_date_day`, `manual_date_month`, `manual_date_year`), a short hint, the Save/Cancel
buttons, and confirmation via `parseManualDateOrNull(day, month, year)`. Keep the outcome
contract: confirming a valid date calls `onConfirm(LocalDate)`, which updates the parent picker.

Requirements:

- Header: add a header at the top of the modal showing the current date in the same style as
  the main calendar picker's header (green background `0xFF74C792`, centered, enlarged).
  Use the abbreviated format from task 05: e.g. `21 Nov 2026` (no weekday, 3-letter month).
  The date shown should be the one the user is editing in the parent picker (i.e. the date
  passed down to this modal as the current selection).
- Inputs: replace the three Day/Month/Year inputs with ONE single text input where the user
  types the full date, e.g. `21/11/2026` (dd/MM/yyyy). This should be the same `dd/MM/uuuu`
  format the app already uses for displayed dates.
- Validation: keep validating the single entered text; on invalid input show an error message
  (keep the `manual_date_error` behavior). On valid input call `onConfirm(date)`.
  Note the existing test tags `manual_date_day`, `manual_date_month` and `manual_date_year` and
  the `parseManualDateOrNull` helper will no longer be needed; use the app's date parser
  (`parseDateOrNull`) with the `dd/MM/yyyy` format for the single input.
- Keep the Save/Cancel buttons and the `manual_date_confirm` test tag (and the `manual_date_error`
  tag) — only the input shape changes. The edit button in the main picker header must keep
  opening this modal.

Tests (a test change is REQUIRED with any main change):
- Update the structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  (it currently asserts the 3-field manual modal and `parseManualDateOrNull` usage).
- Update the UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt`
  (currently types into `manual_date_day`/`manual_date_month`/`manual_date_year`).

## Definition of done

- [ ] Modal shows a green header with the current date in the abbreviated format (no weekday)
- [ ] One single text input replaces the three Day/Month/Year fields
- [ ] Invalid input shows a validation error; valid input confirms the date
- [ ] Save/Cancel buttons and `manual_date_confirm`/`manual_date_error` tags preserved
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added to cover the new layout

## Run log

-   - Bonus (emulator was running): `./gradlew connectedDebugAndroidTest` scoped to `DatePickerModalUiTest` — all 11 tests pass on `medium_phone`.
-   Assumptions: kept the "Save"/"Cancel" labels per the task text (the screenshot shows OK/CANCEL); chose `KeyboardType.Phone` for the input (Compose has no date keyboard; the phone keypad offers the `/` separator).
-   STATUS: DONE Manual date modal rebuilt with green current-date header + single dd/MM/yyyy input (DatePickerModal.kt), obsolete parseManualDateOrNull removed (DatePickerLogic.kt), requirements/UI/Logic tests updated; verified with ./gradlew testDebugUnitTest (229 green) plus DatePickerModalUiTest on the emulator (11 green).
-   Now I have the full picture. Let me make the changes, starting with the main component.
-   Now remove the obsolete helper from `DatePickerLogic.kt` and its unit tests:
-   Now update the requirements test:
-   Compose BOM 2024.06.00 supports `performTextReplacement`. Now set env and run the unit tests:
-   All unit tests pass. Now verify the androidTest source set compiles (it isn't covered by `testDebugUnitTest`):
-   Let me try running the UI tests on the emulator to verify the changed androidTest behavior:
-   All 11 UI tests pass on the emulator, including the two reworked manual-entry tests. Final check of the diff scope:
-   All checks green. Here's a summary of the work:
-   ## Changes
-   **`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`**
-   - Rewrote `ManualDateEntryDialog` to match the reference screenshot:
-   - Green header (`0xFF74C792`, tags `manual_date_header` / `manual_date_header_date`) showing the date being edited — the parent picker's current selection (`currentDate = selectedDate` is now passed down) — in the same centered, enlarged 26.sp compact format (`d MMM yyyy`, e.g. "21 Nov 2026") as the main picker header.
-   - Replaced the three Day/Month/Year fields with one `dd/MM/yyyy` text input (`manual_date_input`), pre-filled with the current date via `formatDate` (as in the screenshot), filtered to digits + `/`.
-   - Validation now uses the app's `parseDateOrNull` (strict `dd/MM/uuuu`); invalid input shows the error under `manual_date_error`, valid input calls `onConfirm(date)`.
-   - Kept Save (`manual_date_confirm`) and Cancel (`manual_date_cancel`) buttons; the header edit button still opens the modal.
-   - Deleted the now-unused `ManualDateField` composable and imports.
-   **`app/src/main/java/com/lorenzo/monefyme/ui/util/DatePickerLogic.kt`** — removed the obsolete `parseManualDateOrNull` helper (validation now covered by `parseDateOrNull`, already tested in `FormattersTest`).
-   **Tests**
-   - `DatePickerModalRequirementsTest.kt`: updated `headerEditButtonOpensTheValidatedManualDateModal` (single input, `parseDateOrNull`, old tags/helper gone, Save/Cancel + tags preserved) and added `manualModalShowsAGreenHeaderWithTheDateBeingEdited` (green header above the input, shows the passed-down date in the compact format, centered and ≥22.sp).
-   - `DatePickerLogicTest.kt`: removed the tests for the deleted helper.
-   - `DatePickerModalUiTest.kt`: both manual-entry tests now type the full date into `manual_date_input` (via `performTextReplacement`, since the field is pre-filled), and the positive test also asserts the green header date ("1 Oct 2025") and the pre-filled "01/10/2025".
-   ## Verification
-   - `./gradlew testDebugUnitTest` — 229 tests, 0 failures/errors.
-   - `./gradlew compileDebugAndroidTestKotlin` — compiles.
-   - Bonus (emulator was running): `./gradlew connectedDebugAndroidTest` scoped to `DatePickerModalUiTest` — all 11 tests pass on `medium_phone`.
-   Assumptions: kept the "Save"/"Cancel" labels per the task text (the screenshot shows OK/CANCEL); chose `KeyboardType.Phone` for the input (Compose has no date keyboard; the phone keypad offers the `/` separator).
-   STATUS: DONE Manual date modal rebuilt with green current-date header + single dd/MM/yyyy input (DatePickerModal.kt), obsolete parseManualDateOrNull removed (DatePickerLogic.kt), requirements/UI/Logic tests updated; verified with ./gradlew testDebugUnitTest (229 green) plus DatePickerModalUiTest on the emulator (11 green).
