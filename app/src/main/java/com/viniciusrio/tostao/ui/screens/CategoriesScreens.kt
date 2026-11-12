package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.viniciusrio.tostao.domain.CategoryGroup
import com.viniciusrio.tostao.ui.components.AppIcons
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.viniciusrio.tostao.domain.Limits
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
    val cats = data.categories.filter { it.type == type }
    val groups = data.groups.filter { it.type == type }.sortedBy { it.order }
    val byGroup = cats.groupBy { data.groupOf(it)?.id }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Categorias", vm::back) {
            IconButton(onClick = { vm.navigate(Route.GroupForm(type = type)) }) { Icon(Icons.Outlined.CreateNewFolder, "Novo grupo") }
            IconButton(onClick = { vm.navigate(Route.CategoryForm(type = type)) }) { Icon(Icons.Outlined.Add, "Nova categoria") }
        }
        SegmentedChoice(listOf(CategoryType.EXPENSE to "Despesas", CategoryType.INCOME to "Receitas"), type, { type = it })
        LazyColumn {
            if (groups.isEmpty()) {
                items(cats, key = { it.id }) { c -> CategoryRow(vm, data, c) }
            } else {
                groups.forEach { g ->
                    val members = byGroup[g.id].orEmpty()
                    item(key = "g:${g.id}") { GroupRow(vm, g, members.size) }
                    items(members, key = { it.id }) { c -> CategoryRow(vm, data, c, indented = true) }
                }
                val loose = byGroup[null].orEmpty()
                if (loose.isNotEmpty()) {
                    item(key = "sem-grupo") { SectionHeader("Sem grupo") }
                    items(loose, key = { it.id }) { c -> CategoryRow(vm, data, c) }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(vm: MainViewModel, g: CategoryGroup, count: Int) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar grupo") { vm.navigate(Route.GroupForm(g.id, g.type)) }
            .heightIn(min = 56.dp).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(g.icon, g.colorArgb)
        Spacer(Modifier.size(14.dp))
        Text(g.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            if (count == 1) "1 categoria" else "$count categorias",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryRow(vm: MainViewModel, data: FinanceData, c: Category, indented: Boolean = false) {
    val uses = data.transactions.count { it.categoryId == c.id }
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar categoria") { vm.navigate(Route.CategoryForm(c.id, c.type)) }
            .heightIn(min = 56.dp).padding(start = if (indented) 40.dp else 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
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

/** Escolha do grupo da categoria: "Sem grupo" ou um dos [groups] (já filtrados pelo tipo). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupChoice(groups: List<CategoryGroup>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Sem grupo") })
        groups.sortedBy { it.order }.forEach { g ->
            FilterChip(
                selected = selected == g.id, onClick = { onSelect(g.id) }, label = { Text(g.name) },
                leadingIcon = { Icon(AppIcons.of(g.icon), null, tint = Color(g.colorArgb), modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

/**
 * Cria uma categoria sem sair do formulário (transação ou planejamento). Cor e ícone ficam no
 * padrão e podem ser ajustados depois em Configurações > Categorias. [onCreated] recebe o id
 * já gerado, para o formulário selecioná-la assim que ela chegar do banco.
 */
@Composable
fun NewCategoryDialog(vm: MainViewModel, data: FinanceData, type: CategoryType, onDismiss: () -> Unit, onCreated: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var groupId by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = data.groups.filter { it.type == type }
    val trimmed = name.trim()
    val duplicate = data.categories.any { it.type == type && it.name.equals(trimmed, ignoreCase = true) }
    val valid = trimmed.length in 1..Limits.CATEGORY_NAME && !duplicate
    val create = {
        if (valid) {
            val id = vm.newId()
            val group = groupId?.takeIf { g -> groups.any { it.id == g } }
            vm.saveCategory(Category(id, trimmed, type, 0xFF2F9E6E, "dots", 0, group), isNew = true)
            onCreated(id)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == CategoryType.INCOME) "Nova categoria de receita" else "Nova categoria de despesa") },
        text = {
            Column {
                OutlinedTextField(
                    name, { name = it.take(Limits.CATEGORY_NAME) }, label = { Text("Nome") }, singleLine = true, isError = duplicate,
                    supportingText = if (duplicate) ({ Text("Já existe uma categoria com esse nome") }) else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { create() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (groups.isNotEmpty()) {
                    Text("Grupo", Modifier.padding(top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall)
                    GroupChoice(groups, groupId, { groupId = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = create, enabled = valid) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
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
    var groupId by rememberSaveable { mutableStateOf(data.groupOf(existing)?.id) }
    var confirmDelete by remember { mutableStateOf(false) }
    val groups = data.groups.filter { it.type == type }

    val trimmed = name.trim()
    val duplicate = data.categories.any { it.id != id && it.type == type && it.name.equals(trimmed, ignoreCase = true) }
    val inUse = existing != null && (
        data.transactions.any { it.categoryId == existing.id } || data.rules.any { it.categoryId == existing.id } ||
            data.budgets.any { existing.id in it.categoryIds }
        )
    val typeLocked = inUse && existing!!.type != type
    val valid = trimmed.length in 1..Limits.CATEGORY_NAME && !duplicate && !typeLocked

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Nova categoria" else "Editar categoria", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir categoria") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Trocar o tipo tira a categoria do grupo: o grupo é sempre do mesmo tipo.
            SegmentedChoice(
                listOf(CategoryType.EXPENSE to "Despesa", CategoryType.INCOME to "Receita"), type,
                { if (it != type) { type = it; groupId = null } },
            )
            if (typeLocked) {
                Text(
                    "Esta categoria está em uso e não pode mudar de tipo.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                name, { name = it.take(Limits.CATEGORY_NAME) }, label = { Text("Nome") }, singleLine = true, isError = duplicate,
                supportingText = if (duplicate) ({ Text("Já existe uma categoria com esse nome") }) else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (groups.isNotEmpty()) {
                SectionHeader("Grupo")
                GroupChoice(groups, groupId?.takeIf { g -> groups.any { it.id == g } }, { groupId = it }, Modifier.padding(horizontal = 20.dp))
            }
            SectionHeader("Cor")
            ColorPicker(color) { color = it }
            SectionHeader("Ícone")
            IconPicker(icon, color) { icon = it }
            Spacer(Modifier.size(24.dp))
        }
        Button(
            enabled = valid,
            onClick = {
                val group = groupId?.takeIf { g -> groups.any { it.id == g } }
                vm.saveCategory(Category(existing?.id ?: vm.newId(), trimmed, type, color, icon, existing?.order ?: 0, group), existing == null)
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

@Composable
fun CategoryGroupFormScreen(vm: MainViewModel, data: FinanceData, id: String?, initialType: CategoryType) {
    val existing = remember(id) { id?.let { data.groupById[it] } }
    // Grupo excluído (ou id inválido) com a tela aberta: volta.
    if (id != null && data.groupById[id] == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val current = existing?.let { g -> data.categories.filter { data.groupOf(it)?.id == g.id } }.orEmpty()
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: initialType) }
    var color by rememberSaveable { mutableStateOf(existing?.colorArgb ?: 0xFF2F9E6E) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "dots") }
    var members by rememberSaveable { mutableStateOf(current.map { it.id }) }
    var confirmDelete by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicate = data.groups.any { it.id != id && it.type == type && it.name.trim().equals(trimmed, ignoreCase = true) }
    // Com categorias, o tipo fica travado: elas teriam de sair do grupo antes.
    val typeLocked = current.isNotEmpty()
    val valid = trimmed.length in 1..Limits.GROUP_NAME && !duplicate
    val candidates = data.categories.filter { it.type == type }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Novo grupo" else "Editar grupo", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir grupo") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SegmentedChoice(
                listOf(CategoryType.EXPENSE to "Despesa", CategoryType.INCOME to "Receita"), type,
                { if (!typeLocked && it != type) { type = it; members = emptyList() } },
            )
            if (typeLocked) {
                Text(
                    "O tipo não muda enquanto o grupo tiver categorias.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                name, { name = it.take(Limits.GROUP_NAME) }, label = { Text("Nome do grupo") }, singleLine = true, isError = duplicate,
                supportingText = if (duplicate) ({ Text("Já existe um grupo com esse nome") }) else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            SectionHeader("Cor")
            ColorPicker(color) { color = it }
            SectionHeader("Ícone")
            IconPicker(icon, color) { icon = it }
            SectionHeader("Categorias")
            if (candidates.isEmpty()) {
                Text(
                    "Nenhuma categoria deste tipo.", Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            candidates.forEach { c ->
                val checked = c.id in members
                val other = data.groupOf(c)?.takeIf { it.id != id }
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(checked, role = Role.Checkbox) { members = if (it) members + c.id else members - c.id }
                        .heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Spacer(Modifier.size(12.dp))
                    IconBadge(c.icon, c.colorArgb, size = 32.dp)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                        if (other != null) {
                            Text(
                                if (checked) "Sai de \"${other.name}\"" else "Em \"${other.name}\"",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(24.dp))
        }
        Button(
            enabled = valid,
            onClick = {
                val g = CategoryGroup(existing?.id ?: vm.newId(), trimmed, type, color, icon, existing?.order ?: 0)
                vm.saveGroup(g, members.filter { m -> candidates.any { it.id == m } }.toSet(), existing == null)
                vm.back()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }

    if (confirmDelete && existing != null) {
        val n = current.size
        val body = when (n) {
            0 -> "O grupo não tem categorias."
            1 -> "1 categoria ficará sem grupo. Ela e as transações dela continuam como estão."
            else -> "$n categorias ficarão sem grupo. Elas e as transações delas continuam como estão."
        }
        ConfirmDialog(
            "Excluir o grupo \"${existing.name}\"?", body, "Excluir",
            onConfirm = { vm.deleteGroup(existing); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }
}
