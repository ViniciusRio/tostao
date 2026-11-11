package com.viniciusrio.tostao.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class DueItem(val rule: PlannedRule, val dueDate: LocalDate, val daysFromToday: Long)

data class PlannedOverview(
    val overdue: List<DueItem>,
    val upcoming: List<DueItem>,
) {
    fun total(items: List<DueItem>, type: TxType) =
        items.filter { it.rule.type == type }.sumOf { it.rule.amountCents }
}

object Planned {
    /** Folga para mostrar, perto da virada, o que vence no começo do mês seguinte. */
    const val UPCOMING_DAYS = 7L

    /**
     * Atrasados e próximos. "Próximos" são as ocorrências que vencem até o fim do mês de [today]
     * ou nos próximos [UPCOMING_DAYS] dias. Assim, ao pagar a conta mensal deste mês, a
     * ocorrência do mês seguinte só volta a aparecer quando estiver perto.
     */
    fun overview(rules: List<PlannedRule>, today: LocalDate): PlannedOverview {
        val items = rules.mapNotNull { r ->
            Recurrence.nextDue(r)?.let { DueItem(r, it, ChronoUnit.DAYS.between(today, it)) }
        }.sortedBy { it.dueDate }
        return PlannedOverview(
            overdue = items.filter { it.daysFromToday < 0 },
            upcoming = items.filter {
                it.daysFromToday >= 0 && (it.daysFromToday < UPCOMING_DAYS || YearMonth.from(it.dueDate) == YearMonth.from(today))
            },
        )
    }

    /**
     * Saldo previsto ao fim do mês de [today]: total atual das contas incluídas + transações
     * com data futura no mês + ocorrências planejadas pendentes até o último dia do mês.
     */
    fun forecastEndOfMonth(data: FinanceData, today: LocalDate, nowMillis: Long, endOfMonthMillis: Long): Long {
        val included = data.accounts.filter { it.includeInTotal }.map { it.id }.toSet()
        var total = Balances.compute(data, nowMillis).includedTotalCents
        for (t in data.transactions) {
            if (t.dateTimeMillis <= nowMillis || t.dateTimeMillis > endOfMonthMillis) continue
            Balances.applyEffect(t) { id, v -> if (id in included) total += v }
        }
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        for (r in data.rules) {
            if (r.accountId !in included) continue
            val n = Recurrence.pendingUntil(r, endOfMonth).size
            val sign = if (r.type == TxType.INCOME) 1 else -1
            total += sign * r.amountCents * n
        }
        return total
    }

    fun relativeDue(days: Long): String = when {
        days == 0L -> "vence hoje"
        days == 1L -> "vence amanhã"
        days > 1 -> "vence em $days dias"
        days == -1L -> "venceu ontem"
        else -> "atrasado há ${-days} dias"
    }
}
