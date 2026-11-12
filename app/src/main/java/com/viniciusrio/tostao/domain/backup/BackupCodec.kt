package com.viniciusrio.tostao.domain.backup

import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategoryGroup
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.Limits
import com.viniciusrio.tostao.domain.Money
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.Recurrence
import com.viniciusrio.tostao.domain.RecurrenceUnit
import com.viniciusrio.tostao.domain.Transaction
import com.viniciusrio.tostao.domain.TxType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Resultado da leitura de um arquivo de backup. */
sealed interface BackupResult {
    data class Valid(val data: FinanceData, val createdAt: OffsetDateTime) : BackupResult {
        val accounts get() = data.accounts.size
        val transactions get() = data.transactions.size
        val rules get() = data.rules.size
        val budgets get() = data.budgets.size
        val groups get() = data.groups.size
    }

    data class Invalid(val reason: String) : BackupResult
}

object BackupCodec {
    const val APP_ID = "tostao"
    const val FORMAT_VERSION = 2

    /** Teto de tamanho, igual na leitura e na geração, para o app nunca gerar um backup que recusaria. */
    const val MAX_BYTES = 50L * 1024 * 1024

    const val MAX_CONSUMED = 200_000
    private val MIN_DATE = LocalDate.of(1900, 1, 1)
    private val MAX_DATE = LocalDate.of(2200, 12, 31)

    const val MSG_NOT_BACKUP = "Este arquivo não é um backup do Tostão."
    const val MSG_NEWER = "Este backup foi feito por uma versão mais nova do Tostão. Atualize o app para restaurá-lo."
    const val MSG_CORRUPT = "O arquivo de backup está corrompido ou incompleto."

