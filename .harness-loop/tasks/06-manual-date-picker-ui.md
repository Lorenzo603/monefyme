---
status: todo
attempts: 0
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
