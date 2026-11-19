package com.lorenzo.monefyme.ui.screen

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wiring that makes clicking a pie chart category icon slot open the add-expense flow
 * with that category preselected.
 *
 * The path is: ExpensePieChart reports the clicked slot through onCategorySlotClick (covered by
 * ExpensePieChartClickTest) -> HomeScreen hands the category to onAddExpenseForCategory ->
 * BudgetApp records it in the ViewModel (MainViewModel.requestAddExpense, covered by
 * MainViewModelTest) and navigates to the add-expense route -> AddTransactionScreen consumes it and
 * shows the category, adding the transaction with it from the calculator's bottom button.
 *
 * The composables need an emulator to click through, so their wiring is asserted by reading the
 * source files (the pattern used by HomeRestoreWiringTest); the gesture and the preselection are
 * exercised end-to-end by PieChartSectionClickUiTest and AddExpensePreselectedCategoryUiTest in
 * app/src/androidTest.
 */
class PieChartSlotAddExpenseWiringTest {

    private val homeScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt")
    }

    private val budgetAppSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/BudgetApp.kt")
    }

    private val addTransactionScreenSource by lazy {
        readSource("app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt")
    }

    // --- HomeScreen -------------------------------------------------------------

    @Test
    fun homeScreenRequiresTheAddExpenseForCategoryCallback() {
        val signature = homeScreenSource.substringAfter("fun HomeScreen(").substringBefore("\n) {")
        assertTrue(
            "HomeScreen must expose an onAddExpenseForCategory callback so a clicked pie chart icon " +
                "slot can open the add-expense flow for that category:\n$signature",
            signature.contains("onAddExpenseForCategory: (String) -> Unit,"),
        )
        assertFalse(
            "The callback must not have a silent no-op default: a caller that forgets the wiring " +
                "must fail to compile instead of the icon slot silently doing nothing.",
            signature.contains("onAddExpenseForCategory: (String) -> Unit ="),
        )
    }

    @Test
    fun homeScreenRoutesIconSlotClicksToTheAddExpenseCallback() {
        val pieChartCall = homeScreenSource.substringAfter("ExpensePieChart(")
            .substringBefore("\n                        )")
        assertTrue(
            "HomeScreen must listen for pie chart icon slot clicks:\n$pieChartCall",
            pieChartCall.contains("onCategorySlotClick = {"),
        )
        assertTrue(
            "A clicked icon slot must open the add-expense flow with that category:\n$pieChartCall",
            pieChartCall.contains("onAddExpenseForCategory(categoryName)"),
        )
        assertTrue(
            "Clicking a ring section must still expand the transaction history (unchanged " +
                "behaviour):\n$pieChartCall",
            pieChartCall.contains("onCategoryClick = {") &&
                pieChartCall.contains("viewModel.requestCategoryExpansion(categoryName)"),
        )
    }

    // --- BudgetApp --------------------------------------------------------------

    @Test
    fun budgetAppRecordsTheCategoryAndOpensTheAddExpenseRoute() {
        val block = budgetAppSource.substringAfter("onAddExpenseForCategory = {")
            .substringBefore("\n                    },")
        assertTrue(
            "BudgetApp must record the clicked category in the ViewModel so the add-expense screen " +
                "can preselect it:\n$block",
            block.contains("viewModel.requestAddExpense(categoryName)"),
        )
        assertTrue(
            "BudgetApp must open the add-expense route for the clicked icon slot:\n$block",
            block.contains("navController.navigate(Routes.AddExpense)"),
        )
    }

    // --- AddTransactionScreen ---------------------------------------------------

    @Test
    fun addTransactionScreenPicksUpThePreselectedCategory() {
        assertTrue(
            "AddTransactionScreen must consume the pending add-expense category request so the " +
                "clicked pie chart category is preselected.",
            addTransactionScreenSource.contains("viewModel.consumeAddExpenseCategoryRequest()"),
        )
        assertTrue(
            "The consumed request must be kept in state so it survives recompositions but is not " +
                "re-applied to a later open of the screen.",
            addTransactionScreenSource.contains("var preselectedCategory by rememberSaveable"),
        )
    }

    @Test
    fun addTransactionScreenShowsAndConfirmsThePreselectedCategory() {
        assertTrue(
            "The screen must show which category was preselected.",
            addTransactionScreenSource.contains("\"add_transaction_selected_category\""),
        )
        assertTrue(
            "The calculator must be told which category was preselected.",
            addTransactionScreenSource.contains("chosenCategory = chosenCategory,"),
        )
        assertTrue(
            "With a category preselected the calculator's bottom button confirms that category " +
                "instead of opening the picker.",
            addTransactionScreenSource.contains("onConfirmCategory = {"),
        )
        val confirmBlock = addTransactionScreenSource.substringAfter("onConfirmCategory = {")
            .substringBefore("\n                },")
        assertTrue(
            "Confirming must save the expense with the preselected category:\n$confirmBlock",
            confirmBlock.contains("chosenCategory?.let { persistTransaction(it) }"),
        )
        assertTrue(
            "The confirm button must be tagged for UI tests and show the chosen category.",
            addTransactionScreenSource.contains("\"add_transaction_confirm_button\"") &&
                addTransactionScreenSource.contains("ADD \\\""),
        )
        assertTrue(
            "Without a preselected category the calculator must keep its original CHOOSE CATEGORY " +
                "button.",
            addTransactionScreenSource.contains("CHOOSE CATEGORY"),
        )
    }
}

private fun readSource(relativePath: String): String {
    val file = File(findProjectRoot(), relativePath)
    assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
    // Normalise line endings so the assertions below are independent of the checkout setting.
    return file.readText().replace("\r\n", "\n")
}

private fun findProjectRoot(): File {
    val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
    var dir: File? = start
    while (dir != null) {
        val current: File = dir
        if (File(current, "settings.gradle.kts").exists() && File(current, "app").isDirectory) {
            return current
        }
        dir = current.parentFile
    }
    throw IllegalStateException("Could not locate the project root starting from $start")
}
