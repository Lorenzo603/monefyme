package com.lorenzo.monefyme.ui.category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lorenzo.monefyme.data.model.CategoryItem

/** Icon to show for a stored category, whatever the source of the category is. */
fun CategoryItem.resolveIcon(): ImageVector {
    iconName?.let { CategoryIconRegistry.findByName(it) }?.let { return it }
    return DefaultCategories.findByName(displayName)?.icon ?: DefaultCategories.fallbackIcon
}

/** Color to show for a stored category, whatever the source of the category is. */
fun CategoryItem.resolveColor(): Color {
    colorArgb?.let { return Color(it.toLong() and 0xFFFFFFFFL) }
    return DefaultCategories.findByName(displayName)?.color ?: DefaultCategories.fallbackColor
}
