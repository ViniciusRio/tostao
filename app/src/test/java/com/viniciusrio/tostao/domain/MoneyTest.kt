package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesBrazilianInput() {
        assertEquals(4590L, Money.parse("45,90"))
        assertEquals(4590L, Money.parse("45,9"))
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(4590L, Money.parse("45.90"))
        assertEquals(123400L, Money.parse("1.234"))
        assertEquals(500L, Money.parse("R$ 5"))
        assertEquals(50L, Money.parse(",5"))
        assertEquals(Money.MAX_CENTS, Money.parse("999.999.999,99"))
    }

    @Test fun rejectsInvalidInput() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1,234"))
        assertNull(Money.parse("1,2,3"))
        assertNull(Money.parse("12.34.5"))
        assertNull(Money.parse("1.000.000.000,00"))
        assertNull(Money.parse("-5"))
    }

    @Test fun formatsWithSymbolBeforeAndSign() {
        assertEquals("R$ 1.234,56", Money.format(123456))
        assertEquals("-R$ 1.234,50", Money.format(-123450))
        assertEquals("+R$ 10,00", Money.format(1000, signed = true))
        assertEquals("R$ 0,05", Money.format(5))
        assertEquals("R$ 0,00", Money.format(0, signed = true))
    }

    @Test fun centsSumIsExact() {
        val total = List(10) { Money.parse("0,10")!! }.sum()
        assertEquals(100L, total)
    }
}
