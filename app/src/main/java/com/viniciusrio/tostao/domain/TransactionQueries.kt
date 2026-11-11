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

    /**
     * Transações de uma categoria no mês com o recorte e o agrupamento exatos de [Reports.month]:
     * efetivadas (até [nowMillis]), do [type] pedido (transferências nunca entram), sem ajustes.
     * [categoryId] null = "Sem categoria", que inclui as órfãs (categoria inexistente);
     * um id que não existe mais devolve lista vazia (não há linha dele no relatório).
     * Não confere `Category.type`, como o relatório. [accountId] null = todas as contas.
     * A soma MUST bater com o `totalCents` da linha correspondente do relatório. Mais recentes primeiro.
     */
    fun byCategory(
        data: FinanceData,
        categoryId: String?,
        type: CategoryType,
        month: YearMonth,
        zone: ZoneId,
        nowMillis: Long,
        accountId: String? = null,
    ): List<Transaction> {
        val range = Periods.monthRange(month, zone)
        val txType = if (type == CategoryType.EXPENSE) TxType.EXPENSE else TxType.INCOME
        // Uma categoria que não existe mais não tem linha no relatório: lista vazia, não "Sem categoria".
        if (categoryId != null && categoryId !in data.categoryById) return emptyList()
        val key = categoryId
        return data.transactions.asSequence()
            .filter { it.type == txType && !it.isAdjustment && it.dateTimeMillis in range && it.dateTimeMillis <= nowMillis }
            .filter { accountId == null || it.accountId == accountId }
            .filter { t -> t.categoryId?.let(data.categoryById::get)?.id == key }
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
