---
status: done
attempts: 2
title: "Transaction hisoty screens are different"
source: backlog.md
depends_on: nothing
---

## Task
Currently there appears to be two transaction history screens: one is displayed when dragging up the balance box section. The other is displayed when clicking on an icon category in the pie chart.
This should not be, instead there should be only one transacton history screen and that is the one displayed when dragging the balance box section up.
Clicking on a category icon should result in expanding the same transaction history, resulting in an animation where the section is dragging up.

## Run log

-   I'll start by reading the task file and understanding the repository structure, then locate and consolidate the two transaction history screens.
-   Let me look at the ExpensePieChart component to understand what parameters it needs:
-   Now let me understand the full scope of changes needed. Let me check if there are any other references to these navigation callbacks:
-   Now I'll implement the changes to consolidate the transaction history screens. Let me start by modifying HomeScreen to expand the inline sheet when a pie chart category is clicked:
-   Let me verify that there are no remaining references to the old navigation callbacks and that the implementation is complete:
-   Let me verify the test file again to ensure the test names and logic are correct:
-   Let me verify there are no compilation errors by checking the full test output:
-   Let me do one final verification by checking that the implementation properly handles the animation when expanding the sheet from a pie chart click:
-   Let me verify all the changes are correct by looking at the complete modified sections one more time:
-   Perfect! All tests are passing. Let me verify the final state by checking if there are any other references or issues to address:
-   Perfect! I have successfully completed the task. Here's a summary of the changes:
-   ## Summary
-   I consolidated the two transaction history screens into a single inline transaction history sheet that appears on the HomeScreen. The changes eliminate the separate navigation screen and instead use smooth animations to expand the same sheet.
-   ### Changes Made:
-   **1. HomeScreen.kt**
-   - Removed `onOpenTransactionHistory` and `onOpenTransactionHistorySwipe` parameters from HomeScreen function signature
-   - Moved `onHistoryOffsetChange` and `onToggleHistoryFull` callbacks to the HomeScreen level (were previously defined inline in BalanceAndHistorySheet)
-   - Updated pie chart click handler: instead of `onOpenTransactionHistory()`, now calls `onToggleHistoryFull(true)` to expand the inline sheet with smooth animation
-   - The category expansion is still requested via `viewModel.requestCategoryExpansion(categoryName)`
-   **2. BudgetApp.kt**
-   - Removed `onOpenTransactionHistory` and `onOpenTransactionHistorySwipe` callback definitions from HomeScreen call
-   - TransactionHistoryScreen route remains available in navigation graph for future use
-   **3. HomeRestoreWiringTest.kt**
-   - Updated test name: `pieChartSectionClickOpensTransactionHistoryWithThatCategoryExpanded` → `pieChartSectionClickExpandsInlineTransactionHistoryWithThatCategoryExpanded`
-   - Updated test assertions to verify `onToggleHistoryFull(true)` is called instead of `onOpenTransactionHistory()`
-   ### Result:
-   - **Dragging balance box up**: Expands inline transaction history (existing behavior preserved)
-   - **Clicking pie chart category**: Now expands the same inline transaction history with smooth drag-up animation, with the selected category's accordion pre-expanded
-   - All tests passing ✓
-   STATUS: DONE Consolidated two transaction history screens into single inline sheet; pie chart clicks now expand the balance box transaction history with smooth animation instead of navigating away
