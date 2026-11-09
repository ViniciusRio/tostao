package com.viniciusrio.tostao.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Período da exportação, sempre em datas civis no fuso do aparelho. */
sealed interface ExportPeriod {
    data object All : ExportPeriod
    data class Year(val year: Int) : ExportPeriod
    data class Month(val month: java.time.YearMonth) : ExportPeriod

    /** Datas inclusivas; [end] não pode ser anterior a [start]. */
    data class Range(val start: LocalDate, val end: LocalDate) : ExportPeriod {
        val isValid get() = !end.isBefore(start)
    }

    /** Intervalo em millis, ou null para "Tudo". */
    fun millisRange(zone: ZoneId): LongRange? {
        fun range(first: LocalDate, last: LocalDate) =
            first.atStartOfDay(zone).toInstant().toEpochMilli()..(last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1)
        return when (this) {
            All -> null
            is Year -> range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            is Month -> range(month.atDay(1), month.atEndOfMonth())
            is Range -> range(start, end)
        }
    }
}

object CsvExport {
    const val BOM = "\uFEFF"
    private const val SEP = ";"
    private const val EOL = "\r\n"
    private val HEADER = listOf("Data", "Hora", "Tipo", "Valor", "Conta", "Conta de destino", "Categoria", "Título", "Observação", "Ajuste")
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    /** Transações do período, em ordem cronológica (empate por id). */
    fun select(data: FinanceData, period: ExportPeriod, zone: ZoneId): List<Transaction> {
        val range = period.millisRange(zone)
        return data.transactions
            .filter { range == null || it.dateTimeMillis in range }
            .sortedWith(compareBy({ it.dateTimeMillis }, { it.id }))
    }

    fun build(data: FinanceData, period: ExportPeriod, zone: ZoneId): String {
        val sb = StringBuilder(BOM)
        sb.append(HEADER.joinToString(SEP, transform = ::quote)).append(EOL)
        for (t in select(data, period, zone)) {
            val at = Instant.ofEpochMilli(t.dateTimeMillis).atZone(zone)
            val fields = listOf(
                dateFmt.format(at),
                timeFmt.format(at),
                when (t.type) {
                    TxType.INCOME -> "Receita"
                    TxType.EXPENSE -> "Despesa"
                    TxType.TRANSFER -> "Transferência"
                },
                amount(if (t.type == TxType.EXPENSE) -t.amountCents else t.amountCents),
                text(data.accountById[t.accountId]?.name.orEmpty()),
                text(t.toAccountId?.let { data.accountById[it]?.name }.orEmpty()),
                text(t.categoryId?.let { data.categoryById[it]?.name }.orEmpty()),
                text(t.title),
                text(t.note),
                if (t.isAdjustment) "Sim" else "Não",
            )
            sb.append(fields.joinToString(SEP)).append(EOL)
        }
        return sb.toString()
    }

    /** "-1234,50": vírgula decimal, sem milhar nem símbolo, para a planilha ler como número. */
    fun amount(cents: Long): String {
        val a = kotlin.math.abs(cents)
        return (if (cents < 0) "-" else "") + "${a / 100},${(a % 100).toString().padStart(2, '0')}"
    }

    /** Texto do usuário: neutraliza fórmula e escapa. */
    fun text(value: String): String = quote(neutralize(value))

    private fun neutralize(value: String): String {
        if (value.isEmpty()) return value
        val first = value.trimStart(' ', '\n', '\r', '\t').firstOrNull()
        val risky = value[0] == '\t' || value[0] == '\r' || (first != null && first in "=+-@")
        return if (risky) "'$value" else value
    }

    private fun quote(value: String): String =
        if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
