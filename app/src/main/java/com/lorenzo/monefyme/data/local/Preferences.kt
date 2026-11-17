package com.lorenzo.monefyme.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Key for storing whether transactions should be grouped by category (true) or by date (false).
 */
internal const val GROUP_BY_CATEGORY_KEY = "group_by_category"

// In-memory storage for preferences (shared between production and tests)
private val memoryPreferences = mutableMapOf<String, Any?>()

// Shared MutableStateFlow instance to maintain state across calls
private val groupByCategoryFlowInstance = MutableStateFlow(true)

/**
 * Provides access to the group-by-category preference as a StateFlow.
 * Defaults to true (group by category) if no value has been set.
 */
internal fun getGroupByCategoryFlow(): StateFlow<Boolean> {
    // Update the flow with current stored value on each call
    val currentValue = memoryPreferences[GROUP_BY_CATEGORY_KEY] as? Boolean ?: true
    groupByCategoryFlowInstance.value = currentValue
    return groupByCategoryFlowInstance
}

/**
 * Saves whether transactions should be grouped by category (true) or by date (false).
 */
internal fun saveGroupByCategory(groupedByCategory: Boolean) {
    memoryPreferences[GROUP_BY_CATEGORY_KEY] = groupedByCategory
    // Update the flow state directly so observers see the change immediately
    groupByCategoryFlowInstance.value = groupedByCategory
}
