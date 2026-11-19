---
status: todo
attempts: 1
title: "transaction history toggle button icon fix"
source: backlog/next.md
depends_on: nothing
---

## Task

Transaction history -> toggle button icon fix.

The grouping toggle button shown when the transaction history is open (in
`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`, the
`IconButton` with testTag `grouping_toggle_button` that calls
`viewModel.toggleGroupByCategory()`) currently uses the hamburger `Icons.Outlined.Menu`
icon, which is the same icon as the open-history button and does not convey
what the toggle does.

Requirements:
- Give the toggle button an icon that clearly communicates the grouping mode:
  show an appropriate icon for the current grouping state (e.g. one for
  "grouped by category" and a different one for "grouped by date") so the user
  can tell which grouping is active and that tapping switches it.
- Do not change the toggle behaviour itself, only the icon (and its
  `contentDescription` if needed so it matches the state).
- The other icons in the app bar (hamburger for open/close history) must stay
  as they are.
