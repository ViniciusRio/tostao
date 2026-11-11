package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.TransactionQueries
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.BackTopBar
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.TransactionRow
import java.time.YearMonth

/** Transações de uma categoria no mês, com as mesmas regras (e o mesmo total) da linha de Relatórios. */
@Composable
fun CategoryTransactionsScreen(vm: MainViewModel, data: FinanceData, route: Route.CategoryTransactions) {
    val hidden by vm.hideValues.collectAsState()
    val minute by vm.minute.collectAsState()
    val today = remember(minute) { vm.today() }
    // O mês desta tela é local: trocá-lo aqui não altera Relatórios.
    var monthText by rememberSaveable { mutableStateOf(route.month) }
    val month = YearMonth.parse(monthText)
    val category = route.categoryId?.let { data.categoryById[it] }
    val removed = route.categoryId != null && category == null

    // Só uma categoria real pode deixar de existir; "Sem categoria" nunca é expulsa.
    LaunchedEffect(removed) {
        if (removed && vm.backStack.lastOrNull() == route) vm.back()
    }
    if (removed) return

    val name = category?.name ?: "Sem categoria"
    val txs = remember(data, route, month, minute) {
        TransactionQueries.byCategory(data, route.categoryId, route.type, month, vm.zone, vm.nowMillis(), route.accountId)
    }
    val groups = remember(txs, today) { TransactionQueries.groupByDay(txs, vm.zone, today) }
    val total = txs.sumOf { it.amountCents }
    val count = txs.size
    val countText = if (count == 1) "1 transação" else "$count transações"
    val noun = if (route.type == CategoryType.EXPENSE) "despesa" else "receita"

    Column(Modifier.fillMaxSize()) {
        BackTopBar(name, vm::back)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                        .clearAndSetSemantics {
                            contentDescription = "$name, ${if (hidden) "valor oculto" else Money.format(total)}, $countText"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(category?.icon ?: "dots", category?.colorArgb ?: 0xFF7D7D85, size = 48.dp)
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            countText, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyText(total, hidden = hidden, style = MaterialTheme.typography.titleLarge)
                }
            }
            item { MonthSwitcher(month, Periods.monthLabel(month, today)) { monthText = it.toString() } }
            if (groups.isEmpty()) {
                item { EmptyState("Nada neste mês", "Nenhuma $noun em $name neste mês.") }
            }
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
                        // Itens do mesmo tipo: subtotal sem sinal, como o total e a linha do relatório.
                        MoneyText(
                            g.items.sumOf { it.amountCents }, hidden = hidden, style = MaterialTheme.typography.labelLarge,
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
