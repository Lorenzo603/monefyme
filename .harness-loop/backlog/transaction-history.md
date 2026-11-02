---
status: todo
attempts: 1
title: "Transaction History"
source: backlog.md
depends_on: nothing

## Task

Implement the following functionality:
- Remove the list transactions page, the one that is reached when clicking the "Open transaction list" button to the left of the balance box. The feature described by this task will replace the functionality provided by that page.
- Move the functionality of the "Open restore" button to a new button located in the Sidebar menu.
- When the user clicks on the balance box, the list of transactions of the current month should be displayed:
  - The user can achieve the same effect by swiping up on the icon buttons that are placed to the left and right of the balance box.

- The list of transactions will be grouped by category or by their transaction date:
  - There will be a button to toggle grouping method. It will default to `by category`, and the user can click it to toggle it to `by date`
  - The groups start collapsed, clicking on a group will expand that group and it will be possible to see every single transaction belonging to that group
  - When grouped by category, the group "header" will display:
    - the icon of the category
    - the name of the category
    - the number of transactions belonging to that group
    - the total monetary value of the transactions belonging to that group
  - When grouped by date, the group "header" will display:
    - the date of the transaction
    - the number of transactions belonging to that group
    - the total monetary value of the transactions belonging to that group
- Clicking on a transaction opens the edit expense screen where the user can:
  - edit the value of the transaction
  - delete the transaction from button located on the top right corner
  - click a `CHANGE CATEGORY` button to change be prompted with a category selection screen to select a new category for the transaction

