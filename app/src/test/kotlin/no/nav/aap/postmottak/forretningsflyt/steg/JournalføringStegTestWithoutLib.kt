package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.postmottak.sakogbehandling.behandling.JournalpostRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.Ident
import no.nav.aap.postmottak.sakogbehandling.behandling.Journalpost
import no.nav.aap.postmottak.sakogbehandling.behandling.JournalpostStatus
import no.nav.aap.postmottak.joark.Joark
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


// midelertidig demonstrasjon av JournalføringStegTest uten mocking bibliotek

class BehandlingRepositoryTestDouble: BehandlingRepository {

    lateinit var behandlingStub: Behandling

    override fun opprettBehandling(journalpostId: JournalpostId): BehandlingId {
        TODO("Not yet implemented")
    }

    override fun hentMedLås(behandlingId: BehandlingId, versjon: Long?): Behandling {
        return behandlingStub
    }

    override fun hentMedLås(journalpostId: JournalpostId, versjon: Long?): Behandling {
        TODO("Not yet implemented")
    }

    override fun hent(journalpostId: JournalpostId, versjon: Long?): Behandling {
        TODO("Not yet implemented")
    }

}

class JournalpostRepositoryTestDouble: JournalpostRepository {

    lateinit var journalpostStub: Journalpost

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost? {
        return journalpostStub
    }

    override fun lagre(journalpost: Journalpost, behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }
}

class JoarkTestDouble() : Joark {

    var oppdaterCounter = 0
    var ferdigstillMaskineltCounter = 0
    var ferdigstillCounter = 0

    var oppdaterCalls: List<Pair<Any, Any>> = mutableListOf()
    var ferdigstillMaskineltCalls: List<Any> = mutableListOf()
    var ferdigstillCalls: List<Any> = mutableListOf()

    override fun førJournalpostPåFagsak(journalpost: Journalpost.MedIdent, fagsakId: String) {
        oppdaterCounter++
        oppdaterCalls += Pair(journalpost, fagsakId)
    }

    override fun førJournalpostPåGenerellSak(journalpost: Journalpost.MedIdent) {
        TODO("Not yet implemented")
    }

    override fun ferdigstillJournalpostMaskinelt(journalpost: Journalpost) {
        ferdigstillMaskineltCounter++
        ferdigstillMaskineltCalls += journalpost
    }

    override fun ferdigstillJournalpost(journalpost: Journalpost, journalfoerendeEnhet: String) {
        ferdigstillCounter++
        ferdigstillCalls += journalpost
    }
    
}

fun generateJournalpost() = Journalpost.MedIdent(
    personident = Ident.Personident("24234"),
    journalpostId = JournalpostId(1234),
    journalførendeEnhet = null,
    status = JournalpostStatus.JOURNALFØRT,
    mottattDato = LocalDate.now(),
    dokumenter = emptyList(),
)


class JournalføringStegTestWithoutLib {

    val journalpostRepository: JournalpostRepositoryTestDouble = JournalpostRepositoryTestDouble()
    val joark: JoarkTestDouble = JoarkTestDouble()

    val journalføringSteg = JournalføringSteg(
        journalpostRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost = generateJournalpost()

        journalpostRepository.journalpostStub = journalpost

        journalføringSteg.utfør(FlytKontekstMedPerioder(BehandlingId(1), TypeBehandling.DokumentHåndtering))

        assertThat(joark.ferdigstillMaskineltCounter).isOne()
        assertThat(joark.ferdigstillMaskineltCalls.first()).isEqualTo(journalpost)

    }
}