package com.viniciusrio.finance.data

import androidx.room.withTransaction
import com.viniciusrio.finance.domain.Account
import com.viniciusrio.finance.domain.Budget
import com.viniciusrio.finance.domain.Category
import com.viniciusrio.finance.domain.FinanceData
import com.viniciusrio.finance.domain.PlannedRule
import com.viniciusrio.finance.domain.Recurrence
import com.viniciusrio.finance.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

/** Snapshot de tudo que uma exclusão de conta remove, para confirmação e desfazer. */
data class AccountDeletion(val account: Account, val transactions: List<Transaction>, val rules: List<PlannedRule>)

/** Resultado de pagar/pular: o que foi gravado, para permitir desfazer só esta ação. */
data class PlannedAction(val ruleId: String, val consumedAfter: Int, val createdTransactionId: String?)

class FinanceRepository(private val db: FinanceDatabase) {
    private val dao = db.dao()

    val data: Flow<FinanceData> = combine(
        dao.accounts(), dao.categories(), dao.transactions(), dao.rules(),
        combine(dao.budgets(), dao.budgetCategories()) { b, bc -> b to bc },
    ) { a, c, t, r, (b, bc) ->
        val byBudget = bc.groupBy({ it.budgetId }, { it.categoryId })
        FinanceData(
            accounts = a.map { it.toDomain() },
            categories = c.map { it.toDomain() },
            transactions = t.map { it.toDomain() },
            rules = r.map { it.toDomain() },
            budgets = b.map { Budget(it.id, it.name, it.limitCents, it.allExpenses, byBudget[it.id].orEmpty().toSet(), it.sortOrder) },
        )
    }

    suspend fun seedIfEmpty() {
        if (dao.categoryCount() == 0) dao.insertCategories(DefaultCategories.all())
    }

    fun newId(): String = UUID.randomUUID().toString()

    suspend fun nextAccountOrder() = dao.maxAccountOrder() + 1
    suspend fun nextBudgetOrder() = dao.maxBudgetOrder() + 1
    suspend fun nextCategoryOrder() = dao.maxCategoryOrder() + 1

    suspend fun saveAccount(a: Account) = dao.upsertAccount(a.toEntity())
    suspend fun reorderAccounts(ordered: List<Account>) =
        dao.upsertAccounts(ordered.mapIndexed { i, a -> a.copy(order = i).toEntity() })

    suspend fun deleteAccount(id: String): AccountDeletion? = db.withTransaction {
        val acc = dao.account(id) ?: return@withTransaction null
        val snapshot = AccountDeletion(
            acc.toDomain(),
            dao.transactionsOfAccount(id).map { it.toDomain() },
            dao.rulesOfAccount(id).map { it.toDomain() },
        )
        dao.deleteTransactionsOfAccount(id)
        dao.deleteRulesOfAccount(id)
        dao.deleteAccount(id)
        snapshot
    }

    suspend fun restoreAccount(d: AccountDeletion) = db.withTransaction {
        dao.upsertAccount(d.account.toEntity())
        d.transactions.forEach { dao.upsertTransaction(it.toEntity()) }
        d.rules.forEach { dao.upsertRule(it.toEntity()) }
    }

    suspend fun saveCategory(c: Category) = dao.upsertCategory(c.toEntity())

    suspend fun deleteCategory(id: String) = db.withTransaction {
        dao.uncategorizeTransactions(id)
        dao.uncategorizeRules(id)
        dao.deleteBudgetsOnlyWithCategory(id)
        dao.deleteCategory(id) // remove as junções por cascata
    }

    suspend fun saveTransaction(t: Transaction) = dao.upsertTransaction(t.toEntity())
    suspend fun deleteTransaction(id: String) = dao.deleteTransaction(id)

    suspend fun saveRule(r: PlannedRule) = dao.upsertRule(r.toEntity())
    suspend fun deleteRule(id: String) = dao.deleteRule(id)

    /** Pagar: cria a transação e avança a regra numa única transação de banco. */
    suspend fun payRule(ruleId: String, nowMillis: Long): PlannedAction? = db.withTransaction {
        val r = dao.rule(ruleId)?.toDomain() ?: return@withTransaction null
        if (Recurrence.nextDue(r) == null) return@withTransaction null
        val t = Transaction(
            id = newId(), type = r.type, amountCents = r.amountCents, accountId = r.accountId, toAccountId = null,
            categoryId = r.categoryId, title = r.title, note = "", dateTimeMillis = nowMillis, plannedRuleId = r.id,
        )
        dao.upsertTransaction(t.toEntity())
        dao.setConsumed(r.id, r.consumedOccurrences + 1)
        PlannedAction(r.id, r.consumedOccurrences + 1, t.id)
    }

    suspend fun skipRule(ruleId: String): PlannedAction? = db.withTransaction {
        val r = dao.rule(ruleId)?.toDomain() ?: return@withTransaction null
        if (Recurrence.nextDue(r) == null) return@withTransaction null
        dao.setConsumed(r.id, r.consumedOccurrences + 1)
        PlannedAction(r.id, r.consumedOccurrences + 1, null)
    }

    /** Desfaz só se a regra não avançou de novo depois da ação. */
    suspend fun undoPlanned(a: PlannedAction): Boolean = db.withTransaction {
        val r = dao.rule(a.ruleId) ?: return@withTransaction false
        if (r.consumedOccurrences != a.consumedAfter) return@withTransaction false
        a.createdTransactionId?.let { dao.deleteTransaction(it) }
        dao.setConsumed(r.id, a.consumedAfter - 1)
        true
    }

    suspend fun saveBudget(b: Budget) = db.withTransaction {
        dao.upsertBudget(b.toEntity())
        dao.clearBudgetCategories(b.id)
        if (!b.allExpenses) dao.insertBudgetCategories(b.categoryIds.map { BudgetCategoryEntity(b.id, it) })
    }

    suspend fun deleteBudget(id: String) = dao.deleteBudget(id)
}
