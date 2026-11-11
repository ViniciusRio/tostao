package com.viniciusrio.tostao.domain

import java.time.YearMonth
import java.time.ZoneId

data class CategorySlice(val category: Category?, val totalCents: Long, val percent: Int)

data class MonthReport(
    val incomeCents: Long,
    val expenseCents: Long,
    val transactionCount: Int,
    val expenseByCategory: List<CategorySlice>,
    val incomeByCategory: List<CategorySlice>,
    val previousExpenseCents: Long,
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
            expenseByCategory = slices(expense, data),
            incomeByCategory = slices(income, data),
            previousExpenseCents = prevExpense,
        )
    }

    private fun slices(txs: List<Transaction>, data: FinanceData): List<CategorySlice> {
        val total = txs.sumOf { it.amountCents }
        if (total == 0L) return emptyList()
        val sums = txs.groupBy { it.categoryId?.let(data.categoryById::get) }
            .map { (cat, list) -> cat to list.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
        val pct = Charts.percents(sums.map { it.second })
        return sums.mapIndexed { i, (cat, sum) -> CategorySlice(cat, sum, pct[i]) }
    }
}
