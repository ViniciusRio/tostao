package com.viniciusrio.tostao.domain

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

    // Relatório por conta: BTG R$ 300,00 e Pix R$ 200,00 de despesas em novembro.
    private val byAccount = FinanceData(
        accounts = listOf(account("BTG"), account("Pix"), account("Investimentos", include = false)),
        categories = listOf(category("Mercado"), category("Lazer"), category("Salário", CategoryType.INCOME)),
        transactions = listOf(
            tx(TxType.EXPENSE, 30000, "BTG", millis(2026, 11, 3), category = "Lazer"),
            tx(TxType.EXPENSE, 15000, "Pix", millis(2026, 11, 4), category = "Mercado"),
            tx(TxType.EXPENSE, 5000, "Pix", millis(2026, 11, 5)),
            tx(TxType.INCOME, 100000, "BTG", millis(2026, 11, 5), category = "Salário"),
            tx(TxType.TRANSFER, 10000, "BTG", millis(2026, 11, 6), to = "Pix"),
            tx(TxType.INCOME, 2000, "Pix", millis(2026, 11, 7), adjustment = true),
            tx(TxType.EXPENSE, 2000, "Pix", millis(2026, 11, 8), adjustment = true),
            tx(TxType.EXPENSE, 7000, "Investimentos", millis(2026, 11, 6), category = "Mercado"),
            tx(TxType.EXPENSE, 4000, "Pix", millis(2026, 10, 10), category = "Mercado"),
            tx(TxType.EXPENSE, 90000, "BTG", millis(2026, 10, 10), category = "Lazer"),
            // Futura: não entra.
            tx(TxType.EXPENSE, 8000, "Pix", millis(2026, 11, 25)),
        ),
    )

    @Test fun reportByAccountKeepsOnlyThatAccount() {
        val pix = Reports.month(byAccount, nov, ZONE, NOW, "Pix")
        assertEquals(20000L, pix.expenseCents)
        assertEquals(0L, pix.incomeCents)
        assertEquals(2, pix.transactionCount)
        assertEquals(listOf("Mercado", null), pix.expenseByCategory.map { it.category?.name })
        assertEquals(4000L, pix.previousExpenseCents)
        assertEquals(emptyList<CategorySlice>(), pix.incomeByCategory)

        val btg = Reports.month(byAccount, nov, ZONE, NOW, "BTG")
        assertEquals(30000L, btg.expenseCents)
        assertEquals(100000L, btg.incomeCents)
        assertEquals(90000L, btg.previousExpenseCents)
        assertEquals(listOf("Lazer"), btg.expenseByCategory.map { it.category?.name })
    }

    @Test fun allAccountsIncludesAccountOutOfTotal() {
        val all = Reports.month(byAccount, nov, ZONE, NOW)
        assertEquals(57000L, all.expenseCents)
        val inv = Reports.month(byAccount, nov, ZONE, NOW, "Investimentos")
        assertEquals(7000L, inv.expenseCents)
        assertEquals(listOf("Mercado"), inv.expenseByCategory.map { it.category?.name })
        // A soma das contas é o total de todas.
        assertEquals(all.expenseCents, listOf("BTG", "Pix", "Investimentos").sumOf { Reports.month(byAccount, nov, ZONE, NOW, it).expenseCents })
        assertEquals(all.incomeCents, listOf("BTG", "Pix", "Investimentos").sumOf { Reports.month(byAccount, nov, ZONE, NOW, it).incomeCents })
    }

    @Test fun accountWithoutMovementIsZero() {
        val r = Reports.month(byAccount, YearMonth.of(2026, 9), ZONE, NOW, "Pix")
        assertEquals(0L, r.expenseCents)
        assertEquals(0L, r.incomeCents)
        assertEquals(0, r.transactionCount)
        assertEquals(emptyList<CategorySlice>(), r.expenseByCategory)
    }
}
