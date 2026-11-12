package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // ---- Filtro por categoria/grupo ----

    private val nov = YearMonth.of(2026, 11)
    private fun ids(l: List<Transaction>) = l.map { it.id }.toSet()

    @Test fun filterByGroupSaude() {
        val g = GroupScenario
        val list = TransactionQueries.filtered(g.data, TransactionFilter(nov, scope = CategoryScope.Group("saude")), ZONE, g.now)
        assertEquals(ids(listOf(g.plano, g.psi)), ids(list))
        val sum = TransactionQueries.scopeSummary(list, CategoryScope.Group("saude"), g.data)
        assertEquals(80_000L, sum.expenseCents)
        assertEquals(0L, sum.incomeCents)
        assertEquals(listOf("plano" to 50_000L, "psi" to 30_000L), sum.children.map { it.category!!.id to it.totalCents })
        assertEquals(listOf(63, 37), sum.children.map { it.percent })
    }

    @Test fun filterByCategoryAndNoCategory() {
        val g = GroupScenario
        val psi = TransactionQueries.filtered(g.data, TransactionFilter(nov, scope = CategoryScope.Category("psi")), ZONE, g.now)
        assertEquals(ids(listOf(g.psi, g.refund)), ids(psi))
        val s = TransactionQueries.scopeSummary(psi, CategoryScope.Category("psi"), g.data)
        assertEquals(30_000L to 5_000L, s.expenseCents to s.incomeCents)
        assertTrue(s.children.isEmpty())
        // "Sem categoria": só a despesa sem categoria, sem transferência nem ajuste.
        val none = TransactionQueries.filtered(g.data, TransactionFilter(nov, scope = CategoryScope.None), ZONE, g.now)
        assertEquals(ids(listOf(g.loose)), ids(none))
    }

    @Test fun scopeSummaryMatchesReports() {
        val g = GroupScenario
        val report = Reports.month(g.data, nov, ZONE, g.now)
        for (item in report.expenseByGroup) {
            val list = TransactionQueries.filtered(g.data, TransactionFilter(nov, TxType.EXPENSE, scope = item.key), ZONE, g.now)
            assertEquals(item.key.toString(), item.totalCents, TransactionQueries.scopeSummary(list, item.key, g.data).expenseCents)
        }
        for (slice in report.expenseByCategory) {
            val key = slice.category?.let { CategoryScope.Category(it.id) } ?: CategoryScope.None
            val list = TransactionQueries.filtered(g.data, TransactionFilter(nov, TxType.EXPENSE, scope = key), ZONE, g.now)
            assertEquals(slice.totalCents, TransactionQueries.scopeSummary(list, key, g.data).expenseCents)
        }
    }

    @Test fun scopeCombinesWithTypeAndAccount() {
        val g = GroupScenario
        val saude = CategoryScope.Group("saude")
        assertTrue(TransactionQueries.filtered(g.data, TransactionFilter(nov, TxType.INCOME, scope = saude), ZONE, g.now).isEmpty())
        assertEquals(ids(listOf(g.psi)), ids(TransactionQueries.filtered(g.data, TransactionFilter(nov, accountId = "btg", scope = saude), ZONE, g.now)))
        // Outubro não entra em novembro.
        assertEquals(ids(listOf(g.october)), ids(TransactionQueries.filtered(g.data, TransactionFilter(YearMonth.of(2026, 10), scope = saude), ZONE, g.now)))
    }

    @Test fun scopeSerializationAndParsing() {
        val g = GroupScenario.data
        listOf(CategoryScope.All, CategoryScope.None, CategoryScope.Category("psi"), CategoryScope.Group("saude")).forEach {
            assertEquals(it, CategoryScope.parseFilter(it.serialize(), g))
        }
        assertEquals(CategoryScope.All, CategoryScope.parseFilter("grp:apagado", g))
        assertEquals(CategoryScope.All, CategoryScope.parseFilter("cat:apagada", g))
        assertEquals(CategoryScope.All, CategoryScope.parseFilter("lixo", g))
        assertEquals(null, CategoryScope.parseRoute("grp:apagado", g))
        assertEquals(null, CategoryScope.parseRoute("all", g))
        assertEquals(CategoryScope.Group("saude"), CategoryScope.parseRoute("grp:saude", g))
        assertEquals(CategoryScope.None, CategoryScope.parseRoute("none", g))
    }

    @Test fun searchFindsByGroupName() {
        val g = GroupScenario
        val found = ids(TransactionQueries.search(g.data, "saude"))
        assertTrue(g.psi.id in found)
        assertTrue(g.refund.id in found)
        assertTrue(g.lazer.id !in found)
        // Grupo excluído: a busca pelo nome dele não acha mais nada pelo grupo.
        val noGroups = g.data.copy(groups = emptyList())
        assertTrue(g.psi.id !in ids(TransactionQueries.search(noGroups, "saude")))
    }
}
