package com.lorenzo.monefyme

import android.app.Application
import com.lorenzo.monefyme.data.local.AppDatabase
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.data.repository.BudgetRepositoryImpl
import com.lorenzo.monefyme.ui.category.DefaultCategories

class MonefyMeApp : Application() {
    val repository: BudgetRepository by lazy {
        val seedCategories = DefaultCategories.all.map { it.normalizedName to it.displayName }
        val database = AppDatabase.getInstance(this, seedCategories)
        BudgetRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
        )
    }
}
