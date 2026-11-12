package com.viniciusrio.tostao.ui.components

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategorySection
import com.viniciusrio.tostao.domain.TransactionQueries
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.ui.theme.Tabular

/**
 * Campo de valor com teclado decimal do sistema; mostra erro quando o texto não é válido.
 * Com [allowSum], aceita parcelas separadas por "+" ("12,50+8,30"), mostra o total e oferece
 * o botão "Somar", que insere "+" no cursor (o teclado decimal nem sempre tem essa tecla).
 */
@Composable
fun AmountField(
    text: String, onText: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    allowNegative: Boolean = false, allowSum: Boolean = false,
) {
    val limit = if (allowSum) 800 else 18
    val body = if (allowNegative) text.removePrefix("-") else text
    val total = if (allowSum) Money.parseSum(body) else Money.parse(body)
    val invalid = text.isNotBlank() && total == null
    val showTotal = allowSum && !invalid && Money.isSum(body)
    fun clean(s: String) = s.filter {
        it.isDigit() || it == ',' || it == '.' || (allowNegative && it == '-') || (allowSum && (it == '+' || it == ' '))
    }
    // O cursor é preciso para o botão "Somar"; o texto continua sendo a fonte da verdade.
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val shown = if (field.text == text) field else TextFieldValue(text, TextRange(text.length))
    fun emit(v: TextFieldValue) {
        val filtered = clean(v.text).take(limit)
        val cursor = clean(v.text.take(v.selection.end)).length.coerceAtMost(filtered.length)
        field = TextFieldValue(filtered, if (filtered == v.text) v.selection else TextRange(cursor))
        if (filtered != text) onText(filtered)
    }
    OutlinedTextField(
        value = shown,
        onValueChange = ::emit,
        label = { Text(label) },
        prefix = { Text("R$ ") },
        isError = invalid,
        supportingText = when {
            invalid -> ({ Text(if (allowSum) "Valor inválido. Exemplo: 1.234,56 ou 12,50+8,30" else "Valor inválido. Exemplo: 1.234,56") })
            showTotal -> ({ Text("= ${Money.format(total!!)}", style = MaterialTheme.typography.bodyMedium.merge(Tabular)) })
            else -> null
        },
        trailingIcon = if (allowSum) ({
            IconButton(onClick = {
                val sel = shown.selection
                val t = shown.text.replaceRange(sel.min, sel.max, "+")
                emit(TextFieldValue(t, TextRange(sel.min + 1)))
            }) { Icon(Icons.Outlined.Add, "Somar: inserir +") }
        }) else null,
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

/**
 * Seletor de categoria por seções ([TransactionQueries.categorySections]). Sem grupos há uma
 * seção só, sem título, e o seletor fica igual a [CategoryChips].
 */
@Composable
fun GroupedCategoryChips(
    sections: List<CategorySection>, selected: Set<String>, onAdd: (() -> Unit)? = null, onToggle: (String) -> Unit,
) {
    if (sections.size <= 1 && sections.firstOrNull()?.group == null) {
        CategoryChips(sections.firstOrNull()?.categories.orEmpty(), selected, onAdd, onToggle)
        return
    }
    sections.forEachIndexed { i, sec ->
        Text(
            sec.group?.name ?: "Sem grupo",
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = if (i == 0) 0.dp else 8.dp, bottom = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CategoryChips(sec.categories, selected, if (i == sections.lastIndex) onAdd else null, onToggle)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>, selected: Set<String>, onAdd: (() -> Unit)? = null, onToggle: (String) -> Unit,
) {
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
        if (onAdd != null) {
            AssistChip(
                onClick = onAdd,
                label = { Text("Nova categoria") },
                leadingIcon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp)) },
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
