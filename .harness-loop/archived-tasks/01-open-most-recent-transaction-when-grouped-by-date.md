---
status: done
attempts: 2
title: "open most recent transaction when grouped by date"
source: backlog.md
depends_on: nothing
---

## Task

when the user clicks on a section of the pie chart, the transaction history open with that specific related category already open.
however, this must be true only if grouping by category is the currently selected toggle of the transaction history.
If transaction history is toggled to be grouped by date, then clicking on a category section of the pie chart should open the transaction history with the date group of the latest transaction of that category expanded.

## Run log

-   Now let me update the existing test to cover the new behavior:
-   Let me re-read the test file to see its current state:
-   Now let me add the new test methods:
-   Now let me run the tests to verify the implementation:
-   I need to add the import for `TransactionListItem` in the HomeScreen file:
-   Now let me run the tests again:
-   There's a test failure. Let me check what the actual date format is:
-   I see - the date format is "dd/MM/uuuu" (e.g., "15/01/2024"). Let me fix the test:
-   Now let me also fix the third test which has a similar issue:
-   Let me run the tests again:
-   All tests pass. Now let me run the full test suite to ensure I haven't broken anything:
-   All tests pass. Let me verify the changes I made are complete and correct:
-   All 5 tests pass successfully. Let me provide a summary:
-   ## Summary of Changes
-   ### Files Modified:
-   1. **`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`**
-   - Added import for `TransactionListItem`
-   - Modified `groupsToExpand()` function to accept a new `transactions` parameter and handle both grouping modes:
-   - When grouped by category: expands the matching category group (existing behavior)
-   - When grouped by date: finds the latest transaction of the clicked category and expands its date group (new behavior)
-   - Updated `LaunchedEffect` in `TransactionGroupedList` to include `transactions` and `isGroupedByCategory` in its dependencies and pass them to `groupsToExpand()`
-   2. **`app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenExpansionRequestTest.kt`**
-   - Added import for `TransactionListItem` and `LocalDate`
-   - Added 3 new test methods:
-   - `groupsToExpandExpandsDateGroupForLatestTransactionWhenGroupedByDate()`: Tests that when grouping by date, clicking a category expands the date group containing its latest transaction
-   - `groupsToExpandExpandsCategoryWhenGroupedByCategory()`: Tests that when grouping by category, clicking a category expands its accordion
-   - `groupsToExpandReturnsEmptyWhenNoTransactionsMatchRequestedCategory()`: Tests edge case where no transactions match the requested category
-   ### Verification:
-   - Ran `./gradlew testDebugUnitTest` - all 29 tests pass
-   STATUS: DONE implemented feature to expand date group of latest transaction when grouped by date, added 3 new unit tests covering the behavior, all 29 tests pass
