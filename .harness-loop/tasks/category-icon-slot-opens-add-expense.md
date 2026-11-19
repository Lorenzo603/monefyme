---
status: todo
attempts: 1
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
