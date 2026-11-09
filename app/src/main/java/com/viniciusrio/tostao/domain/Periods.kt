package com.viniciusrio.tostao.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object Periods {
    /** Intervalo [início, fim] em millis do mês no fuso [zone]. */
    fun monthRange(month: YearMonth, zone: ZoneId): LongRange {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start..end
    }

    fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private val monthsShort = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez.")
    private val monthsLong = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val weekdays = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    /** "17 de set.", com ano quando diferente do ano de [today]. */
    fun shortDate(date: LocalDate, today: LocalDate): String {
        val base = "${date.dayOfMonth} de ${monthsShort[date.monthValue - 1]}"
        return if (date.year == today.year) base else "$base de ${date.year}"
    }

    /** "Hoje", "Ontem" ou "quinta, 17 de set.". */
    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> "${weekdays[date.dayOfWeek.value - 1]}, ${shortDate(date, today)}"
    }

    /** "Novembro de 2026", ou só "Novembro" no ano corrente. */
    fun monthLabel(month: YearMonth, today: LocalDate): String {
        val name = monthsLong[month.monthValue - 1].replaceFirstChar { it.uppercase() }
        return if (month.year == today.year) name else "$name de ${month.year}"
    }
}
