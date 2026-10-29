package com.lorenzo.monefyme.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lorenzo.monefyme.data.local.entity.TransactionEntity
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT
            t.id,
            t.dateEpochDay,
            t.categoryId,
            c.displayName AS categoryName,
            t.amountCents,
            t.type,
            t.description,
            t.account,
            t.currency
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY t.dateEpochDay DESC, t.id DESC
        """,
    )
    fun observeByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionListItem>>

    @Query(
        """
        SELECT
            c.displayName AS categoryName,
            SUM(t.amountCents) AS totalCents
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
          AND t.type = 'EXPENSE'
        GROUP BY c.id, c.displayName
        ORDER BY totalCents DESC
        """,
    )
    fun observeExpenseSlicesByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseSlice>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
