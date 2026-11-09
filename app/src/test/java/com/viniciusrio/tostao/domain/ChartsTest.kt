package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ChartsTest {
    private fun slice(name: String, cents: Long) = CategorySlice(category(name), cents, 0)

    @Test fun percentsAlwaysSumToHundred() {
        assertEquals(listOf(34, 33, 33), Charts.percents(listOf(1, 1, 1)))
        assertEquals(100, Charts.percents(listOf(600, 380, 10, 10)).sum())
        assertEquals(listOf(0, 0), Charts.percents(listOf(0, 0)))
    }

    @Test fun groupsTwoOrMoreSmallCategoriesAsOthers() {
        val pie = Charts.pie(listOf(slice("Mercado", 60000), slice("Lazer", 38000), slice("Saúde", 1000), slice("Educação", 1000)))
        assertEquals(listOf("Mercado", "Lazer", "Outras"), pie.map { it.label })
        assertEquals(listOf(60, 38, 2), pie.map { it.percent })
        assertEquals(2000L, pie.last().cents)
        assertTrue(pie.last().isOthers)
    }

    @Test fun othersTakesItsPlaceInDescendingOrder() {
        val input = listOf(slice("A", 4500)) + (1..4).map { slice("M$it", 1000) } + (1..15).map { slice("p$it", 100) }
        val pie = Charts.pie(input)
        assertEquals(listOf("A", "Outras", "M1", "M2", "M3", "M4"), pie.map { it.label })
        assertEquals(100, pie.sumOf { it.percent })
    }

    @Test fun singleSmallCategoryStaysOnItsOwn() {
        val pie = Charts.pie(listOf(slice("Mercado", 9800), slice("Saúde", 200)))
        assertEquals(listOf("Mercado", "Saúde"), pie.map { it.label })
    }

    @Test fun uncategorizedHasLabel() {
        val pie = Charts.pie(listOf(CategorySlice(null, 100, 100)))
        assertEquals("Sem categoria", pie.single().label)
    }

    @Test fun emptyWhenNoData() {
        assertTrue(Charts.pie(emptyList()).isEmpty())
    }

    @Test fun describesTopThree() {
        val pie = Charts.pie(listOf(slice("Mercado", 60000), slice("Lazer", 38000), slice("Saúde", 1000), slice("Educação", 1000)))
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
}
