package no.nav.aap.fordeler.regler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AldersregelTest {
    @Test
    fun `Journalpost for person som er fylt 60 år skal ikke til Kelvin`(){
        val fødselsdato = LocalDate.of(1960,2, 15)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertFalse(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person yngre enn 60 år skal til Kelvin`(){
        val fødselsdato = LocalDate.of(1960,2, 15)
        val nåDato = LocalDate.of(2020, 2, 14)
        assertTrue(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person yngre enn 22 år skal ikke til Kelvin`(){
        val fødselsdato = LocalDate.of(1998,2, 16)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertFalse(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person som er fylt 22 år skal til Kelvin`(){
        val fødselsdato = LocalDate.of(1998,2, 15)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertTrue(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }
    
}