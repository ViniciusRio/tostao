package com.viniciusrio.tostao.domain

import java.time.LocalDate

enum class TxType { INCOME, EXPENSE, TRANSFER }

enum class CategoryType { INCOME, EXPENSE }

enum class RecurrenceUnit { DAY, WEEK, MONTH, YEAR }

data class Account(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val icon: String,
    val initialBalanceCents: Long,
    val includeInTotal: Boolean,
    val order: Int,
)

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val colorArgb: Long,
    val icon: String,
    val order: Int,
    /** Grupo opcional, sempre do mesmo tipo; um id sem grupo correspondente vale como "Sem grupo". */
    val groupId: String? = null,
)

/** Grupo ("guarda-chuva") opcional de categorias do mesmo tipo. [order] é a ordem de criação. */
data class CategoryGroup(
    val id: String,
    val name: String,
    val type: CategoryType,
    val colorArgb: Long,
    val icon: String,
    val order: Int,
)

data class Transaction(
    val id: String,
    val type: TxType,
    val amountCents: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val title: String,
    val note: String,
    val dateTimeMillis: Long,
    val plannedRuleId: String? = null,
    val isAdjustment: Boolean = false,
)

/**
 * Regra planejada. As ocorrências nunca são materializadas: a próxima pendente é
 * calculada a partir de [startDate] + [consumedOccurrences] intervalos.
 */
data class PlannedRule(
    val id: String,
    val type: TxType,
    val amountCents: Long,
    val accountId: String,
    val categoryId: String?,
    val title: String,
    val startDate: LocalDate,
    val unit: RecurrenceUnit?,
    val interval: Int,
    val endDate: LocalDate?,
    val consumedOccurrences: Int,
)

data class Budget(
    val id: String,
    val name: String,
    val limitCents: Long,
    val allExpenses: Boolean,
    val categoryIds: Set<String>,
    val order: Int,
)

/** Fotografia de todos os dados do app, base das funções puras de cálculo. */
data class FinanceData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val rules: List<PlannedRule> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val groups: List<CategoryGroup> = emptyList(),
) {
    val categoryById: Map<String, Category> by lazy { categories.associateBy { it.id } }
    val groupById: Map<String, CategoryGroup> by lazy { groups.associateBy { it.id } }

    /** Grupo da categoria; um grupo inexistente ou de outro tipo é tratado como "Sem grupo". */
    fun groupOf(c: Category?): CategoryGroup? =
        c?.groupId?.let(groupById::get)?.takeIf { it.type == c.type }
    val accountById: Map<String, Account> by lazy { accounts.associateBy { it.id } }
}
