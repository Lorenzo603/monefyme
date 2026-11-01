package com.lorenzo.monefyme.ui.category

import com.lorenzo.monefyme.data.model.CategoryItem

/**
 * Display order used by the category pickers: the seeded categories first, in the order they are
 * defined in [DefaultCategories], and then the categories that were stored later (imports and
 * manually added categories), oldest first.
 */
object CategoryOrder {
    fun seededFirst(categories: List<CategoryItem>): List<CategoryItem> {
        val pending = LinkedHashMap<String, CategoryItem>()
        categories.forEach { pending.putIfAbsent(normalize(it.displayName), it) }

        val ordered = ArrayList<CategoryItem>(DefaultCategories.all.size + categories.size)
        DefaultCategories.all.forEach { definition ->
            val stored = pending.remove(definition.normalizedName)
            ordered += stored
                ?: CategoryItem(id = 0L, displayName = definition.displayName, iconName = null, colorArgb = null)
        }
        ordered += pending.values.sortedBy { it.id }
        return ordered
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}
