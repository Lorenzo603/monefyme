---
status: todo
attempts: 1
title: "fix failing test"
source: backlog.md
depends_on: nothing
---

## Task

Fix the following tests:
```
furrerl@MACNBLX036920 monefyme % ./gradlew connectedDebugAndroidTest                                    

> Task :app:connectedDebugAndroidTest
Starting 35 tests on medium_phone(AVD) - 16

com.lorenzo.monefyme.ui.ActionButtonsLayoutTest > actionButtonsRemainVisibleWhenTransactionHistoryIsOpen[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to inject touch input.
        Reason: Expected exactly '1' node but found '2' nodes that satisfy: (TestTag = 'balance_history_left')

com.lorenzo.monefyme.ui.HomeRestoreNavigationTest > balanceBoxLeftButtonOpensTransactionHistory[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to assert the following: (OnClick is defined)
        Reason: Expected exactly '1' node but found '2' nodes that satisfy: (TestTag = 'balance_history_left')

com.lorenzo.monefyme.ui.HomeRestoreNavigationTest > balanceBoxSideButtonsUseHamburgerIconsAndNotRestoreIcons[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to assert count of nodes.
        Reason: Expected '2' nodes but could not find any node that satisfies: (ContentDescription = 'Open transactionhistory' (ignoreCase: false))

com.lorenzo.monefyme.ui.HomeRestoreNavigationTest > balanceBoxRightButtonOpensTransactionHistory[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to assert the following: (OnClick is defined)
        Reason: Expected exactly '1' node but found '2' nodes that satisfy: (TestTag = 'balance_history_right')

medium_phone(AVD) - 16 Tests 14/35 completed. (0 skipped) (4 failed)
medium_phone(AVD) - 16 Tests 23/35 completed. (0 skipped) (4 failed)
medium_phone(AVD) - 16 Tests 33/35 completed. (0 skipped) (4 failed)

> Task :app:connectedDebugAndroidTest

com.lorenzo.monefyme.ui.TransactionHistoryStickyDragTest > draggingHistoryToTheTopKeepsItStuckFullyExpanded[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to inject touch input.
        Reason: Expected exactly '1' node but found '2' nodes that satisfy: (TestTag = 'balance_history_left')

com.lorenzo.monefyme.ui.TransactionHistoryStickyDragTest > draggedOpenHistoryStaysOpenUntilDraggedDownAgain[medium_phone(AVD) - 16] FAILED 
        java.lang.AssertionError: Failed to inject touch input.
        Reason: Expected exactly '1' node but found '2' nodes that satisfy: (TestTag = 'balance_history_left')
Tests on medium_phone(AVD) - 16 failed: There was 6 failure(s).

Finished 35 tests on medium_phone(AVD) - 16

> Task :app:connectedDebugAndroidTest FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:connectedDebugAndroidTest'.
> There were failing tests. See the report at: file:///Users/furrerl/perp/monefyme/app/build/reports/androidTests/connected/debug/index.html

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 41s
```