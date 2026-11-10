package com.viniciusrio.tostao.domain.backup

/** Estado guardado sobre backups (preferências do aparelho). */
data class BackupState(
    val lastBackupAtMillis: Long?,
    val lastFingerprint: String?,
    val snoozeUntilMillis: Long?,
)

object BackupReminder {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val STALE_DAYS = 30
    const val FIRST_DAYS = 7
    const val SNOOZE_DAYS = 7

    /**
     * Mostra o lembrete quando há transações, os dados mudaram desde o último backup e o prazo
     * venceu: 30 dias desde o último backup, ou, sem backup algum, 7 dias desde a transação
     * efetivada mais antiga. "Agora não" adia por 7 dias.
     */
    fun shouldShow(
        state: BackupState,
        currentFingerprint: String,
        oldestEffectiveTxMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (oldestEffectiveTxMillis == null) return false
        if (state.snoozeUntilMillis != null && nowMillis < state.snoozeUntilMillis) return false
        if (state.lastFingerprint == currentFingerprint) return false
        val last = state.lastBackupAtMillis
        return if (last == null) {
            nowMillis - oldestEffectiveTxMillis >= FIRST_DAYS * DAY_MS
        } else {
            nowMillis - last >= STALE_DAYS * DAY_MS
        }
    }
}
