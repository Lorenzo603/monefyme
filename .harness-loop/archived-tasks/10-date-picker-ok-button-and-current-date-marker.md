---
status: done
attempts: 2
title: "Date picker: explicit OK confirmation and current-date marker"
source: backlog.md
depends_on: nothing
---

## Task

In the calendar date picker modal (`DatePickerModal`, file
`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`) used for transaction
dates:

Current behavior: clicking a day immediately confirms the selection by calling
`onDateSelected(date)` — the parent updates right away. There is no OK button.

Required behavior:

- Clicking a day in the calendar only SELECTS the new date (the header keeps showing that
  date); the action is NOT confirmed yet. The current/applied date (the date passed into the
  picker as `currentDate`) must remain visible as a marker: a black circle outline drawn on
  top of that day's cell. If the user re-selects that same date, the new selection and the
  outline coincide — both still render on the same cell.
- The bottom buttons row becomes `[Cancel] [OK]` (Cancel keeps the current green text style;
  OK is the new confirming button). Only clicking OK confirms the selection: it calls
  `onDateSelected(selectedDate)`. Clicking Cancel dismisses the modal without confirming.
- The manual entry modal (`ManualDateEntryDialog`, opened via the header edit button) is
  unchanged by this task: a valid Save there still confirms immediately through
  `onConfirm(date)`. Note that after this change the main modal no longer confirms on day
  click, so the newly pending selection only becomes applied once the main modal's OK button
  is clicked.
- Keep the existing test tags stable: `date_picker_calendar_pager`,
  `date_picker_day_<year>_<month>_<day>`, `date_picker_title`, `date_picker_header`,
  `date_picker_selected_date`, `date_picker_edit_button`, `date_picker_month_year_dropdown`,
  `date_picker_previous_month`, `date_picker_next_month`. Add a tag such as `date_picker_ok`
  for the new OK button.

Tests (a test change is REQUIRED with any main change):
- Update structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  and the UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt`
  to expect: day click does NOT confirm, OK confirms, current date is outlined, and Cancel
  does nothing.

## Definition of done

- [ ] Day click only selects (header updates); it does NOT call `onDateSelected`
- [ ] The applied (current) date stays marked with a black circle outline on top of its cell
- [ ] The bottom row shows Cancel + OK; only OK calls `onDateSelected(selectedDate)`
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added

## Run log

- 2026-11-23 21:20:11 attempt 2: status=done exit=0 log=10-date-picker-ok-button-and-current-date-marker-run2.log — DatePickerModal now only selects on day click, keeps the applied date as a black circle outline, and confirms via a new OK button; tests updated and green.
-   The task is complete. Summary of what changed:
-   **`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`**
-   - Day click now only moves the pending selection (`selectedDate = ...` + header update); it no longer calls `onDateSelected`.
-   - `CalendarMonthPage` now takes the applied `currentDate`; that day's cell renders a black circle outline (tag `date_picker_current_date_outline`) on top of the cell, which stays visible even when another day is selected (both render on the same cell if re-selecting the current date).
-   - Bottom row is now `[Cancel] [OK]`: Cancel keeps the green text style and dismisses without confirming; the new OK button (tag `date_picker_ok`, filled green) confirms via `onDateSelected(selectedDate)`.
-   - Manual entry modal behavior unchanged — a valid Save still confirms immediately via `onConfirm(date)`.
-   **Unit tests** (`app/src/test/.../DatePickerModalRequirementsTest.kt`): added three tests — `dayClickOnlySelectsAndDoesNotConfirm`, `bottomRowShowsCancelAndOkAndOnlyOkConfirms`, `appliedCurrentDateStaysMarkedWithABlackCircleOutline`.
-   **UI tests** (`app/src/androidTest/.../DatePickerModalUiTest.kt`): rewrote the day-click test to assert it does NOT confirm and that OK confirms; added `cancelDismissWithoutConfirming`, `appliedDateStaysMarkedWithABlackCircleOutline`, `currentDateOutlineRemainsAfterSelectingAnotherDay`; updated the year-jump test to confirm via OK.
-   Verified with `./gradlew testDebugUnitTest` (green, incl. the 3 new tests) and `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.DatePickerModalUiTest` (16/16 pass on the emulator).
-   STATUS: DONE — DatePickerModal now only selects on day click, keeps the applied date as a black circle outline, and confirms via a new OK button; tests updated and green.
-   The task is complete. Summary of what changed:
-   **`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`**
-   - Day click now only moves the pending selection (`selectedDate = ...` + header update); it no longer calls `onDateSelected`.
-   - `CalendarMonthPage` now takes the applied `currentDate`; that day's cell renders a black circle outline (tag `date_picker_current_date_outline`) on top of the cell, which stays visible even when another day is selected (both render on the same cell if re-selecting the current date).
-   - Bottom row is now `[Cancel] [OK]`: Cancel keeps the green text style and dismisses without confirming; the new OK button (tag `date_picker_ok`, filled green) confirms via `onDateSelected(selectedDate)`.
-   - Manual entry modal behavior unchanged — a valid Save still confirms immediately via `onConfirm(date)`.
-   **Unit tests** (`app/src/test/.../DatePickerModalRequirementsTest.kt`): added three tests — `dayClickOnlySelectsAndDoesNotConfirm`, `bottomRowShowsCancelAndOkAndOnlyOkConfirms`, `appliedCurrentDateStaysMarkedWithABlackCircleOutline`.
-   **UI tests** (`app/src/androidTest/.../DatePickerModalUiTest.kt`): rewrote the day-click test to assert it does NOT confirm and that OK confirms; added `cancelDismissWithoutConfirming`, `appliedDateStaysMarkedWithABlackCircleOutline`, `currentDateOutlineRemainsAfterSelectingAnotherDay`; updated the year-jump test to confirm via OK.
-   Verified with `./gradlew testDebugUnitTest` (green, incl. the 3 new tests) and `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.DatePickerModalUiTest` (16/16 pass on the emulator).
-   STATUS: DONE — DatePickerModal now only selects on day click, keeps the applied date as a black circle outline, and confirms via a new OK button; tests updated and green.
