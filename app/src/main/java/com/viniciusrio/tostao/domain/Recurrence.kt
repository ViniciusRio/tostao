package com.viniciusrio.tostao.domain

import java.time.LocalDate

object Recurrence {
    /**
     * Data da ocorrência de índice [index] (0 = primeira). Meses e anos são somados a partir
     * da data inicial, então o dia âncora é preservado: 31/jan -> 28/fev -> 31/mar.
     */
    fun occurrence(rule: PlannedRule, index: Int): LocalDate {
        val n = rule.interval.coerceAtLeast(1).toLong() * index
        return when (rule.unit) {
            null -> rule.startDate
            RecurrenceUnit.DAY -> rule.startDate.plusDays(n)
            RecurrenceUnit.WEEK -> rule.startDate.plusWeeks(n)
            RecurrenceUnit.MONTH -> rule.startDate.plusMonths(n)
            RecurrenceUnit.YEAR -> rule.startDate.plusYears(n)
        }
    }

    /** Próxima ocorrência pendente, ou null se a regra terminou. */
    fun nextDue(rule: PlannedRule): LocalDate? = dueAt(rule, rule.consumedOccurrences)

    fun dueAt(rule: PlannedRule, index: Int): LocalDate? {
        if (rule.unit == null) return if (index == 0) rule.startDate else null
        val d = occurrence(rule, index)
        return if (rule.endDate != null && d.isAfter(rule.endDate)) null else d
    }

    /**
     * Ocorrências pendentes com data <= [until]. O teto só protege contra dados corrompidos:
     * 200 mil ocorrências diárias equivalem a mais de 500 anos.
     */
    fun pendingUntil(rule: PlannedRule, until: LocalDate, limit: Int = 200_000): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var i = rule.consumedOccurrences
        while (out.size < limit) {
            val d = dueAt(rule, i) ?: break
            if (d.isAfter(until)) break
            out += d
            i++
        }
        return out
    }
}
