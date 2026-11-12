package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Limits
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.Transaction
import com.viniciusrio.tostao.domain.TransactionQueries
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.components.AccountChips
import com.viniciusrio.tostao.ui.components.AmountField
import com.viniciusrio.tostao.ui.components.BackTopBar
import com.viniciusrio.tostao.ui.components.GroupedCategoryChips
import com.viniciusrio.tostao.ui.components.ConfirmDialog
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.SegmentedChoice
import java.time.Instant
import java.time.LocalDateTime

@Composable
fun TransactionFormScreen(vm: MainViewModel, data: FinanceData, initialType: TxType, editId: String?) {
    val existing = remember(editId) { editId?.let { id -> data.transactions.firstOrNull { it.id == id } } }
    val start = remember(editId) {
        existing?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it.dateTimeMillis), vm.zone) }
            ?: LocalDateTime.ofInstant(Instant.ofEpochMilli(vm.nowMillis()), vm.zone).withNano(0)
    }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: initialType) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.plain(it.amountCents) } ?: "") }
    var accountId by rememberSaveable {
        mutableStateOf(existing?.accountId ?: vm.lastAccountId()?.takeIf { it in data.accountById } ?: data.accounts.firstOrNull()?.id)
    }
    var toAccountId by rememberSaveable { mutableStateOf(existing?.toAccountId) }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(start.toLocalDate()) }
    var time by remember { mutableStateOf(start.toLocalTime()) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Um toque só: um segundo toque em Salvar antes de a tela fechar não pode gravar de novo.
    var saving by remember { mutableStateOf(false) }
    // Id da nova transação fixado na primeira abertura e mantido na rotação: se Salvar for tocado
    // de novo (ex.: depois de girar a tela durante a gravação), o upsert regrava a mesma transação.
    val newId = rememberSaveable { existing?.id ?: vm.newId() }
    val failures by vm.saveFailures.collectAsState()
    LaunchedEffect(failures) { saving = false }
    var addingCategory by rememberSaveable { mutableStateOf(false) }
    // Categorias criadas pelo diálogo ("TIPO:id"): valem como seleção mesmo antes de o banco
    // devolvê-las na lista, inclusive se o usuário criar várias alternando entre despesa e receita.
    var createdCategories by rememberSaveable { mutableStateOf(listOf<String>()) }
    val focus = remember { FocusRequester() }

    if (editId != null && existing == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }

    val catType = if (type == TxType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val recent = remember(data) { TransactionQueries.recentCategoryIds(data) }
    val sections = remember(data, catType, recent) { TransactionQueries.categorySections(data, catType, recent) }
    val cats = sections.flatMap { it.categories }
    val effectiveCategory = categoryId?.takeIf { id ->
        cats.any { it.id == id } || "${catType.name}:$id" in createdCategories
    }

    val cents = Money.parseSum(amount)
    val sameAccount = type == TxType.TRANSFER && toAccountId != null && toAccountId == accountId
    val valid = cents != null && cents > 0 && accountId != null &&
        (type != TxType.TRANSFER || (toAccountId != null && !sameAccount))

    val save = {
        if (valid && !saving) {
            saving = true
            val millis = date.atTime(time).atZone(vm.zone).toInstant().toEpochMilli()
            val t = Transaction(
                id = newId, type = type, amountCents = cents!!, accountId = accountId!!,
                toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                categoryId = if (type == TxType.TRANSFER) null else effectiveCategory,
                title = title.trim(), note = note.trim(), dateTimeMillis = millis,
                plannedRuleId = existing?.plannedRuleId, isAdjustment = existing?.isAdjustment ?: false,
            )
            vm.saveTransaction(t, existing == null)
            vm.back()
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(if (existing == null) "Nova transação" else "Editar transação", vm::back) {
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir transação") }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SegmentedChoice(
                listOf(TxType.EXPENSE to "Despesa", TxType.INCOME to "Receita", TxType.TRANSFER to "Transferir"),
                type, { type = it },
            )
            AmountField(
                amount, { amount = it }, "Valor",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp).focusRequester(focus),
                allowSum = true,
            )
            SectionHeader(if (type == TxType.TRANSFER) "De" else "Conta")
            AccountChips(data.accounts, accountId, { accountId = it })
            if (type == TxType.TRANSFER) {
                SectionHeader("Para")
                AccountChips(data.accounts, toAccountId, { toAccountId = it }, exclude = accountId)
                if (sameAccount) {
                    Text(
                        "Origem e destino não podem ser a mesma conta. Escolha outra conta de destino.",
                        Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error,
                    )
                }
                if (data.accounts.size < 2) {
                    Text(
                        "Você precisa de pelo menos duas contas para transferir.",
                        Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                SectionHeader("Categoria")
                GroupedCategoryChips(sections, setOfNotNull(effectiveCategory), onAdd = { addingCategory = true }) { id ->
                    categoryId = if (effectiveCategory == id) null else id
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                title, { title = it.take(Limits.TITLE) }, label = { Text("Título (opcional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            OutlinedTextField(
                note, { note = it.take(Limits.NOTE) }, label = { Text("Observação (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                minLines = 2,
            )
            DateTimeRow(date, time, vm.today(), { date = it }, { time = it })
            if (existing?.isAdjustment == true) {
                Text(
                    "Ajuste de saldo: conta no saldo, mas não entra em relatórios e orçamentos.",
                    Modifier.padding(20.dp), style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = save, enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text("Salvar") }
    }

    LaunchedEffect(Unit) { if (existing == null) focus.requestFocus() }

    if (addingCategory) {
        NewCategoryDialog(vm, data, catType, onDismiss = { addingCategory = false }) { id ->
            createdCategories = createdCategories + "${catType.name}:$id"
            categoryId = id
            addingCategory = false
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Excluir transação?", "Você poderá desfazer logo em seguida.", "Excluir",
            onConfirm = { vm.deleteTransaction(existing); vm.back() }, onDismiss = { confirmDelete = false },
        )
    }
}
