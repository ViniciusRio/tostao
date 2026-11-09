package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.BudgetStatus
import com.viniciusrio.tostao.domain.Budgets
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.Planned
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.Recurrence
import com.viniciusrio.tostao.domain.RecurrenceUnit
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.AccountChips
import com.viniciusrio.tostao.ui.components.AmountField
import com.viniciusrio.tostao.ui.components.BackTopBar
import com.viniciusrio.tostao.ui.components.CategoryChips
import com.viniciusrio.tostao.ui.components.ConfirmDialog
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.SegmentedChoice
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import java.time.LocalDate
import java.time.YearMonth

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(vm: MainViewModel, data: FinanceData) {
    val tab = vm.planningTab
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { vm.planningTab = 0 }, text = { Text("Planejados") })
            Tab(tab == 1, { vm.planningTab = 1 }, text = { Text("Orçamentos") })
        }
        if (tab == 0) RulesList(vm, data) else BudgetsList(vm, data)
    }
}

private fun recurrenceText(r: PlannedRule): String {
    val n = r.interval
    val base = when (r.unit) {
        null -> return "Uma vez"
        RecurrenceUnit.DAY -> if (n == 1) "Todo dia" else "A cada $n dias"
        RecurrenceUnit.WEEK -> if (n == 1) "Toda semana" else "A cada $n semanas"
        RecurrenceUnit.MONTH -> if (n == 1) "Todo mês" else "A cada $n meses"
        RecurrenceUnit.YEAR -> if (n == 1) "Todo ano" else "A cada $n anos"
    }
    return base
}

