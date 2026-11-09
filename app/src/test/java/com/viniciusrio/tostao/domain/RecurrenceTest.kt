package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private fun dates(r: PlannedRule, n: Int) = (0 until n).map { Recurrence.dueAt(r, it) }

    @Test fun monthlyKeepsAnchorDay() {
        val r = rule(LocalDate.of(2027, 1, 31), RecurrenceUnit.MONTH)
        assertEquals(
            listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30)),
            dates(r, 4),
        )
    }

    @Test fun yearlyOnLeapDay() {
        val r = rule(LocalDate.of(2028, 2, 29), RecurrenceUnit.YEAR)
        assertEquals(
            listOf(
                LocalDate.of(2028, 2, 29), LocalDate.of(2029, 2, 28), LocalDate.of(2030, 2, 28),
                LocalDate.of(2031, 2, 28), LocalDate.of(2032, 2, 29),
            ),
            dates(r, 5),
        )
    }

    @Test fun everyTwoMonthsUntilEndDate() {
        val r = rule(LocalDate.of(2027, 1, 10), RecurrenceUnit.MONTH, interval = 2, end = LocalDate.of(2027, 5, 15))
        assertEquals(listOf(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 5, 10), null), dates(r, 4))
    }

    @Test fun oneTimeHasSingleOccurrence() {
        val r = rule(LocalDate.of(2027, 1, 10), null)
        assertEquals(LocalDate.of(2027, 1, 10), Recurrence.nextDue(r))
        assertNull(Recurrence.nextDue(r.copy(consumedOccurrences = 1)))
    }

    @Test fun weeklyPendingUntil() {
        val r = rule(LocalDate.of(2026, 11, 3), RecurrenceUnit.WEEK)
        assertEquals(4, Recurrence.pendingUntil(r, LocalDate.of(2026, 11, 30)).size)
    }
}
