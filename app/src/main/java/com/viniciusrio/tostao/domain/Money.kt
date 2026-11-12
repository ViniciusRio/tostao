package com.viniciusrio.tostao.domain

import kotlin.math.abs

object Money {
    const val MAX_CENTS: Long = 99_999_999_999L

    /** Converte texto digitado em pt-BR ("1.234,56", "45,9", "45.90") para centavos. */
    fun parse(input: String): Long? {
        val raw = input.replace("R$", "").replace(" ", "").replace(" ", "").trim()
        if (raw.isEmpty() || raw.any { !it.isDigit() && it != ',' && it != '.' }) return null
        val (intPart, decPart) = when {
            raw.contains(',') -> {
                if (raw.count { it == ',' } > 1) return null
                val i = raw.substringBefore(',')
                if (i.contains('.') && !thousandsOk(i)) return null
                i.replace(".", "") to raw.substringAfter(',')
            }
            raw.contains('.') -> {
                val last = raw.substringAfterLast('.')
                if (raw.count { it == '.' } == 1 && last.length in 1..2) {
                    raw.substringBefore('.') to last
                } else {
                    if (!thousandsOk(raw)) return null
                    raw.replace(".", "") to ""
                }
            }
            else -> raw to ""
        }
        if (decPart.length > 2 || decPart.any { !it.isDigit() }) return null
        if (intPart.isEmpty() && decPart.isEmpty()) return null
        if (intPart.length > 12) return null
        val cents = (intPart.ifEmpty { "0" }.toLong() * 100) + decPart.padEnd(2, '0').ifEmpty { "00" }.toLong()
        return if (cents > MAX_CENTS) null else cents
    }

    const val MAX_SUM_PARTS = 50

    private fun sumParts(input: String): List<String> =
        input.replace(" ", "").replace("\u00A0", "").split('+')

    /**
     * Soma de parcelas separadas por "+" ("12,50+8,30+15"), cada uma no formato de [parse].
     * Inválido com parcela vazia, mais de [MAX_SUM_PARTS] parcelas, total zero ou acima de [MAX_CENTS].
     */
    fun parseSum(input: String): Long? {
        val parts = sumParts(input)
        if (parts.size > MAX_SUM_PARTS) return null
        var total = 0L
        for (part in parts) {
            if (part.isEmpty()) return null
            total += parse(part) ?: return null
            if (total > MAX_CENTS) return null
        }
        return if (total > 0) total else null
    }

    /** Se o texto tem mais de uma parcela ("12+3"). */
    fun isSum(input: String): Boolean = input.contains('+')

    private fun thousandsOk(s: String): Boolean {
        val groups = s.split('.')
        return groups.first().length in 1..3 && groups.drop(1).all { it.length == 3 }
    }

    /** "R$ 1.234,56"; com [signed], "+R$ 10,00" e "-R$ 10,00". Negativos sempre com "-". */
    fun format(cents: Long, signed: Boolean = false): String {
        val sign = when {
            cents < 0 -> "-"
            signed && cents > 0 -> "+"
            else -> ""
        }
        return "${sign}R$ ${plain(abs(cents))}"
    }

    /** Valor sem símbolo: "1.234,56". Útil para campos de edição. */
    fun plain(cents: Long): String {
        val a = abs(cents)
        val reais = (a / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        val dec = (a % 100).toString().padStart(2, '0')
        return (if (cents < 0) "-" else "") + "$reais,$dec"
    }

    const val HIDDEN = "R$ •••••"
}
