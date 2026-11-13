---
status: done
attempts: 2
title: "bugfix transaction history category toggle"
source: backlog.md
depends_on: nothing
---

## Task

Fix this bug:
Expected: When clicking an icon category or when clicking a category section in the pie chart, the transaction history should be opened with that specific category already expanded.
Actual: When clicking an icon category or when clicking a category section in the pie chart, the transaction history is opened but all categories are collapsed.

## Run log

-   Now let me check the HomeScreenDragHistoryTest to understand the test pattern better:
-   Now I understand the issue. Let me create the fix and tests. The bug is in `HomeScreen.kt` in the `BalanceAndHistorySheet` function - it always passes `expandCategory = null` to `TransactionGroupedList`, but it should pass the expansion request from the ViewModel instead.
-   Let me fix the issue:
-   Now let me write tests to verify the fix. I'll add tests to the existing test file:
-   Let me add a test to verify the fix. First, let me read the file to find the right location:
-   Let me write a separate test file for this specific test:
-   Now let me run the tests to verify the fix works:
-   Let me verify the fix by viewing the changed code:
-   Perfect! Let me verify the complete fix one more time by looking at the key changes:
-   Perfect! All tests pass. Let me create a final summary of the changes:
-   ## Summary
-   I have successfully fixed the bug where clicking a pie chart section or category icon would open the transaction history with all categories collapsed.
-   ### Changes Made
-   #### 1. **Fixed HomeScreen.kt** (`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`)
-   Modified the `BalanceAndHistorySheet` composable to properly handle category expansion requests:
-   - Added collection of `viewModel.categoryExpansionRequest`
-   - Added a `remember` state for `categoryToExpand`
-   - Added a `LaunchedEffect` to consume the expansion request via `viewModel.consumeCategoryExpansionRequest()`
-   - Changed `expandCategory` parameter from `null` to `categoryToExpand` in the `TransactionGroupedList` call
-   This mirrors the implementation already present in `TransactionHistoryScreen.kt`.
-   #### 2. **Added Tests** (`app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenExpansionRequestTest.kt`)
-   Created a new test file with comprehensive tests:
-   - `balanceAndHistorySheetConsumesThePieChartExpansionRequest()` - Verifies the fix is properly implemented
-   - `homeScreenRequestsCategoryExpansionOnPieChartClick()` - Verifies the pie chart click triggers expansion
-   ### Test Results
-   ✓ All 22 unit tests pass (20 existing + 2 new)
-   ✓ No test failures or errors
-   ✓ Build successful
-   The fix ensures that when a user clicks on a pie chart section or category icon, the transaction history will open with that specific category's accordion already expanded, providing the expected user experience.
-   STATUS: DONE - Fixed transaction history category expansion on pie chart click; added tests to verify the fix works correctly. All 22 unit tests pass.
