package com.viniciusrio.tostao.domain

import java.time.YearMonth
import java.time.ZoneId

/** Fatia do gráfico de pizza. [categories] tem mais de um item só na fatia "Outras". */
data class PieSlice(
    val label: String,
    val cents: Long,
    val percent: Int,
    val colorArgb: Long,
    val categories: List<Category?>,
) {
    val isOthers: Boolean get() = categories.size > 1
}

data class MonthTotals(val month: YearMonth, val incomeCents: Long, val expenseCents: Long)

object Charts {
    const val OTHERS_THRESHOLD_PERCENT = 3
    const val OTHERS_COLOR = 0xFF9E9EA6
    private const val UNCATEGORIZED_COLOR = 0xFF7D7D85

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

    /**
     * Fatias em ordem decrescente de valor ("Outras" incluída). Categorias abaixo de 3% viram
     * "Outras" quando houver duas ou mais; uma só continua como fatia própria.
     */
    fun pie(slices: List<CategorySlice>): List<PieSlice> {
        val total = slices.sumOf { it.totalCents }
        if (total <= 0L) return emptyList()
        val small = slices.filter { it.totalCents * 100 < OTHERS_THRESHOLD_PERCENT * total }
        val grouped = if (small.size >= 2) small.toSet() else emptySet()
        val kept = slices.filter { it !in grouped }
        val parts = (kept.map { listOf(it) } + if (grouped.isEmpty()) emptyList() else listOf(grouped.toList()))
            .sortedByDescending { p -> p.sumOf { it.totalCents } }
        val cents = parts.map { p -> p.sumOf { it.totalCents } }
        val pct = percents(cents)
        return parts.mapIndexed { i, p ->
            if (p.size > 1) {
                PieSlice("Outras", cents[i], pct[i], OTHERS_COLOR, p.map { it.category })
            } else {
                val c = p.single().category
                PieSlice(c?.name ?: "Sem categoria", cents[i], pct[i], c?.colorArgb ?: UNCATEGORIZED_COLOR, listOf(c))
            }
        }
    }

    /** Resumo para leitor de tela: as três maiores fatias com percentual. */
    fun describe(kindLabel: String, pie: List<PieSlice>): String =
        "Gráfico de $kindLabel: " + pie.take(3).joinToString(", ") { "${it.label} ${it.percent}%" } +
            if (pie.size > 3) " e mais ${pie.size - 3}" else ""

    /** Receitas e despesas dos [months] meses que terminam em [last], do mais antigo ao mais recente. */
    fun trend(data: FinanceData, last: YearMonth, zone: ZoneId, nowMillis: Long, months: Int = 6): List<MonthTotals> =
        (months - 1 downTo 0).map { back ->
            val m = last.minusMonths(back.toLong())
            val r = Reports.month(data, m, zone, nowMillis)
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
