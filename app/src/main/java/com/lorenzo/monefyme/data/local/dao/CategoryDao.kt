package com.lorenzo.monefyme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lorenzo.monefyme.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun findById(categoryId: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET normalizedName = :normalizedName, displayName = :displayName, iconName = :iconName, colorArgb = :colorArgb WHERE id = :categoryId")
    suspend fun update(categoryId: Long, normalizedName: String, displayName: String, iconName: String?, colorArgb: Int?): Int

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long): Int
}
