package com.viniciusrio.tostao.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TransactionEntity::class,
        PlannedRuleEntity::class, BudgetEntity::class, BudgetCategoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        fun create(context: Context): FinanceDatabase =
            Room.databaseBuilder(context, FinanceDatabase::class.java, "finance.db").build()

        fun inMemory(context: Context): FinanceDatabase =
            Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
    }
}
