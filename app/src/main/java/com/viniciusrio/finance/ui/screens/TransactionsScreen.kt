package com.viniciusrio.finance.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import com.viniciusrio.finance.domain.FinanceData
import com.viniciusrio.finance.domain.Periods
import com.viniciusrio.finance.domain.TransactionFilter
import com.viniciusrio.finance.domain.TransactionQueries
import com.viniciusrio.finance.domain.TxType
import com.viniciusrio.finance.ui.MainViewModel
import com.viniciusrio.finance.ui.Route
import com.viniciusrio.finance.ui.components.EmptyState
import com.viniciusrio.finance.ui.components.MoneyText
import com.viniciusrio.finance.ui.components.TransactionRow
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
    val now = vm.nowMillis()
    val filtered = remember(data, month, type, accountId, minute) {
        TransactionQueries.filtered(data, TransactionFilter(month, type, accountId), vm.zone, now)
    }
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
            item { FilterChip(type == TxType.TRANSFER, { type = TxType.TRANSFER }, { Text("Transferências") }) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(accountId == null, { accountId = null }, { Text("Todas as contas") }) }
            items(data.accounts, key = { it.id }) { a ->
                FilterChip(accountId == a.id, { accountId = if (accountId == a.id) null else a.id }, { Text(a.name) })
            }
        }
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
}
