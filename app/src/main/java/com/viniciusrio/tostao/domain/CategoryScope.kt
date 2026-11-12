package com.viniciusrio.tostao.domain

/**
 * Recorte por categoria usado no filtro de Transações, na tela de transações da categoria/grupo
 * e como chave dos itens de Relatórios. Fora de [All], só entram receitas e despesas (sem
 * transferências nem ajustes), como em Relatórios.
 */
sealed interface CategoryScope {
    /** Todas as transações. */
    data object All : CategoryScope

    /** "Sem categoria": receita ou despesa sem categoria (ou com uma categoria que não existe mais). */
    data object None : CategoryScope

    data class Category(val id: String) : CategoryScope

    /**
     * Grupo: transações das categorias do grupo cujo tipo é o do grupo. Uma receita numa
     * categoria de despesa agrupada não conta para o grupo (no relatório ela vira item próprio).
     */
    data class Group(val id: String) : CategoryScope

    /** String estável para `rememberSaveable` e rotas: `all`, `none`, `cat:<id>`, `grp:<id>`. */
    fun serialize(): String = when (this) {
        All -> "all"
        None -> "none"
        is Category -> "cat:$id"
        is Group -> "grp:$id"
    }

    /** Se a transação está no recorte. Não filtra mês, tipo nem conta. */
    fun matches(t: Transaction, data: FinanceData): Boolean {
        if (this == All) return true
        if (t.type == TxType.TRANSFER || t.isAdjustment) return false
        val cat = t.categoryId?.let(data.categoryById::get)
        return when (this) {
            None -> cat == null
            is Category -> cat?.id == id
            is Group -> cat != null && data.groupOf(cat)?.let { it.id == id && it.type == t.categoryType() } == true
            All -> true
        }
    }

    companion object {
        private fun parse(s: String?): CategoryScope? = when {
            s == null -> null
            s == "all" -> All
            s == "none" -> None
            s.startsWith("cat:") && s.length > 4 -> Category(s.substring(4))
            s.startsWith("grp:") && s.length > 4 -> Group(s.substring(4))
            else -> null
        }

        private fun exists(scope: CategoryScope, data: FinanceData) = when (scope) {
            is Category -> scope.id in data.categoryById
            is Group -> scope.id in data.groupById
            else -> true
        }

        /** Aba Transações: texto inválido ou item que não existe mais volta para [All]. */
        fun parseFilter(s: String?, data: FinanceData): CategoryScope =
            parse(s)?.takeIf { exists(it, data) } ?: All

        /**
         * Tela de transações da categoria/grupo: `null` para [All], texto inválido ou item que não
         * existe mais, e a tela volta para Relatórios. Nunca vira "todas as transações".
         */
        fun parseRoute(s: String?, data: FinanceData): CategoryScope? =
            parse(s)?.takeIf { it != All && exists(it, data) }
    }
}

/** Tipo de categoria correspondente a uma receita ou despesa. */
fun Transaction.categoryType(): CategoryType = if (type == TxType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
