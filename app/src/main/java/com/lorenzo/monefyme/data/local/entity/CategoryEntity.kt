package com.lorenzo.monefyme.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["normalizedName"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val normalizedName: String,
    val displayName: String,
    val iconName: String? = null,
    val colorArgb: Int? = null,
)
