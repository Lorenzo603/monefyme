package com.lorenzo.monefyme.ui.category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryDef(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
) {
    val normalizedName: String = displayName.trim().lowercase()
}
