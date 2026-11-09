package com.viniciusrio.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.viniciusrio.finance.domain.Account
import com.viniciusrio.finance.domain.Category
import com.viniciusrio.finance.domain.Money
import com.viniciusrio.finance.ui.theme.Tabular

/** Campo de valor com teclado decimal do sistema; mostra erro quando o texto não é válido. */
@Composable
fun AmountField(text: String, onText: (String) -> Unit, label: String, modifier: Modifier = Modifier, allowNegative: Boolean = false) {
    val body = if (allowNegative) text.removePrefix("-") else text
    val invalid = text.isNotBlank() && Money.parse(body) == null
    OutlinedTextField(
        value = text,
        onValueChange = { v -> onText(v.filter { it.isDigit() || it == ',' || it == '.' || (allowNegative && it == '-') }.take(18)) },
        label = { Text(label) },
        prefix = { Text("R$ ") },
        isError = invalid,
        supportingText = if (invalid) ({ Text("Valor inválido. Exemplo: 1.234,56") }) else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.merge(Tabular),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AccountChips(accounts: List<Account>, selected: String?, onSelect: (String) -> Unit, exclude: String? = null) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items(accounts.filter { it.id != exclude }, key = { it.id }) { a ->
            FilterChip(
                selected = a.id == selected,
                onClick = { onSelect(a.id) },
                label = { Text(a.name) },
                leadingIcon = { Icon(AppIcons.of(a.icon), null, tint = Color(a.colorArgb), modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(categories: List<Category>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { c ->
            FilterChip(
                selected = c.id in selected,
                onClick = { onToggle(c.id) },
                label = { Text(c.name) },
                leadingIcon = { Icon(AppIcons.of(c.icon), null, tint = Color(c.colorArgb), modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
fun ColorPicker(selected: Long, onSelect: (Long) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items(AppIcons.palette) { c ->
            val isSel = c == selected
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color(c))
                    .then(if (isSel) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(c) }
                    .semantics { this.selected = isSel; contentDescription = "Cor ${AppIcons.palette.indexOf(c) + 1}" },
                contentAlignment = Alignment.Center,
            ) { if (isSel) Icon(Icons.Outlined.Check, null, tint = Color.White) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, color: Long, onSelect: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcons.all.forEach { (name, vector) ->
            val isSel = name == selected
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (isSel) Color(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(role = Role.RadioButton) { onSelect(name) }
                    .semantics { this.selected = isSel; contentDescription = "Ícone $name" },
                contentAlignment = Alignment.Center,
            ) { Icon(vector, null, tint = if (isSel) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun <T> SegmentedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) ->
            FilterChip(
                selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
