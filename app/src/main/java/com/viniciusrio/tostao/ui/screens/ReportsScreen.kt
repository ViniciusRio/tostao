package com.viniciusrio.tostao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Periods
import com.viniciusrio.tostao.domain.Reports
import com.viniciusrio.tostao.ui.MainViewModel
import com.viniciusrio.tostao.ui.components.EmptyState
import com.viniciusrio.tostao.ui.components.IconBadge
import com.viniciusrio.tostao.ui.components.MoneyText
import com.viniciusrio.tostao.ui.components.SectionHeader
import com.viniciusrio.tostao.ui.components.SegmentedChoice
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import java.time.YearMonth

@Composable
fun ReportsScreen(vm: MainViewModel, data: FinanceData) {
    val hidden by vm.hideValues.collectAsState()
    val today = vm.today()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    var kind by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    val minute by vm.minute.collectAsState()
    val report = remember(data, month, minute) { Reports.month(data, month, vm.zone, vm.nowMillis()) }
    val sem = LocalSemantic.current
    val slices = if (kind == CategoryType.EXPENSE) report.expenseByCategory else report.incomeByCategory

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item { MonthSwitcher(month, Periods.monthLabel(month, today)) { monthText = it.toString() } }
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
        item {
            SegmentedChoice(listOf(CategoryType.EXPENSE to "Despesas", CategoryType.INCOME to "Receitas"), kind, { kind = it })
        }
        item { SectionHeader("Por categoria") }
        if (slices.isEmpty()) {
            item { EmptyState("Sem dados", "Nenhuma ${if (kind == CategoryType.EXPENSE) "despesa" else "receita"} neste mês.") }
        }
        items(slices, key = { it.category?.id ?: "none" }) { s ->
            val color = Color(s.category?.colorArgb ?: 0xFF7D7D85)
            val name = s.category?.name ?: "Sem categoria"
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                    .clearAndSetSemantics { contentDescription = "$name, ${if (hidden) "valor oculto" else Money.format(s.totalCents)}, ${s.percent}%" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(s.category?.icon ?: "dots", s.category?.colorArgb ?: 0xFF7D7D85, size = 36.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        MoneyText(s.totalCents, hidden = hidden)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(s.percent.coerceIn(1, 100) / 100f).background(color))
                        }
                        Text("  ${s.percent}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
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
