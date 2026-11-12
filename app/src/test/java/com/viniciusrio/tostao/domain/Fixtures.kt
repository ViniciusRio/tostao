package com.viniciusrio.tostao.domain

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

/**
 * Cenário do spec de grupos, em novembro de 2026: Plano de saúde R$ 500,00 e Psicólogo R$ 300,00
 * (grupo Saúde, que também tem Seguro de vida sem movimento), Lazer R$ 200,00 sem grupo, e
 * ainda uma despesa sem categoria, uma transferência, um ajuste e uma receita de R$ 50,00 em
 * Psicólogo (tipo divergente). Pets (grupo) e Educação (sem grupo) ficam sem movimento.
 */
object GroupScenario {
    val now = millis(2026, 11, 28)
    val saude = CategoryGroup("saude", "Saúde", CategoryType.EXPENSE, 0xFF00897B, "health", 0)
    val pets = CategoryGroup("pets", "Pets", CategoryType.EXPENSE, 0xFF8D6E63, "pet", 1)
    val cats = listOf(
        category("plano").copy(name = "Plano de saúde", groupId = "saude", order = 0),
        category("psi").copy(name = "Psicólogo", groupId = "saude", order = 1),
        category("seguro").copy(name = "Seguro de vida", groupId = "saude", order = 2),
        category("lazer").copy(name = "Lazer", order = 3),
        category("edu").copy(name = "Educação", order = 4),
        category("vet").copy(name = "Veterinário", groupId = "pets", order = 5),
    )
    val plano = tx(TxType.EXPENSE, 50_000, "pix", millis(2026, 11, 3), category = "plano")
    val psi = tx(TxType.EXPENSE, 30_000, "btg", millis(2026, 11, 5), category = "psi")
    val lazer = tx(TxType.EXPENSE, 20_000, "pix", millis(2026, 11, 6), category = "lazer")
    val loose = tx(TxType.EXPENSE, 1_000, "pix", millis(2026, 11, 7))
    val transfer = tx(TxType.TRANSFER, 5_000, "pix", millis(2026, 11, 8), to = "btg")
    val adjustment = tx(TxType.EXPENSE, 700, "pix", millis(2026, 11, 9), adjustment = true)
    val refund = tx(TxType.INCOME, 5_000, "pix", millis(2026, 11, 10), category = "psi")
    val october = tx(TxType.EXPENSE, 9_900, "pix", millis(2026, 10, 10), category = "plano")
    val data = FinanceData(
        accounts = listOf(account("pix"), account("btg")),
        categories = cats,
        groups = listOf(saude, pets),
        transactions = listOf(plano, psi, lazer, loose, transfer, adjustment, refund, october),
    )
}