@Composable
private fun RulesList(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    val minute by vm.minute.collectAsState()
    val today = remember(minute) { vm.today() }
    val (active, done) = remember(data) { data.rules.partition { Recurrence.nextDue(it) != null } }
    val sorted = active.sortedBy { Recurrence.nextDue(it) }
    val sem = LocalSemantic.current
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = {
                    if (data.accounts.isEmpty()) vm.navigate(Route.AccountForm()) else vm.navigate(Route.RuleForm())
                }) { Text("Novo planejamento") }
            }
        }
        if (data.rules.isEmpty()) {
            item {
                EmptyState(
                    "Nada planejado",
                    "Cadastre contas fixas, parcelas e salários. Você verá só a próxima ocorrência de cada um, sem poluir o extrato.",
                )
            }
        }
        items(sorted, key = { it.id }) { r ->
            val due = Recurrence.nextDue(r)!!
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, due)
            val cat = r.categoryId?.let { data.categoryById[it] }
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar") { vm.navigate(Route.RuleForm(r.id)) }
                    .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(cat?.icon ?: "bill", cat?.colorArgb ?: 0xFF5D7A8C)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${recurrenceText(r)} · próximo ${Periods.shortDate(due, today)} (${Planned.relativeDue(days)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (days < 0) sem.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoneyText(
                    if (r.type == TxType.INCOME) r.amountCents else -r.amountCents, signed = true, hidden = hidden,
                    color = if (r.type == TxType.INCOME) sem.income else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (done.isNotEmpty()) {
            item { SectionHeader("Concluídos") }
            items(done, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar") { vm.navigate(Route.RuleForm(r.id)) }
                        .heightIn(min = 56.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Concluído", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BudgetsList(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    val today = vm.today()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    val minute by vm.minute.collectAsState()
    val progress = remember(data, month, minute) { Budgets.progress(data, month, vm.zone, vm.nowMillis()) }
    val sem = LocalSemantic.current
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item { MonthSwitcher(month, Periods.monthLabel(month, today)) { monthText = it.toString() } }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { vm.navigate(Route.BudgetForm()) }) { Text("Novo orçamento") }
            }
        }
        if (progress.isEmpty()) {
            item { EmptyState("Nenhum orçamento", "Defina um limite mensal por categoria (ex.: Mercado R$ 800) e acompanhe quanto ainda pode gastar.") }
        }
        items(progress, key = { it.budget.id }) { p ->
            val color = when (p.status) {
                BudgetStatus.NORMAL -> MaterialTheme.colorScheme.primary
                BudgetStatus.WARNING -> sem.warning
                BudgetStatus.EXCEEDED -> sem.expense
            }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable(role = Role.Button, onClickLabel = "Editar orçamento") { vm.navigate(Route.BudgetForm(p.budget.id)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.budget.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${p.percent}%", style = MaterialTheme.typography.labelLarge, color = color)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { p.ratio.coerceIn(0f, 1f) }, color = color,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        MoneyText(p.spentCents, hidden = hidden, style = MaterialTheme.typography.bodyMedium)
                        Text(" de ", style = MaterialTheme.typography.bodyMedium)
                        MoneyText(p.budget.limitCents, hidden = hidden, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(if (hidden) p.status.name.let { statusOnly(p.status) } else p.statusText, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}

private fun statusOnly(s: BudgetStatus) = when (s) {
    BudgetStatus.NORMAL -> "Dentro do limite"
    BudgetStatus.WARNING -> "Atenção: perto do limite"
    BudgetStatus.EXCEEDED -> "Limite excedido"
}

@Composable
fun RuleFormScreen(vm: MainViewModel, data: FinanceData, id: String?) {
    val existing = remember(id) { id?.let { i -> data.rules.firstOrNull { it.id == i } } }
    if (id != null && existing == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val today = vm.today()
    var type by rememberSaveable { mutableStateOf(existing?.type ?: TxType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.plain(it.amountCents) } ?: "") }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var accountId by rememberSaveable { mutableStateOf(existing?.accountId ?: data.accounts.firstOrNull()?.id) }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var start by remember { mutableStateOf(existing?.startDate ?: today) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit) }
    var interval by rememberSaveable { mutableStateOf((existing?.interval ?: 1).toString()) }
    var hasEnd by rememberSaveable { mutableStateOf(existing?.endDate != null) }
    var end by remember { mutableStateOf(existing?.endDate ?: today.plusYears(1)) }
    var confirmDelete by remember { mutableStateOf(false) }

    val catType = if (type == TxType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val cats = data.categories.filter { it.type == catType }
    val effectiveCategory = categoryId?.takeIf { c -> cats.any { it.id == c } }
    val cents = Money.parse(amount)
    val n = interval.toIntOrNull()
    val endOk = !hasEnd || unit == null || !end.isBefore(start)
    val valid = cents != null && cents > 0 && title.isNotBlank() && accountId != null &&
        (unit == null || (n != null && n in 1..99)) && endOk

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Novo planejamento" else "Editar planejamento", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir planejamento") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SegmentedChoice(listOf(TxType.EXPENSE to "Despesa", TxType.INCOME to "Receita"), type, { type = it })
            OutlinedTextField(
                title, { title = it.take(60) }, label = { Text("Título") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            AmountField(amount, { amount = it }, "Valor", Modifier.padding(horizontal = 20.dp))
            SectionHeader("Conta")
            AccountChips(data.accounts, accountId, { accountId = it })
            SectionHeader("Categoria")
            CategoryChips(cats, setOfNotNull(effectiveCategory)) { c -> categoryId = if (effectiveCategory == c) null else c }
            SectionHeader("Quando")
            DateButton(if (unit == null) "Data" else "Primeira", start, today, { start = it }, Modifier.padding(horizontal = 20.dp))
            SectionHeader("Repetir")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to "Não", RecurrenceUnit.DAY to "Dias", RecurrenceUnit.WEEK to "Semanas", RecurrenceUnit.MONTH to "Meses", RecurrenceUnit.YEAR to "Anos")
                    .forEach { (u, label) -> FilterChip(unit == u, { unit = u }, { Text(label) }) }
            }
            if (unit != null) {
                OutlinedTextField(
                    interval, { interval = it.filter(Char::isDigit).take(2) }, singleLine = true,
                    label = { Text("A cada quantos") }, isError = n == null || n !in 1..99,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Switch) { hasEnd = !hasEnd }.padding(horizontal = 20.dp).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tem data final", Modifier.weight(1f))
                    Switch(hasEnd, null)
                }
                if (hasEnd) {
                    DateButton("Até", end, today, { end = it }, Modifier.padding(horizontal = 20.dp))
                    if (!endOk) Text("A data final deve ser depois da primeira.", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            enabled = valid,
            onClick = {
                val newUnit = unit
                val newInterval = if (newUnit == null) 1 else n!!
                val scheduleChanged = existing == null || existing.startDate != start || existing.unit != newUnit ||
                    existing.interval != newInterval
                val r = PlannedRule(
                    id = existing?.id ?: vm.newId(), type = type, amountCents = cents!!, accountId = accountId!!,
                    categoryId = effectiveCategory, title = title.trim(), startDate = start, unit = newUnit,
                    interval = newInterval, endDate = if (hasEnd && newUnit != null) end else null,
                    consumedOccurrences = if (scheduleChanged) 0 else existing!!.consumedOccurrences,
                )
                vm.saveRule(r, existing == null)
                vm.back()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Excluir \"${existing.title}\"?", "Transações já pagas continuam no extrato. Você poderá desfazer logo em seguida.", "Excluir",
            onConfirm = { vm.deleteRule(existing); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun BudgetFormScreen(vm: MainViewModel, data: FinanceData, id: String?) {
    val existing = remember(id) { id?.let { i -> data.budgets.firstOrNull { it.id == i } } }
    if (id != null && existing == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.plain(it.limitCents) } ?: "") }
    var all by rememberSaveable { mutableStateOf(existing?.allExpenses ?: false) }
    var selected by remember { mutableStateOf(existing?.categoryIds ?: emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val cents = Money.parse(amount)
    val valid = name.isNotBlank() && cents != null && cents > 0 && (all || selected.isNotEmpty())
    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Novo orçamento" else "Editar orçamento", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir orçamento") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                name, { name = it.take(40) }, label = { Text("Nome") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AmountField(amount, { amount = it }, "Limite por mês", Modifier.padding(horizontal = 20.dp))
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Switch) { all = !all }.padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Todas as despesas", Modifier.weight(1f))
                Switch(all, null)
            }
            if (!all) {
                SectionHeader("Categorias")
                CategoryChips(data.categories.filter { it.type == CategoryType.EXPENSE }, selected) { c ->
                    selected = if (c in selected) selected - c else selected + c
                }
                if (selected.isEmpty()) {
                    Text("Escolha ao menos uma categoria.", Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            enabled = valid,
            onClick = {
                vm.saveBudget(
                    Budget(existing?.id ?: vm.newId(), name.trim(), cents!!, all, if (all) emptySet() else selected, existing?.order ?: 0),
                    existing == null,
                )
                vm.back()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Excluir \"${existing.name}\"?", "As transações não são afetadas.", "Excluir",
            onConfirm = { vm.deleteBudget(existing); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }
}
