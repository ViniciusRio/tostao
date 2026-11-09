package com.viniciusrio.tostao.domain.backup

import com.viniciusrio.tostao.domain.backup.BackupReminder.DAY_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReminderTest {
    private val now = 1_000 * DAY_MS
    private val never = BackupState(null, null, null)

    private fun show(state: BackupState, fp: String = "novo", oldest: Long? = now - 60 * DAY_MS, at: Long = now) =
        BackupReminder.shouldShow(state, fp, oldest, at)

    @Test fun oldBackupWithChanges() = assertTrue(show(BackupState(now - 35 * DAY_MS, "velho", null)))

    @Test fun oldBackupWithoutChanges() = assertFalse(show(BackupState(now - 40 * DAY_MS, "novo", null)))

    @Test fun recentBackup() = assertFalse(show(BackupState(now - 29 * DAY_MS, "velho", null)))

    @Test fun neverBackedUpAfterSevenDays() = assertTrue(show(never, oldest = now - 7 * DAY_MS))

    @Test fun neverBackedUpBeforeSevenDays() = assertFalse(show(never, oldest = now - 6 * DAY_MS))

    @Test fun noTransactions() = assertFalse(show(never, oldest = null))

    @Test fun snoozeHidesUntilItEnds() {
        val snoozed = BackupState(now - 35 * DAY_MS, "velho", now + DAY_MS)
        assertFalse(show(snoozed))
        assertTrue(show(snoozed, at = now + DAY_MS))
    }

    @Test fun deadlinePassesWithoutDataChange() {
        val state = BackupState(now - 29 * DAY_MS, "velho", null)
        assertFalse(show(state, at = now))
        assertTrue(show(state, at = now + DAY_MS))
    }
}
