---
status: done
attempts: 3
title: "value of amount input box is not displayed"
source: backlog.md
depends_on: nothing
---

## Task

the value of the input boxes that are supposed to display the amount of a transaction as the user is clicking the numbered buttons in the add expense screen is not displayed or visible.
When the user clicks the numbers, nothing is displayed in the big input box.
Howeverm numbers are still registered internally, because after clicking some numbers the "change category" button works and the add expense feature itself works without issues, it seems just a visual bug where the value box is not visible as the user is clicking the number buttons.
Fix the problem.

## Run log

-   Let me check the `amountDisplayColor` variable:
-   Now I see the issue! The `amountDisplayColor` defaults to `Color.White`, and it's used for the Text displaying the amount. Let me check what the background color of that area is:
-   Found it! The issue is clear now:
-   1. The `Surface` has a **green background** (`color = Color(0xFF79C896)`)
-   2. The amount text uses `amountDisplayColor` which defaults to **`Color.White`**
-   3. White text on a light green background (`0xFF79C896`) is essentially invisible!
-   The fix is to change the default `amountDisplayColor` from `Color.White` to a dark color that's visible on the green background. Looking at the code, the EUR currency label uses `Color(0xFF2F5E4B)` which should work well for the amount text too.
-   Let me create a test first and then fix the issue:
-   Now let me run this test to confirm it fails before the fix:
-   Good, the tests fail as expected. Let me adjust the test to match the actual code format:
-   Perfect! The test confirms the issue: `targetValue = if (amountFlashError) Color(0xFFE53935) else Color.White` - the default is `Color.White` which is invisible on the green background. Now let me fix the source code:
-   Now let me update the test to reflect the fix:
-   Now let me run the tests again:
-   Let me check what's in the actual text section:
-   I see - the text still uses `color = amountDisplayColor`, not the literal color value. Let me fix the test:
-   Let me debug this by checking what's in the textDef:
-   The issue is that 200 characters might not be enough. Let me check what's actually being captured:
-   All tests pass. Now let me run all the unit tests to make sure everything still works:
-   Excellent! All tests pass. Let me verify the fix one more time by looking at the changed code:
-   Perfect! The fix is complete. Here's a summary:
-   ## Summary
-   **Problem:** The amount input box value was not displayed when users clicked number buttons in the add expense screen.
-   **Root Cause:** The `amountDisplayColor` variable used `Color.White` as its default value, but the amount display Surface has a green background (`Color(0xFF79C896)`). White text on this light green background is essentially invisible.
-   **Fix Applied:** Changed the default `targetValue` in `amountDisplayColor` from `Color.White` to `Color(0xFF2F5E4B)` (a dark green that matches the EUR currency label color) in `app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt`.
-   **Test Added:** Created `AddTransactionScreenAmountDisplayTest.kt` with three tests:
-   1. `amountDisplayColorIsVisibleOnGreenBackground` - Verifies the color is not white and uses a visible dark color
-   2. `amountTextUsesAmountDisplayColor` - Verifies the Text composable uses amountDisplayColor for its color
-   3. `surfaceHasGreenBackground` - Verifies the Surface has the expected green background
-   **Verification:** Ran `./gradlew testDebugUnitTest` - all tests pass (BUILD SUCCESSFUL).
-   STATUS: DONE fixed invisible amount text by changing default color from white to dark green (0xFF2F5E4B) in AddTransactionScreen.kt and added AddTransactionScreenAmountDisplayTest.kt
