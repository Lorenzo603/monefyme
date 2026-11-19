---
status: todo
attempts: 1
title: "improve date picker"
source: backlog/next.md
depends_on: nothing
---

## Task

Improve the date picker used to select transaction dates.

The date picker is shown from `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt`.
Rebuild/improve it (a custom Compose date picker dialog is acceptable if the
Material 3 `DatePickerDialog` cannot meet these requirements) with all of the
following:

- Smaller "Select date" title size in the dialog.
- A green header with the selected date displayed bigger and centered in the
  header.
- In the header, add an edit button that opens a modal to input the exact date
  manually (e.g. day/month/year fields), with validation of the entered date.
- Add a dropdown box showing the selected month and year; opening it lets the
  user pick (or cycle through) years to jump to a different year.
- Scrolling to previous/next months by scrolling the calendar, and move the
  previous/next month buttons to the top, next to the month/year dropdown.
- More space for the day buttons (larger hit targets / spacing in the calendar
  grid).

Requirements:
- Selecting a date (via calendar, month/year navigation, or manual entry) must
  return the same `LocalDate` result the caller receives today and keep the
  existing callers working.
- Cover the new selectable behaviour with tests where it is logic-testable
  (e.g. month/year navigation math, manual date input parsing/validation).
