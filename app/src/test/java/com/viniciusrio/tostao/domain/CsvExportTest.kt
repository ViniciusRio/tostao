package com.viniciusrio.tostao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CsvExportTest {
    private val nubank = account("a").copy(name = "Nubank")
    private val carteira = account("b").copy(name = "Carteira")
    private val mercado = category("m").copy(name = "Mercado")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZONE).toInstant().toEpochMilli()

    private fun data(vararg t: Transaction) = FinanceData(listOf(nubank, carteira), listOf(mercado), t.toList())

    private fun lines(csv: String) = csv.removePrefix(CsvExport.BOM).split("\r\n").dropLast(1)

    @Test fun headerBomAndCrlf() {
        val csv = CsvExport.build(data(), ExportPeriod.All, ZONE)
        assertTrue(csv.startsWith(CsvExport.BOM))
        assertTrue(csv.endsWith("\r\n"))
        assertEquals(listOf("Data;Hora;Tipo;Valor;Conta;Conta de destino;Categoria;Título;Observação;Ajuste"), lines(csv))
    }

    @Test fun expenseLineFromSpec() {
        val t = tx(TxType.EXPENSE, 123_450, "a", at(2026, 11, 5, 14, 30), category = "m", title = "Feira")
        assertEquals("05/11/2026;14:30;Despesa;-1234,50;Nubank;;Mercado;Feira;;Não", lines(CsvExport.build(data(t), ExportPeriod.All, ZONE))[1])
    }

    @Test fun signsTransferAndAdjustment() {
        val income = tx(TxType.INCOME, 5, "a", at(2026, 11, 1, 8, 0))
        val transfer = tx(TxType.TRANSFER, 10_000, "a", at(2026, 11, 2, 9, 5), to = "b")
        val adj = tx(TxType.EXPENSE, 2_000, "b", at(2026, 11, 3, 10, 0), adjustment = true, title = "Ajuste de saldo")
        val l = lines(CsvExport.build(data(adj, transfer, income), ExportPeriod.All, ZONE))
        assertEquals("01/11/2026;08:00;Receita;0,05;Nubank;;;;;Não", l[1])
        assertEquals("02/11/2026;09:05;Transferência;100,00;Nubank;Carteira;;;;Não", l[2])
        assertEquals("03/11/2026;10:00;Despesa;-20,00;Carteira;;;Ajuste de saldo;;Sim", l[3])
    }

    @Test fun escapesQuotesSemicolonsAndNewlines() {
        val t = tx(TxType.EXPENSE, 100, "a", at(2026, 11, 5, 12, 0)).copy(note = "Pago em 2x; \"à vista\" não\nsegunda linha")
        val csv = CsvExport.build(data(t), ExportPeriod.All, ZONE)
        assertTrue(csv.contains(";\"Pago em 2x; \"\"à vista\"\" não\nsegunda linha\";Não\r\n"))
    }

    @Test fun neutralizesFormulas() {
        assertEquals("'=SOMA(A1:A9)", CsvExport.text("=SOMA(A1:A9)"))
        assertEquals("'  +55 11 9999", CsvExport.text("  +55 11 9999"))
        assertEquals("'-10", CsvExport.text("-10"))
        assertEquals("'@cmd", CsvExport.text("@cmd"))
        assertEquals("\"'\tx\"".replace("\"", ""), CsvExport.text("\tx"))
        assertEquals("\"'\n=1\"", CsvExport.text("\n=1"))
        assertEquals("Café", CsvExport.text("Café"))
        assertEquals("a=b", CsvExport.text("a=b"))
    }

    @Test fun chronologicalOrderWithStableTie() {
        val t1 = tx(TxType.EXPENSE, 1, "a", at(2026, 11, 2, 9, 0), title = "segundo")
        val t2 = tx(TxType.EXPENSE, 1, "a", at(2026, 11, 1, 9, 0), title = "primeiro")
        val l = lines(CsvExport.build(data(t1, t2), ExportPeriod.All, ZONE))
        assertTrue(l[1].contains("primeiro") && l[2].contains("segundo"))
    }

    @Test fun periodsUseLocalDayBorders() {
        val lastSecondOct = LocalDateTime.of(2026, 10, 31, 23, 59, 59).atZone(ZONE).toInstant().toEpochMilli()
        val firstNov = at(2026, 11, 1, 0, 0)
        val lastNov = LocalDateTime.of(2026, 11, 30, 23, 59, 59).atZone(ZONE).toInstant().toEpochMilli()
        val firstDec = at(2026, 12, 1, 0, 0)
        val d = data(
            tx(TxType.EXPENSE, 1, "a", lastSecondOct), tx(TxType.EXPENSE, 1, "a", firstNov),
            tx(TxType.EXPENSE, 1, "a", lastNov), tx(TxType.EXPENSE, 1, "a", firstDec),
            tx(TxType.EXPENSE, 1, "a", at(2025, 12, 31, 12, 0)),
        )
        assertEquals(2, CsvExport.select(d, ExportPeriod.Month(YearMonth.of(2026, 11)), ZONE).size)
        assertEquals(4, CsvExport.select(d, ExportPeriod.Year(2026), ZONE).size)
        assertEquals(5, CsvExport.select(d, ExportPeriod.All, ZONE).size)
        assertEquals(1, CsvExport.select(d, ExportPeriod.Range(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 30)), ZONE).size)
        assertEquals(2, CsvExport.select(d, ExportPeriod.Range(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)), ZONE).size)
    }

    @Test fun rangeValidity() {
        assertTrue(ExportPeriod.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)).isValid)
        assertTrue(!ExportPeriod.Range(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)).isValid)
    }
}
