package com.viniciusrio.tostao.ui

import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.TxType

sealed interface Route {
    /** Destinos da barra inferior. */
    sealed interface Tab : Route
    data object Home : Tab
    data class Transactions(val accountId: String? = null) : Tab
    data object Planning : Tab
    data object Reports : Tab

    data class TxForm(val type: TxType, val editId: String? = null) : Route
    data object Accounts : Route
    data class AccountForm(val id: String? = null) : Route
    data object Categories : Route
    data class CategoryForm(val id: String? = null, val type: CategoryType = CategoryType.EXPENSE) : Route
    data class RuleForm(val id: String? = null) : Route
    data class BudgetForm(val id: String? = null) : Route
    data object Search : Route
    data object Settings : Route
}
