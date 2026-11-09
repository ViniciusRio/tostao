package com.viniciusrio.tostao.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DayGroup(val date: LocalDate, val label: String, val netCents: Long, val items: List<Transaction>)

data class TransactionFilter(
    val month: YearMonth,
    val type: TxType? = null,
    val accountId: String? = null,
)

object TransactionQueries {
    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

    /** Transações efetivadas (até [nowMillis]) do filtro, mais recentes primeiro. */
    fun filtered(data: FinanceData, filter: TransactionFilter, zone: ZoneId, nowMillis: Long): List<Transaction> {
        val range = Periods.monthRange(filter.month, zone)
        return data.transactions.asSequence()
            .filter { it.dateTimeMillis in range && it.dateTimeMillis <= nowMillis }
            .filter { filter.type == null || it.type == filter.type }
            .filter { filter.accountId == null || it.accountId == filter.accountId || it.toAccountId == filter.accountId }
            .sortedByDescending { it.dateTimeMillis }
            .toList()
    }

    /** Agrupa por dia; o total do dia ignora transferências (não mudam o patrimônio). */
    fun groupByDay(txs: List<Transaction>, zone: ZoneId, today: LocalDate): List<DayGroup> =
        txs.groupBy { Periods.localDate(it.dateTimeMillis, zone) }
            .toSortedMap(compareByDescending { it })
            .map { (d, list) -> DayGroup(d, Periods.dayLabel(d, today), list.sumOf { signedCents(it) }, list) }

    fun signedCents(t: Transaction): Long = when (t.type) {
        TxType.INCOME -> t.amountCents
        TxType.EXPENSE -> -t.amountCents
        TxType.TRANSFER -> 0
    }

    fun search(data: FinanceData, query: String): List<Transaction> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        return data.transactions.filter { t ->
            val cat = t.categoryId?.let { data.categoryById[it]?.name }.orEmpty()
            normalize(t.title).contains(q) || normalize(t.note).contains(q) ||
                normalize(cat).contains(q) || Money.plain(t.amountCents).contains(q)
        }.sortedByDescending { it.dateTimeMillis }
    }

    /** Ids de categorias ordenados por uso mais recente, para sugerir no formulário. */
    fun recentCategoryIds(data: FinanceData): List<String> =
        data.transactions.sortedByDescending { it.dateTimeMillis }.mapNotNull { it.categoryId }.distinct()

    fun lastUsedAccountId(data: FinanceData): String? =
        data.transactions.maxByOrNull { it.dateTimeMillis }?.accountId?.takeIf { it in data.accountById }
}
