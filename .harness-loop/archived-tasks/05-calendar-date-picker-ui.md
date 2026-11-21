---
status: done
attempts: 1
title: "Improve calendar date picker UI"
source: backlog/next.md
depends_on: nothing
---

## Task

Improve the calendar date picker modal used for transaction dates in the Add Transaction
flow (component `DatePickerModal`, file `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`),
using the reference screenshot @.harness-loop/task-assets/calendar-date-picker-modal.png.

Structure and behavior to preserve: picking a day (or confirming a valid manual date)
reports the chosen `LocalDate` via `onDateSelected`; the calendar pages month-by-month via
the pager (±100 years around the shown month); the edit button in the header opens the manual
entry modal (task 06); the year picker button opens the year menu (task 07).

Requirements:

- Header: keep it green (`0xFF74C792`), keep the selected date centered and enlarged in it with the
  edit button at the right, but make it span the WHOLE modal window: full-bleed across the
  dialog's full width, with no inset horizontal padding.
- Corners: make the rounded corners less pronounced. Reduce the dialog corner radius
  (`AlertDialog` `RoundedCornerShape(16.dp)`) and the header surface radius
  (`RoundedCornerShape(12.dp)`) to something clearly tighter (e.g. 8.dp dialog, 0–8.dp header).
- Header date format: remove the day of the week entirely and abbreviate the month to the
  standard 3-letter abbreviation. Example: `21 Nov 2026` (was `Monday, 21 November 2026`).
- Row order: move the year picker button (currently the month/year dropdown) to the LEFT.
  Both previous and next month arrows must sit to its right, on the same row.
  Layout: `[year picker] [previous month] [next month]`.
- Day cells: remove the background color of non-selected dates (non-selected dates must have no
  filled background). The selected date's highlight must be ROUND (a filled circle, not an
  elongated rounded rectangle). Today's bold text marker may be kept.
- Keep the following test tags stable: `date_picker_title`, `date_picker_header`,
  `date_picker_selected_date`, `date_picker_edit_button`, `date_picker_month_year_dropdown`,
  `date_picker_previous_month`, `date_picker_next_month`, `date_picker_calendar_pager`.

Tests (a test change is REQUIRED with any main change):
- Update the structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  (its assertions currently assume prev/next sit left/right of the dropdown, and full-width 10.dp rounded day cells).
- Update/extend the UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt` if it asserts the old layout.

## Definition of done

- [ ] Header is green and spans the whole modal window; selected date centered in it
- [ ] Corners are visibly less pronounced
- [ ] Header date has no weekday and uses a 3-letter month abbreviation
- [ ] Year picker button on the left; previous and next month arrows on its right, same row
- [ ] Non-selected dates have no background; selected date is a filled round circle
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added to cover the new layout

## Run log

-   Both are substring-window issues in the structure test. Let me measure the actual distances:
-   Unit tests green. Now compile the androidTest sources and check the emulator:
-   `assertDoesNotExist` is a member function, not an importable extension. Removing the bogus import:
-   My changes from this session are all in place. The last androidTest compile failed — let me see the errors:
-   `assertTextEquals` is only an extension on `SemanticsNodeInteraction`, not a matcher factory. Fixing the call:
-   Let me verify the negative-padding fix is in place and run the tests:
-   All fixes in place. Now run the full verification:
-   Unit tests and androidTest compilation pass. Since the previous crash was runtime-only, let me run the UI test on the connected emulator to verify the dialog actually renders:
-   Let me check the current state of the file first, since the previous script failed partway:
-   The previous script failed (heredoc quoting), so the file still has the crashing `(-24.dp)` version. Let me view the full file and rewrite it properly:
-   The file looks like it reverted to the original state. Let me check git status to understand what's tracked vs modified:
-   Git is clean — the harness reset my previous edits. Fresh implementation. Let me look at the current test files:
-   The file was reset to original by the harness. Let me re-check the reference screenshot, then implement everything fresh:
-   The screenshot confirms the design. Now let me read the UI test and check for other references:
-   The harness reset everything to original — clean slate. Let me check the material3 version, then implement:
-   Now I'll implement all changes in `DatePickerModal.kt`:
-   Now verify the main file's new structure:
-   Main file looks good. Now update the structure test:
-   Now update the androidTest UI test:
-   Let me fix that weekday assertion (it would fail on an empty node list) and simplify:
-   Now set env and run the unit tests:
-   Compilation passed; 2 tests fail. Let me see which:
-   Both are substring-window issues in the structure test. Let me measure the actual distances:
-   Unit tests green. Now compile the androidTest sources and check the emulator:
-   `assertDoesNotExist` is a member function, not an importable extension. Removing the bogus import:
-   My changes from this session are all in place. The last androidTest compile failed — let me see the errors:
-   `assertTextEquals` is only an extension on `SemanticsNodeInteraction`, not a matcher factory. Fixing the call:
-   Let me verify the negative-padding fix is in place and run the tests:
-   All fixes in place. Now run the full verification:
-   Unit tests and androidTest compilation pass. Since the previous crash was runtime-only, let me run the UI test on the connected emulator to verify the dialog actually renders:
