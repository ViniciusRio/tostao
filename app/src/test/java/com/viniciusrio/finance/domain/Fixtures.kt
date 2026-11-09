package com.viniciusrio.finance.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

fun millis(y: Int, m: Int, d: Int, h: Int = 12): Long =
    LocalDateTime.of(y, m, d, h, 0).atZone(ZONE).toInstant().toEpochMilli()

fun account(id: String, initial: Long = 0, include: Boolean = true, order: Int = 0) =
    Account(id, id, 0xFF6750A4, "wallet", initial, include, order)

fun category(id: String, type: CategoryType = CategoryType.EXPENSE) =
    Category(id, id, type, 0xFF6750A4, "tag", 0)

private var seq = 0
fun tx(
    type: TxType, cents: Long, account: String, at: Long,
    to: String? = null, category: String? = null, title: String = "", adjustment: Boolean = false,
) = Transaction("t${seq++}", type, cents, account, to, category, title, "", at, isAdjustment = adjustment)

fun rule(
    start: LocalDate, unit: RecurrenceUnit?, interval: Int = 1, end: LocalDate? = null,
    consumed: Int = 0, cents: Long = 100, type: TxType = TxType.EXPENSE, account: String = "a",
) = PlannedRule("r${seq++}", type, cents, account, null, "Regra", start, unit, interval, end, consumed)
