package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.SafTilknyttetDokument
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.gateway.Sak
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class TilknyttetJournalpostTest {
    private val kilde = JournalpostId(10)
    private val treff = SafTilknyttetJournalpost(
        "20", Sak("S1", Fagsystem.KELVIN.name, Sakstype.FAGSAK), listOf(SafTilknyttetDokument("1"))
    )

    @Test
    fun `finner den tilknyttede journalposten på riktig Kelvin-sak`() {
        val journalposter = listOf(
            treff.copy(journalpostId = kilde.toString()),
            treff.copy(journalpostId = "30", sak = Sak("S2", Fagsystem.KELVIN.name, Sakstype.FAGSAK)),
            treff,
        )
        assertThat(journalposter.finnPåKelvinSak(kilde, setOf("1"), "S1")).isEqualTo(JournalpostId(20))
    }

    @Test
    fun `stopper ved flere tilknytninger på samme sak`() {
        val journalposter = listOf(treff, treff.copy(journalpostId = "21"))
        assertThatIllegalStateException().isThrownBy { journalposter.finnPåKelvinSak(kilde, setOf("1"), "S1") }
            .withMessageContaining("Flere tilknytninger")
    }
}
