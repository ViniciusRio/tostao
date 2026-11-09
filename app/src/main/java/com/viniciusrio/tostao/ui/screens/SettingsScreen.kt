package com.viniciusrio.tostao.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.viniciusrio.tostao.BuildConfig
import com.viniciusrio.tostao.domain.CsvExport
import com.viniciusrio.tostao.domain.ExportPeriod
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.ui.DataOp
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.Route
import com.viniciusrio.tostao.ui.components.BackTopBar
import com.viniciusrio.tostao.ui.components.SectionHeader
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val fileDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

private fun dateTimeLabel(millis: Long, vm: MainViewModel): String {
    val at = Instant.ofEpochMilli(millis).atZone(vm.zone)
    return "${Periods.shortDate(at.toLocalDate(), vm.today())}, ${"%02d:%02d".format(at.hour, at.minute)}"
}

/** Abre o seletor para salvar o backup; usado em Configurações e no cartão da Início. */
@Composable
fun rememberBackupLauncher(vm: MainViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { vm.backupTo(it) }
    return { if (vm.dataOp.value == null) launcher.launch("tostao-backup-${fileDate.format(vm.today())}.json") }
}

@Composable
fun SettingsScreen(vm: MainViewModel, data: FinanceData) {
    val state by vm.backupState.collectAsState()
    val op by vm.dataOp.collectAsState()
    val busy = op != null
    val backup = rememberBackupLauncher(vm)
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { vm.readRestore(it) }
    var exporting by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Configurações", vm::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionHeader("Organização", Modifier.semantics { heading() })
            SettingsItem(Icons.Outlined.Category, "Categorias", "Criar, editar e excluir categorias", enabled = true) {
                vm.navigate(Route.Categories)
            }

            SectionHeader("Seus dados", Modifier.semantics { heading() })
            SettingsItem(
                Icons.Outlined.CloudUpload, "Fazer backup",
                "Último backup: " + (state.lastBackupAtMillis?.let { dateTimeLabel(it, vm) } ?: "Nunca"),
                enabled = !busy, onClick = backup,
            )
            SettingsItem(Icons.Outlined.Restore, "Restaurar backup", "Substitui todos os dados atuais pelos do arquivo", enabled = !busy) {
                // Alguns provedores (Drive) não informam o tipo de um .json; aceitar qualquer arquivo e validar o conteúdo.
                restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            }
            SettingsItem(Icons.Outlined.TableChart, "Exportar transações (CSV)", "Para abrir no Excel ou no Google Planilhas", enabled = !busy) {
                exporting = true
            }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.WarningAmber, null)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "O backup e o CSV não têm senha e contêm seus dados financeiros. Guarde os arquivos num local de confiança.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SectionHeader("Sobre", Modifier.semantics { heading() })
            SettingsItem(Icons.Outlined.Info, "Tostão ${BuildConfig.VERSION_NAME}", "Cada vintém no lugar. Seus dados ficam só neste aparelho.", enabled = true, onClick = null)
            Spacer(Modifier.size(24.dp))
        }
    }

    if (exporting) ExportSheet(vm, data) { exporting = false }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, enabled: Boolean, onClick: (() -> Unit)?) {
    val clickable = if (onClick != null) {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        Modifier.fillMaxWidth().then(clickable).heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
    }
}

