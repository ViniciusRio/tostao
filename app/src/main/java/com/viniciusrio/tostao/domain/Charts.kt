package com.viniciusrio.tostao.domain

import java.time.YearMonth
import java.time.ZoneId

/** Item de entrada da pizza: uma categoria, um grupo ou "Sem categoria", identificado por [key]. */
data class ChartItem(val key: CategoryScope, val label: String, val cents: Long, val colorArgb: Long)

/** Fatia do gráfico de pizza. [keys] tem mais de um item só na fatia "Outras". */
data class PieSlice(
    val label: String,
    val cents: Long,
    val percent: Int,
    val colorArgb: Long,
    val keys: List<CategoryScope>,
) {
    val isOthers: Boolean get() = keys.size > 1
}

data class MonthTotals(val month: YearMonth, val incomeCents: Long, val expenseCents: Long)

object Charts {
    const val OTHERS_THRESHOLD_PERCENT = 3
    const val OTHERS_COLOR = 0xFF9E9EA6
    private const val UNCATEGORIZED_COLOR = Reports.UNCATEGORIZED_COLOR

    /**
     * Percentuais inteiros que somam exatamente 100 (método do maior resto).
     * Lista vazia ou total zero devolve zeros.
     */
    fun percents(values: List<Long>): List<Int> {
        val total = values.sum()
        if (total <= 0L) return values.map { 0 }
        val floors = values.map { (it * 100 / total).toInt() }
        var missing = 100 - floors.sum()
        val byRemainder = values.indices.sortedByDescending { (values[it] * 100) % total }
        val out = floors.toMutableList()
        for (i in byRemainder) {
            if (missing == 0) break
            out[i]++; missing--
        }
        return out
    }

    /** Itens da visão por categoria: chave [CategoryScope.Category] ou [CategoryScope.None]. */
    fun categoryItems(slices: List<CategorySlice>): List<ChartItem> = slices.map { s ->
        val c = s.category
        if (c == null) {
            ChartItem(CategoryScope.None, "Sem categoria", s.totalCents, UNCATEGORIZED_COLOR)
        } else {
            ChartItem(CategoryScope.Category(c.id), c.name, s.totalCents, c.colorArgb)
        }
    }

    /** Itens da visão por grupo, com a chave de cada [GroupSlice]. */
    fun groupItems(slices: List<GroupSlice>): List<ChartItem> =
        slices.map { ChartItem(it.key, it.label, it.totalCents, it.colorArgb) }

    /**
     * Fatias em ordem decrescente de valor ("Outras" incluída). Itens abaixo de 3% viram
     * "Outras" quando houver dois ou mais; um só continua como fatia própria.
     */
    fun pie(items: List<ChartItem>): List<PieSlice> {
        val total = items.sumOf { it.cents }
        if (total <= 0L) return emptyList()
        val small = items.filter { it.cents * 100 < OTHERS_THRESHOLD_PERCENT * total }
        val grouped = if (small.size >= 2) small.toSet() else emptySet()
        val kept = items.filter { it !in grouped }
        val parts = (kept.map { listOf(it) } + if (grouped.isEmpty()) emptyList() else listOf(grouped.toList()))
            .sortedByDescending { p -> p.sumOf { it.cents } }
        val cents = parts.map { p -> p.sumOf { it.cents } }
        val pct = percents(cents)
        return parts.mapIndexed { i, p ->
            if (p.size > 1) {
                PieSlice("Outras", cents[i], pct[i], OTHERS_COLOR, p.map { it.key })
            } else {
                val it0 = p.single()
                PieSlice(it0.label, cents[i], pct[i], it0.colorArgb, listOf(it0.key))
            }
        }
    }

    /** Chaves das linhas destacadas pela fatia [index] (as de "Outras" ou a da própria fatia). */
    fun keysOf(pie: List<PieSlice>, index: Int?): Set<CategoryScope> =
        index?.let { pie.getOrNull(it) }?.keys?.toSet().orEmpty()

    /** Resumo para leitor de tela: as três maiores fatias com percentual. */
    fun describe(kindLabel: String, pie: List<PieSlice>): String =
        "Gráfico de $kindLabel: " + pie.take(3).joinToString(", ") { "${it.label} ${it.percent}%" } +
            if (pie.size > 3) " e mais ${pie.size - 3}" else ""

    /**
     * Receitas e despesas dos [months] meses que terminam em [last], do mais antigo ao mais recente.
     * [accountId] null = todas as contas; com a conta, o mesmo recorte de [Reports.month].
     */
    fun trend(
        data: FinanceData, last: YearMonth, zone: ZoneId, nowMillis: Long, months: Int = 6, accountId: String? = null,
    ): List<MonthTotals> =
        (months - 1 downTo 0).map { back ->
            val m = last.minusMonths(back.toLong())
            val r = Reports.month(data, m, zone, nowMillis, accountId)
            MonthTotals(m, r.incomeCents, r.expenseCents)
        }

    /** Resumo do gráfico de evolução para leitor de tela. */
    fun describeTrend(months: List<MonthTotals>, hidden: Boolean, monthName: (YearMonth) -> String): String {
        val withData = months.filter { it.incomeCents > 0 || it.expenseCents > 0 }
        if (withData.isEmpty()) return "Evolução dos últimos ${months.size} meses: sem receitas nem despesas"
        val top = withData.maxBy { it.expenseCents }
        val value = if (hidden) "" else " (${Money.format(top.expenseCents)})"
        return "Evolução dos últimos ${months.size} meses: ${withData.size} com movimento; maior despesa em ${monthName(top.month)}$value"
    }

    private val monthAbbr = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    fun monthShort(m: YearMonth): String = monthAbbr[m.monthValue - 1]
}
