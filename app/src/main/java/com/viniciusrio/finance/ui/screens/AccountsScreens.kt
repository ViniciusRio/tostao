package com.viniciusrio.finance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.viniciusrio.finance.domain.Account
import com.viniciusrio.finance.domain.Balances
import com.viniciusrio.finance.domain.FinanceData
import com.viniciusrio.finance.domain.Money
import com.viniciusrio.finance.ui.MainViewModel
import com.viniciusrio.finance.ui.Route
import com.viniciusrio.finance.ui.components.AmountField
import com.viniciusrio.finance.ui.components.BackTopBar
import com.viniciusrio.finance.ui.components.ColorPicker
import com.viniciusrio.finance.ui.components.ConfirmDialog
import com.viniciusrio.finance.ui.components.EmptyState
import com.viniciusrio.finance.ui.components.IconBadge
import com.viniciusrio.finance.ui.components.IconPicker
import com.viniciusrio.finance.ui.components.MoneyText
import com.viniciusrio.finance.ui.components.SectionHeader

@Composable
fun AccountsScreen(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    val minute by vm.minute.collectAsState()
    val summary = remember(data, minute) { Balances.compute(data, vm.nowMillis()) }
    var reorder by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Contas", vm::back) {
            if (data.accounts.size > 1) {
                IconButton(onClick = { reorder = !reorder }) {
                    Icon(Icons.Outlined.SwapVert, if (reorder) "Concluir reordenação" else "Reordenar contas")
                }
            }
            IconButton(onClick = { vm.navigate(Route.AccountForm()) }) { Icon(Icons.Outlined.Add, "Nova conta") }
        }
        if (data.accounts.isEmpty()) {
            EmptyState("Nenhuma conta", "Crie a primeira conta para começar.", "Criar conta") { vm.navigate(Route.AccountForm()) }
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCard("No saldo total", summary.includedTotalCents, hidden, Modifier.weight(1f))
            TotalCard("Fora do total", summary.excludedTotalCents, hidden, Modifier.weight(1f))
        }
        if (reorder) {
            Text(
                "Use as setas para mudar a ordem. Ela vale em todas as listas.",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
            )
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            itemsIndexed(summary.perAccount, key = { _, it -> it.account.id }) { i, ab ->
                val a = ab.account
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = !reorder, role = Role.Button, onClickLabel = "Editar conta") {
                            vm.navigate(Route.AccountForm(a.id))
                        }
                        .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(a.icon, a.colorArgb)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = MaterialTheme.typography.bodyLarge)
                        if (!a.includeInTotal) {
                            Text("Fora do saldo total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (reorder) {
                        val list = summary.perAccount.map { it.account }
                        IconButton(enabled = i > 0, onClick = { vm.reorderAccounts(list.move(i, i - 1)) }) {
                            Icon(Icons.Outlined.ArrowUpward, "Mover ${a.name} para cima")
                        }
                        IconButton(enabled = i < list.lastIndex, onClick = { vm.reorderAccounts(list.move(i, i + 1)) }) {
                            Icon(Icons.Outlined.ArrowDownward, "Mover ${a.name} para baixo")
                        }
                    } else {
                        MoneyText(
                            ab.balanceCents, hidden = hidden, colorBySign = true,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.move(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }

@Composable
private fun TotalCard(label: String, cents: Long, hidden: Boolean, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(cents, hidden = hidden, colorBySign = true, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AccountFormScreen(vm: MainViewModel, data: FinanceData, id: String?) {
    val existing = remember(id) { id?.let { data.accountById[it] } }
    if (id != null && existing == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var initial by rememberSaveable { mutableStateOf(existing?.let { Money.plain(it.initialBalanceCents) } ?: "") }
    var color by rememberSaveable { mutableStateOf(existing?.colorArgb ?: 0xFF2C8FC9) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "bank") }
    var include by rememberSaveable { mutableStateOf(existing?.includeInTotal ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicate = data.accounts.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }
    val initialCents = parseSigned(initial)
    val valid = trimmed.length in 1..40 && !duplicate && initialCents != null
    val balance = existing?.let { e -> Balances.compute(data, vm.nowMillis()).perAccount.firstOrNull { it.account.id == e.id }?.balanceCents }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Nova conta" else "Editar conta", vm::back) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir conta") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (existing != null && balance != null) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Saldo atual", style = MaterialTheme.typography.labelLarge)
                        MoneyText(balance, style = MaterialTheme.typography.headlineSmall, colorBySign = true, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { adjusting = true }) { Text("Ajustar saldo") }
                            OutlinedButton(onClick = { vm.navigate(Route.Transactions(existing.id)) }) { Text("Ver transações") }
                        }
                    }
                }
            }
            OutlinedTextField(
                name, { name = it.take(40) }, label = { Text("Nome da conta") }, singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) ({ Text("Já existe uma conta com esse nome") }) else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AmountField(
                initial, { initial = it }, "Saldo inicial", Modifier.padding(horizontal = 20.dp), allowNegative = true,
            )
            Text(
                "Use o saldo que a conta tinha antes da primeira transação registrada aqui. Pode ser negativo (ex.: cartão).",
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Switch) { include = !include }
                    .padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Incluir no saldo total")
                    Text("Desligue para investimentos ou contas que não quer somar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = include, onCheckedChange = null)
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
                vm.saveAccount(
                    Account(existing?.id ?: vm.newId(), trimmed, color, icon, initialCents!!, include, existing?.order ?: 0),
                    isNew = existing == null,
                )
                vm.back()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }

    if (confirmDelete && existing != null) {
        val txCount = data.transactions.count { it.accountId == existing.id || it.toAccountId == existing.id }
        val transfers = data.transactions.count { it.toAccountId != null && (it.accountId == existing.id || it.toAccountId == existing.id) }
        val rules = data.rules.count { it.accountId == existing.id }
        val body = buildString {
            append("Serão excluídas $txCount transações")
            if (transfers > 0) append(" (incluindo $transfers transferências; o saldo da outra conta de cada uma muda)")
            append(" e $rules planejamentos desta conta. Você poderá desfazer logo em seguida.")
        }
        ConfirmDialog(
            "Excluir \"${existing.name}\"?", body, "Excluir",
            onConfirm = { vm.deleteAccount(existing.id); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }

    if (adjusting && existing != null) {
        var target by remember { mutableStateOf(balance?.let { Money.plain(it) } ?: "") }
        val t = parseSigned(target)
        AlertDialog(
            onDismissRequest = { adjusting = false },
            title = { Text("Ajustar saldo") },
            text = {
                Column {
                    Text("Informe o saldo real. A diferença vira uma transação \"Ajuste de saldo\", que não entra em relatórios.")
                    AmountField(target, { target = it }, "Saldo real", allowNegative = true)
                }
            },
            confirmButton = {
                TextButton(enabled = t != null, onClick = { vm.adjustBalance(existing, t!!); adjusting = false }) { Text("Ajustar") }
            },
            dismissButton = { TextButton(onClick = { adjusting = false }) { Text("Cancelar") } },
        )
    }
}

/** Aceita "-" à frente para saldos negativos. Vazio vale zero. */
fun parseSigned(text: String): Long? {
    val t = text.trim()
    if (t.isEmpty() || t == "-") return 0L
    val neg = t.startsWith("-")
    val v = Money.parse(t.removePrefix("-")) ?: return null
    return if (neg) -v else v
}
