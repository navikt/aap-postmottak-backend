package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.Regelresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegelResultatTest {

    @Test
    fun `KelvinSakRegel skal overstyre alle andre regler bortsett fra reisestønad`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ErIkkeReisestønadRegel" to true,
                "GeografiskTilknytningRegel" to false,
                "MaksAntallPersonerIKelvinRegel" to false,
            )
        )
        
        assertThat(regelResultat.skalTilKelvin()).isTrue()
    }

    @Test
    fun `Dersom KelvinSakRegel er den eneste som gir false, skal journalposten til Kelvin`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "GeografiskTilknytningRegel" to true,
                "ErIkkeReisestønadRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            )
        )

        assertThat(regelResultat.skalTilKelvin()).isTrue()
    }

    @Test
    fun `Reisestønad skal alltid til Arena`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ErIkkeReisestønadRegel" to false,
                "GeografiskTilknytningRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            )
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }

    @Test
    fun `Dersom én av øvrige regler gir false, skal journalposten ikke til Kelvin`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "GeografiskTilknytningRegel" to false,
                "MaksAntallPersonerIKelvinRegel" to true
            )
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }
}