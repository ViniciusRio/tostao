package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TransactionQueriesTest {
    private val today = LocalDate.of(2026, 11, 10)

    @Test fun searchIgnoresAccentsAndCase() {
        val data = FinanceData(transactions = listOf(tx(TxType.EXPENSE, 4590, "a", millis(2026, 1, 1), title = "Farmácia")))
        assertEquals(1, TransactionQueries.search(data, "FARMACIA").size)
        assertEquals(1, TransactionQueries.search(data, "45,90").size)
        assertEquals(0, TransactionQueries.search(data, "mercado").size)
    }

    @Test fun groupsByDayWithRelativeLabels() {
        val txs = listOf(
            tx(TxType.EXPENSE, 100, "a", millis(2026, 11, 10)),
            tx(TxType.INCOME, 500, "a", millis(2026, 11, 10, 8)),
            tx(TxType.EXPENSE, 100, "a", millis(2026, 11, 9)),
            tx(TxType.EXPENSE, 100, "a", millis(2026, 9, 17)),
        )
        val g = TransactionQueries.groupByDay(txs, ZONE, today)
        assertEquals(listOf("Hoje", "Ontem", "quinta, 17 de set."), g.map { it.label })
        assertEquals(400L, g[0].netCents)
    }

    @Test fun filterHidesFutureAndOtherMonths() {
        val data = FinanceData(
            transactions = listOf(
                tx(TxType.EXPENSE, 1, "a", millis(2026, 11, 1)),
                tx(TxType.EXPENSE, 1, "a", millis(2026, 11, 20)),
                tx(TxType.EXPENSE, 1, "a", millis(2026, 10, 31)),
                tx(TxType.TRANSFER, 1, "b", millis(2026, 11, 2), to = "a"),
            ),
        )
        val f = TransactionQueries.filtered(data, TransactionFilter(YearMonth.of(2026, 11), accountId = "a"), ZONE, millis(2026, 11, 10))
        assertEquals(2, f.size)
    }

    @Test fun monthLabel() {
        assertEquals("Novembro", Periods.monthLabel(YearMonth.of(2026, 11), today))
        assertEquals("Março de 2025", Periods.monthLabel(YearMonth.of(2025, 3), today))
    }

    @Test fun sameTimeKeepsRepositoryOrderSoLastAddedStaysOnTop() {
        // O repositório entrega as transações da mais recém-inserida para a mais antiga.
        val at = millis(2026, 11, 10)
        val older = tx(TxType.EXPENSE, 100, "a", at)
        val newer = tx(TxType.EXPENSE, 200, "a", at)
        val data = FinanceData(accounts = listOf(account("a", 0)), transactions = listOf(newer, older))
        val list = TransactionQueries.filtered(data, TransactionFilter(YearMonth.of(2026, 11)), ZONE, at)
        assertEquals(listOf(newer.id, older.id), list.map { it.id })
        assertEquals(listOf(newer.id, older.id), TransactionQueries.groupByDay(list, ZONE, LocalDate.of(2026, 11, 10))[0].items.map { it.id })
    }

    // ---- Seletor de categoria agrupado ----

    private fun cat(id: String, order: Int, group: String? = null, type: CategoryType = CategoryType.EXPENSE) =
        category(id, type).copy(order = order, groupId = group)

    private fun grp(id: String, order: Int, type: CategoryType = CategoryType.EXPENSE) =
        CategoryGroup(id, id, type, 0, "tag", order)

    private fun sections(data: FinanceData, recent: List<String>, type: CategoryType = CategoryType.EXPENSE) =
        TransactionQueries.categorySections(data, type, recent).map { (it.group?.id ?: "-") to it.categories.map { c -> c.id } }

    @Test fun categorySectionsWithoutGroupsKeepTodaysOrder() {
        val data = FinanceData(categories = listOf(cat("a", 0), cat("b", 1), cat("c", 2), cat("in", 3, type = CategoryType.INCOME)))
        assertEquals(listOf("-" to listOf("c", "a", "b")), sections(data, listOf("c", "in")))
        // Um grupo só de receitas não muda o seletor de despesas.
        val incomeGroup = data.copy(groups = listOf(grp("renda", 0, CategoryType.INCOME)))
        assertEquals(listOf("-" to listOf("c", "a", "b")), sections(incomeGroup, listOf("c")))
    }

    @Test fun categorySectionsOrderGroupsByMostRecentUse() {
        val data = FinanceData(
            categories = listOf(
                cat("plano", 0, "saude"), cat("psi", 1, "saude"), cat("luz", 2, "contas"), cat("agua", 3, "contas"),
                cat("lazer", 4), cat("mercado", 5), cat("orfa", 6, "apagado"),
            ),
            groups = listOf(grp("saude", 0), grp("contas", 1), grp("vazio", 2)),
        )
        // Água (Contas) foi usada por último: Contas vem antes de Saúde; dentro, recentes primeiro.
        assertEquals(
            listOf("contas" to listOf("agua", "luz"), "saude" to listOf("psi", "plano"), "-" to listOf("mercado", "lazer", "orfa")),
            sections(data, listOf("agua", "mercado", "psi")),
        )
        // Sem uso: grupos pela ordem de criação; grupo vazio não aparece; grupo inexistente vale "Sem grupo".
        assertEquals(
            listOf("saude" to listOf("plano", "psi"), "contas" to listOf("luz", "agua"), "-" to listOf("lazer", "mercado", "orfa")),
            sections(data, emptyList()),
        )
    }
}
