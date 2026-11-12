package com.viniciusrio.tostao.data

import androidx.room.withTransaction
import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategoryGroup
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.Recurrence
import com.viniciusrio.tostao.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

/** Snapshot de tudo que uma exclusão de conta remove, para confirmação e desfazer. */
data class AccountDeletion(val account: Account, val transactions: List<Transaction>, val rules: List<PlannedRule>)

/** Resultado de pagar/pular: o que foi gravado, para permitir desfazer só esta ação. */
data class PlannedAction(val ruleId: String, val consumedAfter: Int, val createdTransactionId: String?)

class FinanceRepository(private val db: FinanceDatabase) {
    private val dao = db.dao()

    /** Gancho de teste: roda no meio de [replaceAll], depois de apagar e antes de inserir. */
    internal var afterClearForTest: (suspend () -> Unit)? = null

    val data: Flow<FinanceData> = combine(
        dao.accounts(), dao.categories(), dao.transactions(), dao.rules(),
        combine(dao.budgets(), dao.budgetCategories(), dao.groups()) { b, bc, g -> Triple(b, bc, g) },
    ) { a, c, t, r, (b, bc, g) ->
        val byBudget = bc.groupBy({ it.budgetId }, { it.categoryId })
        FinanceData(
            accounts = a.map { it.toDomain() },
            categories = c.map { it.toDomain() },
            transactions = t.map { it.toDomain() },
            rules = r.map { it.toDomain() },
            budgets = b.map { Budget(it.id, it.name, it.limitCents, it.allExpenses, byBudget[it.id].orEmpty().toSet(), it.sortOrder) },
            groups = g.map { it.toDomain() },
        )
    }

    /**
     * Cria as categorias padrão só na primeira instalação ([alreadySeeded] falso e tabela vazia).
     * Depois disso nunca mais, mesmo que o usuário apague todas ou restaure um backup sem categorias.
     */
    suspend fun seedOnFirstRun(alreadySeeded: Boolean) {
        if (!alreadySeeded && dao.categoryCount() == 0) dao.insertCategories(DefaultCategories.all())
    }

    /** Todas as tabelas lidas numa única transação: um retrato consistente para o backup. */
    suspend fun snapshot(): FinanceData = db.withTransaction {
        val byBudget = dao.allBudgetCategories().groupBy({ it.budgetId }, { it.categoryId })
        FinanceData(
            accounts = dao.allAccounts().map { it.toDomain() },
            categories = dao.allCategories().map { it.toDomain() },
            transactions = dao.allTransactions().map { it.toDomain() },
            rules = dao.allRules().map { it.toDomain() },
            budgets = dao.allBudgets().map {
                Budget(it.id, it.name, it.limitCents, it.allExpenses, byBudget[it.id].orEmpty().toSet(), it.sortOrder)
            },
            groups = dao.allGroups().map { it.toDomain() },
        )
    }

    /** Troca todos os dados por [d] numa única transação: ou tudo muda, ou nada muda. */
    suspend fun replaceAll(d: FinanceData) = db.withTransaction {
        dao.clearAllBudgetCategories()
        dao.clearBudgets()
        dao.clearTransactions()
        dao.clearRules()
        dao.clearCategories()
        dao.clearAccounts()
        dao.clearGroups()
        afterClearForTest?.invoke()
        dao.insertAccounts(d.accounts.map { it.toEntity() })
        dao.insertGroups(d.groups.map { it.toEntity() })
        dao.insertCategoriesStrict(d.categories.map { it.toEntity() })
        dao.insertTransactions(d.transactions.map { it.toEntity() })
        dao.insertRules(d.rules.map { it.toEntity() })
        dao.insertBudgets(d.budgets.map { it.toEntity() })
        dao.insertBudgetCategoriesStrict(d.budgets.flatMap { b -> b.categoryIds.map { BudgetCategoryEntity(b.id, it) } })
    }

    fun newId(): String = UUID.randomUUID().toString()

    suspend fun nextAccountOrder() = dao.maxAccountOrder() + 1
    suspend fun nextBudgetOrder() = dao.maxBudgetOrder() + 1
    suspend fun nextCategoryOrder() = dao.maxCategoryOrder() + 1
    suspend fun nextGroupOrder() = dao.maxGroupOrder() + 1

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

    /**
     * Grava a categoria. Um grupo de outro tipo (ex.: a categoria trocou de tipo) ou inexistente
     * é descartado: a categoria fica "Sem grupo".
     */
    suspend fun saveCategory(c: Category) = db.withTransaction {
        val group = c.groupId?.let { dao.group(it) }
        val keep = group != null && group.type == c.type.name
        dao.upsertCategory(c.copy(groupId = if (keep) c.groupId else null).toEntity())
    }

    /**
     * Grava o grupo e suas categorias numa transação: quem saiu fica sem grupo e quem entrou
     * passa para este grupo, saindo do anterior. Só categorias do mesmo tipo entram, e uma de outro
     * tipo que estivesse no grupo sai dele.
     */
    suspend fun saveGroup(g: CategoryGroup, memberIds: Set<String>) = db.withTransaction {
        dao.upsertGroup(g.toEntity())
        dao.ungroupExcept(g.id, memberIds.toList(), g.type.name)
        if (memberIds.isNotEmpty()) dao.setGroup(g.id, memberIds.toList(), g.type.name)
    }

    /** Exclui o grupo; as categorias continuam, sem grupo. */
    suspend fun deleteGroup(id: String) = db.withTransaction {
        dao.ungroupAll(id)
        dao.deleteGroup(id)
    }

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
    /**
     * [expectedConsumed] é o estado que a tela mostrava: se a regra já avançou (toque duplo),
     * nada é feito, evitando pagar duas ocorrências sem querer.
     */
    suspend fun payRule(ruleId: String, nowMillis: Long, expectedConsumed: Int? = null): PlannedAction? = db.withTransaction {
        val r = dao.rule(ruleId)?.toDomain() ?: return@withTransaction null
        if (expectedConsumed != null && r.consumedOccurrences != expectedConsumed) return@withTransaction null
        if (Recurrence.nextDue(r) == null) return@withTransaction null
        val t = Transaction(
            id = newId(), type = r.type, amountCents = r.amountCents, accountId = r.accountId, toAccountId = null,
            categoryId = r.categoryId, title = r.title, note = "", dateTimeMillis = nowMillis, plannedRuleId = r.id,
        )
        dao.upsertTransaction(t.toEntity())
        dao.setConsumed(r.id, r.consumedOccurrences + 1)
        PlannedAction(r.id, r.consumedOccurrences + 1, t.id)
    }

    suspend fun skipRule(ruleId: String, expectedConsumed: Int? = null): PlannedAction? = db.withTransaction {
        val r = dao.rule(ruleId)?.toDomain() ?: return@withTransaction null
        if (expectedConsumed != null && r.consumedOccurrences != expectedConsumed) return@withTransaction null
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
