package com.lorenzo.monefyme.ui.screen

import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.TransactionType
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the redesigned transaction-history group cards (TransactionGroupedList in HomeScreen.kt),
 * following the reference screenshots of the Monefy-style history:
 *
 * - The group header and its entry rows share one container color, so an open accordion reads as
 *   a single seamless block (no green header on top of white entry cards).
 * - Every entry row carries a small round dot: red for an expense, green for an income.
 * - The number of entries in a group sits right after the group's name, circled in green; the old
 *   small gray count next to the total is gone.
 * - The group total is red for an expense group and green for an income group (majority type,
 *   a tie counting as expense).
 * - Grouped by category, an entry shows only the amount (left) and the date (right); grouped by
 *   date, it shows only the category (left) and the amount (right).
 */
class TransactionGroupCardTest {

    private val source by lazy { readSource(SOURCE_PATH) }

    // --- shared header/entry background -------------------------------------------------------

    @Test
    fun headerAndEntryRowsShareTheSameContainerColor() {
        val header = functionBody("private fun GroupHeader(")
        val row = functionBody("private fun TransactionRow(")

        assertTrue(
            "The group header must use the shared group-card container color.",
            header.contains("containerColor = GroupCardContainerColor"),
        )
        assertTrue(
            "The entry rows must use the very same shared group-card container color, so the " +
                "header and its entries have no background difference.",
            row.contains("containerColor = GroupCardContainerColor"),
        )
        assertFalse(
            "Entry rows must no longer be white cards distinct from the green header.",
            row.contains("containerColor = Color.White"),
        )
        assertFalse(
            "The header must no longer hard-code its own light-green background literal.",
            header.contains("containerColor = Color(0xFF"),
        )
        assertFalse(
            "The entry rows must no longer hard-code their own background literal.",
            row.contains("containerColor = Color(0xFF"),
        )
    }

    // --- green-circled count right after the group name ----------------------------------------

    @Test
    fun groupCountBadgeIsCircledInGreenAndSitsRightAfterTheGroupName() {
        val header = functionBody("private fun GroupHeader(")

        val nameIndex = header.indexOf("text = groupName")
        val badgeIndex = header.indexOf("testTag(\"group_count_\$groupName\")")
        val totalIndex = header.indexOf("centsToDisplay(totalAmount)")

        assertTrue("The header must still render the group name.", nameIndex >= 0)
        assertTrue(
            "The header must show the entry count in a badge tagged group_count_<groupName>.",
            badgeIndex >= 0,
        )
        assertTrue("The header must still render the group total.", totalIndex >= 0)
        assertTrue(
            "The count badge must be placed after the group's name (name, then count, then total).",
            nameIndex < badgeIndex && badgeIndex < totalIndex,
        )

        // The badge wraps the number in a green circle.
        val badgeStart = header.lastIndexOf("Box(", badgeIndex)
        assertTrue("The count badge must be a Box.", badgeStart >= 0)
        val badgeBlock = header.substring(badgeStart, header.indexOf(")", badgeIndex))
        assertTrue(
            "The count badge must be circled (CircleShape).",
            badgeBlock.contains("CircleShape"),
        )
        assertTrue(
            "The count badge must be circled in green (the income green).",
            badgeBlock.contains("background(GroupEntryIncomeColor)"),
        )
        assertTrue(
            "The badge must contain the group's entry count.",
            header.substring(badgeStart).contains("\"\$transactionCount\""),
        )

        // The old small gray count in the right-hand column is gone.
        assertFalse(
            "The header must not keep the old small gray count (bodySmall gray text).",
            header.contains("style = MaterialTheme.typography.bodySmall"),
        )
    }

    // --- red/green total by group type -----------------------------------------------------------

