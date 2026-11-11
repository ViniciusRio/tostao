package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class CategoryTransactionsTest {
    private val nov = YearMonth.of(2026, 11)
    private val oct = YearMonth.of(2026, 10)
    private val now = millis(2026, 11, 10, 12)

    private val base = FinanceData(
        accounts = listOf(account("a"), account("b")),
        categories = listOf(category("Mercado"), category("Lazer"), category("Salário", CategoryType.INCOME)),
        transactions = listOf(
            tx(TxType.EXPENSE, 20000, "a", millis(2026, 11, 3), category = "Mercado"),
            tx(TxType.EXPENSE, 30000, "b", millis(2026, 11, 5), category = "Mercado"),
            tx(TxType.EXPENSE, 10000, "a", millis(2026, 11, 9), category = "Mercado"),
            // Futuras: mais tarde hoje e em dia seguinte.
            tx(TxType.EXPENSE, 5000, "a", millis(2026, 11, 10, 18), category = "Mercado"),
            tx(TxType.EXPENSE, 7000, "a", millis(2026, 11, 20), category = "Mercado"),
            // Ajuste e transferência nunca entram.
            tx(TxType.EXPENSE, 900, "a", millis(2026, 11, 4), category = "Mercado", adjustment = true),
            tx(TxType.TRANSFER, 800, "a", millis(2026, 11, 4), to = "b", category = "Mercado"),
            // Tipo oposto na mesma categoria; receita numa categoria de despesa.
            tx(TxType.INCOME, 1500, "a", millis(2026, 11, 6), category = "Mercado"),
            tx(TxType.EXPENSE, 4000, "a", millis(2026, 11, 7), category = "Lazer"),
            tx(TxType.INCOME, 500000, "a", millis(2026, 11, 5), category = "Salário"),
            // Sem categoria: nula e órfã.
            tx(TxType.EXPENSE, 1200, "a", millis(2026, 11, 2)),
            tx(TxType.EXPENSE, 3400, "b", millis(2026, 11, 8), category = "apagada"),
            // Outro mês.
            tx(TxType.EXPENSE, 99900, "a", millis(2026, 10, 15), category = "Mercado"),
            tx(TxType.EXPENSE, 2200, "a", millis(2026, 10, 16), category = "apagada"),
        ),
    )

    private fun ids(list: List<Transaction>) = list.map { it.id }

    /** Toda linha do relatório (os dois tipos) bate com a soma de byCategory, e nada fica de fora. */
    private fun assertMatchesReport(data: FinanceData, month: YearMonth, nowMillis: Long = now, accountId: String? = null) {
        val r = Reports.month(data, month, ZONE, nowMillis, accountId)
        for ((type, slices) in listOf(CategoryType.EXPENSE to r.expenseByCategory, CategoryType.INCOME to r.incomeByCategory)) {
            for (s in slices) {
                val list = TransactionQueries.byCategory(data, s.category?.id, type, month, ZONE, nowMillis, accountId)
                assertEquals("$type ${s.category?.id}", s.totalCents, list.sumOf { it.amountCents })
            }
            // Categorias sem linha no relatório somam zero.
            val withLine = slices.map { it.category?.id }.toSet()
            for (id in data.categories.map { it.id } + listOf<String?>(null)) {
                if (id in withLine) continue
                assertEquals(0L, TransactionQueries.byCategory(data, id, type, month, ZONE, nowMillis, accountId).sumOf { it.amountCents })
            }
        }
    }

    @Test fun matchesReportInCurrentAndOtherMonth() {
        assertMatchesReport(base, nov)
        assertMatchesReport(base, oct)
    }

    @Test fun mercadoListsOnlyEffectiveExpensesNewestFirst() {
        val list = TransactionQueries.byCategory(base, "Mercado", CategoryType.EXPENSE, nov, ZONE, now)
        assertEquals(listOf(10000L, 30000L, 20000L), list.map { it.amountCents })
        assertEquals(60000L, list.sumOf { it.amountCents })
    }

    @Test fun futureLaterTodayCountsOnceItsTimeArrives() {
        val later = millis(2026, 11, 10, 19)
        val list = TransactionQueries.byCategory(base, "Mercado", CategoryType.EXPENSE, nov, ZONE, later)
        assertEquals(65000L, list.sumOf { it.amountCents })
        assertMatchesReport(base, nov, later)
    }

    @Test fun noCategoryIncludesOrphans() {
        val list = TransactionQueries.byCategory(base, null, CategoryType.EXPENSE, nov, ZONE, now)
        assertEquals(listOf(3400L, 1200L), list.map { it.amountCents })
    }

    @Test fun oppositeTypeAndIncomeInExpenseCategory() {
        val income = TransactionQueries.byCategory(base, "Mercado", CategoryType.INCOME, nov, ZONE, now)
        assertEquals(listOf(1500L), income.map { it.amountCents })
        val r = Reports.month(base, nov, ZONE, now)
        assertEquals(1500L, r.incomeByCategory.single { it.category?.id == "Mercado" }.totalCents)
    }

    @Test fun deletedCategoryIdIsEmptyNotNoCategory() {
        assertTrue(TransactionQueries.byCategory(base, "apagada", CategoryType.EXPENSE, nov, ZONE, now).isEmpty())
    }

    @Test fun monthBoundaryFollowsZone() {
        // 31/10 23:30 em São Paulo é 1/11 em UTC: fica em outubro.
        val lateOct = LocalDateTime.of(2026, 10, 31, 23, 30).atZone(ZONE).toInstant().toEpochMilli()
        val earlyNov = LocalDateTime.of(2026, 11, 1, 0, 0).atZone(ZONE).toInstant().toEpochMilli()
        val data = FinanceData(
            categories = listOf(category("Mercado")),
            transactions = listOf(
                tx(TxType.EXPENSE, 100, "a", lateOct, category = "Mercado"),
                tx(TxType.EXPENSE, 200, "a", earlyNov, category = "Mercado"),
            ),
        )
        assertEquals(listOf(100L), TransactionQueries.byCategory(data, "Mercado", CategoryType.EXPENSE, oct, ZONE, now).map { it.amountCents })
        assertEquals(listOf(200L), TransactionQueries.byCategory(data, "Mercado", CategoryType.EXPENSE, nov, ZONE, now).map { it.amountCents })
        assertMatchesReport(data, oct)
        assertMatchesReport(data, nov)
    }

    @Test fun accountFilterKeepsOnlyThatAccount() {
        val onlyB = TransactionQueries.byCategory(base, "Mercado", CategoryType.EXPENSE, nov, ZONE, now, accountId = "b")
        assertEquals(listOf(30000L), onlyB.map { it.amountCents })
        val all = TransactionQueries.byCategory(base, "Mercado", CategoryType.EXPENSE, nov, ZONE, now)
        assertEquals(ids(all.filter { it.accountId == "a" }), ids(TransactionQueries.byCategory(base, "Mercado", CategoryType.EXPENSE, nov, ZONE, now, accountId = "a")))
    }

    @Test fun accountTotalsMatchReportFilteredBySameAccount() {
        for (acc in listOf(null, "a", "b", "inexistente")) {
            assertMatchesReport(base, nov, accountId = acc)
            assertMatchesReport(base, oct, accountId = acc)
        }
    }
}
