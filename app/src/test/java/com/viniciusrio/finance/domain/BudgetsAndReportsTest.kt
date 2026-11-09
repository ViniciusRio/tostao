package com.viniciusrio.finance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class BudgetsAndReportsTest {
    private val nov = YearMonth.of(2026, 11)
    private val NOW = millis(2026, 11, 10)

    @Test fun futureExpensesDoNotCountYet() {
        val data = FinanceData(
            budgets = listOf(Budget("b", "Tudo", 10000, true, emptySet(), 0)),
            transactions = listOf(tx(TxType.EXPENSE, 2000, "a", millis(2026, 11, 25))),
        )
        assertEquals(0L, Budgets.progress(data, nov, ZONE, NOW).single().spentCents)
        assertEquals(0L, Reports.month(data, nov, ZONE, NOW).expenseCents)
    }

    @Test fun budgetExceededText() {
        val data = FinanceData(
            categories = listOf(category("Mercado")),
            budgets = listOf(Budget("b", "Mercado", 80000, false, setOf("Mercado"), 0)),
            transactions = listOf(
                tx(TxType.EXPENSE, 85000, "a", millis(2026, 11, 5), category = "Mercado"),
                tx(TxType.EXPENSE, 99999, "a", millis(2026, 10, 5), category = "Mercado"),
            ),
        )
        val p = Budgets.progress(data, nov, ZONE, NOW).single()
        assertEquals(BudgetStatus.EXCEEDED, p.status)
        assertEquals("Excedido em R$ 50,00", p.statusText)
    }

    @Test fun warningFromEightyPercent() {
        val b = Budget("b", "x", 10000, true, emptySet(), 0)
        assertEquals(BudgetStatus.NORMAL, BudgetProgress(b, 7999).status)
        assertEquals(BudgetStatus.WARNING, BudgetProgress(b, 8000).status)
        assertEquals(BudgetStatus.WARNING, BudgetProgress(b, 10000).status)
    }

    @Test fun adjustmentsStayOutOfBudgetsAndReports() {
        val data = FinanceData(
            budgets = listOf(Budget("b", "Tudo", 10000, true, emptySet(), 0)),
            transactions = listOf(tx(TxType.EXPENSE, 2000, "a", millis(2026, 11, 5), adjustment = true)),
        )
        assertEquals(0L, Budgets.progress(data, nov, ZONE, NOW).single().spentCents)
        assertEquals(0L, Reports.month(data, nov, ZONE, NOW).expenseCents)
    }

    @Test fun reportRanksCategoriesAndComparesMonths() {
        val data = FinanceData(
            categories = listOf(category("Mercado"), category("Lazer")),
            transactions = listOf(
                tx(TxType.EXPENSE, 60000, "a", millis(2026, 11, 3), category = "Mercado"),
                tx(TxType.EXPENSE, 40000, "a", millis(2026, 11, 4), category = "Lazer"),
                tx(TxType.EXPENSE, 20000, "a", millis(2026, 11, 4)),
                tx(TxType.TRANSFER, 70000, "a", millis(2026, 11, 4), to = "b"),
                tx(TxType.EXPENSE, 100000, "a", millis(2026, 10, 10)),
            ),
        )
        val r = Reports.month(data, nov, ZONE, NOW)
        assertEquals(120000L, r.expenseCents)
        assertEquals(listOf("Mercado", "Lazer", null), r.expenseByCategory.map { it.category?.name })
        assertEquals(50, r.expenseByCategory[0].percent)
        assertEquals("+R$ 200,00 (+20%) em relação ao mês anterior", r.deltaText())
    }

    @Test fun noPreviousMonthHasNoPercent() {
        val data = FinanceData(transactions = listOf(tx(TxType.EXPENSE, 100, "a", millis(2026, 11, 3))))
        assertNull(Reports.month(data, nov, ZONE, NOW).expenseDeltaPercent)
    }
}