    private val pretty = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
    }
    private val compact = Json { encodeDefaults = true }
    private val reader = Json { ignoreUnknownKeys = true }

    /** JSON legível do backup. Listas ordenadas por id, para a saída ser estável. */
    fun encode(data: FinanceData, createdAt: OffsetDateTime): String =
        pretty.encodeToString(BackupFile.serializer(), BackupFile(APP_ID, FORMAT_VERSION, createdAt.toString(), toDto(data)))

    /** Hash da forma canônica dos dados (sem data de criação): igual se e só se os dados forem iguais. */
    fun fingerprint(data: FinanceData): String {
        val bytes = compact.encodeToString(BackupData.serializer(), toDto(data, keepTransactionOrder = false)).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).take(16).joinToString("") { "%02x".format(it) }
    }

    fun decode(text: String): BackupResult {
        // 1. Estrutura: é um backup do Tostão, de versão suportada?
        val root = try {
            reader.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return BackupResult.Invalid(MSG_NOT_BACKUP)
        if ((root["app"] as? JsonPrimitive)?.takeIf { it.isString }?.content != APP_ID) return BackupResult.Invalid(MSG_NOT_BACKUP)
        val version = (root["formatVersion"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            ?: return BackupResult.Invalid(MSG_CORRUPT)
        if (version > FORMAT_VERSION) return BackupResult.Invalid(MSG_NEWER)
        if (version < 1) return BackupResult.Invalid(MSG_CORRUPT)

        // 2. Decodificação dos tipos. O formato 1 não tinha grupos: os campos de grupo de um arquivo
        // v1 são removidos antes de decodificar, então nem um valor de tipo errado neles o recusa.
        val file = try {
            reader.decodeFromJsonElement(BackupFile.serializer(), if (version < 2) withoutGroups(root) else root)
        } catch (_: SerializationException) {
            return BackupResult.Invalid(MSG_CORRUPT)
        } catch (_: IllegalArgumentException) {
            return BackupResult.Invalid(MSG_CORRUPT)
        }
        val createdAt = try {
            OffsetDateTime.parse(file.createdAt)
        } catch (_: DateTimeParseException) {
            return BackupResult.Invalid(MSG_CORRUPT)
        }

        // 3. Regras de negócio e referências.
        return try {
            BackupResult.Valid(Validator(file.data).run(), createdAt)
        } catch (e: InvalidBackup) {
            BackupResult.Invalid("$MSG_CORRUPT ${e.message}")
        }
    }

    /** Cópia do JSON sem `data.categoryGroups` e sem `groupId` nas categorias (leitura do formato 1). */
    private fun withoutGroups(root: JsonObject): JsonObject {
        val data = root["data"] as? JsonObject ?: return root
        val categories = (data["categories"] as? JsonArray)?.let { arr ->
            JsonArray(arr.map { c -> if (c is JsonObject) JsonObject(c - "groupId") else c })
        }
        val cleaned = data - "categoryGroups" + (if (categories != null) mapOf("categories" to categories) else emptyMap())
        return JsonObject(root + ("data" to JsonObject(cleaned)))
    }

    private class InvalidBackup(message: String) : Exception(message)

    private class Validator(val d: BackupData) {
        fun fail(what: String, index: Int, problem: String): Nothing = throw InvalidBackup("$what ${index + 1}: $problem.")

        fun <T> uniqueIds(items: List<T>, what: String, id: (T) -> String) {
            val seen = HashSet<String>()
            items.forEachIndexed { i, it ->
                val v = id(it)
                if (v.isBlank()) fail(what, i, "identificador vazio")
                if (!seen.add(v)) fail(what, i, "identificador repetido")
            }
        }

        fun name(value: String, max: Int, what: String, i: Int) {
            if (value.isBlank() || value.length > max) fail(what, i, "nome vazio ou com mais de $max caracteres")
        }

        fun date(value: String, what: String, i: Int): LocalDate {
            val d = try {
                LocalDate.parse(value)
            } catch (_: DateTimeParseException) {
                fail(what, i, "data inválida")
            }
            if (d.isBefore(MIN_DATE) || d.isAfter(MAX_DATE)) fail(what, i, "data fora do intervalo aceito")
            return d
        }

        fun run(): FinanceData {
            uniqueIds(d.accounts, "Conta") { it.id }
            uniqueIds(d.categories, "Categoria") { it.id }
            uniqueIds(d.transactions, "Transação") { it.id }
            uniqueIds(d.plannedRules, "Planejamento") { it.id }
            uniqueIds(d.budgets, "Orçamento") { it.id }
            uniqueIds(d.categoryGroups, "Grupo") { it.id }

            val groupNames = HashSet<Pair<CategoryType, String>>()
            val groups = d.categoryGroups.mapIndexed { i, g ->
                name(g.name, Limits.GROUP_NAME, "Grupo", i)
                val type = CategoryType.entries.firstOrNull { it.name == g.type } ?: fail("Grupo", i, "tipo desconhecido")
                if (!groupNames.add(type to g.name.trim().lowercase())) fail("Grupo", i, "nome repetido")
                CategoryGroup(g.id, g.name, type, g.colorArgb, g.icon, g.order)
            }
            val groupType = groups.associate { it.id to it.type }

            val accountNames = HashSet<String>()
            val accounts = d.accounts.mapIndexed { i, a ->
                name(a.name, Limits.ACCOUNT_NAME, "Conta", i)
                if (!accountNames.add(a.name.trim().lowercase())) fail("Conta", i, "nome repetido")
                if (a.initialBalanceCents !in -Money.MAX_CENTS..Money.MAX_CENTS) fail("Conta", i, "saldo inicial fora do limite")
                Account(a.id, a.name, a.colorArgb, a.icon, a.initialBalanceCents, a.includeInTotal, a.order)
            }
            val accountIds = accounts.mapTo(HashSet()) { it.id }

            val categoryNames = HashSet<Pair<CategoryType, String>>()
            val categories = d.categories.mapIndexed { i, c ->
                name(c.name, Limits.CATEGORY_NAME, "Categoria", i)
                val type = CategoryType.entries.firstOrNull { it.name == c.type } ?: fail("Categoria", i, "tipo desconhecido")
                if (!categoryNames.add(type to c.name.trim().lowercase())) fail("Categoria", i, "nome repetido")
                if (c.groupId != null) {
                    val gt = groupType[c.groupId] ?: fail("Categoria", i, "grupo inexistente")
                    if (gt != type) fail("Categoria", i, "grupo de outro tipo")
                }
                Category(c.id, c.name, type, c.colorArgb, c.icon, c.order, c.groupId)
            }
            val categoryType = categories.associate { it.id to it.type }

            fun checkCategory(what: String, i: Int, type: TxType, categoryId: String?) {
                if (categoryId == null) return
                val ct = categoryType[categoryId] ?: fail(what, i, "categoria inexistente")
                val expected = if (type == TxType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                if (ct != expected) fail(what, i, "categoria de outro tipo")
            }

            val transactions = d.transactions.mapIndexed { i, t ->
                val type = TxType.entries.firstOrNull { it.name == t.type } ?: fail("Transação", i, "tipo desconhecido")
                // Ajustes podem passar do limite comum: a diferença entre dois saldos não é limitada.
                val max = if (t.isAdjustment) Long.MAX_VALUE else Money.MAX_CENTS
                if (t.amountCents !in 1..max) fail("Transação", i, "valor fora do limite")
                if (t.accountId !in accountIds) fail("Transação", i, "conta inexistente")
                if (t.title.length > Limits.TITLE) fail("Transação", i, "título longo demais")
                if (t.note.length > Limits.NOTE) fail("Transação", i, "observação longa demais")
                if (type == TxType.TRANSFER) {
                    val to = t.toAccountId ?: fail("Transação", i, "transferência sem conta de destino")
                    if (to !in accountIds) fail("Transação", i, "conta de destino inexistente")
                    if (to == t.accountId) fail("Transação", i, "transferência para a mesma conta")
                    if (t.categoryId != null) fail("Transação", i, "transferência com categoria")
                } else {
                    if (t.toAccountId != null) fail("Transação", i, "conta de destino em transação que não é transferência")
                    checkCategory("Transação", i, type, t.categoryId)
                }
                // plannedRuleId pode apontar para regra já excluída: as transações pagas permanecem.
                Transaction(
                    t.id, type, t.amountCents, t.accountId, t.toAccountId, t.categoryId, t.title, t.note,
                    t.dateTimeMillis, t.plannedRuleId, t.isAdjustment,
                )
            }

            val rules = d.plannedRules.mapIndexed { i, r ->
                val what = "Planejamento"
                val type = TxType.entries.firstOrNull { it.name == r.type && it != TxType.TRANSFER } ?: fail(what, i, "tipo inválido")
                if (r.amountCents !in 1..Money.MAX_CENTS) fail(what, i, "valor fora do limite")
                if (r.accountId !in accountIds) fail(what, i, "conta inexistente")
                if (r.title.isBlank() || r.title.length > Limits.TITLE) fail(what, i, "título vazio ou longo demais")
                checkCategory(what, i, type, r.categoryId)
                val start = date(r.startDate, what, i)
                val unit = r.unit?.let { u -> RecurrenceUnit.entries.firstOrNull { it.name == u } ?: fail(what, i, "recorrência desconhecida") }
                val end = r.endDate?.let { date(it, what, i) }
                if (unit == null) {
                    if (r.interval != 1 || end != null) fail(what, i, "regra única com intervalo ou data final")
                } else {
                    if (r.interval !in Limits.INTERVAL) fail(what, i, "intervalo fora de 1 a 99")
                    if (end != null && end.isBefore(start)) fail(what, i, "data final antes da inicial")
                }
                if (r.consumedOccurrences !in 0..MAX_CONSUMED) fail(what, i, "progresso inválido")
                val rule = PlannedRule(r.id, type, r.amountCents, r.accountId, r.categoryId, r.title, start, unit, r.interval, end, r.consumedOccurrences)
                // Só importa que dê para calcular: uma regra a cada 99 anos já paga pode vencer depois de 2200.
                try {
                    Recurrence.nextDue(rule)
                } catch (_: ArithmeticException) {
                    fail(what, i, "próxima data impossível de calcular")
                } catch (_: java.time.DateTimeException) {
                    fail(what, i, "próxima data impossível de calcular")
                }
                rule
            }

            val budgets = d.budgets.mapIndexed { i, b ->
                val what = "Orçamento"
                name(b.name, Limits.BUDGET_NAME, what, i)
                if (b.limitCents !in 1..Money.MAX_CENTS) fail(what, i, "limite fora do intervalo")
                if (b.categoryIds.toSet().size != b.categoryIds.size) fail(what, i, "categoria repetida")
                if (b.allExpenses) {
                    if (b.categoryIds.isNotEmpty()) fail(what, i, "\"todas as despesas\" com categorias")
                } else {
                    if (b.categoryIds.isEmpty()) fail(what, i, "sem categorias")
                    b.categoryIds.forEach {
                        val ct = categoryType[it] ?: fail(what, i, "categoria inexistente")
                        if (ct != CategoryType.EXPENSE) fail(what, i, "categoria de receita")
                    }
                }
                Budget(b.id, b.name, b.limitCents, b.allExpenses, b.categoryIds.toSet(), b.order)
            }

            return FinanceData(accounts, categories, transactions, rules, budgets, groups)
        }
    }

    private fun toDto(data: FinanceData, keepTransactionOrder: Boolean = true) = BackupData(
        accounts = data.accounts.sortedBy { it.id }.map {
            AccountDto(it.id, it.name, it.colorArgb, it.icon, it.initialBalanceCents, it.includeInTotal, it.order)
        },
        categories = data.categories.sortedBy { it.id }.map {
            CategoryDto(it.id, it.name, it.type.name, it.colorArgb, it.icon, it.order, it.groupId)
        },
        // No arquivo, a ordem de inserção (vinda do banco) é preservada: ela é o desempate de
        // horários iguais na lista e precisa sobreviver à restauração. A impressão digital ignora ordem.
        transactions = (if (keepTransactionOrder) data.transactions else data.transactions.sortedBy { it.id }).map {
            TransactionDto(
                it.id, it.type.name, it.amountCents, it.accountId, it.toAccountId, it.categoryId, it.title, it.note,
                it.dateTimeMillis, it.plannedRuleId, it.isAdjustment,
            )
        },
        plannedRules = data.rules.sortedBy { it.id }.map {
            PlannedRuleDto(
                it.id, it.type.name, it.amountCents, it.accountId, it.categoryId, it.title, it.startDate.toString(),
                it.unit?.name, it.interval, it.endDate?.toString(), it.consumedOccurrences,
            )
        },
        budgets = data.budgets.sortedBy { it.id }.map {
            BudgetDto(it.id, it.name, it.limitCents, it.allExpenses, it.categoryIds.sorted(), it.order)
        },
        categoryGroups = data.groups.sortedBy { it.id }.map {
            CategoryGroupDto(it.id, it.name, it.type.name, it.colorArgb, it.icon, it.order)
        },
    )
}
