package com.viniciusrio.tostao.domain.backup

import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.CategoryGroup
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.Recurrence
import com.viniciusrio.tostao.domain.RecurrenceUnit
import com.viniciusrio.tostao.domain.TxType
import com.viniciusrio.tostao.domain.account
import com.viniciusrio.tostao.domain.category
import com.viniciusrio.tostao.domain.millis
import com.viniciusrio.tostao.domain.rule
import com.viniciusrio.tostao.domain.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BackupCodecTest {
    private val created = OffsetDateTime.parse("2026-11-09T14:30:00-03:00")

    private fun sample(): FinanceData {
        val a = account("a", initial = -12_345).copy(name = "Nubank", order = 1)
        val b = account("b", include = false).copy(name = "Carteira", order = 0)
        val food = category("food").copy(name = "Alimentação")
        val salary = category("salary", CategoryType.INCOME).copy(name = "Salário")
        val r = rule(LocalDate.of(2026, 1, 31), RecurrenceUnit.MONTH, interval = 1, end = LocalDate.of(2027, 1, 31), consumed = 3)
            .copy(categoryId = "food", title = "Aluguel")
        return FinanceData(
            accounts = listOf(a, b),
            categories = listOf(food, salary),
            transactions = listOf(
                tx(TxType.EXPENSE, 4590, "a", millis(2026, 11, 5), category = "food", title = "Feira").copy(note = "linha 1\n\"aspas\""),
                tx(TxType.INCOME, 500_000, "a", millis(2026, 11, 1), category = "salary"),
                tx(TxType.TRANSFER, 10_000, "a", millis(2026, 11, 2), to = "b"),
                tx(TxType.EXPENSE, 2_000, "b", millis(2026, 11, 3), adjustment = true, title = "Ajuste de saldo"),
                tx(TxType.EXPENSE, 100, "a", millis(2026, 11, 4)).copy(plannedRuleId = "regra-excluida"),
            ),
            rules = listOf(r, rule(LocalDate.of(2026, 12, 1), null, account = "b")),
            budgets = listOf(
                Budget("bud1", "Comida", 80_000, false, setOf("food"), 0),
                Budget("bud2", "Tudo", 300_000, true, emptySet(), 1),
            ),
        )
    }

    /** Amostra com ids fixos (as fábricas de teste usam um contador), para valores de referência. */
    private fun stableSample() = sample().let { d ->
        d.copy(
            transactions = d.transactions.mapIndexed { i, t -> t.copy(id = "tx$i") },
            rules = d.rules.mapIndexed { i, r -> r.copy(id = "rule$i") },
        )
    }

    private fun FinanceData.normalized() = FinanceData(
        accounts.sortedBy { it.id }, categories.sortedBy { it.id }, transactions.sortedBy { it.id },
        rules.sortedBy { it.id }, budgets.sortedBy { it.id }, groups.sortedBy { it.id },
    )

    private fun valid(text: String) = BackupCodec.decode(text) as BackupResult.Valid
    private fun invalid(text: String) = (BackupCodec.decode(text) as BackupResult.Invalid).reason

    /** Gera o JSON da amostra e aplica uma troca de texto para corromper um campo. */
    private fun tampered(data: FinanceData = sample(), from: String, to: String): String {
        val json = BackupCodec.encode(data, created)
        assertTrue("trecho \"$from\" não encontrado", json.contains(from))
        return json.replaceFirst(from, to)
    }

    @Test fun roundTripKeepsEverything() {
        val data = sample()
        val back = valid(BackupCodec.encode(data, created))
        assertEquals(data.normalized(), back.data.normalized())
        assertEquals(created, back.createdAt)
        val r = back.data.rules.first { it.title == "Aluguel" }
        assertEquals(Recurrence.nextDue(data.rules.first { it.title == "Aluguel" }), Recurrence.nextDue(r))
    }

    @Test fun outputHasHeaderAndIsStable() {
        val d = sample()
        val json = BackupCodec.encode(d, created)
        assertTrue(json.contains("\"app\": \"tostao\""))
        assertTrue(json.contains("\"formatVersion\": 2"))
        val shuffled = d.let { it.copy(accounts = it.accounts.reversed()) }
        assertEquals(json, BackupCodec.encode(shuffled, created))
    }

    @Test fun keepsTransactionInsertionOrder() {
        // A ordem de inserção desempata horários iguais na lista; a restauração precisa mantê-la.
        val d = sample().let { it.copy(transactions = it.transactions.reversed()) }
        assertEquals(d.transactions.map { it.id }, valid(BackupCodec.encode(d, created)).data.transactions.map { it.id })
    }

    @Test fun fingerprintIgnoresOrderButNotContent() {
        val d = sample()
        assertEquals(BackupCodec.fingerprint(d), BackupCodec.fingerprint(d.copy(transactions = d.transactions.reversed())))
        val changed = d.copy(transactions = d.transactions.map { if (it.title == "Feira") it.copy(amountCents = 4591) else it })
        assertNotEquals(BackupCodec.fingerprint(d), BackupCodec.fingerprint(changed))
    }

    @Test fun rejectsNonBackups() {
        assertEquals(BackupCodec.MSG_NOT_BACKUP, invalid("\u0089PNG\r\n"))
        assertEquals(BackupCodec.MSG_NOT_BACKUP, invalid("""{"name": "outro app"}"""))
        assertEquals(BackupCodec.MSG_NOT_BACKUP, invalid("""[1, 2, 3]"""))
        assertEquals(BackupCodec.MSG_NOT_BACKUP, invalid(""))
    }

    @Test fun rejectsNewerVersion() {
        assertEquals(BackupCodec.MSG_NEWER, invalid(tampered(from = "\"formatVersion\": 2", to = "\"formatVersion\": 3")))
    }

    @Test fun rejectsTruncatedOrMistyped() {
        val json = BackupCodec.encode(sample(), created)
        assertEquals(BackupCodec.MSG_NOT_BACKUP, invalid(json.take(json.length / 2)))
        assertTrue(invalid(json.replace("\"accounts\"", "\"contas\"")).startsWith(BackupCodec.MSG_CORRUPT))
        assertTrue(invalid(tampered(from = "\"amountCents\": 4590", to = "\"amountCents\": \"x\"")).startsWith(BackupCodec.MSG_CORRUPT))
    }

    @Test fun ignoresUnknownKeys() {
        valid(tampered(from = "\"formatVersion\": 2,", to = "\"formatVersion\": 2, \"extra\": true,"))
    }

    @Test fun rejectsUnknownEnum() {
        assertTrue(invalid(tampered(from = "\"type\": \"INCOME\"", to = "\"type\": \"LOAN\"")).contains("tipo"))
    }

    @Test fun rejectsDuplicateIdAndDuplicateAccountName() {
        val d = sample()
        assertTrue(invalid(BackupCodec.encode(d.copy(accounts = d.accounts + d.accounts[0].copy(name = "Outra")), created)).contains("repetido"))
        assertTrue(invalid(BackupCodec.encode(d.copy(accounts = d.accounts + account("z").copy(name = "NUBANK")), created)).contains("nome repetido"))
    }

    @Test fun rejectsAmountsOutOfRangeButAcceptsLargeAdjustment() {
        val d = sample()
        val tooBig = d.copy(transactions = d.transactions + tx(TxType.EXPENSE, Money.MAX_CENTS + 1, "a", 0))
        assertTrue(invalid(BackupCodec.encode(tooBig, created)).contains("valor"))
        val zero = d.copy(transactions = d.transactions + tx(TxType.EXPENSE, 0, "a", 0))
        assertTrue(invalid(BackupCodec.encode(zero, created)).contains("valor"))
        valid(BackupCodec.encode(d.copy(transactions = d.transactions + tx(TxType.INCOME, Money.MAX_CENTS * 5000, "a", 0, adjustment = true)), created))
    }

    @Test fun acceptsAdjustmentEditedIntoTransferOrWithCategory() {
        val d = sample()
        val edited = d.transactions + tx(TxType.TRANSFER, 500, "a", 0, to = "b", adjustment = true) +
            tx(TxType.EXPENSE, 500, "a", 0, category = "food", adjustment = true)
        valid(BackupCodec.encode(d.copy(transactions = edited), created))
    }

    @Test fun rejectsBadTransfers() {
        val d = sample()
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.TRANSFER, 1, "a", 0, to = "a"))), created)).contains("mesma conta"))
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.TRANSFER, 1, "a", 0, to = "b", category = "food"))), created)).contains("categoria"))
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.TRANSFER, 1, "a", 0))), created)).contains("destino"))
    }

    @Test fun rejectsCategoryOfWrongTypeAndBrokenReferences() {
        val d = sample()
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.EXPENSE, 1, "a", 0, category = "salary"))), created)).contains("outro tipo"))
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.EXPENSE, 1, "fantasma", 0))), created)).contains("conta inexistente"))
        assertTrue(invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.EXPENSE, 1, "a", 0, category = "x"))), created)).contains("categoria inexistente"))
    }

    @Test fun reasonNamesTheRecord() {
        val d = sample()
        val reason = invalid(BackupCodec.encode(d.copy(transactions = listOf(tx(TxType.EXPENSE, 1, "fantasma", 0))), created))
        assertEquals("${BackupCodec.MSG_CORRUPT} Transação 1: conta inexistente.", reason)
    }

    @Test fun budgetRules() {
        val d = sample()
        fun withBudget(b: Budget) = BackupCodec.encode(d.copy(budgets = listOf(b)), created)
        assertTrue(invalid(withBudget(Budget("x", "X", 100, false, emptySet(), 0))).contains("sem categorias"))
        assertTrue(invalid(withBudget(Budget("x", "X", 100, true, setOf("food"), 0))).contains("todas as despesas"))
        assertTrue(invalid(withBudget(Budget("x", "X", 100, false, setOf("salary"), 0))).contains("receita"))
        assertTrue(invalid(withBudget(Budget("x", "X", 0, true, emptySet(), 0))).contains("limite"))
        val json = withBudget(Budget("x", "X", 100, false, setOf("food"), 0))
        assertTrue(invalid(json.replace("\"food\"\n", "\"food\",\n        \"food\"\n")).contains("repetida"))
    }

    @Test fun plannedRuleRules() {
        val d = sample()
        fun with(r: com.viniciusrio.tostao.domain.PlannedRule) = BackupCodec.encode(d.copy(rules = listOf(r)), created)
        val start = LocalDate.of(2026, 1, 10)
        assertTrue(invalid(with(rule(start, RecurrenceUnit.MONTH, interval = 0))).contains("intervalo"))
        assertTrue(invalid(with(rule(start, RecurrenceUnit.MONTH, interval = 100))).contains("intervalo"))
        assertTrue(invalid(with(rule(start, null, end = start.plusDays(3)))).contains("regra única"))
        assertTrue(invalid(with(rule(start, RecurrenceUnit.DAY, end = start.minusDays(1)))).contains("data final"))
        assertTrue(invalid(with(rule(start, RecurrenceUnit.DAY, consumed = -1))).contains("progresso"))
        // A cada 99 anos, já paga duas vezes: o app grava isso, então o backup precisa aceitar.
        valid(with(rule(start, RecurrenceUnit.YEAR, interval = 99, consumed = 2)))
        valid(with(rule(start, RecurrenceUnit.YEAR, interval = 99, consumed = 200_000)))
        assertTrue(invalid(with(rule(start, RecurrenceUnit.DAY, type = TxType.TRANSFER))).contains("tipo"))
        assertTrue(invalid(with(rule(LocalDate.of(1800, 1, 1), RecurrenceUnit.DAY))).contains("data"))
        valid(with(rule(start, RecurrenceUnit.DAY, consumed = 200_000)))
        assertTrue(invalid(with(rule(start, RecurrenceUnit.DAY, consumed = 200_001))).contains("progresso"))
    }

    // ---- Grupos (formato 2) ----

    private fun withGroups(): FinanceData {
        val d = sample()
        val saude = CategoryGroup("g-saude", "Saúde", CategoryType.EXPENSE, 0xFF00897B, "health", 0)
        val renda = CategoryGroup("g-renda", "Renda", CategoryType.INCOME, 0xFF43A047, "work", 1)
        val vazio = CategoryGroup("g-pets", "Pets", CategoryType.EXPENSE, 0xFF8D6E63, "pet", 2)
        val cats = d.categories.map {
            when (it.id) {
                "food" -> it.copy(groupId = "g-saude")
                "salary" -> it.copy(groupId = "g-renda")
                else -> it
            }
        } + category("psi").copy(name = "Psicólogo", groupId = "g-saude") + category("lazer").copy(name = "Lazer")
        return d.copy(categories = cats, groups = listOf(saude, renda, vazio))
    }

    @Test fun roundTripKeepsGroups() {
        val d = withGroups()
        val back = valid(BackupCodec.encode(d, created))
        assertEquals(d.normalized(), back.data.normalized())
        assertEquals(3, back.groups)
        assertEquals(setOf("food", "psi"), back.data.categories.filter { it.groupId == "g-saude" }.map { it.id }.toSet())
    }

    /** O JSON do formato 1, como a versão anterior do app gerava (sem nenhum campo de grupo). */
    private fun asVersion1(json: String) = json.replace("\"formatVersion\": 2", "\"formatVersion\": 1")

    @Test fun version1WithoutGroupsIsRestored() {
        val d = sample()
        val json = asVersion1(BackupCodec.encode(d, created))
        assertTrue(!json.contains("groupId") && !json.contains("categoryGroups"))
        val back = valid(json)
        assertEquals(d.normalized(), back.data.normalized())
        assertTrue(back.data.groups.isEmpty())
    }

    @Test fun version1IgnoresGroupFields() {
        val back = valid(asVersion1(BackupCodec.encode(withGroups(), created)))
        assertTrue(back.data.groups.isEmpty())
        assertTrue(back.data.categories.all { it.groupId == null })
        assertEquals(0, back.groups)
    }

    @Test fun version1IgnoresMalformedGroupFields() {
        // Num v1, os campos de grupo são ignorados mesmo com tipo errado (não recusam o arquivo).
        val json = asVersion1(BackupCodec.encode(sample(), created))
            .replaceFirst("\"budgets\":", "\"categoryGroups\": \"x\",\n    \"budgets\":")
            .replaceFirst("\"name\": \"Alimentação\",", "\"name\": \"Alimentação\", \"groupId\": 42,")
        assertTrue(json.contains("\"categoryGroups\": \"x\"") && json.contains("\"groupId\": 42"))
        val back = valid(json)
        assertTrue(back.data.groups.isEmpty())
        assertTrue(back.data.categories.all { it.groupId == null })
        // No formato 2 os mesmos campos são lidos e um tipo errado recusa o arquivo.
        assertTrue(invalid(json.replace("\"formatVersion\": 1", "\"formatVersion\": 2")).startsWith(BackupCodec.MSG_CORRUPT))
    }

    @Test fun version1ThenGroupsThenVersion2KeepsIdsAndLinks() {
        val original = sample()
        val v1 = valid(asVersion1(BackupCodec.encode(original, created))).data
        val grouped = v1.copy(
            groups = listOf(CategoryGroup("g", "Casa", CategoryType.EXPENSE, 0, "home", 0)),
            categories = v1.categories.map { if (it.id == "food") it.copy(groupId = "g") else it },
        )
        val back = valid(BackupCodec.encode(grouped, created)).data
        assertEquals(grouped.normalized(), back.normalized())
        assertEquals(original.transactions.map { it.id to it.categoryId }, back.transactions.map { it.id to it.categoryId })
        assertEquals(original.budgets.sortedBy { it.id }, back.budgets.sortedBy { it.id })
    }

    @Test fun fingerprintWithoutGroupsIsTheSameAsFormat1() {
        // Valor calculado com o código do formato 1 (main antes deste change) para a mesma amostra:
        // atualizar o app sem mudar dados não pode disparar o lembrete de backup.
        assertEquals("d969311af6305cbe91f643b482eb6929", BackupCodec.fingerprint(stableSample()))
        assertNotEquals(BackupCodec.fingerprint(stableSample()), BackupCodec.fingerprint(withGroups()))
    }

    @Test fun rejectsInvalidGroups() {
        val d = withGroups()
        val dupName = d.copy(groups = d.groups + CategoryGroup("g-2", "saúde ", CategoryType.EXPENSE, 0, "tag", 9))
        assertTrue(invalid(BackupCodec.encode(dupName, created)).contains("Grupo 4: nome repetido"))
        val missing = d.copy(categories = d.categories.map { if (it.id == "lazer") it.copy(groupId = "nao-existe") else it })
        assertTrue(invalid(BackupCodec.encode(missing, created)).contains("grupo inexistente"))
        val otherType = d.copy(categories = d.categories.map { if (it.id == "salary") it.copy(groupId = "g-saude") else it })
        assertTrue(invalid(BackupCodec.encode(otherType, created)).contains("grupo de outro tipo"))
        // Mesmo nome em tipos diferentes é permitido, e o grupo pode repetir o nome de uma categoria.
        val sameNameOtherType = d.copy(groups = d.groups + CategoryGroup("g-3", "Saúde", CategoryType.INCOME, 0, "tag", 9))
        valid(BackupCodec.encode(sameNameOtherType, created))
        valid(BackupCodec.encode(d.copy(groups = d.groups + CategoryGroup("g-4", "Alimentação", CategoryType.EXPENSE, 0, "tag", 9)), created))
    }
}
