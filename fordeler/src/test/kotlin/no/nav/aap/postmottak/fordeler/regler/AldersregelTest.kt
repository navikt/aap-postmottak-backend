package no.nav.aap.postmottak.fordeler.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AldersregelTest {
    @Test
    fun `Søknad for person som er fylt 65 år skal ikke til Kelvin`(){
        val fødselsdato = LocalDate.of(1955,2, 15);
        val nåDato = LocalDate.of(2020, 2, 15);
        assertFalse(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Søknad for person yngre enn 65 år skal ikke til Kelvin`(){
        val fødselsdato = LocalDate.of(1955,2, 15);
        val nåDato = LocalDate.of(2020, 2, 14);
        assertTrue(Aldersregel().vurder(AldersregelInput(fødselsdato, nåDato)))
    }
    
}