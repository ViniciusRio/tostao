package com.viniciusrio.tostao

import androidx.test.platform.app.InstrumentationRegistry
import com.viniciusrio.tostao.data.FinanceDatabase
import com.viniciusrio.tostao.data.FinanceRepository
import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.RecurrenceUnit
import com.viniciusrio.tostao.domain.Transaction
import com.viniciusrio.tostao.domain.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FinanceRepositoryTest {
    private lateinit var db: FinanceDatabase
    private lateinit var repo: FinanceRepository

    @Before fun setUp() {
        db = FinanceDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
        repo = FinanceRepository(db)
    }

    @After fun tearDown() = db.close()

    private fun acc(id: String) = Account(id, id, 0, "bank", 0, true, 0)
    private fun tx(id: String, from: String, to: String? = null, cat: String? = null) = Transaction(
        id, if (to != null) TxType.TRANSFER else TxType.EXPENSE, 100, from, to, cat, "", "", 1L,
    )

    @Test fun seedsDefaultCategoriesOnce() = runBlocking {
        repo.seedOnFirstRun(alreadySeeded = false); repo.seedOnFirstRun(alreadySeeded = false)
        assertEquals(14, repo.data.first().categories.size)
    }

    @Test fun doesNotSeedAgainAfterFirstRun() = runBlocking {
        repo.seedOnFirstRun(alreadySeeded = true)
        assertTrue(repo.data.first().categories.isEmpty())
    }

    private fun backupSample() = com.viniciusrio.tostao.domain.FinanceData(
        accounts = listOf(acc("X").copy(order = 1), acc("Y").copy(name = "Y2", order = 0)),
        categories = listOf(Category("food", "Comida", CategoryType.EXPENSE, 0, "tag", 0)),
        transactions = listOf(tx("n1", "X", cat = "food"), tx("n2", "X", to = "Y")),
        rules = listOf(PlannedRule("rr", TxType.EXPENSE, 500, "Y", "food", "Luz", LocalDate.of(2026, 1, 31), RecurrenceUnit.MONTH, 1, null, 4)),
        budgets = listOf(Budget("bb", "Comida", 1000, false, setOf("food"), 0), Budget("all", "Tudo", 9000, true, emptySet(), 1)),
    )

    @Test fun replaceAllSwapsEverythingAndSnapshotReadsItBack() = runBlocking {
        repo.seedOnFirstRun(alreadySeeded = false)
        repo.saveAccount(acc("old")); repo.saveTransaction(tx("old-t", "old"))
        val incoming = backupSample()
        repo.replaceAll(incoming)
        val snap = repo.snapshot()
        assertEquals(incoming.accounts.sortedBy { it.id }, snap.accounts.sortedBy { it.id })
        assertEquals(incoming.categories, snap.categories)
        assertEquals(incoming.transactions.sortedBy { it.id }, snap.transactions.sortedBy { it.id })
        assertEquals(incoming.rules, snap.rules)
        assertEquals(4, snap.rules.single().consumedOccurrences)
        assertEquals(incoming.budgets.sortedBy { it.id }, snap.budgets.sortedBy { it.id })
    }

    @Test fun replaceAllFailingMidwayKeepsOldData() = runBlocking {
        repo.saveAccount(acc("old")); repo.saveTransaction(tx("old-t", "old"))
        repo.afterClearForTest = { error("falha simulada") }
        val failed = runCatching { repo.replaceAll(backupSample()) }.isFailure
        repo.afterClearForTest = null
        assertTrue(failed)
        val snap = repo.snapshot()
        assertEquals(listOf("old"), snap.accounts.map { it.id })
        assertEquals(listOf("old-t"), snap.transactions.map { it.id })
    }

    @Test fun deletingAccountRemovesTransfersBothWaysAndCanBeRestored() = runBlocking {
        repo.saveAccount(acc("A")); repo.saveAccount(acc("B"))
        repo.saveTransaction(tx("t1", "A"))
        repo.saveTransaction(tx("t2", "B", to = "A"))
        repo.saveTransaction(tx("t3", "B"))
        repo.saveRule(PlannedRule("r", TxType.EXPENSE, 1, "A", null, "x", LocalDate.of(2026, 1, 1), null, 1, null, 0))
        val d = repo.deleteAccount("A")!!
        var data = repo.data.first()
        assertEquals(listOf("t3"), data.transactions.map { it.id })
        assertTrue(data.rules.isEmpty())
        repo.restoreAccount(d)
        data = repo.data.first()
        assertEquals(3, data.transactions.size)
        assertEquals(1, data.rules.size)
    }

    @Test fun payAndUndoOnlyLatestAction() = runBlocking {
        repo.saveAccount(acc("A"))
        repo.saveRule(PlannedRule("r", TxType.EXPENSE, 59540, "A", null, "Parcela", LocalDate.of(2026, 10, 17), RecurrenceUnit.MONTH, 1, null, 0))
        val first = repo.payRule("r", 1000L)!!
        assertEquals(1, repo.data.first().transactions.size)
        val second = repo.skipRule("r")!!
        assertFalse("ação antiga não pode ser desfeita", repo.undoPlanned(first))
        assertTrue(repo.undoPlanned(second))
        assertTrue(repo.undoPlanned(first))
        val data = repo.data.first()
        assertEquals(0, data.rules.single().consumedOccurrences)
        assertTrue(data.transactions.isEmpty())
    }

    @Test fun oneTimeRuleCannotBePaidTwice() = runBlocking {
        repo.saveAccount(acc("A"))
        repo.saveRule(PlannedRule("r", TxType.EXPENSE, 100, "A", null, "x", LocalDate.of(2026, 1, 1), null, 1, null, 0))
        assertTrue(repo.payRule("r", 1L) != null)
        assertNull(repo.payRule("r", 2L))
        assertNull(repo.skipRule("r"))
        assertEquals(1, repo.data.first().transactions.size)
    }

    @Test fun deletingCategoryUncategorizesAndDropsSingleCategoryBudgets() = runBlocking {
        repo.saveAccount(acc("A"))
        repo.saveCategory(Category("c1", "Mercado", CategoryType.EXPENSE, 0, "cart", 0))
        repo.saveCategory(Category("c2", "Lazer", CategoryType.EXPENSE, 0, "fun", 1))
        repo.saveTransaction(tx("t", "A", cat = "c1"))
        repo.saveRule(PlannedRule("r", TxType.EXPENSE, 1, "A", "c1", "x", LocalDate.of(2026, 1, 1), null, 1, null, 0))
        repo.saveBudget(Budget("only", "Só mercado", 100, false, setOf("c1"), 0))
        repo.saveBudget(Budget("both", "Ambos", 100, false, setOf("c1", "c2"), 1))
        repo.saveBudget(Budget("all", "Tudo", 100, true, emptySet(), 2))
        repo.deleteCategory("c1")
        val data = repo.data.first()
        assertNull(data.transactions.single().categoryId)
        assertNull(data.rules.single().categoryId)
        assertEquals(setOf("both", "all"), data.budgets.map { it.id }.toSet())
        assertEquals(setOf("c2"), data.budgets.first { it.id == "both" }.categoryIds)
    }
}
