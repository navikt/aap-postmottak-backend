package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.Regelresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegelResultatTest {

    @BeforeEach
    fun setUp() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `ManueltOverstyrtTilArenaRegel skal overstyre alle andre regler`() {
        val regelResultat = Regelresultat(
            mapOf(
                "ManueltOverstyrtTilArenaRegel" to true,
                "KelvinSakRegel" to true,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "GeografiskTilknytningRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }

    @Test
    fun `KelvinSakRegel skal overstyre alle andre regler bortsett fra reisestønad og anke`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ArenaSakRegel" to false,
                "ManueltOverstyrtTilArenaRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "GeografiskTilknytningRegel" to false,
                "MaksAntallPersonerIKelvinRegel" to false,
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isTrue()
    }

    @Test
    fun `Dersom KelvinSakRegel er den eneste som gir false, skal journalposten til Kelvin`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "ArenaSakRegel" to false,
                "ManueltOverstyrtTilArenaRegel" to false,
                "GeografiskTilknytningRegel" to true,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isTrue()
    }

    @Test
    fun `Reisestønad skal alltid til Arena`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to false,
                "ManueltOverstyrtTilArenaRegel" to false,
                "ErIkkeAnkeRegel" to true,
                "GeografiskTilknytningRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }

    @Test
    fun `Anke skal via arena-gosys-flyt`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ArenaSakRegel" to false,
                "ManueltOverstyrtTilArenaRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to false,
                "GeografiskTilknytningRegel" to true,
                "MaksAntallPersonerIKelvinRegel" to true,
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }

    @Test
    fun `Dersom én av øvrige regler gir false, skal journalposten ikke til Kelvin`() {
        val regelResultat = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ManueltOverstyrtTilArenaRegel" to false,
                "ErIkkeAnkeRegel" to true,
                "GeografiskTilknytningRegel" to false,
                "MaksAntallPersonerIKelvinRegel" to true
            ),
            forJournalpost = 123L
        )

        assertThat(regelResultat.skalTilKelvin()).isFalse()
    }

    @Test
    fun `Kun søknad går til Kelvin, med mindre det allerede finnes en sak i Kelvin`() {
        val annetDokumentUtenKelvinSak = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "ManueltOverstyrtTilArenaRegel" to false,
                "SøknadRegel" to false
            ),
            forJournalpost = 123L
        )

        assertThat(annetDokumentUtenKelvinSak.skalTilKelvin()).isFalse()

        val søknadUtenKelvinSak = Regelresultat(
            mapOf(
                "KelvinSakRegel" to false,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "ManueltOverstyrtTilArenaRegel" to false,
                "SøknadRegel" to true,
            ),
            forJournalpost = 123L
        )
        assertThat(søknadUtenKelvinSak.skalTilKelvin()).isTrue()

        val annetDokumentMedKelvinSak = Regelresultat(
            mapOf(
                "KelvinSakRegel" to true,
                "ArenaSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
                "ManueltOverstyrtTilArenaRegel" to false,
                "SøknadRegel" to false,
            ),
            forJournalpost = 123L
        )
        assertThat(annetDokumentMedKelvinSak.skalTilKelvin()).isTrue()

    }
}