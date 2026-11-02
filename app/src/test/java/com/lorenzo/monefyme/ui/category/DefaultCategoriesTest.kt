package com.lorenzo.monefyme.ui.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun everyDefaultCategoryHasALowercaseTrimmedNormalizedName() {
        DefaultCategories.all.forEach { def ->
            assertEquals(def.displayName.trim().lowercase(), def.normalizedName)
        }
    }

    @Test
    fun defaultCategoryNamesAreUnique() {
        val names = DefaultCategories.all.map { it.normalizedName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun findByNameMatchesCanonicalNamesIgnoringCaseAndPadding() {
        assertNotNull(DefaultCategories.findByName("Transport"))
        assertNotNull(DefaultCategories.findByName(" transport "))
        assertSame(
            DefaultCategories.findByName("Transport"),
            DefaultCategories.findByName("TRANSPORT"),
        )
    }

    @Test
    fun findByNameResolvesAliasesToCanonicalEntries() {
        assertSame(
            DefaultCategories.findByName("Caffè offerti"),
            DefaultCategories.findByName("caffe offerti"),
        )
        assertSame(
            DefaultCategories.findByName("House"),
            DefaultCategories.findByName("Home"),
        )
        assertSame(
            DefaultCategories.findByName("House"),
            DefaultCategories.findByName("Housing"),
        )
    }

    @Test
    fun findByNameReturnsNullForUnknownCategory() {
        assertNull(DefaultCategories.findByName("Yachts"))
        assertNull(DefaultCategories.findByName(""))
    }

    @Test
    fun everyDefaultCategoryHasIconAndColorDifferentFromFallback() {
        DefaultCategories.all.forEach { def ->
            assertNotNull(def.icon)
            assertTrue(def.color != DefaultCategories.fallbackColor)
        }
        assertNotNull(DefaultCategories.fallbackIcon)
    }
}
