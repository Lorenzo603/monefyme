package com.lorenzo.monefyme.ui.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CategoryIconRegistryTest {
    @Test
    fun iconNamesAreUnique() {
        val names = CategoryIconRegistry.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun everyRegisteredIconResolvesToAVector() {
        CategoryIconRegistry.all.forEach { option ->
            assertNotNull(option.vector)
            assertSame(option.vector, CategoryIconRegistry.findByName(option.name))
        }
    }

    @Test
    fun unknownIconNameResolvesToNull() {
        assertNull(CategoryIconRegistry.findByName("NoSuchIcon"))
        // Registry lookups are case sensitive; default categories use registry names.
        assertNull(CategoryIconRegistry.findByName("shoppingcart"))
    }

    @Test
    fun defaultIconIsPartOfTheRegistry() {
        assertNotNull(CategoryIconRegistry.default)
    }
}
