package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Balances
import com.viniciusrio.tostao.domain.DueItem
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.Planned
import com.viniciusrio.tostao.domain.Reports
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.TransactionRow
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import java.time.YearMonth

@Composable
fun HomeScreen(vm: MainViewModel, data: FinanceData, onAdd: () -> Unit) {
    val hidden by vm.hideValues.collectAsState()
    val showReminder by vm.showBackupReminder.collectAsState()
    val backupState by vm.backupState.collectAsState()
    val minute by vm.minute.collectAsState()
    val today = remember(minute) { vm.today() }
    val now = vm.nowMillis()
    val month = YearMonth.from(today)
    val balances = remember(data, minute) { Balances.compute(data, now) }
    val report = remember(data, month, minute) { Reports.month(data, month, vm.zone, now) }
    val planned = remember(data, today) { Planned.overview(data.rules, today) }
    val forecast = remember(data, minute) {
        Planned.forecastEndOfMonth(data, today, now, Periods.monthRange(month, vm.zone).last)
    }
    val recent = remember(data, minute) {
        data.transactions.filter { it.dateTimeMillis <= now }.sortedByDescending { it.dateTimeMillis }.take(5)
    }
    val sem = LocalSemantic.current

    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Periods.monthLabel(month, today), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = vm::toggleHide) {
                    Icon(
                        if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Mostrar valores" else "Ocultar valores",
                    )
                }
                IconButton(onClick = { vm.navigate(Route.Search) }) { Icon(Icons.Outlined.Search, "Buscar transações") }
                IconButton(onClick = { vm.navigate(Route.Accounts) }) { Icon(Icons.Outlined.AccountBalanceWallet, "Contas") }
                IconButton(onClick = { vm.navigate(Route.Settings) }) { Icon(Icons.Outlined.Settings, "Configurações") }
            }
        }

        if (data.accounts.isEmpty()) {
            item {
                EmptyState(
                    "Cada vintém no lugar",
                    "Comece criando uma conta: cadastre onde seu dinheiro está (banco, carteira, cartão) com o saldo atual. Depois é só registrar receitas e despesas.",
                    "Criar conta",
                ) { vm.navigate(Route.AccountForm()) }
            }
            return@LazyColumn
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(role = Role.Button, onClickLabel = "Ver contas") { vm.navigate(Route.Accounts) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Saldo total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    MoneyText(
                        balances.includedTotalCents, hidden = hidden,
                        style = MaterialTheme.typography.displaySmall,
                        color = if (balances.includedTotalCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Previsto no fim do mês: ", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        MoneyText(
                            forecast, hidden = hidden, style = MaterialTheme.typography.bodyMedium,
                            color = if (forecast < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        "${data.accounts.size} ${if (data.accounts.size == 1) "conta" else "contas"} · toque para detalhar",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (showReminder) {
            item { BackupReminderCard(vm, backupState.lastBackupAtMillis) }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Receitas do mês", report.incomeCents, sem.income, hidden, Modifier.weight(1f)) {
                    vm.navigate(Route.Reports)
                }
                SummaryCard("Despesas do mês", -report.expenseCents, sem.expense, hidden, Modifier.weight(1f)) {
                    vm.navigate(Route.Reports)
                }
            }
        }

        if (planned.overdue.isNotEmpty()) {
            item {
                SectionHeader("Atrasados") {
                    MoneyText(
                        -planned.total(planned.overdue, TxType.EXPENSE) + planned.total(planned.overdue, TxType.INCOME),
                        hidden = hidden, style = MaterialTheme.typography.bodyMedium, color = sem.expense,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
            items(planned.overdue, key = { "o" + it.rule.id }) { DueCard(vm, data, it, hidden, overdue = true) }
        }
        if (planned.upcoming.isNotEmpty()) {
            item { SectionHeader("Próximos 30 dias") }
            items(planned.upcoming, key = { "u" + it.rule.id }) { DueCard(vm, data, it, hidden, overdue = false) }
        }

        item {
            SectionHeader("Recentes") {
                TextButton(onClick = { vm.navigate(Route.Transactions()) }) { Text("Ver todas") }
            }
        }
        if (recent.isEmpty()) {
            item {
                EmptyState("Nenhuma transação ainda", "Toque em + para registrar sua primeira despesa ou receita.", "Adicionar", onAdd)
            }
        } else {
            items(recent, key = { it.id }) { t ->
                TransactionRow(t, data, hidden) { vm.navigate(Route.TxForm(t.type, t.id)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String, cents: Long, color: androidx.compose.ui.graphics.Color, hidden: Boolean,
    modifier: Modifier, onClick: () -> Unit,
) {
    Card(
        modifier.clickable(role = Role.Button, onClickLabel = "Ver relatório", onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            MoneyText(cents, signed = true, hidden = hidden, color = color, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun DueCard(vm: MainViewModel, data: FinanceData, item: DueItem, hidden: Boolean, overdue: Boolean) {
    val r = item.rule
    val cat = r.categoryId?.let { data.categoryById[it] }
    val sem = LocalSemantic.current
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(role = Role.Button, onClickLabel = "Editar planejamento") { vm.navigate(Route.RuleForm(r.id)) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(cat?.icon ?: "bill", cat?.colorArgb ?: 0xFF5D7A8C)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${Periods.shortDate(item.dueDate, vm.today())} · ${Planned.relativeDue(item.daysFromToday)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) sem.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoneyText(
                    if (r.type == TxType.INCOME) r.amountCents else -r.amountCents, signed = true, hidden = hidden,
                    color = if (r.type == TxType.INCOME) sem.income else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.skipRule(r) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Pular") }
                OutlinedButton(onClick = { vm.payRule(r) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (r.type == TxType.INCOME) "Receber" else "Pagar")
                }
            }
        }
    }
}
