package com.viniciusrio.tostao

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.viniciusrio.tostao.data.FinanceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Migração 1 → 2 (grupos de categorias) a partir do schema 1.json exportado, validada contra o
 * 2.json. Nada pode se perder, inclusive budget_categories (que tem ON DELETE CASCADE).
 */
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), FinanceDatabase::class.java,
    )

    @Test fun migrate1To2KeepsEverythingAndLeavesCategoriesUngrouped() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO accounts VALUES ('pix','Pix',4278190335,'bank',100000,1,0)")
            execSQL("INSERT INTO accounts VALUES ('btg','BTG',4278190335,'bank',0,1,1)")
            execSQL("INSERT INTO accounts VALUES ('inv','Invest',4278190335,'trending',0,0,2)")
            execSQL("INSERT INTO categories VALUES ('psi','Psicólogo','EXPENSE',4278190335,'health',0)")
            execSQL("INSERT INTO categories VALUES ('plano','Plano de saúde','EXPENSE',4278190335,'health',1)")
            execSQL("INSERT INTO categories VALUES ('sal','Salário','INCOME',4278190335,'work',100)")
            execSQL(
                "INSERT INTO transactions VALUES ('t1','EXPENSE',30000,'pix',NULL,'psi','','',1,NULL,0)," +
                    "('t2','EXPENSE',50000,'btg',NULL,'plano','Plano','',2,'r1',0)," +
                    "('t3','INCOME',500000,'pix',NULL,'sal','','',3,NULL,0)," +
                    "('t4','TRANSFER',1000,'pix','btg',NULL,'','',4,NULL,0)," +
                    "('t5','INCOME',200,'inv',NULL,NULL,'','',5,NULL,1)",
            )
            execSQL("INSERT INTO planned_rules VALUES ('r1','EXPENSE',50000,'btg','plano','Plano',20000,'MONTH',1,NULL,3)")
            execSQL("INSERT INTO budgets VALUES ('b1','Saúde',100000,0,0),('b2','Tudo',900000,1,1)")
            execSQL("INSERT INTO budget_categories VALUES ('b1','psi'),('b1','plano')")
            close()
        }

        val db = helper.runMigrationsAndValidate(name, 2, true, *FinanceDatabase.MIGRATIONS)

        fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(3, count("accounts"))
        assertEquals(3, count("categories"))
        assertEquals(5, count("transactions"))
        assertEquals(1, count("planned_rules"))
        assertEquals(2, count("budgets"))
        assertEquals(2, count("budget_categories"))
        assertEquals(0, count("category_groups"))
        assertEquals(0, count("categories WHERE groupId IS NOT NULL"))
        db.query("SELECT name, sortOrder FROM categories WHERE id = 'psi'").use {
            it.moveToFirst(); assertEquals("Psicólogo", it.getString(0)); assertEquals(0, it.getInt(1))
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse("foreign_key_check deveria vir vazio", it.moveToFirst()) }
        // A ordem de inserção (rowid) das transações, usada no desempate da lista, continua a mesma.
        db.query("SELECT id FROM transactions ORDER BY rowid").use {
            val ids = buildList { while (it.moveToNext()) add(it.getString(0)) }
            assertEquals(listOf("t1", "t2", "t3", "t4", "t5"), ids)
        }
        db.close()
    }
}
