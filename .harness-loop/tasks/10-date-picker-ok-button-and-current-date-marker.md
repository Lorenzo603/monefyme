---
status: todo
attempts: 1
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

-   STATUS: todo
