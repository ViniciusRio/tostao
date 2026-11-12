package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ChartsTest {
    private fun slice(name: String, cents: Long) = CategorySlice(category(name), cents, 0)
    private fun pie(slices: List<CategorySlice>) = Charts.pie(Charts.categoryItems(slices))

    @Test fun percentsAlwaysSumToHundred() {
        assertEquals(listOf(34, 33, 33), Charts.percents(listOf(1, 1, 1)))
        assertEquals(100, Charts.percents(listOf(600, 380, 10, 10)).sum())
        assertEquals(listOf(0, 0), Charts.percents(listOf(0, 0)))
    }

    @Test fun groupsTwoOrMoreSmallCategoriesAsOthers() {
        val pie = pie(listOf(slice("Mercado", 60000), slice("Lazer", 38000), slice("Saúde", 1000), slice("Educação", 1000)))
        assertEquals(listOf("Mercado", "Lazer", "Outras"), pie.map { it.label })
        assertEquals(listOf(60, 38, 2), pie.map { it.percent })
        assertEquals(2000L, pie.last().cents)
        assertTrue(pie.last().isOthers)
    }

    @Test fun othersTakesItsPlaceInDescendingOrder() {
        val input = listOf(slice("A", 4500)) + (1..4).map { slice("M$it", 1000) } + (1..15).map { slice("p$it", 100) }
        val pie = pie(input)
        assertEquals(listOf("A", "Outras", "M1", "M2", "M3", "M4"), pie.map { it.label })
        assertEquals(100, pie.sumOf { it.percent })
    }

    @Test fun singleSmallCategoryStaysOnItsOwn() {
        val pie = pie(listOf(slice("Mercado", 9800), slice("Saúde", 200)))
        assertEquals(listOf("Mercado", "Saúde"), pie.map { it.label })
    }

    @Test fun uncategorizedHasLabel() {
        val pie = pie(listOf(CategorySlice(null, 100, 100)))
        assertEquals("Sem categoria", pie.single().label)
    }

    @Test fun emptyWhenNoData() {
        assertTrue(pie(emptyList()).isEmpty())
    }

    @Test fun describesTopThree() {
        val pie = pie(listOf(slice("Mercado", 60000), slice("Lazer", 38000), slice("Saúde", 1000), slice("Educação", 1000)))
        assertEquals("Gráfico de despesas: Mercado 60%, Lazer 38%, Outras 2%", Charts.describe("despesas", pie))
    }

    @Test fun trendCoversSixMonthsEndingAtSelected() {
        val data = FinanceData(
            transactions = listOf(
                tx(TxType.EXPENSE, 100, "a", millis(2026, 8, 5)),
                tx(TxType.INCOME, 500, "a", millis(2026, 11, 5)),
                tx(TxType.EXPENSE, 999, "a", millis(2026, 5, 31)),
                tx(TxType.TRANSFER, 777, "a", millis(2026, 9, 1), to = "b"),
            ),
        )
        val t = Charts.trend(data, YearMonth.of(2026, 11), ZONE, millis(2026, 11, 10))
        assertEquals((6..11).map { YearMonth.of(2026, it) }, t.map { it.month })
        assertEquals(100L, t.first { it.month.monthValue == 8 }.expenseCents)
        assertEquals(500L, t.last().incomeCents)
        assertEquals(0L, t.sumOf { it.expenseCents } - 100L)
    }

    @Test fun trendByAccount() {
        val data = FinanceData(
            accounts = listOf(account("BTG"), account("Pix")),
            transactions = listOf(
                tx(TxType.EXPENSE, 100, "Pix", millis(2026, 8, 5)),
                tx(TxType.EXPENSE, 300, "BTG", millis(2026, 8, 6)),
                tx(TxType.INCOME, 500, "BTG", millis(2026, 11, 5)),
                tx(TxType.INCOME, 50, "Pix", millis(2026, 11, 5)),
                tx(TxType.TRANSFER, 777, "BTG", millis(2026, 11, 1), to = "Pix"),
                tx(TxType.INCOME, 20, "Pix", millis(2026, 11, 2), adjustment = true),
            ),
        )
        val last = YearMonth.of(2026, 11)
        val now = millis(2026, 11, 10)
        val pix = Charts.trend(data, last, ZONE, now, accountId = "Pix")
        assertEquals((6..11).map { YearMonth.of(2026, it) }, pix.map { it.month })
        assertEquals(100L, pix.first { it.month.monthValue == 8 }.expenseCents)
        assertEquals(50L, pix.last().incomeCents)
        val btg = Charts.trend(data, last, ZONE, now, accountId = "BTG")
        assertEquals(300L, btg.first { it.month.monthValue == 8 }.expenseCents)
        assertEquals(500L, btg.last().incomeCents)
        assertEquals(Charts.trend(data, last, ZONE, now), Charts.trend(data, last, ZONE, now, accountId = null))
        assertEquals(400L, Charts.trend(data, last, ZONE, now).first { it.month.monthValue == 8 }.expenseCents)
    }

    // ---- Pizza por grupo e linhas destacadas ----

    @Test fun groupPieHighlightsGroupAndOthersRows() {
        val items = listOf(
            ChartItem(CategoryScope.Group("saude"), "Saúde", 80_000, 1),
            ChartItem(CategoryScope.Category("lazer"), "Lazer", 17_000, 2),
            ChartItem(CategoryScope.Group("pets"), "Pets", 2_000, 3),
            ChartItem(CategoryScope.Category("edu"), "Educação", 1_000, 4),
        )
        val pie = Charts.pie(items)
        assertEquals(listOf("Saúde", "Lazer", "Outras"), pie.map { it.label })
        assertEquals(setOf(CategoryScope.Group("saude")), Charts.keysOf(pie, 0))
        assertEquals(setOf(CategoryScope.Group("pets"), CategoryScope.Category("edu")), Charts.keysOf(pie, 2))
        assertEquals(3_000L, pie[2].cents)
        assertTrue(Charts.keysOf(pie, null).isEmpty())
        assertTrue(Charts.keysOf(pie, 9).isEmpty())
    }

    @Test fun categoryItemsUseCategoryAndNoneKeys() {
        val items = Charts.categoryItems(listOf(slice("Mercado", 100), CategorySlice(null, 50, 0)))
        assertEquals(listOf(CategoryScope.Category("Mercado"), CategoryScope.None), items.map { it.key })
        val g = GroupScenario
        val r = Reports.month(g.data, YearMonth.of(2026, 11), ZONE, g.now)
        val pie = Charts.pie(Charts.groupItems(r.expenseByGroup))
        assertEquals(listOf("Saúde", "Lazer", "Sem categoria"), pie.map { it.label })
        assertEquals(listOf(79, 20, 1), pie.map { it.percent })
    }
}
