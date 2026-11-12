package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.CategoryScope
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.Charts
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.Reports
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.DonutCenter
import com.viniciusrio.tostao.ui.components.DonutChart
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.SegmentedChoice
import com.viniciusrio.tostao.ui.components.TrendBars
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import java.time.YearMonth

@Composable
fun ReportsScreen(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    val minute by vm.minute.collectAsState()
    val today = remember(minute) { vm.today() }
    // Mês, tipo e conta vivem no ViewModel para sobreviver à ida e volta de Transações da categoria.
    val chosenMonth by vm.reportMonth.collectAsState()
    val month = chosenMonth ?: YearMonth.from(today)
    val kind by vm.reportKind.collectAsState()
    val chosenAccount by vm.reportAccount.collectAsState()
    // Uma conta excluída deixa de valer na hora, e o filtro volta para "Todas as contas".
    val accountId = chosenAccount?.takeIf { it in data.accountById }
    LaunchedEffect(chosenAccount, accountId) {
        if (chosenAccount != null && accountId == null) vm.reportAccount.value = null
    }
    val setMonth: (YearMonth) -> Unit = { vm.reportMonth.value = it }
    val asOf by vm.reportAsOf.collectAsState()
    val report = remember(data, month, asOf, accountId) { Reports.month(data, month, vm.zone, asOf, accountId) }
    val trend = remember(data, month, asOf, accountId) { Charts.trend(data, month, vm.zone, asOf, accountId = accountId) }
    val sem = LocalSemantic.current
    val slices = if (kind == CategoryType.EXPENSE) report.expenseByCategory else report.incomeByCategory
    val groupSlices = if (kind == CategoryType.EXPENSE) report.expenseByGroup else report.incomeByGroup
    // "Por grupo" só existe quando há grupo do tipo; a escolha fica guardada para quando voltar a haver.
    val hasGroups = data.groups.any { it.type == kind }
    val wantsGroups by vm.reportByGroup.collectAsState()
    val byGroup = wantsGroups && hasGroups
    val pie = remember(slices, groupSlices, byGroup) {
        Charts.pie(if (byGroup) Charts.groupItems(groupSlices) else Charts.categoryItems(slices))
    }
    // Seleção: só a fatia do gráfico. A linha da lista abre Transações da categoria e não seleciona.
    var sliceSel by remember(month, kind, accountId, byGroup) { mutableStateOf<Int?>(null) }
    val highlighted = Charts.keysOf(pie, sliceSel)
    var expanded by remember(month, kind, accountId) { mutableStateOf(setOf<String>()) }
    val kindLabel = if (kind == CategoryType.EXPENSE) "despesas" else "receitas"
    val center = sliceSel?.let { pie.getOrNull(it) }?.let { DonutCenter(it.label, it.cents, it.percent) }
    val open = { scope: CategoryScope ->
        // Fixa o mês exibido: se o dia virar com a tela aberta, a volta mantém este mês.
        vm.reportMonth.value = month
        vm.navigate(Route.CategoryTransactions(scope.serialize(), kind, month.toString(), accountId))
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(accountId == null, { vm.reportAccount.value = null }, { Text("Todas as contas") }) }
                items(data.accounts, key = { it.id }) { a ->
                    FilterChip(accountId == a.id, { vm.reportAccount.value = if (accountId == a.id) null else a.id }, { Text(a.name) })
                }
            }
        }
        item { MonthSwitcher(month, Periods.monthLabel(month, today), onChange = setMonth) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Receitas", report.incomeCents, sem.income, hidden, Modifier.weight(1f))
                Stat("Despesas", -report.expenseCents, sem.expense, hidden, Modifier.weight(1f))
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Resultado do mês", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        MoneyText(report.resultCents, signed = true, hidden = hidden, colorBySign = true, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "${report.transactionCount} transações · despesas: ${if (hidden) "comparação oculta" else report.deltaText()}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionHeader("Últimos 6 meses") }
        item {
            TrendBars(trend, month, hidden, onMonth = setMonth) { Periods.monthLabel(it, today) }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            SegmentedChoice(listOf(CategoryType.EXPENSE to "Despesas", CategoryType.INCOME to "Receitas"), kind, { vm.reportKind.value = it })
        }
        if (hasGroups) {
            item {
                SegmentedChoice(listOf(false to "Por categoria", true to "Por grupo"), byGroup, { vm.reportByGroup.value = it })
            }
        }
        item { SectionHeader(if (byGroup) "Por grupo" else "Por categoria") }
        if (pie.isEmpty()) {
            item { EmptyState("Sem dados", "Nenhuma ${if (kind == CategoryType.EXPENSE) "despesa" else "receita"} neste mês.") }
        } else {
            item {
                DonutChart(
                    pie, sliceSel, center,
                    onTapSlice = { i -> sliceSel = if (i == null || i == sliceSel) null else i },
                    totalLabel = if (kind == CategoryType.EXPENSE) "Despesas" else "Receitas",
                    hidden = hidden, description = Charts.describe(kindLabel, pie),
                )
                Text(
                    if (byGroup) {
                        "Toque numa fatia para ver o valor, num item para ver as transações ou na seta para abrir o grupo."
                    } else {
                        "Toque numa fatia para ver o valor ou numa categoria para ver as transações."
                    },
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (byGroup) {
            groupSlices.forEach { g ->
                val gid = (g.key as? CategoryScope.Group)?.id
                val isOpen = gid != null && gid in expanded
                item(key = g.key.serialize()) {
                    ReportRow(
                        g.label, g.icon, g.colorArgb, g.totalCents, g.percent, g.key in highlighted, hidden,
                        onOpen = { open(g.key) },
                        expand = gid?.let { id ->
                            ExpandAction(isOpen) { expanded = if (isOpen) expanded - id else expanded + id }
                        },
                    )
                }
                if (isOpen) {
                    g.children.forEach { c ->
                        val key = c.category?.let { CategoryScope.Category(it.id) } ?: CategoryScope.None
                        item(key = "${g.key.serialize()}/${key.serialize()}") {
                            ReportRow(
                                c.category?.name ?: "Sem categoria", c.category?.icon ?: Reports.UNCATEGORIZED_ICON,
                                c.category?.colorArgb ?: Reports.UNCATEGORIZED_COLOR, c.totalCents, c.percent,
                                selected = false, hidden = hidden, onOpen = { open(key) }, indent = true,
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(slices, key = { _, s -> s.category?.id ?: "none" }) { _, s ->
                val key = s.category?.let { CategoryScope.Category(it.id) } ?: CategoryScope.None
                ReportRow(
                    s.category?.name ?: "Sem categoria", s.category?.icon ?: Reports.UNCATEGORIZED_ICON,
                    s.category?.colorArgb ?: Reports.UNCATEGORIZED_COLOR, s.totalCents, s.percent,
                    key in highlighted, hidden, onOpen = { open(key) },
                )
            }
        }
    }
}

/** Botão de expandir/recolher um grupo, separado do toque na linha. */
private class ExpandAction(val expanded: Boolean, val toggle: () -> Unit)

/**
 * Linha da lista de Relatórios: o toque abre as transações do item (nunca seleciona a pizza).
 * Num grupo, [expand] mostra um botão próprio para abrir o detalhamento.
 */
@Composable
private fun ReportRow(
    name: String, icon: String, colorArgb: Long, cents: Long, percent: Int, selected: Boolean, hidden: Boolean,
    onOpen: () -> Unit, expand: ExpandAction? = null, indent: Boolean = false,
) {
    val color = Color(colorArgb)
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(start = if (indent) 36.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f)
                .clickable(role = Role.Button, onClick = onOpen)
                .heightIn(min = 56.dp).padding(start = 20.dp, end = if (expand != null) 4.dp else 20.dp, top = 8.dp, bottom = 8.dp)
                .clearAndSetSemantics {
                    contentDescription = "$name, ${if (hidden) "valor oculto" else Money.format(cents)}, $percent%"
                    this.selected = selected
                    onClick("Ver transações") { onOpen(); true }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, colorArgb, size = if (indent) 30.dp else 36.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(name, Modifier.weight(1f), style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                    MoneyText(cents, hidden = hidden)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(percent.coerceIn(1, 100) / 100f).background(color))
                    }
                    Text("  $percent%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (expand != null) {
            IconButton(onClick = expand.toggle, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    if (expand.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    if (expand.expanded) "Recolher $name" else "Expandir $name",
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, cents: Long, color: Color, hidden: Boolean, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(cents, signed = true, hidden = hidden, color = color, style = MaterialTheme.typography.titleLarge)
        }
    }
}
