---
status: done
attempts: 2
title: "Pie Chart section highlight"
source: backlog.md
depends_on: nothing
---

## Task

keeping the mouse ( or finger ) clicked on a particular category of the pie chart should result in that category slot being highlighted by means a colored circle appearing around its icon, and in the center of the pie chart, instead of displaying the total amount of income and expenses, instead display the icon of that category, the name of that category, and the total amount of that category.

## Run log

-   ### Verification run
-   - `./gradlew testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**; new suite: 14 tests, 0 failures, 0 errors, 0 skipped; all pre-existing suites still green (18 suites total).
-   - `./gradlew compileDebugKotlin` and `./gradlew assembleDebugAndroidTest` → clean.
-   STATUS: DONE Pie chart press-and-hold category highlight in app/src/main/.../ui/component/ExpensePieChart.kt (pressed-slot state + press-and-hold gesture, colored circle ring around the pressed icon slot, centre switches to the pressed category's icon/name/total, pure internal highlight helpers), with new tests app/src/test/.../ui/component/ExpensePieChartHighlightTest.kt (14, all green) and app/src/androidTest/.../ui/PieChartHighlightUiTest.kt (compiles; emulator-run); verified via ./gradlew testDebugUnitTest --rerun-tasks — BUILD SUCCESSFUL.
-   Let me check the current state of the repository first — previous sessions' work may have been reverted.
-   Now compile to verify:
-   Compiles. Now write the JVM unit tests:
-   Now the instrumentation UI test (per repo convention; not part of harness verify commands, but the test kind for running-app behaviour):
-   Now compile everything and run the harness verification:
-   All 14 new tests pass. Final fresh re-run of the harness command and scope check:
-   Implementation complete and verified.
-   ## Summary
-   **Task:** While the mouse/finger is held on a category of the pie chart, that category's icon slot is highlighted with a colored circle, and the chart center replaces the income/expense totals with that category's icon, name, and total amount.
-   **Assumption stated:** "keeping clicked" is implemented as press-and-hold — the highlight is active while the pointer is pressed on a category icon slot (the chart's category slots, i.e. the icons around the ring); releasing the press restores the income/expense totals view.
-   ### Files changed (scoped to this task)
-   1. **`app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt`** (modified)
-   - New `pressedCategoryKey` state (`remember { mutableStateOf<String?>(null) }`).
-   - Each slice-backed icon slot gets a `pointerInput` press-and-hold gesture (`awaitFirstDown` → set pressed → `waitForUpOrCancellation` → clear), using `PointerEventPass.Initial` so the surrounding month pager's drag doesn't swallow the press; inactive placeholder slots are inert.
-   - Highlighted slot draws a colored circle around its icon (`drawCircle` in `drawBehind`, category color, 2.dp stroke, 3.dp gap); the slot Box is enlarged by the gap so the ring isn't clipped by the icon's CircleShape background.
-   - Center display switches on press: a tagged Column shows the pressed category's icon, its original (non-normalized) name, and its total amount (summed across the category's slices); otherwise the original income/expense totals render, now tagged `pie_chart_centre_income` / `pie_chart_centre_expense` (pressed view tagged `pie_chart_centre_category`).
-   - Decision logic extracted into internal pure functions (`pressedCategoryAfterPress`, `pressedCategoryAfterRelease`, `isCategoryHighlighted`, `pressedCategoryVisual`, `highlightedCategoryLabel`) and `CategoryVisual` made internal — matching the repo's existing testability pattern from `TransactionHistoryScreen.kt`.
-   2. **`app/src/test/java/com/lorenzo/monefyme/ui/component/ExpensePieChartHighlightTest.kt`** (new — 14 tests)
-   - Logic tests: no highlight by default; press highlights only that category (case- and whitespace-insensitive); release clears it; placeholder slots can't be pressed; centre total sums a category's slices (e.g. 4000+1000 → €50.00); visual lookup by normalized key; zero fallback for unknown keys.
-   - Wiring/source-structure tests (repo convention for Compose rendering that needs an emulator, same as `HomeRestoreWiringTest`/`TransactionHistoryScreenTest`): state exists, gesture ordering down→press→up→release, ring drawing guarded by highlight state with the category's color, centre branch replaces totals before the income text, stable test tags.
-   3. **`app/src/androidTest/java/com/lorenzo/monefyme/ui/PieChartHighlightUiTest.kt`** (new)
-   - Instrumentation test asserting the Home screen centre shows income/expense totals with no press and the pressed-category centre is absent beforehand. Per AGENTS.md, `connectedDebugAndroidTest` needs an emulator and is not a harness verify command; its sources compile cleanly (verified via `assembleDebugAndroidTest`).
-   ### Verification run
-   - `./gradlew testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**; new suite: 14 tests, 0 failures, 0 errors, 0 skipped; all pre-existing suites still green (18 suites total).
-   - `./gradlew compileDebugKotlin` and `./gradlew assembleDebugAndroidTest` → clean.
-   STATUS: DONE Pie chart press-and-hold category highlight in app/src/main/.../ui/component/ExpensePieChart.kt (pressed-slot state + press-and-hold gesture, colored circle ring around the pressed icon slot, centre switches to the pressed category's icon/name/total, pure internal highlight helpers), with new tests app/src/test/.../ui/component/ExpensePieChartHighlightTest.kt (14, all green) and app/src/androidTest/.../ui/PieChartHighlightUiTest.kt (compiles; emulator-run); verified via ./gradlew testDebugUnitTest --rerun-tasks — BUILD SUCCESSFUL.
