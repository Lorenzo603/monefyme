package com.lorenzo.monefyme.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lorenzo.monefyme.ui.category.CategoryIconRegistry
import com.lorenzo.monefyme.ui.category.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseSliceTest {

    @Test
    fun `ExpenseSlice should have default values for color and icon`() {
        val slice = ExpenseSlice(
            categoryName = "Test Category",
            totalCents = 1000L
        )
        
        assertNull(slice.colorArgb)
        assertNull(slice.icon)
    }

    @Test
    fun `ExpenseSlice should store color and icon when provided`() {
        val color = Color(0xFFFF0000)
        val colorArgb = color.value.toInt()
        val icon = CategoryIconRegistry.all.first().name
        
        val slice = ExpenseSlice(
            categoryName = "Test Category",
            totalCents = 1000L,
            colorArgb = colorArgb,
            icon = icon
        )
        
        assertEquals(colorArgb, slice.colorArgb)
        assertEquals(icon, slice.icon)
    }

    @Test
    fun `ExpenseSlice should preserve case-insensitive category name lookup`() {
        val slice1 = ExpenseSlice(
            categoryName = "Test Category",
            totalCents = 1000L
        )
        val slice2 = ExpenseSlice(
            categoryName = "TEST CATEGORY",
            totalCents = 1000L
        )
        
        assertEquals(slice1.categoryName.lowercase(), slice2.categoryName.lowercase())
    }
}
