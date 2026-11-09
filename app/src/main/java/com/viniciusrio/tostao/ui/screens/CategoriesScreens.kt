package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.BackTopBar
import com.viniciusrio.tostao.ui.components.ColorPicker
import com.viniciusrio.tostao.ui.components.ConfirmDialog
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.IconPicker
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.SegmentedChoice

@Composable
fun CategoriesScreen(vm: MainViewModel, data: FinanceData) {
    var type by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Categorias", vm::back) {
            IconButton(onClick = { vm.navigate(Route.CategoryForm(type = type)) }) { Icon(Icons.Outlined.Add, "Nova categoria") }
        }
        SegmentedChoice(listOf(CategoryType.EXPENSE to "Despesas", CategoryType.INCOME to "Receitas"), type, { type = it })
        LazyColumn {
            items(data.categories.filter { it.type == type }, key = { it.id }) { c ->
                val uses = data.transactions.count { it.categoryId == c.id }
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar categoria") { vm.navigate(Route.CategoryForm(c.id, c.type)) }
                        .heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(c.icon, c.colorArgb)
                    Spacer(Modifier.size(14.dp))
                    Text(c.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (uses == 1) "1 transação" else "$uses transações",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFormScreen(vm: MainViewModel, data: FinanceData, id: String?, initialType: CategoryType) {
    val existing = remember(id) { id?.let { data.categoryById[it] } }
    if (id != null && existing == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: initialType) }
    var color by rememberSaveable { mutableStateOf(existing?.colorArgb ?: 0xFF2F9E6E) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "dots") }
    var confirmDelete by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicate = data.categories.any { it.id != id && it.type == type && it.name.equals(trimmed, ignoreCase = true) }
    val inUse = existing != null && (
        data.transactions.any { it.categoryId == existing.id } || data.rules.any { it.categoryId == existing.id } ||
            data.budgets.any { existing.id in it.categoryIds }
        )
    val typeLocked = inUse && existing!!.type != type
    val valid = trimmed.length in 1..30 && !duplicate && !typeLocked

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Nova categoria" else "Editar categoria", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir categoria") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SegmentedChoice(listOf(CategoryType.EXPENSE to "Despesa", CategoryType.INCOME to "Receita"), type, { type = it })
            if (typeLocked) {
                Text(
                    "Esta categoria está em uso e não pode mudar de tipo.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                name, { name = it.take(30) }, label = { Text("Nome") }, singleLine = true, isError = duplicate,
                supportingText = if (duplicate) ({ Text("Já existe uma categoria com esse nome") }) else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            SectionHeader("Cor")
            ColorPicker(color) { color = it }
            SectionHeader("Ícone")
            IconPicker(icon, color) { icon = it }
            Spacer(Modifier.size(24.dp))
        }
        Button(
            enabled = valid,
            onClick = {
                vm.saveCategory(Category(existing?.id ?: vm.newId(), trimmed, type, color, icon, existing?.order ?: 0), existing == null)
                vm.back()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }

    if (confirmDelete && existing != null) {
        val tx = data.transactions.count { it.categoryId == existing.id }
        val rules = data.rules.count { it.categoryId == existing.id }
        val budgetsRemoved = data.budgets.filter { !it.allExpenses && it.categoryIds == setOf(existing.id) }
        val body = buildString {
            append("$tx transações e $rules planejamentos ficarão \"Sem categoria\".")
            if (budgetsRemoved.isNotEmpty()) {
                append(" Os orçamentos ${budgetsRemoved.joinToString { "\"${it.name}\"" }} só usam esta categoria e serão excluídos.")
            }
        }
        ConfirmDialog(
            "Excluir \"${existing.name}\"?", body, "Excluir",
            onConfirm = { vm.deleteCategory(existing); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }
}
