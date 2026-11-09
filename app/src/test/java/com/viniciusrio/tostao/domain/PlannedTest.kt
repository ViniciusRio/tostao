package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlannedTest {
    private val today = LocalDate.of(2026, 11, 10)

    @Test fun classifiesOverdueAndUpcoming() {
        val overdue = rule(LocalDate.of(2026, 10, 17), RecurrenceUnit.MONTH)
        val dueToday = rule(today, null)
        val far = rule(LocalDate.of(2027, 3, 1), null)
        val o = Planned.overview(listOf(overdue, dueToday, far), today)
        assertEquals(listOf(overdue.id), o.overdue.map { it.rule.id })
        assertEquals(listOf(dueToday.id), o.upcoming.map { it.rule.id })
        assertEquals("vence hoje", Planned.relativeDue(o.upcoming[0].daysFromToday))
        assertEquals("atrasado há 24 dias", Planned.relativeDue(o.overdue[0].daysFromToday))
    }

    @Test fun forecastCountsEveryPendingOccurrenceIncludingOverdue() {
        val data = FinanceData(
            accounts = listOf(account("a", 100000)),
            rules = listOf(rule(LocalDate.of(2026, 11, 3), RecurrenceUnit.WEEK, cents = 5000)),
        )
        val now = millis(2026, 11, 10)
        val end = Periods.monthRange(java.time.YearMonth.of(2026, 11), ZONE).last
        assertEquals(80000L, Planned.forecastEndOfMonth(data, today, now, end))
    }

    @Test fun forecastIgnoresExcludedAccountsAndAddsFutureTransactions() {
        val data = FinanceData(
            accounts = listOf(account("a", 200000), account("x", 0, include = false)),
            transactions = listOf(tx(TxType.EXPENSE, 10000, "a", millis(2026, 11, 20))),
            rules = listOf(
                rule(LocalDate.of(2026, 11, 25), null, cents = 50000),
                rule(LocalDate.of(2026, 11, 25), null, cents = 99999, account = "x"),
            ),
        )
        val end = Periods.monthRange(java.time.YearMonth.of(2026, 11), ZONE).last
        assertEquals(140000L, Planned.forecastEndOfMonth(data, today, millis(2026, 11, 10), end))
    }
}
