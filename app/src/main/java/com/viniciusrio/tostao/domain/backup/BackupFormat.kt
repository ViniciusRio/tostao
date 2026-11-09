package com.viniciusrio.tostao.domain.backup

import kotlinx.serialization.Serializable

/*
 * DTOs do arquivo de backup, formato 1. Ficam separados das entidades do Room e das classes de
 * domínio de propósito: mudar o banco ou o domínio não pode mudar o formato sem querer.
 * Campos anuláveis têm padrão null para tolerar ausência; os demais são obrigatórios.
 */

@Serializable
data class BackupFile(
    val app: String,
    val formatVersion: Int,
    val createdAt: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val plannedRules: List<PlannedRuleDto>,
    val budgets: List<BudgetDto>,
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val icon: String,
    val initialBalanceCents: Long,
    val includeInTotal: Boolean,
    val order: Int,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val type: String,
    val colorArgb: Long,
    val icon: String,
    val order: Int,
)

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val amountCents: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val title: String,
    val note: String,
    val dateTimeMillis: Long,
    val plannedRuleId: String? = null,
    val isAdjustment: Boolean,
)

@Serializable
data class PlannedRuleDto(
    val id: String,
    val type: String,
    val amountCents: Long,
    val accountId: String,
    val categoryId: String? = null,
    val title: String,
    /** Data civil ISO, AAAA-MM-DD. */
    val startDate: String,
    val unit: String? = null,
    val interval: Int,
    val endDate: String? = null,
    val consumedOccurrences: Int,
)

@Serializable
data class BudgetDto(
    val id: String,
    val name: String,
    val limitCents: Long,
    val allExpenses: Boolean,
    val categoryIds: List<String>,
    val order: Int,
)
