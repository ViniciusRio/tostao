package com.viniciusrio.tostao.domain

import java.time.YearMonth
import java.time.ZoneId

data class CategorySlice(val category: Category?, val totalCents: Long, val percent: Int)

/**
 * Item da visão "Por grupo": um grupo ([key] = [CategoryScope.Group]), uma categoria sem grupo
 * ([CategoryScope.Category]) ou "Sem categoria" ([CategoryScope.None]). [children] é o
 * detalhamento do grupo por categoria, com o percentual dentro do grupo; vazio nos demais.
 */
data class GroupSlice(
    val key: CategoryScope,
    val label: String,
    val colorArgb: Long,
    val icon: String,
    val totalCents: Long,
    val percent: Int,
    val children: List<CategorySlice>,
)

data class MonthReport(
    val incomeCents: Long,
    val expenseCents: Long,
    val transactionCount: Int,
    val expenseByCategory: List<CategorySlice>,
    val incomeByCategory: List<CategorySlice>,
    val previousExpenseCents: Long,
    val expenseByGroup: List<GroupSlice> = emptyList(),
    val incomeByGroup: List<GroupSlice> = emptyList(),
) {
    val resultCents: Long get() = incomeCents - expenseCents
    val expenseDeltaCents: Long get() = expenseCents - previousExpenseCents
    /** Variação percentual vs mês anterior; null quando o anterior é zero. */
    val expenseDeltaPercent: Int?
        get() = if (previousExpenseCents == 0L) null else (expenseDeltaCents * 100 / previousExpenseCents).toInt()

    fun deltaText(): String {
        if (previousExpenseCents == 0L) return "sem despesas no mês anterior para comparar"
        val pct = expenseDeltaPercent?.let { " (${if (it > 0) "+" else ""}$it%)" } ?: ""
        return when {
            expenseDeltaCents == 0L -> "iguais às do mês anterior"
            else -> "${Money.format(expenseDeltaCents, signed = true)}$pct em relação ao mês anterior"
        }
    }
}

object Reports {
    /**
     * Apenas transações efetivadas (até [nowMillis]) entram. [accountId] null = todas as contas
     * (inclusive as fora do saldo total); com a conta, só as receitas e despesas lançadas nela,
     * no mês e no mês anterior. Transferências e ajustes de saldo nunca entram.
     */
    fun month(data: FinanceData, month: YearMonth, zone: ZoneId, nowMillis: Long, accountId: String? = null): MonthReport {
        val inAccount = { t: Transaction -> accountId == null || t.accountId == accountId }
        val range = Periods.monthRange(month, zone)
        val txs = data.transactions.filter {
            it.type != TxType.TRANSFER && !it.isAdjustment && it.dateTimeMillis in range && it.dateTimeMillis <= nowMillis && inAccount(it)
        }
        val prevRange = Periods.monthRange(month.minusMonths(1), zone)
        val prevExpense = data.transactions
            .filter { it.type == TxType.EXPENSE && !it.isAdjustment && it.dateTimeMillis in prevRange && it.dateTimeMillis <= nowMillis && inAccount(it) }
            .sumOf { it.amountCents }
        val income = txs.filter { it.type == TxType.INCOME }
        val expense = txs.filter { it.type == TxType.EXPENSE }
        return MonthReport(
            incomeCents = income.sumOf { it.amountCents },
            expenseCents = expense.sumOf { it.amountCents },
            transactionCount = txs.size,
            expenseByCategory = TransactionQueries.categorySlices(expense, data),
            incomeByCategory = TransactionQueries.categorySlices(income, data),
            previousExpenseCents = prevExpense,
            expenseByGroup = groupSlices(expense, data, CategoryType.EXPENSE),
            incomeByGroup = groupSlices(income, data, CategoryType.INCOME),
        )
    }

    const val UNCATEGORIZED_COLOR = 0xFF7D7D85
    const val UNCATEGORIZED_ICON = "dots"

    /** Chave do item da visão por grupo para uma transação do relatório do [type]. */
    fun groupKey(t: Transaction, data: FinanceData, type: CategoryType): CategoryScope {
        val cat = t.categoryId?.let(data.categoryById::get) ?: return CategoryScope.None
        val group = data.groupOf(cat)?.takeIf { it.type == type }
        return if (group != null) CategoryScope.Group(group.id) else CategoryScope.Category(cat.id)
    }

    /**
     * Visão "Por grupo": cada grupo com movimento vira um item com a soma das suas categorias;
     * categorias sem grupo e "Sem categoria" continuam como itens próprios. Uma transação cujo tipo
     * difere do tipo do grupo da sua categoria entra como item da categoria. Ordem decrescente.
     */
    fun groupSlices(txs: List<Transaction>, data: FinanceData, type: CategoryType): List<GroupSlice> {
        val total = txs.sumOf { it.amountCents }
        if (total == 0L) return emptyList()
        val byKey = txs.groupBy { groupKey(it, data, type) }
            .map { (key, list) -> Triple(key, list, list.sumOf { it.amountCents }) }
            .sortedByDescending { it.third }
        val pct = Charts.percents(byKey.map { it.third })
        return byKey.mapIndexed { i, (key, list, sum) ->
            when (key) {
                is CategoryScope.Group -> data.groupById.getValue(key.id).let { g ->
                    GroupSlice(key, g.name, g.colorArgb, g.icon, sum, pct[i], TransactionQueries.categorySlices(list, data))
                }
                is CategoryScope.Category -> data.categoryById.getValue(key.id).let { c ->
                    GroupSlice(key, c.name, c.colorArgb, c.icon, sum, pct[i], emptyList())
                }
                else -> GroupSlice(CategoryScope.None, "Sem categoria", UNCATEGORIZED_COLOR, UNCATEGORIZED_ICON, sum, pct[i], emptyList())
            }
        }
    }
}