private enum class PeriodChoice(val label: String) { All("Tudo"), Year("Este ano"), Month("Este mês"), Range("Intervalo") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(vm: MainViewModel, data: FinanceData, onClose: () -> Unit) {
    val today = vm.today()
    var choice by rememberSaveable { mutableStateOf(PeriodChoice.Month) }
    var start by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var end by remember { mutableStateOf(today) }
    val period: ExportPeriod = when (choice) {
        PeriodChoice.All -> ExportPeriod.All
        PeriodChoice.Year -> ExportPeriod.Year(today.year)
        PeriodChoice.Month -> ExportPeriod.Month(YearMonth.from(today))
        PeriodChoice.Range -> ExportPeriod.Range(start, end)
    }
    val rangeOk = (period as? ExportPeriod.Range)?.isValid ?: true
    val count = remember(data, period) { if (rangeOk) CsvExport.select(data, period, vm.zone).size else 0 }
    val currentPeriod by rememberUpdatedState(period)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            vm.exportCsv(uri, currentPeriod)
            onClose()
        }
    }

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Exportar transações", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodChoice.entries.forEach { c ->
                    FilterChip(selected = choice == c, onClick = { choice = c }, label = { Text(c.label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            if (choice == PeriodChoice.Range) {
                DateButton("De", start, today, { start = it }, Modifier.fillMaxWidth())
                DateButton("Até", end, today, { end = it }, Modifier.fillMaxWidth())
                if (!rangeOk) Text("A data final não pode ser anterior à inicial.", color = MaterialTheme.colorScheme.error)
            }
            if (rangeOk && count == 0) Text("Nenhuma transação nesse período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { launcher.launch("tostao-transacoes-${fileDate.format(today)}.csv") },
                enabled = rangeOk && count > 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(if (count > 0) "Exportar ${plural(count, "transação", "transações")}" else "Exportar") }
        }
    }
}

/** Progresso das operações de arquivo e confirmação de restauração; fica acima de qualquer tela. */
@Composable
fun DataOpOverlay(vm: MainViewModel, data: FinanceData) {
    val op by vm.dataOp.collectAsState()
    val pending by vm.pendingRestore.collectAsState()

    op?.let { current ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Spacer(Modifier.size(16.dp))
                    Text(
                        when (current) {
                            DataOp.Backup -> "Salvando backup…"
                            DataOp.Reading -> "Lendo o arquivo…"
                            DataOp.Restore -> "Restaurando…"
                            DataOp.Export -> "Exportando…"
                        },
                    )
                }
            },
        )
    }

    pending?.let { p ->
        val body = buildString {
            append("Backup de ${dateTimeLabel(p.createdAt.toInstant().toEpochMilli(), vm)}.\n\n")
            append("No arquivo: ${counts(p.accounts, p.transactions, p.rules, p.budgets)}.\n")
            append("Agora no app: ${counts(data.accounts.size, data.transactions.size, data.rules.size, data.budgets.size)}.\n\n")
            append("Todos os dados atuais serão apagados e substituídos pelos do arquivo. Isso não pode ser desfeito. ")
            append("Para guardar os dados atuais, cancele e faça um backup antes.")
        }
        AlertDialog(
            onDismissRequest = vm::cancelRestore,
            title = { Text("Substituir todos os dados?") },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = vm::confirmRestore) { Text("Substituir dados", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = vm::cancelRestore) { Text("Cancelar") } },
        )
    }
}

private fun counts(accounts: Int, transactions: Int, rules: Int, budgets: Int) = listOf(
    plural(accounts, "conta", "contas"),
    plural(transactions, "transação", "transações"),
    plural(rules, "planejado", "planejados"),
    plural(budgets, "orçamento", "orçamentos"),
).joinToString(", ")

/** Cartão de lembrete de backup da Início. */
@Composable
fun BackupReminderCard(vm: MainViewModel, lastBackupAt: Long?) {
    val backup = rememberBackupLauncher(vm)
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                "Faça um backup dos seus dados", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                if (lastBackupAt == null) {
                    "Seus dados existem só neste aparelho. Se ele for perdido ou o app for desinstalado, tudo se perde."
                } else {
                    "Seu último backup foi em ${dateTimeLabel(lastBackupAt, vm)} e os dados mudaram desde então."
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = vm::snoozeBackupReminder, modifier = Modifier.heightIn(min = 48.dp)) { Text("Agora não") }
                TextButton(onClick = backup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fazer backup") }
            }
        }
    }
}
