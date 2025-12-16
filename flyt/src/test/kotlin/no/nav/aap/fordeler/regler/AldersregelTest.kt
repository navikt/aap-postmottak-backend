package no.nav.aap.fordeler.regler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AldersregelTest {
    @Test
    fun `Ingen øvre grense for alder til Kelvin`(){
        val fødselsdato = LocalDate.of(1930,2, 15)
        val nåDato = LocalDate.of(2020, 2, 14)
        assertTrue(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
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