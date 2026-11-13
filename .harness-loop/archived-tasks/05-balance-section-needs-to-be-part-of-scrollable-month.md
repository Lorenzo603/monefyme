---
status: done
attempts: 2
title: "Balance section needs to be part of scrollable month"
source: backlog.md
depends_on: nothing
---

## Task

Currently, scrolling to a previous or next month result in the pie chart being scrolled but the balance box remain where it is and it is only updated when the new month is displayed.
Instead, the balance box should be part of the scrolling month itself, when scrolling to a previous or next month the balance box of that month should already be there displaying the correct balance for that month.

## Run log

-   - Pie chart and balance box are in the same scrollable column
-   - Balance box is no longer fixed below the pager
-   ### Key Benefits
-   - ✅ When scrolling to a previous/next month, the balance box moves with the pie chart
-   - ✅ The correct balance for that month displays immediately while scrolling
-   - ✅ All existing tests continue to pass
-   - ✅ New tests verify the correct behavior
-   Perfect! Let me verify the files I modified to ensure everything is in order:
-   Excellent! Let me run one final comprehensive test to ensure all tests pass:
-   Perfect! All tests are passing. Let me verify the implementation one more time by checking the key changes:
-   Perfect! The implementation is complete and verified. Let me provide a final summary:
-   ## Implementation Summary
-   I successfully completed the task to make the balance section part of the scrollable month. Here are the key changes:
-   ### Files Modified:
-   1. **app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt**
-   - Moved `BalanceAndHistorySheet` component into each `HorizontalPager` page
-   - Each page now calculates and displays its own `pageBalance`
-   - Each page passes its own `pageTransactions` to the balance sheet
-   - The balance box scrolls horizontally with the pie chart for each month
-   2. **app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenMonthScrollingTest.kt** (NEW)
-   - Created comprehensive tests to verify the balance box is part of the scrollable month
-   - 5 test methods verify correct behavior
-   ### Test Results:
-   - ✅ All existing tests pass (176 tests)
-   - ✅ New tests verify correct behavior
-   - ✅ Build successful with no errors
-   ### Behavior Change:
-   **Before:** Balance box was fixed below the pager and only updated after scrolling settled on a new month
-   **After:** Balance box scrolls with each month's pie chart and displays the correct balance immediately while scrolling
-   STATUS: DONE Modified app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt to move BalanceAndHistorySheet inside each HorizontalPager page, making the balance box part of the scrollable month. Created app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenMonthScrollingTest.kt with 5 test methods to verify the behavior. All tests pass (./gradlew testDebugUnitTest).
