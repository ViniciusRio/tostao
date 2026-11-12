package com.viniciusrio.tostao.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.screens.AccountFormScreen
import com.viniciusrio.tostao.ui.screens.AccountsScreen
import com.viniciusrio.tostao.ui.screens.BudgetFormScreen
import com.viniciusrio.tostao.ui.screens.CategoryGroupFormScreen
import com.viniciusrio.tostao.ui.screens.CategoriesScreen
import com.viniciusrio.tostao.ui.screens.CategoryTransactionsScreen
import com.viniciusrio.tostao.ui.screens.CategoryFormScreen
import com.viniciusrio.tostao.ui.screens.HomeScreen
import com.viniciusrio.tostao.ui.screens.PlanningScreen
import com.viniciusrio.tostao.ui.screens.ReportsScreen
import com.viniciusrio.tostao.ui.screens.RuleFormScreen
import com.viniciusrio.tostao.ui.screens.SearchScreen
import com.viniciusrio.tostao.ui.screens.DataOpOverlay
import com.viniciusrio.tostao.ui.screens.SettingsScreen
import com.viniciusrio.tostao.ui.screens.TransactionFormScreen
import com.viniciusrio.tostao.ui.screens.TransactionsScreen
import com.viniciusrio.tostao.ui.theme.LocalSemantic

private data class TabItem(val route: Route.Tab, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Route.Home, "Início", Icons.Outlined.Home),
    TabItem(Route.Transactions(), "Transações", Icons.Outlined.ReceiptLong),
    TabItem(Route.Planning, "Planejamento", Icons.Outlined.EventNote),
    TabItem(Route.Reports, "Relatórios", Icons.Outlined.BarChart),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceRoot(vm: MainViewModel) {
    val data by vm.data.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    val route = vm.backStack.last()

    BackHandler(enabled = vm.backStack.size > 1) { vm.back() }

    LaunchedEffect(message?.id) {
        val m = message ?: return@LaunchedEffect
        snackbar.currentSnackbarData?.dismiss()
        val r = snackbar.showSnackbar(
            m.text, actionLabel = if (m.undo != null) "Desfazer" else null,
            duration = if (m.undo != null) SnackbarDuration.Long else SnackbarDuration.Short,
            withDismissAction = m.undo != null,
        )
        if (r == SnackbarResult.ActionPerformed) vm.runUndo(m) else vm.consumeMessage(m.id)
    }

    val d = data
    val openAdd: () -> Unit = {
        if (d != null && d.accounts.isEmpty()) {
            vm.say("Crie uma conta antes de registrar transações")
            vm.navigate(Route.AccountForm())
        } else {
            showAdd = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route is Route.Tab) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route::class == t.route::class,
                            onClick = { vm.navigate(t.route) },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Planejamento tem o próprio "Novo planejamento"; o + de transação ali só confundia.
            if (route is Route.Tab && route != Route.Reports && route != Route.Planning) {
                FloatingActionButton(
                    onClick = openAdd,
                    modifier = Modifier.semantics { contentDescription = "Adicionar transação" },
                ) { Icon(Icons.Outlined.Add, null) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (d == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when (route) {
                    Route.Home -> HomeScreen(vm, d, openAdd)
                    is Route.Transactions -> TransactionsScreen(vm, d, route.accountId)
                    Route.Planning -> PlanningScreen(vm, d)
                    Route.Reports -> ReportsScreen(vm, d)
                    is Route.TxForm -> TransactionFormScreen(vm, d, route.type, route.editId)
                    Route.Accounts -> AccountsScreen(vm, d)
                    is Route.AccountForm -> AccountFormScreen(vm, d, route.id)
                    Route.Categories -> CategoriesScreen(vm, d)
                    is Route.CategoryForm -> CategoryFormScreen(vm, d, route.id, route.type)
                    is Route.GroupForm -> CategoryGroupFormScreen(vm, d, route.id, route.type)
                    is Route.RuleForm -> RuleFormScreen(vm, d, route.id)
                    is Route.BudgetForm -> BudgetFormScreen(vm, d, route.id)
                    is Route.CategoryTransactions -> CategoryTransactionsScreen(vm, d, route)
                    Route.Search -> SearchScreen(vm, d)
                    Route.Settings -> SettingsScreen(vm, d)
                }
            }
        }
    }

    if (d != null) DataOpOverlay(vm, d)

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            val sem = LocalSemantic.current
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Adicionar", style = MaterialTheme.typography.titleLarge)
                AddOption("Despesa", Icons.Outlined.CallMade, sem.expense) { showAdd = false; vm.navigate(Route.TxForm(TxType.EXPENSE)) }
                AddOption("Receita", Icons.Outlined.CallReceived, sem.income) { showAdd = false; vm.navigate(Route.TxForm(TxType.INCOME)) }
                AddOption("Transferência entre contas", Icons.Outlined.SwapHoriz, sem.transfer) {
                    showAdd = false; vm.navigate(Route.TxForm(TxType.TRANSFER))
                }
            }
        }
    }
}

@Composable
private fun AddOption(label: String, icon: ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
