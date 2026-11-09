package com.viniciusrio.finance.domain

import java.time.YearMonth
import java.time.ZoneId

enum class BudgetStatus { NORMAL, WARNING, EXCEEDED }

data class BudgetProgress(
    val budget: Budget,
    val spentCents: Long,
) {
    val remainingCents: Long get() = budget.limitCents - spentCents
    val ratio: Float get() = if (budget.limitCents == 0L) 0f else spentCents.toFloat() / budget.limitCents
    val percent: Int get() = (spentCents * 100 / budget.limitCents.coerceAtLeast(1)).toInt()
    val status: BudgetStatus
        get() = when {
            spentCents > budget.limitCents -> BudgetStatus.EXCEEDED
            spentCents * 10 >= budget.limitCents * 8 -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }
    val statusText: String
        get() = when (status) {
            BudgetStatus.EXCEEDED -> "Excedido em ${Money.format(-remainingCents)}"
            BudgetStatus.WARNING -> "Atenção: restam ${Money.format(remainingCents)}"
            BudgetStatus.NORMAL -> "Restam ${Money.format(remainingCents)}"
        }
}

object Budgets {
    fun progress(data: FinanceData, month: YearMonth, zone: ZoneId, nowMillis: Long): List<BudgetProgress> {
        val range = Periods.monthRange(month, zone)
        val expenses = data.transactions.filter { it.type == TxType.EXPENSE && !it.isAdjustment && it.dateTimeMillis in range && it.dateTimeMillis <= nowMillis }
        return data.budgets.sortedBy { it.order }.map { b ->
            val spent = expenses
                .filter { b.allExpenses || it.categoryId in b.categoryIds }
                .sumOf { it.amountCents }
            BudgetProgress(b, spent)
        }
    }
}
