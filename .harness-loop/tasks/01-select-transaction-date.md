---
status: done
attempts: 1
title: "Select transaction date when adding expense or income"
source: backlog.md
depends_on:

## Task

In the "Add Expense" and "Add Income" screens, when the user clicks the date at the top, a calendar picker modal must open up letting the user select a new date for the transaction. Only the date is important, not the time.
The UI of the calendar picker modal must be appropriate with the rest of the theme of the application.

## Run log

- 2026-11-02 14:31:49 attempt 1: status=done exit=0 log=01-select-transaction-date-run1.log
-   The implementation is complete and verified. Here's a summary of the changes:
-   ## Summary
-   **File Modified:** `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt`
-   **Changes Made:**
-   1. Added imports for `clickable`, `MutableInteractionSource`, `Spacer`, `AlertDialog`, `TextButton`, and `YearMonth`
-   2. Added `showDatePicker` state variable to control the date picker modal visibility
-   3. Added `todayToUse` state variable to track the selected date (separate from `today` which is the current date)
-   4. Made the date display clickable - when users click the date at the top, the `DatePickerModal` opens
-   5. Updated the date display to use `todayToUse` instead of `today`
-   6. Added a custom `DatePickerModal` composable function that:
-   - Shows a calendar grid with day headers (S M T W T F S)
-   - Displays the current month/year at the top
-   - Allows navigation between months using `<` and `>` buttons
-   - Highlights the selected date with the app's green theme color (`0xFF74C792`)
-   - Highlights the current day with bold text
-   - Uses the app's color scheme (background `0xFFEAF8F2`, text `0xFF2D3336`)
-   - Has a "Cancel" button to dismiss the modal
-   **Verification:**
-   - `./gradlew testDebugUnitTest` - PASSED
-   - `./gradlew build` - PASSED
-   STATUS: DONE      Modified AddTransactionScreen.kt to add a clickable date display with a custom themed calendar picker modal; verified with unit tests and full build
-   STATUS: DONE
