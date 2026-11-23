---
status: done
attempts: 2
title: "Dim pie chart section while dragging up to reveal history"
source: backlog.md
depends_on: nothing
---

## Task

In the home screen (`HomeScreen.kt`, component composition with `ExpensePieChart`):

Current behavior: as the user drags up from the balance section to reveal the transaction
history sheet, the pie chart section (above the balance box) is hidden by fading it out:
`graphicsLayer { alpha = 1f - historyRevealOffset }`. At full reveal the pie chart is fully
invisible (alpha = 0f).

Required behavior:

- The pie chart section (the `ExpensePieChart` composable and everything it wraps) must be
  DIMMED as the user drags up to reveal history — its brightness decreases proportionally
  with the reveal offset.
- The dim must NOT fade the pie chart to invisible: keep it visibly (faintly) present at full
  reveal (e.g. clamp the dim to a minimum brightness around 40–50%, e.g. `1f - 0.55f * offset`).
- The dim must be smooth and proportional to the drag offset (a continuous function of
  `historyRevealOffset`), and must reverse when the user drags down.
- Implement the dim via a brightness/exposure/darkening filter (e.g. a `graphicsLayer` color
  filter or a dark overlay) rather than a full `alpha` fade — the section should remain
  dimly visible, not transparent.
- Preserve the existing `historyRevealOffset` contract and all existing test tags
  (`month_pager`, `balance_history_left`, `balance_history_right`, `grouping_toggle_button`,
  `history_sheet`) and the existing drag logic (`detectVerticalDragGestures`, drag end/cancel
  snap-open/snap-closed behavior).

Tests (a test change is REQUIRED with any main change):
- Update `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenDragHistoryTest.kt`
  (and/or `PieChartSectionClickUiTest` / `HomeScreenMonthScrollingTest`) so the dim is asserted
  as a proportional dimming that never reaches alpha 0f / fully transparent.

## Definition of done

- [ ] Pie chart section dims proportionally with the reveal offset (not an alpha fade to 0)
- [ ] Section remains faintly visible at full reveal
- [ ] Drag down reverses the dim smoothly
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added

## Run log

- 2026-11-23 21:22:38 attempt 2: status=done exit=0 log=13-pie-chart-dim-on-drag-run2.log - Pie chart section now dims via a dark overlay proportional to historyRevealOffset (max 55% dim, never fades to invisible); tests updated and full unit test suite green.
-   Summary of changes:
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`: The pie chart section (ExpensePieChart and its wrapper) is now dimmed via a dark overlay (`Box` background `Color.Black.copy(alpha = 0.55f * historyRevealOffset)`) inside a `weight(1f).padding(12.dp)` Box, replacing the `graphicsLayer { alpha = 1f - historyRevealOffset }` crossfade. The overlay is a continuous, proportional function of `historyRevealOffset`, bottoms out at 55% dim (faintly visible at full reveal), and reverses when dragging down. Drag logic, `historyRevealOffset` contract, and all test tags (`month_pager`, `balance_history_left/right`, `grouping_toggle_button`, `history_sheet`) are untouched.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenDragHistoryTest.kt`: Added `pieChartDimIsProportionalToRevealOffsetAndNeverFullyFades` (proportional, never reaches full-black dim, monotonic/reversible) and `homeScreenDimsPieChartWithoutAlphaCrossfade` (source guard: no `1f - historyRevealOffset` alpha crossfade, dark overlay present over the chart, `onCategoryClick`/`onCategorySlotClick` wiring preserved).
-   Verification: `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL; new tests executed (16 tests, 0 failures in `HomeScreenDragHistoryTest`).
-   STATUS: DONE - Pie chart section now dims via a dark overlay proportional to historyRevealOffset (max 55% dim, never fades to invisible); tests updated and full unit test suite green.
-   Summary of changes:
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`: The pie chart section (ExpensePieChart and its wrapper) is now dimmed via a dark overlay (`Box` background `Color.Black.copy(alpha = 0.55f * historyRevealOffset)`) inside a `weight(1f).padding(12.dp)` Box, replacing the `graphicsLayer { alpha = 1f - historyRevealOffset }` crossfade. The overlay is a continuous, proportional function of `historyRevealOffset`, bottoms out at 55% dim (faintly visible at full reveal), and reverses when dragging down. Drag logic, `historyRevealOffset` contract, and all test tags (`month_pager`, `balance_history_left/right`, `grouping_toggle_button`, `history_sheet`) are untouched.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenDragHistoryTest.kt`: Added `pieChartDimIsProportionalToRevealOffsetAndNeverFullyFades` (proportional, never reaches full-black dim, monotonic/reversible) and `homeScreenDimsPieChartWithoutAlphaCrossfade` (source guard: no `1f - historyRevealOffset` alpha crossfade, dark overlay present over the chart, `onCategoryClick`/`onCategorySlotClick` wiring preserved).
-   Verification: `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL; new tests executed (16 tests, 0 failures in `HomeScreenDragHistoryTest`).
-   STATUS: DONE - Pie chart section now dims via a dark overlay proportional to historyRevealOffset (max 55% dim, never fades to invisible); tests updated and full unit test suite green.
