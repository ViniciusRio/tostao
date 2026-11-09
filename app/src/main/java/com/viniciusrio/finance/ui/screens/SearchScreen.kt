package com.viniciusrio.finance.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.viniciusrio.finance.domain.FinanceData
import com.viniciusrio.finance.domain.Periods
import com.viniciusrio.finance.domain.TransactionQueries
import com.viniciusrio.finance.ui.MainViewModel
import com.viniciusrio.finance.ui.Route
import com.viniciusrio.finance.ui.components.BackTopBar
import com.viniciusrio.finance.ui.components.EmptyState
import com.viniciusrio.finance.ui.components.TransactionRow

@Composable
fun SearchScreen(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    var q by rememberSaveable { mutableStateOf("") }
    val results = remember(data, q) { TransactionQueries.search(data, q) }
    val focus = remember { FocusRequester() }
    val today = vm.today()
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Buscar", vm::back)
        OutlinedTextField(
            q, { q = it }, singleLine = true,
            label = { Text("Título, observação, categoria ou valor") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
        )
        when {
            q.isBlank() -> EmptyState("Busque em todos os meses", "Ex.: \"farmacia\", \"Mercado\" ou \"45,90\".")
            results.isEmpty() -> EmptyState("Nada encontrado", "Nenhuma transação corresponde a \"$q\".")
            else -> LazyColumn {
                items(results, key = { it.id }) { t ->
                    val date = Periods.shortDate(Periods.localDate(t.dateTimeMillis, vm.zone), today)
                    TransactionRow(t, data, hidden, subtitle = date + " · " + (data.accountById[t.accountId]?.name ?: "")) {
                        vm.navigate(Route.TxForm(t.type, t.id))
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
