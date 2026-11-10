package com.viniciusrio.tostao.ui

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.viniciusrio.tostao.AppContainer
import com.viniciusrio.tostao.data.DataTransferService
import com.viniciusrio.tostao.data.FinanceRepository
import com.viniciusrio.tostao.data.TransferOutcome
import com.viniciusrio.tostao.domain.ExportPeriod
import com.viniciusrio.tostao.domain.backup.BackupCodec
import com.viniciusrio.tostao.domain.backup.BackupReminder
import com.viniciusrio.tostao.domain.backup.BackupResult
import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Balances
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.Transaction
import com.viniciusrio.tostao.domain.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Mensagem única na tela; uma nova substitui a anterior (e o desfazer dela). */
data class UiMessage(val id: Long, val text: String, val undo: (suspend () -> Unit)? = null, val epoch: Int = 0)

/** Operação de arquivo em andamento; enquanto houver uma, outra não começa. */
enum class DataOp { Backup, Reading, Restore, Export }

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repo: FinanceRepository,
    private val clock: Clock,
    private val prefs: SharedPreferences,
    private val transfer: DataTransferService,
) : ViewModel() {

    /** null enquanto o banco ainda não respondeu, para não piscar o estado vazio. */
    val data: StateFlow<FinanceData?> = repo.data
        .map<FinanceData, FinanceData?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val backStack = mutableStateListOf<Route>(Route.Home)

    /** Aba de Planejamento (0 = planejados, 1 = orçamentos), mantida ao voltar de um formulário. */
    var planningTab by androidx.compose.runtime.mutableIntStateOf(0)

    private val _hideValues = MutableStateFlow(prefs.getBoolean(KEY_HIDE, false))
    val hideValues: StateFlow<Boolean> = _hideValues.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()
    private var messageSeq = 0L

    /** Minuto atual; muda a cada minuto para recalcular saldos, atrasados e "hoje". */
    private val _minute = MutableStateFlow(clock.millis() / 60_000)
    val minute: StateFlow<Long> = _minute.asStateFlow()

    // Backup, restauração e exportação
    private val _dataOp = MutableStateFlow<DataOp?>(null)
    val dataOp: StateFlow<DataOp?> = _dataOp.asStateFlow()

    /** Backup lido e validado, à espera da confirmação do usuário. */
    private val _pendingRestore = MutableStateFlow<BackupResult.Valid?>(null)
    val pendingRestore: StateFlow<BackupResult.Valid?> = _pendingRestore.asStateFlow()

    /** Aumenta a cada restauração; desfazer de antes dela não vale mais. */
    private var dataEpoch = 0

    /** Serializa desfazer e restauração: o desfazer confere o epoch já dentro do lock, logo antes de escrever. */
    private val writeLock = Mutex()

    private val _backupState = MutableStateFlow(transfer.backupState())
    val backupState = _backupState.asStateFlow()

    private val fingerprint: StateFlow<Pair<String, Long?>?> = repo.data
        .debounce(500)
        .map { d ->
            val now = clock.millis()
            BackupCodec.fingerprint(d) to d.transactions.filter { it.dateTimeMillis <= now }.minOfOrNull { it.dateTimeMillis }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Lembrete de backup, reavaliado quando os dados mudam e a cada minuto. */
    val showBackupReminder: StateFlow<Boolean> = combine(fingerprint, _backupState, _minute) { fp, st, _ ->
        fp != null && BackupReminder.shouldShow(st, fp.first, fp.second, clock.millis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun runOp(op: DataOp, block: suspend () -> Unit) {
        if (_dataOp.value != null) return
        _dataOp.value = op
        viewModelScope.launch {
            try {
                block()
            } finally {
                _dataOp.value = null
                _backupState.value = transfer.backupState()
            }
        }
    }

    private fun report(o: TransferOutcome) = say(
        when (o) {
            is TransferOutcome.Ok -> o.message
            is TransferOutcome.Failed -> o.message
        },
    )

    fun backupTo(uri: Uri?) {
        uri ?: return
        runOp(DataOp.Backup) { report(transfer.writeBackup(uri)) }
    }

    fun readRestore(uri: Uri?) {
        uri ?: return
        runOp(DataOp.Reading) {
            when (val r = transfer.readBackup(uri)) {
                is BackupResult.Valid -> _pendingRestore.value = r
                is BackupResult.Invalid -> say(r.reason)
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore() {
        val valid = _pendingRestore.value ?: return
        _pendingRestore.value = null
        runOp(DataOp.Restore) {
            val o = writeLock.withLock {
                transfer.restore(valid).also { if (it is TransferOutcome.Ok) dataEpoch++ }
            }
            if (o is TransferOutcome.Ok) {
                backStack.clear(); backStack.add(Route.Home)
            }
            report(o)
        }
    }

    fun exportCsv(uri: Uri?, period: ExportPeriod) {
        uri ?: return
        val d = data.value ?: return
        runOp(DataOp.Export) { report(transfer.writeCsv(uri, d, period)) }
    }

    fun snoozeBackupReminder() {
        transfer.snooze(clock.millis() + BackupReminder.SNOOZE_DAYS * BackupReminder.DAY_MS)
        _backupState.value = transfer.backupState()
    }

    init {
        viewModelScope.launch {
            repo.seedOnFirstRun(prefs.getBoolean(KEY_SEEDED, false))
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000 - clock.millis() % 60_000)
                _minute.value = clock.millis() / 60_000
            }
        }
    }

    /** Última conta escolhida num formulário (não a de data mais recente). */
    fun lastAccountId(): String? = prefs.getString(KEY_LAST_ACCOUNT, null)

    val zone: ZoneId get() = clock.zone
    fun nowMillis(): Long = clock.millis()
    fun today(): LocalDate = LocalDate.now(clock)

    // Navegação
    fun navigate(r: Route) {
        if (r is Route.Tab) {
            backStack.clear(); backStack.add(r)
        } else {
            backStack.add(r)
        }
    }

    /** Chamado quando o usuário volta da tela raiz; a QuickAddActivity usa para fechar. */
    var onRootBack: (() -> Unit)? = null

    /** Gravações iniciadas por formulários; quem fecha a tela espera por elas. */
    private val pendingWrites = mutableListOf<Job>()
    private var writeFailed = false
    private val _saveFailures = MutableStateFlow(0)

    /** Conta falhas de gravação; o formulário usa para liberar o botão Salvar de novo. */
    val saveFailures: StateFlow<Int> = _saveFailures.asStateFlow()

    /** Espera as gravações pendentes; true se todas deram certo. */
    suspend fun awaitWrites(): Boolean {
        pendingWrites.toList().joinAll()
        return !writeFailed.also { writeFailed = false }
    }

    private fun track(job: Job): Job {
        pendingWrites.removeAll { it.isCompleted }
        pendingWrites += job
        return job
    }

    fun back(): Boolean {
        if (backStack.size <= 1) {
            onRootBack?.invoke()
            return false
        }
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun toggleHide() {
        val v = !_hideValues.value
        _hideValues.value = v
        prefs.edit().putBoolean(KEY_HIDE, v).apply()
    }

    fun say(text: String, undo: (suspend () -> Unit)? = null) {
        _message.value = UiMessage(++messageSeq, text, undo, dataEpoch)
    }

    fun consumeMessage(id: Long) {
        if (_message.value?.id == id) _message.value = null
    }

    fun runUndo(m: UiMessage) {
        consumeMessage(m.id)
        val u = m.undo ?: return
        viewModelScope.launch {
            writeLock.withLock { if (m.epoch == dataEpoch) u() }
        }
    }

    fun newId() = repo.newId()

    // Contas
    fun saveAccount(a: Account, isNew: Boolean) = viewModelScope.launch {
        val toSave = if (isNew) a.copy(order = repo.nextAccountOrder()) else a
        repo.saveAccount(toSave)
        say(if (isNew) "Conta criada" else "Conta salva")
    }

    fun reorderAccounts(list: List<Account>) = viewModelScope.launch { repo.reorderAccounts(list) }

    fun deleteAccount(id: String) = viewModelScope.launch {
        val d = repo.deleteAccount(id) ?: return@launch
        say("Conta \"${d.account.name}\" excluída") { repo.restoreAccount(d) }
    }

    fun adjustBalance(account: Account, targetCents: Long) = viewModelScope.launch {
        val current = Balances.compute(data.value ?: return@launch, nowMillis())
            .perAccount.firstOrNull { it.account.id == account.id }?.balanceCents ?: return@launch
        val (type, cents) = Balances.adjustment(current, targetCents) ?: run { say("O saldo já é esse"); return@launch }
        val t = Transaction(
            repo.newId(), type, cents, account.id, null, null, "Ajuste de saldo", "", nowMillis(), isAdjustment = true,
        )
        repo.saveTransaction(t)
        say("Saldo ajustado") { repo.deleteTransaction(t.id) }
    }

    // Categorias
    fun saveCategory(c: Category, isNew: Boolean) = viewModelScope.launch {
        repo.saveCategory(if (isNew) c.copy(order = repo.nextCategoryOrder()) else c)
        say(if (isNew) "Categoria criada" else "Categoria salva")
    }

    fun deleteCategory(c: Category) = viewModelScope.launch {
        repo.deleteCategory(c.id)
        say("Categoria \"${c.name}\" excluída")
    }

    // Transações
    fun saveTransaction(t: Transaction, isNew: Boolean) = track(viewModelScope.launch {
        try {
            repo.saveTransaction(t)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            writeFailed = true
            _saveFailures.value++
            say("Não foi possível salvar a transação")
            return@launch
        }
        if (!t.isAdjustment) prefs.edit().putString(KEY_LAST_ACCOUNT, t.accountId).apply()
        say(
            when {
                !isNew -> "Transação salva"
                t.type == TxType.INCOME -> "Receita registrada"
                t.type == TxType.EXPENSE -> "Despesa registrada"
                else -> "Transferência registrada"
            },
        )
    })

    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(t.id)
        say("Transação excluída") { repo.saveTransaction(t) }
    }

    // Planejados
    fun saveRule(r: PlannedRule, isNew: Boolean) = viewModelScope.launch {
        repo.saveRule(r)
        say(if (isNew) "Planejamento criado" else "Planejamento salvo")
    }

    fun deleteRule(r: PlannedRule) = viewModelScope.launch {
        repo.deleteRule(r.id)
        say("Planejamento excluído") { repo.saveRule(r) }
    }

    fun payRule(r: PlannedRule) = viewModelScope.launch {
        val a = repo.payRule(r.id, nowMillis()) ?: run { say("Este planejamento não tem ocorrência pendente"); return@launch }
        say("\"${r.title}\" pago") { undoPlanned(a) }
    }

    fun skipRule(r: PlannedRule) = viewModelScope.launch {
        val a = repo.skipRule(r.id) ?: run { say("Este planejamento não tem ocorrência pendente"); return@launch }
        say("\"${r.title}\" pulado") { undoPlanned(a) }
    }

    private suspend fun undoPlanned(a: com.viniciusrio.tostao.data.PlannedAction) {
        if (!repo.undoPlanned(a)) say("Não foi possível desfazer: o planejamento mudou depois")
    }

    // Orçamentos
    fun saveBudget(b: Budget, isNew: Boolean) = viewModelScope.launch {
        repo.saveBudget(if (isNew) b.copy(order = repo.nextBudgetOrder()) else b)
        say(if (isNew) "Orçamento criado" else "Orçamento salvo")
    }

    fun deleteBudget(b: Budget) = viewModelScope.launch {
        repo.deleteBudget(b.id)
        say("Orçamento excluído") { repo.saveBudget(b) }
    }

    companion object {
        private const val KEY_HIDE = "hide_values"
        private const val KEY_LAST_ACCOUNT = DataTransferService.KEY_LAST_ACCOUNT
        private const val KEY_SEEDED = DataTransferService.KEY_SEEDED

        fun factory(c: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(c.repository, c.clock, c.prefs, c.transfer) as T
        }
    }
}
