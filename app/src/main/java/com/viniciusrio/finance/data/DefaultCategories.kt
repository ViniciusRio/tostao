package com.viniciusrio.finance.data

import com.viniciusrio.finance.domain.CategoryType

object DefaultCategories {
    private val expense = listOf(
        Triple("Alimentação", 0xFFE8743BL, "restaurant"),
        Triple("Mercado", 0xFF2F9E6EL, "cart"),
        Triple("Moradia", 0xFF5B6BD5L, "home"),
        Triple("Transporte", 0xFF2C8FC9L, "car"),
        Triple("Saúde", 0xFFD94F70L, "health"),
        Triple("Educação", 0xFF8A5CC7L, "school"),
        Triple("Lazer", 0xFFE0A526L, "fun"),
        Triple("Compras", 0xFFC2569BL, "bag"),
        Triple("Contas e serviços", 0xFF5D7A8CL, "bill"),
        Triple("Outros", 0xFF7D7D85L, "dots"),
    )
    private val income = listOf(
        Triple("Salário", 0xFF2F9E6EL, "work"),
        Triple("Freelance", 0xFF2C8FC9L, "laptop"),
        Triple("Rendimentos", 0xFF8A5CC7L, "trending"),
        Triple("Outros", 0xFF7D7D85L, "dots"),
    )

    fun all(): List<CategoryEntity> =
        expense.mapIndexed { i, (n, c, ic) -> CategoryEntity("def-exp-$i", n, CategoryType.EXPENSE.name, c, ic, i) } +
            income.mapIndexed { i, (n, c, ic) -> CategoryEntity("def-inc-$i", n, CategoryType.INCOME.name, c, ic, 100 + i) }
}
