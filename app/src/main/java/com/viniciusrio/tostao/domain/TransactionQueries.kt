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
    /** Fora de [CategoryScope.All], só receitas e despesas do item (sem transferências nem ajustes). */
    val scope: CategoryScope = CategoryScope.All,
)

/**
 * Resumo de um recorte: despesas e receitas separadas (cada uma mostrada só se diferente de zero)
 * e, para grupo, o detalhamento por categoria com movimento, do maior para o menor, com o
 * percentual dentro do grupo.
 */
data class ScopeSummary(val expenseCents: Long, val incomeCents: Long, val children: List<CategorySlice>)

/** Seção do seletor de categoria: um grupo (ou "Sem grupo", [group] nulo) e suas categorias. */
data class CategorySection(val group: CategoryGroup?, val categories: List<Category>)

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
            .filter { filter.scope.matches(it, data) }
            .sortedByDescending { it.dateTimeMillis }
            .toList()
    }

    /** Resumo de [list] (já filtrada) para o [scope]; o detalhamento só existe para grupo. */
    fun scopeSummary(list: List<Transaction>, scope: CategoryScope, data: FinanceData): ScopeSummary {
        val txs = list.filter { scope != CategoryScope.All && scope.matches(it, data) }
        val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val children = if (scope is CategoryScope.Group) categorySlices(txs, data) else emptyList()
        return ScopeSummary(expense, income, children)
    }

    /** Totais por categoria, do maior para o menor, com percentuais que somam 100. */
    fun categorySlices(txs: List<Transaction>, data: FinanceData): List<CategorySlice> {
        val total = txs.sumOf { it.amountCents }
        if (total == 0L) return emptyList()
        val sums = txs.groupBy { it.categoryId?.let(data.categoryById::get) }
            .map { (cat, list) -> cat to list.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
        val pct = Charts.percents(sums.map { it.second })
        return sums.mapIndexed { i, (cat, sum) -> CategorySlice(cat, sum, pct[i]) }
    }

    /**
     * Transações de um item do relatório no mês, com o recorte e o agrupamento exatos de
     * [Reports.month]: efetivadas (até [nowMillis]), do [type] pedido (transferências nunca entram),
     * sem ajustes. [CategoryScope.None] = "Sem categoria", que inclui as órfãs (categoria
     * inexistente); uma categoria ou um grupo que não existe mais devolve lista vazia (não há linha
     * dele no relatório). Para categoria, não confere `Category.type`, como o relatório; para grupo,
     * vale a regra de [CategoryScope.Group]. [accountId] null = todas as contas.
     * A soma MUST bater com o `totalCents` da linha correspondente do relatório. Mais recentes primeiro.
     */
    fun byScope(
        data: FinanceData,
        scope: CategoryScope,
        type: CategoryType,
        month: YearMonth,
        zone: ZoneId,
        nowMillis: Long,
        accountId: String? = null,
    ): List<Transaction> {
        val range = Periods.monthRange(month, zone)
        val txType = if (type == CategoryType.EXPENSE) TxType.EXPENSE else TxType.INCOME
        val exists = when (scope) {
            is CategoryScope.Category -> scope.id in data.categoryById
            is CategoryScope.Group -> scope.id in data.groupById
            CategoryScope.None -> true
            CategoryScope.All -> false
        }
        if (!exists) return emptyList()
        return data.transactions.asSequence()
            .filter { it.type == txType && !it.isAdjustment && it.dateTimeMillis in range && it.dateTimeMillis <= nowMillis }
            .filter { accountId == null || it.accountId == accountId }
            .filter { scope.matches(it, data) }
            .sortedByDescending { it.dateTimeMillis }
            .toList()
    }

    /** [byScope] de uma categoria; [categoryId] null = "Sem categoria". */
    fun byCategory(
        data: FinanceData,
        categoryId: String?,
        type: CategoryType,
        month: YearMonth,
        zone: ZoneId,
        nowMillis: Long,
        accountId: String? = null,
    ): List<Transaction> = byScope(
        data, categoryId?.let { CategoryScope.Category(it) } ?: CategoryScope.None, type, month, zone, nowMillis, accountId,
    )

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
            val category = t.categoryId?.let(data.categoryById::get)
            val cat = category?.name.orEmpty()
            val group = data.groupOf(category)?.name.orEmpty()
            normalize(t.title).contains(q) || normalize(t.note).contains(q) ||
                normalize(cat).contains(q) || normalize(group).contains(q) || Money.plain(t.amountCents).contains(q)
        }.sortedByDescending { it.dateTimeMillis }
    }

    /** Ids de categorias ordenados por uso mais recente, para sugerir no formulário. */
    fun recentCategoryIds(data: FinanceData): List<String> =
        data.transactions.sortedByDescending { it.dateTimeMillis }.mapNotNull { it.categoryId }.distinct()

    /**
     * Categorias do [type] para o seletor dos formulários. Sem grupos do tipo: uma única seção
     * (sem título), igual à lista de sempre. Com grupos: uma seção por grupo com categorias,
     * ordenadas pelo uso mais recente de qualquer categoria do grupo (empate pela ordem de
     * criação), e "Sem grupo" por último. Dentro de cada seção, as de [recent] primeiro e depois
     * pela ordem da categoria.
     */
    fun categorySections(data: FinanceData, type: CategoryType, recent: List<String>): List<CategorySection> {
        val rank = recent.withIndex().associate { (i, id) -> id to i }
        fun rankOf(id: String) = rank[id] ?: Int.MAX_VALUE
        val cats = data.categories.filter { it.type == type }.sortedWith(compareBy({ rankOf(it.id) }, { it.order }))
        val byGroup = cats.groupBy { data.groupOf(it)?.id }
        val groups = data.groups.filter { it.type == type && !byGroup[it.id].isNullOrEmpty() }
        if (groups.isEmpty()) return listOf(CategorySection(null, cats))
        val grouped = groups
            .sortedWith(compareBy({ g -> byGroup.getValue(g.id).minOf { rankOf(it.id) } }, { it.order }))
            .map { CategorySection(it, byGroup.getValue(it.id)) }
        val loose = byGroup[null].orEmpty()
        return if (loose.isEmpty()) grouped else grouped + CategorySection(null, loose)
    }

    fun lastUsedAccountId(data: FinanceData): String? =
        data.transactions.maxByOrNull { it.dateTimeMillis }?.accountId?.takeIf { it in data.accountById }
}
