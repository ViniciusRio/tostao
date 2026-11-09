package com.viniciusrio.tostao.domain

data class AccountBalance(val account: Account, val balanceCents: Long)

data class BalanceSummary(
    val perAccount: List<AccountBalance>,
    val includedTotalCents: Long,
    val excludedTotalCents: Long,
)

object Balances {
    /** Efeito da transação em cada conta. Transações após [nowMillis] não contam. */
    fun compute(data: FinanceData, nowMillis: Long): BalanceSummary {
        val delta = HashMap<String, Long>()
        for (t in data.transactions) {
            if (t.dateTimeMillis > nowMillis) continue
            applyEffect(t) { id, v -> delta[id] = (delta[id] ?: 0L) + v }
        }
        val per = data.accounts.sortedBy { it.order }.map {
            AccountBalance(it, it.initialBalanceCents + (delta[it.id] ?: 0L))
        }
        return BalanceSummary(
            perAccount = per,
            includedTotalCents = per.filter { it.account.includeInTotal }.sumOf { it.balanceCents },
            excludedTotalCents = per.filterNot { it.account.includeInTotal }.sumOf { it.balanceCents },
        )
    }

    inline fun applyEffect(t: Transaction, add: (accountId: String, cents: Long) -> Unit) {
        when (t.type) {
            TxType.INCOME -> add(t.accountId, t.amountCents)
            TxType.EXPENSE -> add(t.accountId, -t.amountCents)
            TxType.TRANSFER -> {
                add(t.accountId, -t.amountCents)
                t.toAccountId?.let { add(it, t.amountCents) }
            }
        }
    }

    /** Transação de ajuste necessária para levar [currentCents] a [targetCents]; null se igual. */
    fun adjustment(currentCents: Long, targetCents: Long): Pair<TxType, Long>? {
        val diff = targetCents - currentCents
        return when {
            diff > 0 -> TxType.INCOME to diff
            diff < 0 -> TxType.EXPENSE to -diff
            else -> null
        }
    }
}
