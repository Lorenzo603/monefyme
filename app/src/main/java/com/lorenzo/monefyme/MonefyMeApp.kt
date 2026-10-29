package com.lorenzo.monefyme

import android.app.Application
import com.lorenzo.monefyme.data.local.AppDatabase
import com.lorenzo.monefyme.data.repository.BudgetRepository
import com.lorenzo.monefyme.data.repository.BudgetRepositoryImpl

class MonefyMeApp : Application() {
    val repository: BudgetRepository by lazy {
        val database = AppDatabase.getInstance(this)
        BudgetRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
        )
    }
}
