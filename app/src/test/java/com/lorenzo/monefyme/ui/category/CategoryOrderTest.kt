package com.lorenzo.monefyme.ui.category

import com.lorenzo.monefyme.data.model.CategoryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryOrderTest {
    private val seededNames = DefaultCategories.all.map { it.displayName }

    @Test
    fun emptyDatabaseStillShowsEverySeededCategoryInDeclaredOrder() {
        assertEquals(seededNames, CategoryOrder.seededFirst(emptyList()).map { it.displayName })
    }

    @Test
    fun categoriesStoredAfterTheSeedAreAppendedLast() {
        val seededRows = seededNames.mapIndexed { index, name ->
            CategoryItem(id = (index + 1).toLong(), displayName = name, iconName = null, colorArgb = null)
        }
        val stored = seededRows + listOf(
            CategoryItem(id = 101L, displayName = "Uni", iconName = "backpack", colorArgb = 0),
            CategoryItem(id = 102L, displayName = "Books", iconName = null, colorArgb = null),
        )

        // The DAO lists categories alphabetically, so the incoming order cannot be trusted.
        val orderedNames = CategoryOrder.seededFirst(stored.shuffled()).map { it.displayName }

        assertEquals(seededNames + listOf("Uni", "Books"), orderedNames)
    }

    @Test
    fun storedRowMatchingASeededCategoryIsTheOneDisplayed() {
        val stored = listOf(CategoryItem(id = 7L, displayName = "Transport", iconName = "train", colorArgb = null))
        val ordered = CategoryOrder.seededFirst(stored)

        assertEquals(seededNames, ordered.map { it.displayName })

        val transport = ordered.single { it.displayName == "Transport" }
        assertEquals(7L, transport.id)
        assertEquals("train", transport.iconName)
    }

    @Test
    fun seededNamesAreMatchedIgnoringCaseAndPadding() {
        val stored = listOf(
            CategoryItem(id = 5L, displayName = " Transport ", iconName = null, colorArgb = null),
            CategoryItem(id = 6L, displayName = "Uni", iconName = null, colorArgb = null),
        )
        val ordered = CategoryOrder.seededFirst(stored)

        assertEquals(seededNames.size + 1, ordered.size)
        assertEquals(5L, ordered.single { it.displayName == " Transport " }.id)
        assertEquals(0L, ordered.first { it.displayName == "House" }.id)
        assertEquals("Uni", ordered.last().displayName)
    }
}
