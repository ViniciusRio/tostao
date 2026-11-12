package com.viniciusrio.tostao.domain

/** Limites de cadastro compartilhados pelos formulários e pela validação do backup. */
object Limits {
    const val ACCOUNT_NAME = 40
    const val CATEGORY_NAME = 30
    const val GROUP_NAME = 30
    const val BUDGET_NAME = 40
    const val TITLE = 60
    const val NOTE = 500
    val INTERVAL = 1..99
}