    @Test
    fun headerTotalColorFollowsTheMajorityTransactionType() {
        val header = functionBody("private fun GroupHeader(")

        assertTrue(
            "The header must derive its total color from the group's majority transaction type.",
            header.contains("groupTotalTransactionType(groupTransactions)"),
        )
        assertTrue(
            "An expense group total must be red.",
            header.contains("TransactionType.EXPENSE -> GroupEntryExpenseColor"),
        )
        assertTrue(
            "An income group total must be green.",
            header.contains("TransactionType.INCOME -> GroupEntryIncomeColor"),
        )
        val colorIndex = header.indexOf("groupAmountColor")
        val totalIndex = header.indexOf("centsToDisplay(totalAmount)")
        assertTrue(
            "The total amount text must be painted with the type color.",
            colorIndex >= 0 && totalIndex < header.indexOf("color = groupAmountColor"),
        )
    }

    @Test
    fun groupTotalTypeIsTheMajorityOfTheGroupTransactions() {
        assertEquals(TransactionType.EXPENSE, groupTotalTransactionType(listOf(transaction(1, "EXPENSE"))))
        assertEquals(TransactionType.INCOME, groupTotalTransactionType(listOf(transaction(1, "INCOME"))))
        assertEquals(
            "A group with more expenses than incomes is an expense group.",
            TransactionType.EXPENSE,
            groupTotalTransactionType(
                listOf(transaction(1, "EXPENSE"), transaction(2, "EXPENSE"), transaction(3, "INCOME")),
            ),
        )
        assertEquals(
            "A group with more incomes than expenses is an income group.",
            TransactionType.INCOME,
            groupTotalTransactionType(
                listOf(transaction(1, "EXPENSE"), transaction(2, "INCOME"), transaction(3, "INCOME")),
            ),
        )
        assertEquals(
            "A tie between expenses and incomes must be treated as an expense group.",
            TransactionType.EXPENSE,
            groupTotalTransactionType(listOf(transaction(1, "EXPENSE"), transaction(2, "INCOME"))),
        )
    }

    // --- entry row content per grouping mode ------------------------------------------------------

    @Test
    fun entryInCategoryGroupShowsOnlyAmountLeftAndDateRight() {
        val categoryBranch = groupModeBranch("if (isGroupedByCategory) {")

        assertTrue(
            "A category-grouped entry must show the amount.",
            categoryBranch.contains("amountText"),
        )
        assertTrue(
            "A category-grouped entry must show the date on the right (after the amount).",
            categoryBranch.indexOf("amountText") < categoryBranch.indexOf("formatDate(transaction.date)"),
        )
        assertFalse(
            "The category name must not repeat on the entry row - it is already the group name.",
            categoryBranch.contains("transaction.categoryName"),
        )
        assertFalse(
            "The description must not be shown on the entry row.",
            categoryBranch.contains("transaction.description"),
        )
    }

    @Test
    fun entryInDateGroupShowsOnlyCategoryLeftAndAmountRight() {
        val dateBranch = groupModeBranch("} else {")

        assertTrue(
            "A date-grouped entry must show the category on the left.",
            dateBranch.contains("transaction.categoryName"),
        )
        assertTrue(
            "A date-grouped entry must show the amount on the right (after the category).",
            dateBranch.indexOf("transaction.categoryName") < dateBranch.indexOf("amountText"),
        )
        assertFalse(
            "The date must not repeat on the entry row - it is already the group name.",
            dateBranch.contains("formatDate("),
        )
        assertFalse(
            "The account must not be shown on the entry row.",
            dateBranch.contains("transaction.account"),
        )
    }

    @Test
    fun entryRowHasACircularTypeDotRedForExpenseGreenForIncome() {
        val row = functionBody("private fun TransactionRow(")

        val dotIndex = row.indexOf("testTag(\"transaction_type_dot_\${transaction.id}\")")
        assertTrue(
            "Each entry row must show a type dot tagged transaction_type_dot_<id>.",
            dotIndex >= 0,
        )
        assertTrue(
            "The type dot must be circular (CircleShape).",
            row.contains("clip(CircleShape)"),
        )
        assertTrue(
            "An expense entry dot must be red.",
            row.contains("TransactionType.EXPENSE -> GroupEntryExpenseColor"),
        )
        assertTrue(
            "An income entry dot must be green.",
            row.contains("TransactionType.INCOME -> GroupEntryIncomeColor"),
        )
        val dotStart = row.lastIndexOf("Box(", dotIndex)
        val amountIndex = row.indexOf("amountText")
        assertTrue(
            "The dot must come before the amount, i.e. at the left of the row.",
            dotStart >= 0 && dotStart < amountIndex,
        )
    }

