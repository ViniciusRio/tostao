package com.viniciusrio.tostao.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.viniciusrio.tostao.domain.Charts
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.MonthTotals
import com.viniciusrio.tostao.domain.PieSlice
import com.viniciusrio.tostao.ui.theme.LocalSemantic
import com.viniciusrio.tostao.ui.theme.Tabular
import java.time.YearMonth
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/** Espaço entre fatias, em graus; o mesmo valor vale para desenho e toque. */
private const val GAP_DEG = 1.5f
private const val THIN = 0.30f
private const val THICK = 0.42f

/**
 * Índice da fatia no ângulo [deg] (0° no topo, sentido horário), ou null se cair no
 * espaço entre fatias.
 */
fun sliceAt(slices: List<PieSlice>, deg: Float): Int? {
    val total = slices.sumOf { it.cents }.toFloat()
    if (total <= 0f) return null
    val gap = if (slices.size > 1) GAP_DEG else 0f
    var acc = 0f
    slices.forEachIndexed { i, s ->
        val sweep = s.cents / total * 360f
        if (deg >= acc + gap / 2f && deg < acc + sweep - gap / 2f) return i
        acc += sweep
    }
    return null
}

/** O que o centro da rosca mostra quando algo está selecionado. */
data class DonutCenter(val label: String, val cents: Long, val percent: Int)

/**
 * Pizza em rosca. [highlighted] é a fatia destacada; [center] o que aparece no meio
 * (null = total). Tocar numa fatia chama [onTapSlice] com o índice; no vazio, com null.
 */
@Composable
fun DonutChart(
    slices: List<PieSlice>,
    highlighted: Int?,
    center: DonutCenter?,
    onTapSlice: (Int?) -> Unit,
    totalLabel: String,
    hidden: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTapSlice)
    val currentHighlight by rememberUpdatedState(highlighted)
    Box(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 280.dp).fillMaxWidth(0.72f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize()
                    .semantics { contentDescription = description }
                    .pointerInput(slices) {
                        detectTapGestures { p ->
                            val dx = p.x - size.width / 2f
                            val dy = p.y - size.height / 2f
                            val r = sqrt(dx * dx + dy * dy)
                            val outer = min(size.width, size.height) / 2f
                            val deg = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0 + 360.0) % 360.0).toFloat()
                            val i = sliceAt(slices, deg)
                            val width = outer * (if (i != null && i == currentHighlight) THICK else THIN)
                            currentOnTap(if (i != null && r <= outer && r >= outer - width) i else null)
                        }
                    },
            ) {
                val outer = min(size.width, size.height) / 2f
                val total = slices.sumOf { it.cents }.toFloat()
                val gap = if (slices.size > 1) GAP_DEG else 0f
                var start = -90f
                slices.forEachIndexed { i, s ->
                    val sweep = s.cents / total * 360f
                    val isSel = i == highlighted
                    val w = outer * (if (isSel) THICK else THIN)
                    val radius = outer - w / 2f
                    val alpha = if (highlighted == null || isSel) 1f else 0.35f
                    drawArc(
                        color = Color(s.colorArgb).copy(alpha = alpha),
                        startAngle = start + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = Offset(this.center.x - radius, this.center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = w),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.5f)) {
                Text(
                    center?.label ?: totalLabel, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
                Text(
                    if (hidden) Money.HIDDEN else Money.format(center?.cents ?: slices.sumOf { it.cents }),
                    style = MaterialTheme.typography.titleMedium.merge(Tabular), maxLines = 1, textAlign = TextAlign.Center,
                )
                Text(
                    center?.let { "${it.percent}%" } ?: "${slices.size} ${if (slices.size == 1) "fatia" else "fatias"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Barras de receitas e despesas por mês; cada coluna é um botão que abre o mês. */
@Composable
fun TrendBars(
    months: List<MonthTotals>,
    selected: YearMonth,
    hidden: Boolean,
    onMonth: (YearMonth) -> Unit,
    monthName: (YearMonth) -> String,
) {
    val sem = LocalSemantic.current
    val max = months.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) }?.coerceAtLeast(1L) ?: 1L
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .semantics { contentDescription = Charts.describeTrend(months, hidden, monthName) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(sem.income); Text(" Receitas   ", style = MaterialTheme.typography.labelMedium)
            LegendDot(sem.expense); Text(" Despesas", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            months.forEach { m ->
                val isSel = m.month == selected
                val desc = if (hidden) {
                    "${monthName(m.month)}: valores ocultos"
                } else {
                    "${monthName(m.month)}: receitas ${Money.format(m.incomeCents)}, despesas ${Money.format(m.expenseCents)}"
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable(role = Role.Button, onClickLabel = "Abrir mês") { onMonth(m.month) }
                        .semantics(mergeDescendants = true) { contentDescription = desc; this.selected = isSel }
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Bar(m.incomeCents, max, sem.income)
                        Bar(m.expenseCents, max, sem.expense)
                    }
                    Text(
                        Charts.monthShort(m.month), style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.heightIn(min = 24.dp).padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(cents: Long, max: Long, color: Color) {
    val fraction = (cents.toFloat() / max).coerceIn(0f, 1f)
    Box(
        Modifier.width(10.dp).fillMaxHeight(if (cents > 0) fraction.coerceAtLeast(0.02f) else 0.02f)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(if (cents > 0) color else MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
