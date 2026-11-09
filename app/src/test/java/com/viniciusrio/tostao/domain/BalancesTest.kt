package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BalancesTest {
    private val now = millis(2026, 11, 9)

    @Test fun transferMovesMoneyBetweenAccounts() {
        val data = FinanceData(
            accounts = listOf(account("A", 10000), account("B", 0, order = 1)),
            transactions = listOf(
                tx(TxType.EXPENSE, 3010, "A", millis(2026, 11, 1)),
                tx(TxType.TRANSFER, 2000, "A", millis(2026, 11, 2), to = "B"),
            ),
        )
        val s = Balances.compute(data, now)
        assertEquals(4990L, s.perAccount[0].balanceCents)
        assertEquals(2000L, s.perAccount[1].balanceCents)
        assertEquals(6990L, s.includedTotalCents)
    }

    @Test fun futureTransactionDoesNotCount() {
        val data = FinanceData(
            accounts = listOf(account("A", 1000)),
            transactions = listOf(tx(TxType.EXPENSE, 500, "A", millis(2026, 11, 20))),
        )
        assertEquals(1000L, Balances.compute(data, now).includedTotalCents)
    }

    @Test fun excludedAccountsAreSeparated() {
        val data = FinanceData(accounts = listOf(account("A", 1000), account("Inv", 5000, include = false)))
        val s = Balances.compute(data, now)
        assertEquals(1000L, s.includedTotalCents)
        assertEquals(5000L, s.excludedTotalCents)
    }

    @Test fun adjustmentDirection() {
        assertEquals(TxType.EXPENSE to 2000L, Balances.adjustment(50000, 48000))
        assertEquals(TxType.INCOME to 100L, Balances.adjustment(0, 100))
        assertNull(Balances.adjustment(5, 5))
    }
}
