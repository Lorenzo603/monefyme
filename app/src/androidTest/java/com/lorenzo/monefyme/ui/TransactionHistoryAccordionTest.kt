package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.ui.screen.TransactionGroupedList
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * End-to-end coverage for the transaction-history accordions.
 *
 * Every group renders collapsed first: only the group header (icon, name, count, total) is on
 * screen and none of the group's transactions are. Clicking a group header expands that group's
 * accordion - its transactions appear - and clicking it again collapses it. Other groups are not
 * affected by a click.
 *
 * Requires a device/emulator: run with ./gradlew connectedDebugAndroidTest.
 */
class TransactionHistoryAccordionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        CategoryItem(id = 1L, displayName = "Food", iconName = "Restaurant", colorArgb = null),
        CategoryItem(id = 2L, displayName = "Transport", iconName = "LocalTaxi", colorArgb = null),
    )

    private val transactions = listOf(
        TransactionListItem(
            id = 11L,
            dateEpochDay = LocalDate.of(2024, 1, 15).toEpochDay(),
            categoryId = 1L,
            categoryName = "Food",
            amountCents = 1000L,
            type = "EXPENSE",
            description = "Lunch",
            account = "Cash",
            currency = "EUR",
        ),
        TransactionListItem(
            id = 12L,
            dateEpochDay = LocalDate.of(2024, 1, 16).toEpochDay(),
            categoryId = 1L,
            categoryName = "Food",
            amountCents = 2000L,
            type = "EXPENSE",
            description = "Dinner",
            account = "Cash",
            currency = "EUR",
        ),
        TransactionListItem(
            id = 13L,
            dateEpochDay = LocalDate.of(2024, 1, 17).toEpochDay(),
            categoryId = 2L,
            categoryName = "Transport",
            amountCents = 500L,
            type = "EXPENSE",
            description = "Taxi",
            account = "Card",
            currency = "EUR",
        ),
    )

    private fun showGroupedList() {
        composeTestRule.setContent {
            TransactionGroupedList(
                transactions = transactions,
                categoriesMap = categories.associateBy { it.id },
                isGroupedByCategory = true,
                onTransactionClick = {},
            )
        }
    }

    @Test
    fun groupsStartCollapsed() {
        showGroupedList()

        composeTestRule.onNodeWithTag("group_header_Food").assertIsDisplayed()
        composeTestRule.onNodeWithTag("group_header_Transport").assertIsDisplayed()

        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_12").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_13").assertDoesNotExist()
    }

    @Test
    fun clickingGroupHeaderExpandsThatGroupOnly() {
        showGroupedList()

        composeTestRule.onNodeWithTag("group_header_Food").performClick()

        composeTestRule.onNodeWithTag("transaction_row_11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_12").assertIsDisplayed()
        // The Transport accordion stays collapsed.
        composeTestRule.onNodeWithTag("transaction_row_13").assertDoesNotExist()
    }

    @Test
    fun clickingExpandedGroupHeaderCollapsesItAgain() {
        showGroupedList()

        composeTestRule.onNodeWithTag("group_header_Food").performClick()
        composeTestRule.onNodeWithTag("transaction_row_11").assertIsDisplayed()

        composeTestRule.onNodeWithTag("group_header_Food").performClick()
        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_12").assertDoesNotExist()

        // The header itself remains on screen.
        composeTestRule.onNodeWithTag("group_header_Food").assertIsDisplayed()
    }

    @Test
    fun expandedGroupStaysExpandedWhileAnotherGroupIsToggled() {
        showGroupedList()

        composeTestRule.onNodeWithTag("group_header_Food").performClick()
        composeTestRule.onNodeWithTag("group_header_Transport").performClick()

        composeTestRule.onNodeWithTag("transaction_row_11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_13").assertIsDisplayed()

        composeTestRule.onNodeWithTag("group_header_Food").performClick()

        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_13").assertIsDisplayed()
    }
}
