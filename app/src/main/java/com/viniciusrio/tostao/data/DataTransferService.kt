package com.viniciusrio.tostao.data

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import com.viniciusrio.tostao.domain.CsvExport
import com.viniciusrio.tostao.domain.ExportPeriod
import com.viniciusrio.tostao.domain.FinanceData
import com.viniciusrio.tostao.domain.backup.BackupCodec
import com.viniciusrio.tostao.domain.backup.BackupResult
import com.viniciusrio.tostao.domain.backup.BackupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Clock
import java.time.OffsetDateTime

/** Resultado de uma operação de arquivo, já com a mensagem para o usuário. */
sealed interface TransferOutcome {
    data class Ok(val message: String) : TransferOutcome
    data class Failed(val message: String) : TransferOutcome
}

/**
 * Backup, restauração e exportação via Storage Access Framework. Todo I/O roda em
 * [Dispatchers.IO]; as preferências de backup só mudam depois que a operação termina.
 */
class DataTransferService(
    private val resolver: ContentResolver,
    private val repo: FinanceRepository,
    private val clock: Clock,
    private val prefs: SharedPreferences,
) {
    fun backupState() = BackupState(
        lastBackupAtMillis = prefs.getLong(KEY_LAST_AT, -1).takeIf { it >= 0 },
        lastFingerprint = prefs.getString(KEY_LAST_FP, null),
        snoozeUntilMillis = prefs.getLong(KEY_SNOOZE, -1).takeIf { it >= 0 },
    )

    fun snooze(untilMillis: Long) = prefs.edit().putLong(KEY_SNOOZE, untilMillis).apply()

    suspend fun writeBackup(uri: Uri): TransferOutcome = withContext(Dispatchers.IO) {
        val data = repo.snapshot()
        val bytes = BackupCodec.encode(data, OffsetDateTime.now(clock)).toByteArray(Charsets.UTF_8)
        if (bytes.size > BackupCodec.MAX_BYTES) {
            discard(uri)
            return@withContext TransferOutcome.Failed("Seus dados passam de 50 MB e não cabem num backup.")
        }
        if (!write(uri, bytes)) {
            return@withContext TransferOutcome.Failed(
                "Não foi possível salvar o backup. Um arquivo incompleto pode ter ficado no local escolhido.",
            )
        }
        prefs.edit()
            .putLong(KEY_LAST_AT, clock.millis())
            .putString(KEY_LAST_FP, BackupCodec.fingerprint(data))
            .remove(KEY_SNOOZE)
            .apply()
        TransferOutcome.Ok("Backup salvo")
    }

    /** Lê e valida; não altera nada. */
    suspend fun readBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val bytes = try {
            readLimited(uri)
        } catch (_: TooLarge) {
            return@withContext BackupResult.Invalid("O arquivo tem mais de 50 MB e não é um backup do Tostão.")
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: return@withContext BackupResult.Invalid("Não foi possível ler o arquivo. Nenhum dado foi alterado.")
        BackupCodec.decode(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
    }

    /**
     * Substitui todos os dados pelos do backup. Depois, o backup restaurado conta como último
     * backup, e a última conta usada é esquecida se não existir mais.
     */
    suspend fun restore(valid: BackupResult.Valid): TransferOutcome = withContext(Dispatchers.IO) {
        try {
            repo.replaceAll(valid.data)
        } catch (e: Exception) {
            return@withContext TransferOutcome.Failed("A restauração falhou e seus dados não foram alterados.")
        }
        val edit = prefs.edit()
            .putLong(KEY_LAST_AT, valid.createdAt.toInstant().toEpochMilli())
            .putString(KEY_LAST_FP, BackupCodec.fingerprint(valid.data))
            .putBoolean(KEY_SEEDED, true)
            .remove(KEY_SNOOZE)
        val last = prefs.getString(KEY_LAST_ACCOUNT, null)
        if (last != null && valid.data.accounts.none { it.id == last }) edit.remove(KEY_LAST_ACCOUNT)
        edit.apply()
        TransferOutcome.Ok("Backup restaurado")
    }

    suspend fun writeCsv(uri: Uri, data: FinanceData, period: ExportPeriod): TransferOutcome = withContext(Dispatchers.IO) {
        val count = CsvExport.select(data, period, clock.zone).size
        val bytes = CsvExport.build(data, period, clock.zone).toByteArray(Charsets.UTF_8)
        if (!write(uri, bytes)) {
            return@withContext TransferOutcome.Failed("Não foi possível salvar o arquivo CSV.")
        }
        TransferOutcome.Ok(if (count == 1) "1 transação exportada" else "$count transações exportadas")
    }

    /** Sucesso só depois de escrever e fechar; na falha, tenta remover o documento recém-criado. */
    private fun write(uri: Uri, bytes: ByteArray): Boolean = try {
        val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("sem saída")
        out.use { it.write(bytes); it.flush() }
        true
    } catch (_: Exception) {
        discard(uri)
        false
    }

    private fun discard(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: Exception) {
            // Nem todo provedor permite remover; o aviso ao usuário cobre esse caso.
        }
    }

    private class TooLarge : Exception()

    private fun readLimited(uri: Uri): ByteArray? {
        val input = resolver.openInputStream(uri) ?: return null
        return input.use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                if (total > BackupCodec.MAX_BYTES) throw TooLarge()
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    }

    companion object {
        const val KEY_LAST_AT = "backup_last_at"
        const val KEY_LAST_FP = "backup_last_fingerprint"
        const val KEY_SNOOZE = "backup_snooze_until"
        const val KEY_SEEDED = "seeded_defaults"
        const val KEY_LAST_ACCOUNT = "last_account"
    }
}
