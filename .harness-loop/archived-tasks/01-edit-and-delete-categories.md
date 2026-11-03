---
status: done
attempts: 2
title: "Manage categories, edit and delete categories from Sidebar"
source: backlog.md
depends_on:
---

## Task

In the sidebar it is currently possible to see the full list of categories.
When one of those categories is clicked, a new screen should open where it will be possible to edit the category name, edit the category icon, edit the category color, or delete the category altogether by clicking a button with a `bin` or `trash` icon in the top right

## Run log

-   - Added `selectedCategory` state to track the selected category
-   - Added `showEditDialog` state to show the edit dialog
-   - Modified `CategoryRow` to be clickable and trigger the edit dialog
-   - Added `EditCategoryDialog` composable with:
-   - Category name editing
-   - Icon selection
-   - Color selection
-   - Delete button with trash icon in the footer
-   5. **`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`**
-   - Updated `CategoriesSidebar` call to include `onEditCategory` and `onDeleteCategory` callbacks
-   6. **`app/src/main/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModel.kt`**
-   - Added `updateCategory()` method
-   - Added `deleteCategory()` method
-   ### Tests Added:
-   1. **`app/src/test/java/com/lorenzo/monefyme/data/repository/BudgetRepositoryImplTest.kt`**
-   - Added tests for `updateCategory()` (3 tests)
-   - Added tests for `deleteCategory()` (2 tests)
-   - Updated `FakeCategoryDao` to implement new methods
-   2. **`app/src/test/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModelTest.kt`**
-   - Added tests for `updateCategory()` (3 tests)
-   - Added tests for `deleteCategory()` (2 tests)
-   - Updated `FakeBudgetRepository` to implement new methods
-   3. **`app/src/test/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModelFactoryTest.kt`**
-   - Added `updateCategory()` and `deleteCategory()` implementations to the fake repository
-   ### How it works:
-   - When a user clicks on a category in the sidebar, an `EditCategoryDialog` appears
-   - The dialog allows editing the category name, icon, and color
-   - A trash/delete button is displayed in the footer of the dialog
-   - Clicking delete removes the category from the database
-   STATUS: DONE      implementation complete and verified - ran ./gradlew testDebugUnitTest, all tests pass
