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
 * A category can also be handed to the list already expanded ([TransactionGroupedList]'s
 * `expandCategory`, which is how a pie chart section click opens the history): that accordion is
 * open from the start, the others stay collapsed, and the user can still toggle every group.
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

    private fun showGroupedList(expandCategory: String? = null, isGroupedByCategory: Boolean = true) {
        composeTestRule.setContent {
            TransactionGroupedList(
                transactions = transactions,
                categoriesMap = categories.associateBy { it.id },
                isGroupedByCategory = isGroupedByCategory,
                expandCategory = expandCategory,
                onTransactionClick = {},
            )
        }
        composeTestRule.waitForIdle()
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

    // --- opened from a pie chart section click: that category's accordion is already expanded ---

    @Test
    fun requestedCategoryOpensItsAccordionWithoutAnyClick() {
        showGroupedList(expandCategory = "Food")

        composeTestRule.onNodeWithTag("transaction_row_11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_12").assertIsDisplayed()
        // Only the clicked category's accordion opens.
        composeTestRule.onNodeWithTag("transaction_row_13").assertDoesNotExist()
    }

    @Test
    fun requestedCategoryOpensItsAccordionWhateverItsSpelling() {
        // The pie chart and the transactions do not have to spell the category identically.
        showGroupedList(expandCategory = "transport")

        composeTestRule.onNodeWithTag("transaction_row_13").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_12").assertDoesNotExist()
    }

    @Test
    fun unknownRequestedCategoryOpensNoAccordion() {
        showGroupedList(expandCategory = "Salary")

        composeTestRule.onNodeWithTag("group_header_Food").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_12").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_13").assertDoesNotExist()
    }

    @Test
    fun requestedCategoryIsStillCollapsibleAndOthersStillToggleable() {
        showGroupedList(expandCategory = "Food")

        composeTestRule.onNodeWithTag("group_header_Food").performClick()
        composeTestRule.onNodeWithTag("transaction_row_11").assertDoesNotExist()
        composeTestRule.onNodeWithTag("transaction_row_12").assertDoesNotExist()

        composeTestRule.onNodeWithTag("group_header_Transport").performClick()
        composeTestRule.onNodeWithTag("transaction_row_13").assertIsDisplayed()
    }

    @Test
    fun requestedCategoryStaysExpandedAfterRecompositions() {
        showGroupedList(expandCategory = "Food")

        // Expanding another group re-renders the list; the requested accordion must not be
        // forced open again, nor collapsed by it.
        composeTestRule.onNodeWithTag("group_header_Transport").performClick()

        composeTestRule.onNodeWithTag("transaction_row_11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_row_13").assertIsDisplayed()
    }
}
