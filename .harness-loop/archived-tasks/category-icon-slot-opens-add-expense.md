---
status: done
attempts: 2
title: "clicking the category icon slot opens add expense for that category"
source: backlog/next.md
depends_on: nothing
---

## Task

Clicking the category icon slot now takes to add expenses for that category.

The pie chart on the home screen shows a category icon slot for each category
(see `app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt`,
where pressing a slot currently only shows the category info in the centre).
When the user taps/clicks a category icon slot, the app should open the add
transaction flow for an expense with that category already preselected, instead
of only showing the info.

Requirements:
- A click (tap, or mouse click release) on a category icon slot opens the add
  expense flow with the tapped category preselected.
- The existing press-and-hold behaviour of showing the category info in the
  centre of the pie chart must keep working.
- Add or update tests covering the new behaviour where testable (e.g. the
  logic that maps the clicked slot to the category/open-screen request).

## Run log

-   Now HomeScreen and BudgetApp:
-   Now the AddTransactionScreen:
-   Now update the JVM tests. First `ExpensePieChartClickTest.kt`:
-   Now create the new JVM wiring test:
-   Now update the androidTest files:
-   Now create a new androidTest for the preselected category flow:
-   Now let me review the modified AddTransactionScreen region to make sure everything is coherent:
-   Unit tests all pass. Now verify the instrumentation test sources compile:
-   Unit and instrumentation sources compile. Let me try running the affected UI tests on an emulator for real end-to-end coverage:
-   All 12 UI tests pass. Let me also run the neighboring pie chart and history UI tests to make sure nothing regressed:
-   Final harness verification run:
-   All checks are green. Here's a summary of the implementation:
-   ## What changed
-   **Behaviour** — clicking (tap, or mouse-click release without drag) a category icon slot on the home-screen pie chart now opens the add-expense flow with that category preselected; press-and-hold still highlights the slot and shows the category info in the chart's centre. Ring-arc clicks keep expanding the transaction history unchanged.
-   **Production code**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt` — new `onCategorySlotClick: ((String) -> Unit)?` callback; the icon-slot gesture reports through it on a press-without-drag instead of through `onCategoryClick` (which stays for ring sections). Press/release highlight logic untouched.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt` — new required `onAddExpenseForCategory: (String) -> Unit` parameter, wired to `onCategorySlotClick`.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/BudgetApp.kt` — calls `viewModel.requestAddExpense(categoryName)` then navigates to the existing `add-expense` route.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModel.kt` — `addExpenseCategoryRequest` state with `requestAddExpense` / `consumeAddExpenseCategoryRequest` (mirrors the existing expansion-request pattern; blanks ignored, consumed once).
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt` — consumes the pending request into `rememberSaveable` state, shows "Category: X" (`add_transaction_selected_category`), and the calculator's bottom button becomes `ADD "X"` (`add_transaction_confirm_button`) that validates the amount and saves an EXPENSE for that category; without a preselection the original "CHOOSE CATEGORY" flow is unchanged.
-   **Tests**
-   - `app/src/test/.../ExpensePieChartClickTest.kt` — updated/added slot-click wiring assertions (icon slot reports via `onCategorySlotClick`, never `onCategoryClick`; hold still highlights).
-   - `app/src/test/.../MainViewModelTest.kt` — 6 new tests for the request/consume/trim logic.
-   - `app/src/test/.../PieChartSlotAddExpenseWiringTest.kt` (new) — guards HomeScreen → BudgetApp → AddTransactionScreen wiring.
-   - `app/src/androidTest/.../PieChartSectionClickUiTest.kt` — icon-slot click test now asserts the add-expense callback, not history expansion.
-   - `app/src/androidTest/.../AddExpensePreselectedCategoryUiTest.kt` (new) — end-to-end: preselection shown, confirm saves the expense with that category, zero-amount guard, no-request keeps the picker flow.
-   - `app/src/androidTest/.../TransactionHistoryTitleTest.kt` — updated `HomeScreen(...)` call for the new required parameter.
-   **Verified**: `./gradlew testDebugUnitTest` (209 tests, 0 failures); `:app:compileDebugAndroidTestKotlin`; and on the running emulator `connectedDebugAndroidTest` for `PieChartSectionClickUiTest` + `AddExpensePreselectedCategoryUiTest` (12/12) plus `PieChartHighlightUiTest`, `PieChartConnectorUiTest`, `TransactionHistoryTitleTest`, `TransactionHistoryAccordionTest` (15/15) — all green.
-   Caveat: assumption made — "preselected" means the add screen shows the category and its primary button saves with it (the screen previously had no pre-selection concept); changing to another category still requires backing out and tapping a different slot.
-   STATUS: DONE Pie chart category icon slot click now opens add-expense with the category preselected (ExpensePieChart onCategorySlotClick, HomeScreen onAddExpenseForCategory, BudgetApp navigation + MainViewModel.requestAddExpense/consumeAddExpenseCategoryRequest, AddTransactionScreen preselection UI); added/updated JVM tests (ExpensePieChartClickTest, MainViewModelTest, PieChartSlotAddExpenseWiringTest) and UI tests (PieChartSectionClickUiTest, new AddExpensePreselectedCategoryUiTest); verified with ./gradlew testDebugUnitTest (209/209) and connectedDebugAndroidTest on the affected classes (27/27).
