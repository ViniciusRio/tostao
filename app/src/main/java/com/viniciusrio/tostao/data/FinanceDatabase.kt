package com.viniciusrio.tostao.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TransactionEntity::class,
        PlannedRuleEntity::class, BudgetEntity::class, BudgetCategoryEntity::class, CategoryGroupEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        /**
         * 1 → 2: grupos de categorias. Só acrescenta estrutura (sem reconstruir tabelas: um
         * DROP TABLE categories apagaria budget_categories por cascata). O SQL de criação é o
         * `createSql` do schema 2.json exportado; a coluna vem da diferença entre 1.json e 2.json.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `icon` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `groupId` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_groupId` ON `categories` (`groupId`)")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2)

        fun create(context: Context): FinanceDatabase =
            Room.databaseBuilder(context, FinanceDatabase::class.java, "finance.db").addMigrations(*MIGRATIONS).build()

        fun inMemory(context: Context): FinanceDatabase =
            Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
    }
}
