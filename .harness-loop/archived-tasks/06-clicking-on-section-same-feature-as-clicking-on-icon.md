---
status: done
attempts: 2
title: "Holding click or finger on category section of the pie chart"
source: backlog.md
depends_on: nothing
---

## Task

Holding a click (or a finger) on a category section of the pie chart should result in the same effect as when holding click on the icon of a category: that is, highlighting the icon and showing the category icon, category name and total amount of that category for that month at the center of  the circle.
The click (without holding it) should still bring the user to the transaction history with that category already expanded, which is like it already works today for both the pie chart section click and the icon category click.

## Run log

-   ### 2. Updated `app/src/test/java/com/lorenzo/monefyme/ui/component/ExpensePieChartClickTest.kt`
-   - Added test `holdingAPressOnASectionHighlightsLikeHoldingOnAnIcon()` to verify that:
-   - The ring gesture handler sets the pressed category state
-   - The state is cleared after release
-   - The operations happen in the correct order
-   ### 3. Updated `app/src/test/java/com/lorenzo/monefyme/ui/component/ExpensePieChartHighlightTest.kt`
-   - Added test `pressingSectionOnPieChartSetsAndReleasingClearsThePressedCategory()` to verify that ring sections use the same press/release state machine as icon slots
-   Let me verify the tests one more time with verbose output:
-   Perfect! All tests pass successfully. Let me verify the implementation one more time by checking the key aspects:
-   Excellent! All tests pass successfully. Let me create a final summary of the implementation:
-   ## Implementation Complete
-   I have successfully implemented the feature to make holding a click on a pie chart section behave the same as holding a click on a category icon.
-   ### Changes Made:
-   1. **`app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt`**
-   - Modified the Canvas's `pointerInput` gesture handler to set `pressedCategoryKey` when a user presses on a pie chart section
-   - Added `pressedCategoryKey = pressedCategoryAfterPress(pressedCategoryKey, categoryKey)` after detecting a press
-   - Added `pressedCategoryKey = pressedCategoryAfterRelease(pressedCategoryKey)` after the release
-   - This uses the same state machine as icon slots, enabling automatic highlighting and center display
-   - Regular clicks (without holding/dragging) still navigate to transaction history via `onCategoryClick`
-   2. **`app/src/test/java/com/lorenzo/monefyme/ui/component/ExpensePieChartClickTest.kt`**
-   - Added test `holdingAPressOnASectionHighlightsLikeHoldingOnAnIcon()` to verify the ring gesture handler correctly uses the press/release state machine
-   3. **`app/src/test/java/com/lorenzo/monefyme/ui/component/ExpensePieChartHighlightTest.kt`**
-   - Added test `pressingSectionOnPieChartSetsAndReleasingClearsThePressedCategory()` to ensure ring sections and icon slots use the same state machine
-   ### Behavior:
-   - ✅ Holding a click on a pie chart section now highlights the corresponding icon and displays category info in the center
-   - ✅ Quick clicks on sections still navigate to transaction history with that category expanded
-   - ✅ Same behavior as holding on an icon slot
-   - ✅ All existing tests pass
-   - ✅ New tests verify the implementation
-   STATUS: DONE implementation complete and verified; all unit tests pass (./gradlew testDebugUnitTest)