    // --- preserved interaction and test tags -------------------------------------------------------

    @Test
    fun accordionTagsAndCategoryHeaderIconArePreserved() {
        val header = functionBody("private fun GroupHeader(")
        val row = functionBody("private fun TransactionRow(")
        val list = functionBody("internal fun TransactionGroupedList(")

        assertTrue(
            "The group header test tag group_header_<groupName> must be preserved.",
            header.contains("testTag(\"group_header_\$groupName\")"),
        )
        assertTrue(
            "The entry row test tag transaction_row_<id> must be preserved.",
            row.contains("testTag(\"transaction_row_\${transaction.id}\")"),
        )
        assertTrue(
            "The header must keep the expand/collapse chevron.",
            header.contains("Icons.Outlined.KeyboardArrowUp") &&
                header.contains("Icons.Outlined.KeyboardArrowDown"),
        )
        assertTrue(
            "The header must keep the expand/collapse click handling of the accordion.",
            list.contains("expandedGroupsAfterToggle(expandedGroups, groupName)"),
        )
        val iconGuard = header.indexOf("if (isGroupedByCategory && categoryItem != null)")
        assertTrue(
            "The category icon must stay in the header for category groups.",
            iconGuard >= 0 && header.indexOf("categoryItem.resolveIcon()") > iconGuard,
        )
    }

    // --- helpers -----------------------------------------------------------------------------------

    /** The source of [name] up to the next top-level declaration. */
    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Function '$signature' not found in $SOURCE_PATH", start >= 0)
        var end = source.indexOf("\n@Composable", start + signature.length)
        var plainFun = source.indexOf("\ninternal fun ", start + signature.length)
        if (plainFun in 0 until end) end = plainFun
        plainFun = source.indexOf("\nprivate fun ", start + signature.length)
        if (plainFun in 0 until end) end = plainFun
        val internalVal = source.indexOf("\ninternal val ", start + signature.length)
        if (internalVal in 0 until end) end = internalVal
        if (end < 0) end = source.length
        return source.substring(start, end)
    }

    /**
     * The body of one of the two grouping-mode branches inside TransactionRow: the category branch
     * (`if (isGroupedByCategory) { ... }`) or the date branch (`} else { ... }`).
     */
    private fun groupModeBranch(marker: String): String {
        val row = functionBody("private fun TransactionRow(")
        val start = row.indexOf(marker)
        assertTrue("Branch '$marker' not found in TransactionRow", start >= 0)
        val afterMarker = start + marker.length
        val elseIndex = row.indexOf("} else {", afterMarker)
        val endIndex = if (marker == "} else {" || elseIndex < 0) row.indexOf("\n        }", afterMarker) else elseIndex
        assertTrue("Could not find the end of branch '$marker'", endIndex > afterMarker)
        return row.substring(afterMarker, endIndex)
    }

    private fun transaction(id: Long, type: String) = TransactionListItem(
        id = id,
        dateEpochDay = LocalDate.of(2024, 3, 5).toEpochDay(),
        categoryId = 1L,
        categoryName = "Food",
        amountCents = 1000L,
        type = type,
        description = "",
        account = "",
        currency = "EUR",
    )

    private fun readSource(relativePath: String): String {
        val file = File(findProjectRoot(), relativePath)
        assertTrue("Cannot find $relativePath under ${findProjectRoot().absolutePath}", file.exists())
        // Normalise line endings so the assertions are independent of the checkout setting.
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

    private companion object {
        const val SOURCE_PATH = "app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt"
    }
}
