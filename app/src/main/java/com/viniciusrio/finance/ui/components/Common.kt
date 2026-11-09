package com.viniciusrio.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.viniciusrio.finance.domain.Category
import com.viniciusrio.finance.domain.FinanceData
import com.viniciusrio.finance.domain.Money
import com.viniciusrio.finance.domain.Transaction
import com.viniciusrio.finance.domain.TxType
import com.viniciusrio.finance.ui.theme.LocalSemantic
import com.viniciusrio.finance.ui.theme.Tabular

/** Valor monetário; com [hidden] mostra "R$ •••••" e anuncia "valor oculto". */
@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    signed: Boolean = false,
    color: Color = Color.Unspecified,
    hidden: Boolean = false,
    colorBySign: Boolean = false,
) {
    val sem = LocalSemantic.current
    val c = when {
        hidden -> color
        colorBySign && cents < 0 -> sem.expense
        colorBySign && cents > 0 && signed -> sem.income
        else -> color
    }
    Text(
        text = if (hidden) Money.HIDDEN else Money.format(cents, signed),
        modifier = modifier.semantics { if (hidden) contentDescription = "valor oculto" },
        style = style.merge(Tabular),
        color = c,
        maxLines = 1,
    )
}

@Composable
fun IconBadge(icon: String, colorArgb: Long, size: Dp = 40.dp, description: String? = null) {
    val color = Color(colorArgb)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.of(icon), contentDescription = description, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.size(8.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
        },
        actions = { actions() },
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

fun txTitle(t: Transaction, data: FinanceData): String = when {
    t.title.isNotBlank() -> t.title
    t.type == TxType.TRANSFER -> "Transferência"
    else -> t.categoryId?.let { data.categoryById[it]?.name } ?: if (t.type == TxType.INCOME) "Receita" else "Despesa"
}

/** Linha de transação usada na Início, na lista e na busca. */
@Composable
fun TransactionRow(t: Transaction, data: FinanceData, hidden: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    val cat: Category? = t.categoryId?.let { data.categoryById[it] }
    val account = data.accountById[t.accountId]?.name ?: "Conta excluída"
    val sub = subtitle ?: when (t.type) {
        TxType.TRANSFER -> "$account → ${t.toAccountId?.let { data.accountById[it]?.name } ?: "?"}"
        else -> listOfNotNull(cat?.name?.takeIf { t.title.isNotBlank() }, account).joinToString(" · ")
    }
    val signed = when (t.type) {
        TxType.INCOME -> t.amountCents
        TxType.EXPENSE -> -t.amountCents
        TxType.TRANSFER -> t.amountCents
    }
    val sem = LocalSemantic.current
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar", onClick = onClick)
            .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (t.type == TxType.TRANSFER) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(sem.transfer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.SwapHoriz, null, tint = sem.transfer) }
        } else {
            IconBadge(cat?.icon ?: "dots", cat?.colorArgb ?: 0xFF7D7D85)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(txTitle(t, data), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        MoneyText(
            signed, signed = t.type != TxType.TRANSFER, hidden = hidden,
            color = when (t.type) {
                TxType.INCOME -> sem.income
                TxType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                TxType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

fun typeLabel(t: TxType) = when (t) {
    TxType.INCOME -> "Receita"
    TxType.EXPENSE -> "Despesa"
    TxType.TRANSFER -> "Transferência"
}
