package com.lorenzo.monefyme.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.lorenzo.monefyme.MainActivity
import org.junit.Rule
import org.junit.Test

class ScratchPagerDiagnoseTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun diagnose() {
        composeTestRule.waitForIdle()
        val bar = composeTestRule.onNodeWithTag("balance_bar").fetchSemanticsNode().boundsInRoot
        val pager = composeTestRule.onNodeWithTag("month_pager").fetchSemanticsNode().boundsInRoot
        val out = mutableListOf<String>()
        out += "bar=${bar.toShortString()} pager=${pager.toShortString()} start=${centred()}"

        composeTestRule.onNodeWithTag("month_pager").performTouchInput {
            swipeRight(startX = (pager.left + pager.right) / 2f - 200f, startY = bar.top + 10f)
        }
        settle()
        out += "after pager swipe at bar top y=${bar.top + 10f}: ${centred()}"

        composeTestRule.onNodeWithTag("month_pager").performTouchInput {
            swipeRight(startX = (pager.left + pager.right) / 2f - 200f, startY = (bar.top + bar.bottom) / 2f)
        }
        settle()
        out += "after pager swipe at bar middle: ${centred()}"

        composeTestRule.onNodeWithTag("balance_bar").performTouchInput {
            swipeRight(startX = bar.left + 60f, startY = (bar.top + bar.bottom) / 2f)
        }
        settle()
        out += "after bar swipe from left: ${centred()}"

        composeTestRule.onNodeWithTag("balance_box").performTouchInput {
            swipeRight(startX = 40f, startY = 40f, durationMillis = 100L)
        }
        settle()
        out += "after balance box swipe fast: ${centred()}"

        throw AssertionError(out.joinToString(" | "))
    }

    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(1_200L)
        composeTestRule.waitForIdle()
    }

    private fun centred(): String {
        val t = composeTestRule.onNodeWithTag("month_ticker").fetchSemanticsNode().boundsInRoot
        val cx = (t.left + t.right) / 2f
        val labels = composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { n ->
                val text = (n.config[SemanticsProperties.Text] as? List<*>)?.firstOrNull()?.toString() ?: return@mapNotNull null
                val b = n.boundsInRoot
                if (b.top < t.top - 1f || b.bottom > t.bottom + 1f || b.right <= b.left) return@mapNotNull null
                Triple(text, (b.left + b.right) / 2f, b.top)
            }
            .sortedBy { it.second }
        return labels.joinToString(",") { "${it.first}@${it.second.toInt()}" }
    }
}
