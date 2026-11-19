---
status: done
attempts: 2
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

## Run log

-   All checks are green. Summary of the work:
-   **Changes**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/util/DatePickerLogic.kt` (new) — pure, JVM-testable logic: absolute month index math for the pager, `shiftMonth` prev/next navigation, `yearOptions` year-dropdown windows, and `parseManualDateOrNull` manual day/month/year parsing with full calendar validation (leap years, month/day ranges, year 1..9999).
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt` (new) — rebuilt custom Compose date picker with every requested feature: smaller `titleMedium` "Select date" title; green header with the selected date enlarged (26 sp bold) and centered; a header edit button opening a manual day/month/year entry modal with validation errors; a month/year dropdown box whose menu lets you pick a year or cycle the window with ‹/› buttons to jump years; a swipeable `HorizontalPager` calendar (scrolling pages months) with the prev/next month buttons moved to the top beside the dropdown; and day cells enlarged to full-column width × 44 dp with extra vertical spacing (old: 40 dp, tight). Day tags are month-scoped (`date_picker_day_2025_10_15`) for unambiguous testing.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt` — the old private dialog is replaced by the shared component; the caller keeps the identical `DatePickerModal(currentDate = todayToUse, onDateSelected = {…}, onDismiss = {…})` contract returning the same `LocalDate`.
-   **Verification (all green)**
-   - `./gradlew testDebugUnitTest` — 239 tests pass, including the new `DatePickerLogicTest` (month/year navigation math, manual entry parsing/validation, leap-year rules) and `DatePickerModalRequirementsTest` (source-structure coverage of each UI requirement, matching this repo's existing test convention).
-   - `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.DatePickerModalUiTest` — all 9 new end-to-end tests pass on the running emulator (day tap returns the same `LocalDate`, prev/next buttons incl. Dec→Jan wrap, year dropdown pick + cycling, calendar swipe paging, manual entry valid → returns date, invalid → shows error and returns nothing).
-   Caveat: the connected test is not part of the harness `verify_commands` (needs an emulator), but it was run manually here and passes.
-   STATUS: DONE Rebuilt transaction date picker (new ui/component/DatePickerModal.kt + ui/util/DatePickerLogic.kt; AddTransactionScreen now uses the shared component with the same LocalDate contract); added DatePickerLogicTest, DatePickerModalRequirementsTest (./gradlew testDebugUnitTest green) and DatePickerModalUiTest (all 9 connectedDebugAndroidTest tests pass on emulator)
-   Baseline unit tests pass. Now let me check remaining usages and then write the new code.
-   Now writing the new pure logic helpers:
-   Now update the screen to use the new component and drop the old dialog:
-   Main compiles. Now the unit tests:
-   No `isDisplayed()` matcher factory exists in this ui-test version. I'll make day tags month-unique instead:
-   Now update the androidTest to use month-scoped day tags:
-   Three assertion mistakes in my new test (the code is right, the test expectations were wrong). Fixing the tests:
-   All unit tests pass and androidTest compiles. Let me try running the new UI test on the emulator to validate the picker end-to-end:
-   All three failures are ambiguity/expectation issues in my UI test. Fixing them:
-   All 9 UI instrumentation tests pass on the emulator. Final run of the harness verification command:
-   All checks are green. Summary of the work:
-   **Changes**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/util/DatePickerLogic.kt` (new) — pure, JVM-testable logic: absolute month index math for the pager, `shiftMonth` prev/next navigation, `yearOptions` year-dropdown windows, and `parseManualDateOrNull` manual day/month/year parsing with full calendar validation (leap years, month/day ranges, year 1..9999).
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt` (new) — rebuilt custom Compose date picker with every requested feature: smaller `titleMedium` "Select date" title; green header with the selected date enlarged (26 sp bold) and centered; a header edit button opening a manual day/month/year entry modal with validation errors; a month/year dropdown box whose menu lets you pick a year or cycle the window with ‹/› buttons to jump years; a swipeable `HorizontalPager` calendar (scrolling pages months) with the prev/next month buttons moved to the top beside the dropdown; and day cells enlarged to full-column width × 44 dp with extra vertical spacing (old: 40 dp, tight). Day tags are month-scoped (`date_picker_day_2025_10_15`) for unambiguous testing.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt` — the old private dialog is replaced by the shared component; the caller keeps the identical `DatePickerModal(currentDate = todayToUse, onDateSelected = {…}, onDismiss = {…})` contract returning the same `LocalDate`.
-   **Verification (all green)**
-   - `./gradlew testDebugUnitTest` — 239 tests pass, including the new `DatePickerLogicTest` (month/year navigation math, manual entry parsing/validation, leap-year rules) and `DatePickerModalRequirementsTest` (source-structure coverage of each UI requirement, matching this repo's existing test convention).
-   - `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.DatePickerModalUiTest` — all 9 new end-to-end tests pass on the running emulator (day tap returns the same `LocalDate`, prev/next buttons incl. Dec→Jan wrap, year dropdown pick + cycling, calendar swipe paging, manual entry valid → returns date, invalid → shows error and returns nothing).
-   Caveat: the connected test is not part of the harness `verify_commands` (needs an emulator), but it was run manually here and passes.
-   STATUS: DONE Rebuilt transaction date picker (new ui/component/DatePickerModal.kt + ui/util/DatePickerLogic.kt; AddTransactionScreen now uses the shared component with the same LocalDate contract); added DatePickerLogicTest, DatePickerModalRequirementsTest (./gradlew testDebugUnitTest green) and DatePickerModalUiTest (all 9 connectedDebugAndroidTest tests pass on emulator)
