---
status: done
attempts: 2
title: "Manual date entry: calendar icon returns to calendar picker, does not confirm"
source: backlog.md
depends_on: 10-date-picker-ok-button-and-current-date-marker
---

## Task

In the manual date entry modal (`ManualDateEntryDialog`, file
`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`) — opened by the
edit button in the main date picker's green header:

Current behavior: the calendar-day icon (`Icons.Filled.CalendarToday`, top-right of the modal's
green header, currently a plain `Icon` with no click action) is decoration only. The Save
button confirms the entered date immediately via `onConfirm(date)`, which closes the modal
and confirms the date in the parent picker.

Required behavior:

- The Save button is UNCHANGED: a valid entry confirms the date immediately (closes the modal
  and confirms the date in the parent picker); an invalid entry shows the error under
  `manual_date_error` and keeps the modal open.
- The header calendar icon becomes a CLICKABLE button that acts as "back to the calendar
  picker": it (a) closes the manual entry modal, (b) sets the parsed/entered date as the
  SELECTED date in the main picker (the header shows it, the calendar page scrolls to it if
  needed), and (c) does NOT confirm the date outright — the parent is not updated through
  `onDateSelected` (or any confirm callback). The main picker's OK button is what confirms.
  This mirrors the Cancel button in spirit except that the selection (not the parent date)
  is kept.
- If the entered date is invalid, the calendar icon must NOT close the modal and must NOT set
  the selection: it shows the same error as Save and stays open.
- Keep the test tags stable: `manual_date_header`, `manual_date_header_date`,
  `manual_date_input`, `manual_date_error`, `manual_date_cancel`, `manual_date_confirm`.
  Give the calendar icon its own tag (e.g. `manual_date_calendar_button`).
- The calendar icon is the ONLY clickable element in the header; do not change the edit
  button in the main picker or the Save/Cancel buttons.

Tests (a test change is REQUIRED with any main change):
- Update structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  and UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt` to
  cover: the Save path confirms immediately, the calendar icon returns to the picker with the
  date selected but unconfirmed, and the icon does not close the modal on invalid input.

## Definition of done

- [ ] Header calendar icon is a button that closes the manual entry modal and selects the
      entered date in the main picker without confirming
- [ ] Invalid input keeps the modal open (error shown) for both Save and the calendar icon
- [ ] Save behavior unchanged (immediate confirm)
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added

## Run log

-   - `ManualDateEntryDialog` gained an `onSelect` callback and a shared `validate()` helper (one error message, identical for Save and the calendar button).
-   - The header calendar icon is now a real `IconButton` (tag `manual_date_calendar_button`) that, on a valid entry, calls `onSelect` — closing the modal, making the entered date the pending selection in the main picker (header + pager scroll, via a shared `selectManualDate` helper) — without confirming to the caller. On invalid entry it shows the same error as Save and stays open.
-   - Save is unchanged: valid → confirms immediately via `onConfirm` (select + `onDateSelected`); invalid → error, stays open.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt` — 4 new structure tests: stable tags (incl. the new calendar button tag), the icon is an `IconButton` that is the only header control and routes through `onSelect` (not `onConfirm`), shared validation/error between Save and the calendar icon, and the parent wiring (`onSelect` → `selectManualDate` without `onDateSelected`; `onConfirm` → helper + `onDateSelected`).
-   - `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt` — 3 new UI tests: Save confirms immediately and closes; calendar icon returns to the picker with the date pending (unconfirmed, header shows it, OK then confirms); invalid input keeps the modal open with the error and no selection.
-   **Verified**
-   - `./gradlew testDebugUnitTest` — green (22/22 DatePickerModalRequirementsTest, no failures anywhere)
-   - `./gradlew connectedDebugAndroidTest` on the running emulator — green (19/19 DatePickerModalUiTest including the 3 new tests)
-   STATUS: DONE ./gradlew testDebugUnitTest and connectedDebugAndroidTest both pass; calendar icon now selects-without-confirming, Save unchanged, tests added/updated
-   Let me look at the util helpers and the screen caller briefly:
-   Now I'll make the main change. First the parent wiring:
-   Now the `ManualDateEntryDialog` itself:
-   Now the tests. First the unit structure test:
-   Now the UI test:
-   Now let's run the unit tests. First check the env setup:
-   Unit tests pass. The androidTest file was also updated — let me check whether an emulator is available to run it:
-   An emulator is running, so I'll run the instrumentation UI tests to verify the new coverage:
-   All 19 UI tests pass (0 failures), including the 3 new ones. Final review of the diff:
-   Everything is implemented and verified. Here's a summary:
-   **Changes**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`
-   - `ManualDateEntryDialog` gained an `onSelect` callback and a shared `validate()` helper (one error message, identical for Save and the calendar button).
-   - The header calendar icon is now a real `IconButton` (tag `manual_date_calendar_button`) that, on a valid entry, calls `onSelect` — closing the modal, making the entered date the pending selection in the main picker (header + pager scroll, via a shared `selectManualDate` helper) — without confirming to the caller. On invalid entry it shows the same error as Save and stays open.
-   - Save is unchanged: valid → confirms immediately via `onConfirm` (select + `onDateSelected`); invalid → error, stays open.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt` — 4 new structure tests: stable tags (incl. the new calendar button tag), the icon is an `IconButton` that is the only header control and routes through `onSelect` (not `onConfirm`), shared validation/error between Save and the calendar icon, and the parent wiring (`onSelect` → `selectManualDate` without `onDateSelected`; `onConfirm` → helper + `onDateSelected`).
-   - `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt` — 3 new UI tests: Save confirms immediately and closes; calendar icon returns to the picker with the date pending (unconfirmed, header shows it, OK then confirms); invalid input keeps the modal open with the error and no selection.
-   **Verified**
-   - `./gradlew testDebugUnitTest` — green (22/22 DatePickerModalRequirementsTest, no failures anywhere)
-   - `./gradlew connectedDebugAndroidTest` on the running emulator — green (19/19 DatePickerModalUiTest including the 3 new tests)
-   STATUS: DONE ./gradlew testDebugUnitTest and connectedDebugAndroidTest both pass; calendar icon now selects-without-confirming, Save unchanged, tests added/updated
