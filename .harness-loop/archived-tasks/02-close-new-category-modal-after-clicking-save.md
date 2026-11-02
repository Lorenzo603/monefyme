---
status: done
attempts: 1
title: "Bigfix - close `new category` modal after clicking on `Save`"
source: backlog.md
depends_on:

## Task

Bug: When the user clicks the `Save` button in the modal where it adds a new category, the new category is added correctly but the modal does not close and remains there.
Expected: When the user clicks the `Save` button in the modal where it adds a new category, the modal should close automatically.
Note: This works as expected when adding a category from the sidebar menu, but it does not work when adding a category from the `add expense` page.

## Run log

- 2026-11-02 14:36:54 attempt 1: status=done exit=0 log=02-close-new-category-modal-after-clicking-save-run1.log Fixed AddTransactionScreen.kt to close new category modal after successful save; ran testDebugUnitTest
-   The fix is complete. Here's a summary of what was changed:
-   ## Summary
-   **File Changed:** `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt`
-   **Change:** Modified the `persistCategory` callback to close the "New Category" dialog after successfully saving a category.
-   **Before:**
-   ```kotlin
-   val persistCategory: suspend (String, String, Int) -> Boolean = { displayName, iconName, colorArgb ->
-   viewModel.saveCategory(displayName, iconName, colorArgb)
-   }
-   ```
-   **After:**
-   ```kotlin
-   val persistCategory: suspend (String, String, Int) -> Boolean = { displayName, iconName, colorArgb ->
-   val success = viewModel.saveCategory(displayName, iconName, colorArgb)
-   if (success) showAddCategoryDialog = false
-   success
-   }
-   ```
-   This matches the behavior in `CategoriesSidebar.kt` (sidebar menu) where the modal correctly closes after saving.
-   **Verification:**
-   - `./gradlew testDebugUnitTest` - PASSED
-   STATUS: DONE      Fixed AddTransactionScreen.kt to close new category modal after successful save; ran testDebugUnitTest
