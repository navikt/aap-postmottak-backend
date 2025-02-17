package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class MaksAntallPersonerIKelvinRegelTest {
    @Test
    fun `Dersom maks antall personer allerede i Kelvin skal regel gi false`() {
        val regel = MaksAntallPersonerIKelvinRegel(2)
        val personer = genererTestPersoner(listOf(Ident("ident1"), Ident("ident2"), Ident("ident3")))
        val personerIKelvin = personer.subList(0, 2)

        val actual = regel.vurder(MaksAntallPersonerIKelvinRegelInput(personerIKelvin))

        assertFalse(actual)
    }

    @Test
    fun `Dersom maks antall personer ikke er i Kelvin skal regel gi true`() {
        val regel = MaksAntallPersonerIKelvinRegel(3)
        val personer = genererTestPersoner(listOf(Ident("ident1"), Ident("ident2"), Ident("ident3")))
        val personerIKelvin = personer.subList(0, 2)

        val actual = regel.vurder(MaksAntallPersonerIKelvinRegelInput(personerIKelvin))

        assertTrue(actual)
    }

    private fun genererTestPersoner(identer: List<Ident>) = identer.mapIndexed { index, ident ->
        Person(index.toLong(), UUID.randomUUID(), listOf(ident))
    }
}