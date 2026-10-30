package com.lorenzo.monefyme.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lorenzo.monefyme.data.local.dao.CategoryDao
import com.lorenzo.monefyme.data.local.dao.TransactionDao
import com.lorenzo.monefyme.data.local.entity.CategoryEntity
import com.lorenzo.monefyme.data.local.entity.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "monefyme.db"
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(
            context: Context,
            seedCategories: List<Pair<String, String>> = emptyList(),
        ): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).addCallback(SeedCallback(seedCategories)).build().also { instance = it }
            }
        }

        private class SeedCallback(
            private val categories: List<Pair<String, String>>,
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                categories.forEach { (normalizedName, displayName) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (normalizedName, displayName) VALUES (?, ?)",
                        arrayOf(normalizedName, displayName),
                    )
                }
            }
        }
    }
}
