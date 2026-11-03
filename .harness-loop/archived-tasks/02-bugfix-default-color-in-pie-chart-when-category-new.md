---
status: done
attempts: 2
title: "Bug - new category in pie chart has default color and icon"
source: backlog.md
depends_on: nothing
---

## Task

Bug: When adding a transaction of a category that is not in the pie chart, that category is added in the pie chart with the default color and the default icon
Expected: The category should be displayed in the pie chart with the proper category color and category icon that are selected for that category.

## Run log

-   I need to update the query to return the columns with the correct aliases that match the ExpenseSlice field names:
-   Let me check the generated stub again to see if it was updated:
-   The stub file looks correct. Let me try a more aggressive clean:
-   Let me look at the exact code and check if there's a typo:
-   I see the issue now. The code is iterating over `slicesWithMeta` which contains `SliceMeta` objects, not `ExpenseSlice` objects. The `SliceMeta` class doesn't have `icon` or `colorArgb` fields. I need to use the original `slices` list instead.
-   The tests pass. Now I need to add a test to verify the fix. Let me check if there are existing tests for the ExpensePieChart or ExpenseSlice:
-   Let me create a test for ExpenseSlice to verify the color and icon handling:
-   Let me fix the test - the color conversion issue:
-   Let me check how to properly convert Color to Int:
-   All tests pass. Now let me verify that all the changes are correct by reviewing the modified files:
-   All changes look correct. Let me run the tests one more time to ensure everything is working:
-   All tests pass. Let me provide a summary of the changes:
-   ## Summary
-   I fixed the bug where new categories in the pie chart were displaying with default color and icon instead of the user-selected ones.
-   ### Changes Made:
-   1. **`app/src/main/java/com/lorenzo/monefyme/data/model/ExpenseSlice.kt`**
-   - Added `colorArgb: Int?` and `icon: String?` fields to store category appearance information
-   2. **`app/src/main/java/com/lorenzo/monefyme/data/local/dao/TransactionDao.kt`**
-   - Updated the `observeExpenseSlicesByDateRange` query to include `c.colorArgb AS colorArgb` and `c.iconName AS icon` columns
-   3. **`app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt`**
-   - Modified `slicesWithMeta` to use the slice's `colorArgb` field when available, falling back to default category color
-   - Added `sliceVisualsByCategory` map to provide proper icons and colors for active categories from the slice data
-   - Added import for `CategoryIconRegistry`
-   4. **`app/src/test/java/com/lorenzo/monefyme/data/model/ExpenseSliceTest.kt`** (new file)
-   - Added unit tests for the ExpenseSlice data class to verify color and icon handling
-   ### How the fix works:
-   - When a transaction with a new category is added, the database query now returns the category's `colorArgb` and `iconName` from the categories table
-   - The `ExpensePieChart` uses these values directly instead of looking up the category in `DefaultCategories`
-   - This ensures that user-selected colors and icons are displayed correctly for all categories, including newly added ones
-   STATUS: DONE      implementation complete, ran `./gradlew testDebugUnitTest` - all tests pass
