package com.lorenzo.monefyme.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(
            value = [
                "dateEpochDay",
                "categoryId",
                "amountCents",
                "type",
                "description",
                "account",
            ],
            unique = true,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochDay: Long,
    val categoryId: Long,
    val amountCents: Long,
    val type: String,
    val description: String,
    val account: String,
    val currency: String,
)
