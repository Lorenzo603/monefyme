package com.lorenzo.monefyme.ui.category

import androidx.compose.ui.graphics.Color
import com.lorenzo.monefyme.data.model.CategoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CategoryAppearanceTest {
    private fun item(
        displayName: String,
        iconName: String? = null,
        colorArgb: Int? = null,
    ) = CategoryItem(id = 1L, displayName = displayName, iconName = iconName, colorArgb = colorArgb)

    @Test
    fun storedIconNameWinsOverEverythingElse() {
        val resolved = item("Food", iconName = "Star").resolveIcon()
        assertSame(CategoryIconRegistry.findByName("Star"), resolved)
    }

    @Test
    fun categoryWithoutStoredIconFallsBackToItsDefaultDefinition() {
        val foodDefault = requireNotNull(DefaultCategories.findByName("Food"))
        assertSame(foodDefault.icon, item("Food").resolveIcon())
        // Aliases of default categories resolve too.
        assertSame(
            requireNotNull(DefaultCategories.findByName("House")).icon,
            item("Home").resolveIcon(),
        )
    }

    @Test
    fun unknownCategoryWithoutStoredIconUsesTheFallbackIcon() {
        assertSame(DefaultCategories.fallbackIcon, item("Yachts").resolveIcon())
    }

    @Test
    fun storedColorWinsOverTheDefaultDefinition() {
        val stored = Color(0xFF00FF00L)
        assertEquals(stored, item("House", colorArgb = 0xFF00FF00.toInt()).resolveColor())
    }

    @Test
    fun categoryWithoutStoredColorUsesItsDefaultDefinition() {
        val houseDefault = requireNotNull(DefaultCategories.findByName("House"))
        assertEquals(houseDefault.color, item("House").resolveColor())
    }

    @Test
    fun unknownCategoryWithoutStoredColorUsesTheFallbackColor() {
        assertEquals(DefaultCategories.fallbackColor, item("Yachts").resolveColor())
    }
}
