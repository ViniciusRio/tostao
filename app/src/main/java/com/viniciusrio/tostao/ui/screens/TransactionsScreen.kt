package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import com.viniciusrio.tostao.domain.CategoryScope
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.Reports
import com.viniciusrio.tostao.domain.ScopeSummary
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.TransactionFilter
import com.viniciusrio.tostao.domain.TransactionQueries
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.TransactionRow
import java.time.YearMonth

@Composable
fun MonthSwitcher(month: YearMonth, label: String, onChange: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mês anterior")
        }
        Text(
            label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading(); liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Próximo mês")
        }
    }
}

@Composable
fun TransactionsScreen(vm: MainViewModel, data: FinanceData, initialAccount: String?) {
    val hidden by vm.hideValues.collectAsState()
    val minute by vm.minute.collectAsState()
    val today = remember(minute) { vm.today() }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    var type by rememberSaveable { mutableStateOf<TxType?>(null) }
    var accountId by rememberSaveable { mutableStateOf(initialAccount) }
    // Recorte por categoria/grupo guardado como texto estável; um item excluído volta para "Todas".
    var scopeText by rememberSaveable { mutableStateOf(CategoryScope.All.serialize()) }
    val scope = CategoryScope.parseFilter(scopeText, data)
    var pickingScope by rememberSaveable { mutableStateOf(false) }
    val now = vm.nowMillis()
    val filtered = remember(data, month, type, accountId, scope, minute) {
        TransactionQueries.filtered(data, TransactionFilter(month, type, accountId, scope), vm.zone, now)
    }
    val summary = remember(filtered, scope, data) { TransactionQueries.scopeSummary(filtered, scope, data) }
    val groups = remember(filtered, today) { TransactionQueries.groupByDay(filtered, vm.zone, today) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { MonthSwitcher(month, Periods.monthLabel(month, today)) { monthText = it.toString() } }
            IconButton(onClick = { vm.navigate(Route.Search) }) { Icon(Icons.Outlined.Search, "Buscar transações") }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(type == null, { type = null }, { Text("Todas") }) }
            item { FilterChip(type == TxType.EXPENSE, { type = TxType.EXPENSE }, { Text("Despesas") }) }
            item { FilterChip(type == TxType.INCOME, { type = TxType.INCOME }, { Text("Receitas") }) }
            // Transferências não têm categoria: o filtro de categoria volta para "Todas" e fica indisponível.
            item {
                FilterChip(type == TxType.TRANSFER, { type = TxType.TRANSFER; scopeText = CategoryScope.All.serialize() }, { Text("Transferências") })
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(accountId == null, { accountId = null }, { Text("Todas as contas") }) }
            items(data.accounts, key = { it.id }) { a ->
                FilterChip(accountId == a.id, { accountId = if (accountId == a.id) null else a.id }, { Text(a.name) })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            FilterChip(
                selected = scope != CategoryScope.All,
                onClick = { pickingScope = true },
                enabled = type != TxType.TRANSFER,
                label = { Text("Categoria: ${scopeLabel(scope, data)}") },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                modifier = Modifier.semantics { onClick("Escolher categoria ou grupo") { pickingScope = true; true } },
            )
        }
        if (scope != CategoryScope.All) ScopeSummaryCard(scopeLabel(scope, data), summary, hidden)
        if (groups.isEmpty()) {
            EmptyState("Nada neste mês", "Não há transações para o período e filtros escolhidos.")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                groups.forEach { g ->
                    item(key = "h" + g.date) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                g.label, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            MoneyText(
                                g.netCents, signed = true, hidden = hidden, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                    }
                    items(g.items, key = { it.id }) { t ->
                        TransactionRow(t, data, hidden) { vm.navigate(Route.TxForm(t.type, t.id)) }
                    }
                }
            }
        }
    }
    if (pickingScope) {
        ScopePickerSheet(
            data, type, scope,
            onPick = { scopeText = it.serialize(); pickingScope = false },
            onDismiss = { pickingScope = false },
        )
    }
}

private fun scopeLabel(scope: CategoryScope, data: FinanceData): String = when (scope) {
    CategoryScope.All -> "Todas"
    CategoryScope.None -> "Sem categoria"
    is CategoryScope.Category -> data.categoryById[scope.id]?.name ?: "Todas"
    is CategoryScope.Group -> data.groupById[scope.id]?.name ?: "Todas"
}

/** Resumo do filtro: despesas e receitas do mês (cada uma só quando há) e, no grupo, o detalhamento. */
@Composable
private fun ScopeSummaryCard(title: String, s: ScopeSummary, hidden: Boolean) {
    val sem = LocalSemantic.current
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (s.expenseCents == 0L && s.incomeCents == 0L) {
                Text("Sem receitas nem despesas no mês", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.expenseCents != 0L) SummaryLine("Despesas", s.expenseCents, sem.expense, hidden)
            if (s.incomeCents != 0L) SummaryLine("Receitas", s.incomeCents, sem.income, hidden)
            s.children.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.category?.name ?: "Sem categoria", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    MoneyText(c.totalCents, hidden = hidden, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, cents: Long, color: androidx.compose.ui.graphics.Color, hidden: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        MoneyText(cents, hidden = hidden, color = color, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Seletor do filtro: "Todas", "Sem categoria", cada grupo com suas categorias recuadas e as
 * categorias sem grupo. Com o tipo "Todas", mostra despesas e receitas separadas por título.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopePickerSheet(
    data: FinanceData, type: TxType?, current: CategoryScope, onPick: (CategoryScope) -> Unit, onDismiss: () -> Unit,
) {
    val types = when (type) {
        TxType.EXPENSE -> listOf(CategoryType.EXPENSE)
        TxType.INCOME -> listOf(CategoryType.INCOME)
        else -> listOf(CategoryType.EXPENSE, CategoryType.INCOME)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item { ScopeOption("Todas", null, null, 0, current == CategoryScope.All) { onPick(CategoryScope.All) } }
            item { ScopeOption("Sem categoria", "dots", Reports.UNCATEGORIZED_COLOR, 0, current == CategoryScope.None) { onPick(CategoryScope.None) } }
            types.forEach { ct ->
                if (types.size > 1) {
                    item(key = "t:$ct") {
                        Text(
                            if (ct == CategoryType.EXPENSE) "Despesas" else "Receitas",
                            Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val cats = data.categories.filter { it.type == ct }
                val byGroup = cats.groupBy { data.groupOf(it)?.id }
                data.groups.filter { it.type == ct }.sortedBy { it.order }.forEach { g ->
                    val scope = CategoryScope.Group(g.id)
                    item(key = "g:${g.id}") { ScopeOption(g.name, g.icon, g.colorArgb, 0, current == scope, "Grupo") { onPick(scope) } }
                    byGroup[g.id].orEmpty().forEach { c ->
                        val cs = CategoryScope.Category(c.id)
                        item(key = "c:${c.id}") { ScopeOption(c.name, c.icon, c.colorArgb, 1, current == cs) { onPick(cs) } }
                    }
                }
                byGroup[null].orEmpty().forEach { c ->
                    val cs = CategoryScope.Category(c.id)
                    item(key = "c:${c.id}") { ScopeOption(c.name, c.icon, c.colorArgb, 0, current == cs) { onPick(cs) } }
                }
            }
        }
    }
}

@Composable
private fun ScopeOption(
    label: String, icon: String?, color: Long?, indent: Int, selected: Boolean, hint: String? = null, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp).padding(start = (20 + indent * 28).dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && color != null) {
            IconBadge(icon, color, size = 32.dp)
            Spacer(Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = if (indent == 0 && hint != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}
