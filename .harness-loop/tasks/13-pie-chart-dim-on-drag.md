---
status: todo
attempts: 1
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

-   STATUS: todo
