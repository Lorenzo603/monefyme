package com.lorenzo.monefyme.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

class BudgetAppSmokeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_coreElementsAreVisible() {
        composeTestRule.onNodeWithText("Monefy").assertIsDisplayed()
        composeTestRule.onNodeWithText("All accounts", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_expense_button", useUnmergedTree = true).assert(hasClickAction())
        composeTestRule.onNodeWithTag("add_income_button", useUnmergedTree = true).assert(hasClickAction())
    }
}
