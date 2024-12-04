package no.nav.aap.postmottak.fordeler.regler

import org.junit.jupiter.api.Assertions.*
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
        assert(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person yngre enn 18 år skal ikke til Kelvin`(){
        val fødselsdato = LocalDate.of(2002,2, 16)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertFalse(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person som er fylt 18 år skal til Kelvin`(){
        val fødselsdato = LocalDate.of(2002,2, 15)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertTrue(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }
    
}